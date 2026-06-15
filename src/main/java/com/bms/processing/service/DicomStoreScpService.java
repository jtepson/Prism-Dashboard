package com.bms.processing.service;

import org.springframework.stereotype.Service;

@Service
public class DicomStoreScpService {

    private volatile boolean running = false;
    private volatile int listeningPort = -1;
    private volatile String listeningAeTitle;

    public boolean startListener(
            String aeTitle,
            int port,
            String storagePath
    ) {
        // stub listener for now so that workflow is shown before wiring dcm storescp into other stuff - 6152026
        running = true;
        listeningPort = port;
        listeningAeTitle = aeTitle;

        return true;
    }

    public boolean isRunning() {
        return running;
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public String getListeningAeTitle() {
        return listeningAeTitle;
    }
}