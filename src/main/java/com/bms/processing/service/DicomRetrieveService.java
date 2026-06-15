package com.bms.processing.service;

import com.bms.processing.model.DicomReportResult;
import com.bms.processing.model.DicomRetrieveResult;
import org.springframework.stereotype.Service;

@Service
public class DicomRetrieveService {

    private final DicomStoreScpService dicomStoreScpService;

    public DicomRetrieveResult retrieveReport(
            DicomReportResult report,
            String storagePath
    ) {
        DicomRetrieveResult result = new DicomRetrieveResult();

        // retrieve flow starts here, storescp gets started before the archive is asked to send anything - updated 6152026

        boolean listenerStarted =
                dicomStoreScpService.startListener(
                        "PRISM_DASHBOARD",
                        11113,
                        storagePath
                );

        result.setSuccess(listenerStarted);
        result.setMessage(
                listenerStarted
                        ? "Listener started."
                        : "Listener failed to start."
        );

        return result;
    }

    public DicomRetrieveService(
            DicomStoreScpService dicomStoreScpService
    ) {
        this.dicomStoreScpService = dicomStoreScpService;
    }

}