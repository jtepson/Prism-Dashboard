package com.bms.processing.service;

import com.bms.processing.entity.CaseRecordEntity;
import com.bms.processing.entity.CaseSecureShareEntity;
import com.bms.processing.entity.PatientFileEntity;
import com.bms.processing.repository.CaseSecureShareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
public class SecureShareService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CaseSecureShareRepository repository;

    public SecureShareService(CaseSecureShareRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CreatedSecureShare createShare(
            CaseRecordEntity caseRecord,
            String recipientName,
            String recipientEmail,
            Set<PatientFileEntity> files,
            LocalDateTime expiresAt,
            boolean allowView,
            boolean allowDownload,
            Integer maxDownloads,
            String createdBy
    ) {
        if (caseRecord == null || caseRecord.getId() == null) {
            throw new IllegalArgumentException("A valid case is required.");
        }

        if (recipientName == null || recipientName.isBlank()) {
            throw new IllegalArgumentException("Recipient name is required.");
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required.");
        }

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one file must be selected."
            );
        }

        if (expiresAt == null || !expiresAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Expiration must be in the future."
            );
        }

        if (maxDownloads != null && maxDownloads < 1) {
            throw new IllegalArgumentException(
                    "Maximum downloads must be at least 1."
            );
        }

        // allows for only the patient file specified to be shared and never another pt - updated 08252026
        for (PatientFileEntity file : files) {
            if (file.getCaseRecordId() == null
                    || !file.getCaseRecordId().equals(caseRecord.getId())) {

                throw new IllegalArgumentException(
                        "All shared files must belong to the selected case."
                );
            }
        }

        String rawToken = generateToken();

        CaseSecureShareEntity share = new CaseSecureShareEntity();

        share.setCaseRecord(caseRecord);
        share.setRecipientName(recipientName.trim());
        share.setRecipientEmail(recipientEmail.trim().toLowerCase());

        // raw token is never stored
        share.setTokenHash(hashToken(rawToken));

        share.setAllowView(allowView);
        share.setAllowDownload(allowDownload);

        share.setExpiresAt(expiresAt);
        share.setMaxDownloads(maxDownloads);
        share.setDownloadCount(0);

        share.setCreatedBy(createdBy);
        share.setCreatedAt(LocalDateTime.now());

        share.setFiles(files);

        CaseSecureShareEntity saved = repository.save(share);

        // rawToken here is returned exactly once so that the app can build the ext url - 08252026
        return new CreatedSecureShare(saved, rawToken);
    }

    @Transactional(readOnly = true)
    public CaseSecureShareEntity validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new SecureShareAccessException(
                    "Invalid secure share."
            );
        }

        String tokenHash = hashToken(rawToken);

        CaseSecureShareEntity share = repository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new SecureShareAccessException(
                                "Invalid secure share."
                        )
                );

        validateShareAccess(share);

        return share;
    }

    @Transactional
    public void revokeShare(Long shareId) {
        CaseSecureShareEntity share = repository.findById(shareId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Secure share not found."
                        )
                );

        if (share.getRevokedAt() == null) {
            share.setRevokedAt(LocalDateTime.now());
            repository.save(share);
        }
    }

    @Transactional(readOnly = true)
    public List<CaseSecureShareEntity> findByCase(
            CaseRecordEntity caseRecord
    ) {
        if (caseRecord == null || caseRecord.getId() == null) {
            return List.of();
        }

        return repository
                .findByCaseRecordIdOrderByCreatedAtDesc(
                        caseRecord.getId()
                );
    }

    public boolean isActive(CaseSecureShareEntity share) {
        if (share == null) {
            return false;
        }

        if (share.getRevokedAt() != null) {
            return false;
        }

        if (share.getExpiresAt() == null
                || !share.getExpiresAt().isAfter(LocalDateTime.now())) {
            return false;
        }

        return share.getMaxDownloads() == null
                || share.getDownloadCount() < share.getMaxDownloads();
    }

    private void validateShareAccess(CaseSecureShareEntity share) {
        if (share.getRevokedAt() != null) {
            throw new SecureShareAccessException(
                    "This secure share has been revoked."
            );
        }

        if (share.getExpiresAt() == null
                || !share.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new SecureShareAccessException(
                    "This secure share has expired."
            );
        }

        if (share.getMaxDownloads() != null
                && share.getDownloadCount()
                        >= share.getMaxDownloads()) {

            throw new SecureShareAccessException(
                    "This secure share is no longer available."
            );
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable.",
                    e
            );
        }
    }

    public record CreatedSecureShare(
            CaseSecureShareEntity share,
            String rawToken
    ) {
    }
}