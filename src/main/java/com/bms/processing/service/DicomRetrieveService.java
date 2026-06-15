package com.bms.processing.service;

import com.bms.processing.model.DicomReportResult;
import com.bms.processing.model.DicomRetrieveResult;
import org.springframework.stereotype.Service;

@Service
public class DicomRetrieveService {

    public DicomRetrieveResult retrieveReport(
            DicomReportResult report,
            String storagePath
    ) {
        DicomRetrieveResult result = new DicomRetrieveResult();

        // Updated 6/15/2026.
        // Starting simple here before wiring the actual C-MOVE/store listener.
        result.setSuccess(false);
        result.setMessage("Retrieve not implemented yet.");
        result.setFilePath(null);

        return result;
    }
}