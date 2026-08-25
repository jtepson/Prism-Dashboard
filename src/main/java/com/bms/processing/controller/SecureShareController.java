package com.bms.processing.controller;

import com.bms.processing.entity.CaseSecureShareEntity;
import com.bms.processing.service.SecureShareAccessException;
import com.bms.processing.service.SecureShareService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bms.processing.entity.PatientFileEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.file.Path;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/share")
public class SecureShareController {

    private final SecureShareService secureShareService;

    public SecureShareController(
            SecureShareService secureShareService
    ) {
        this.secureShareService = secureShareService;
    }

    // validate secure share link
    @GetMapping("/{token}")
    public ResponseEntity<String> openShare(
            @PathVariable String token
    ) {
        try {
            CaseSecureShareEntity share =
                    secureShareService.validateToken(token);

            return ResponseEntity.ok(
                    "Secure share available for "
                            + share.getRecipientEmail()
            );

        } catch (SecureShareAccessException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(ex.getMessage());
        }
    }

    // send secure share verification code
    @PostMapping("/{token}/code")
    public ResponseEntity<String> sendCode(
            @PathVariable String token
    ) {
        try {
            secureShareService.sendAccessCode(token);

            return ResponseEntity.ok(
                    "Verification code sent."
            );

        } catch (SecureShareAccessException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(ex.getMessage());
        }
    }

    // verify secure share verification code - updated 08252026
    @PostMapping("/{token}/verify")
    public ResponseEntity<String> verifyCode(
            @PathVariable String token,
            @RequestParam String code,
            HttpSession session
    ) {
        try {
            CaseSecureShareEntity share =
                    secureShareService.verifyAccessCode(
                            token,
                            code
                    );

            session.setAttribute(
                    sessionKey(share.getId()),
                    LocalDateTime.now().plusMinutes(15)
            );

            return ResponseEntity.ok(
                    "Verification successful."
            );

        } catch (SecureShareAccessException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(ex.getMessage());
        }
    }

    //secure view endpoint - updated 08252026
    @GetMapping("/{token}/files/{fileId}/view")
    public ResponseEntity<Resource> viewFile(
            @PathVariable String token,
            @PathVariable Long fileId,
            HttpSession session
    ) {
        CaseSecureShareEntity share =
                secureShareService.validateToken(token);

        if (!isSessionVerified(share.getId(), session)) {
            throw new SecureShareAccessException(
                    "Verification is required."
            );
        }

        if (!Boolean.TRUE.equals(share.getAllowView())) {
            throw new SecureShareAccessException(
                    "Viewing is not permitted for this share."
            );
        }

        PatientFileEntity file =
                secureShareService.getAuthorizedFile(
                        share,
                        fileId
                );

        Resource resource =
                new FileSystemResource(
                        Path.of(file.getStoragePath())
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(file.getOriginalFileName())
                                .build()
                                .toString()
                )
                .header(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store, no-cache, must-revalidate"
                )
                .contentType(resolveMediaType(file))
                .body(resource);
    }

    //secure download endpoint - updated 08252026
    @GetMapping("/{token}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String token,
            @PathVariable Long fileId,
            HttpSession session
    ) {
        CaseSecureShareEntity share =
                secureShareService.validateToken(token);

        if (!isSessionVerified(share.getId(), session)) {
            throw new SecureShareAccessException(
                    "Verification is required."
            );
        }

        if (!Boolean.TRUE.equals(share.getAllowDownload())) {
            throw new SecureShareAccessException(
                    "Downloading is not permitted for this share."
            );
        }

        PatientFileEntity file =
                secureShareService.getAuthorizedFile(
                        share,
                        fileId
                );

        Resource resource =
                new FileSystemResource(
                        Path.of(file.getStoragePath())
                );

        secureShareService.recordDownload(share);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.getOriginalFileName())
                                .build()
                                .toString()
                )
                .header(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store, no-cache, must-revalidate"
                )
                .contentType(resolveMediaType(file))
                .body(resource);
    }

    private String sessionKey(Long shareId) {
        return "secureShareVerified:" + shareId;
    }

    private boolean isSessionVerified(
            Long shareId,
            HttpSession session
    ) {
        Object value = session.getAttribute(
                sessionKey(shareId)
        );

        if (!(value instanceof LocalDateTime expiresAt)) {
            return false;
        }

        if (!expiresAt.isAfter(LocalDateTime.now())) {
            session.removeAttribute(
                    sessionKey(shareId)
            );

            return false;
        }

        return true;
    }

    private MediaType resolveMediaType(
            PatientFileEntity file
    ) {
        if (file.getContentType() == null
                || file.getContentType().isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(
                    file.getContentType()
            );
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}