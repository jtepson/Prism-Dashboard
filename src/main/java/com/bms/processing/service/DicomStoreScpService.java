package com.bms.processing.service;

import org.springframework.stereotype.Service;

@Service
public class DicomStoreScpService {

    public boolean startListener(
            String aeTitle,
            int port,
            String storagePath
    ) {
        // Updated 6/15/2026.
        // This will be the temp listener BMS_CACHE sends retrieved PDFs to.
        return false;
    }
}