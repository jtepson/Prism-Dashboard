package com.bms.processing.service;

import com.bms.processing.entity.DicomConfigEntity;
import com.bms.processing.model.DicomReportResult;
import com.bms.processing.model.DicomRetrieveResult;
import org.springframework.stereotype.Service;

@Service
public class DicomRetrieveService {

    private final DicomStoreScpService dicomStoreScpService;
    private final DicomConfigService dicomConfigService;

    public DicomRetrieveService(
            DicomStoreScpService dicomStoreScpService,
            DicomConfigService dicomConfigService
    ) {
        this.dicomStoreScpService = dicomStoreScpService;
        this.dicomConfigService = dicomConfigService;
    }

    // retrieve flow starts here, storescp gets started before the archive is asked to send anything - updated 6152026
    public DicomRetrieveResult retrieveReport(
            DicomReportResult report,
            String storagePath
    ) {
        DicomRetrieveResult result = new DicomRetrieveResult();

        DicomConfigEntity config =
                dicomConfigService.getActiveConfiguration();

        if (config == null) {
            result.setSuccess(false);
            result.setMessage("No active DICOM configuration found.");
            return result;
        }

        // reworked to use a configured port rather than have it hard coded - updated 6152026
        boolean listenerStarted =
                dicomStoreScpService.startListener(
                        config.getRetrieveAeTitle(),
                        config.getRetrievePort(),
                        storagePath
                );

        result.setSuccess(listenerStarted);
        result.setSopInstanceUid(report.getSopInstanceUid());
        result.setMessage(
                listenerStarted
                        ? "Listener started."
                        : "Listener failed to start."
        );

        return result;
    }
}