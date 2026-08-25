package com.bms.processing.controller;

import com.bms.processing.entity.CaseSecureShareEntity;
import com.bms.processing.service.SecureShareAccessException;
import com.bms.processing.service.SecureShareService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}