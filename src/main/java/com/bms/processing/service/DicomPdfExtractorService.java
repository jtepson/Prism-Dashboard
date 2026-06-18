package com.bms.processing.service;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DicomPdfExtractorService {

    public Path extractPdf(
            Path dicomFile,
            Path targetPdf
    ) throws IOException {

        try (DicomInputStream dicomInputStream =
                     new DicomInputStream(dicomFile.toFile())) {

            Attributes attributes =
                    dicomInputStream.readDataset(-1, -1);

            byte[] pdfBytes =
                    attributes.getBytes(Tag.EncapsulatedDocument);

            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new IOException("No encapsulated PDF found in DICOM object.");
            }

            Files.createDirectories(targetPdf.getParent());
            Files.write(targetPdf, pdfBytes);

            return targetPdf;
        }
    }
}