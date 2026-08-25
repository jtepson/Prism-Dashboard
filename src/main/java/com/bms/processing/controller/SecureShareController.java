package com.bms.processing.controller;

import com.bms.processing.entity.CaseSecureShareEntity;
import com.bms.processing.service.SecureShareAccessException;
import com.bms.processing.service.SecureShareService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}