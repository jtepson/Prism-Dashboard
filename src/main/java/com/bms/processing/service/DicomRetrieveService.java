package com.bms.processing.service;

import com.bms.processing.entity.DicomConfigEntity;
import com.bms.processing.model.DicomReportResult;
import com.bms.processing.model.DicomRetrieveResult;
import org.springframework.stereotype.Service;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DimseRSP;
import org.dcm4che3.net.Priority;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
        
        if (!listenerStarted) {

                result.setSuccess(false);
                result.setMessage("Failed to start DICOM listener.");

                return result;
        }

        // once listener is up it now tells archive to send the pdf - 6162026
        boolean moveStarted =
                        moveReport(config, report);

        result.setSuccess(moveStarted);
        result.setSopInstanceUid(report.getSopInstanceUid());

        result.setMessage(
                moveStarted
                        ? "Retrieve request sent."
                        : "Retrieve request failed."
        );

        return result;
    }

    private boolean moveReport(
                DicomConfigEntity config,
                DicomReportResult report
        ) {
        Association association = null;

        ExecutorService executorService =
                Executors.newSingleThreadExecutor();

        ScheduledExecutorService scheduledExecutorService =
                Executors.newSingleThreadScheduledExecutor();

        try {
                Device device = new Device("prism-dashboard-move-scu");

                device.setExecutor(executorService);
                device.setScheduledExecutor(scheduledExecutorService);

                ApplicationEntity localAe =
                        new ApplicationEntity(config.getLocalAeTitle());

                Connection localConnection = new Connection();
                Connection remoteConnection = new Connection();

                remoteConnection.setHostname(config.getRemoteHost());
                remoteConnection.setPort(config.getRemotePort());

                device.addConnection(localConnection);
                device.addApplicationEntity(localAe);
                localAe.addConnection(localConnection);

                AAssociateRQ request = new AAssociateRQ();
                request.setCallingAET(config.getLocalAeTitle());
                request.setCalledAET(config.getRemoteAeTitle());

                request.addPresentationContext(
                        new PresentationContext(
                                1,
                                UID.StudyRootQueryRetrieveInformationModelMove,
                                UID.ImplicitVRLittleEndian
                        )
                );

                association = localAe.connect(
                        localConnection,
                        remoteConnection,
                        request
                );

                Attributes keys = new Attributes();

                keys.setString(Tag.QueryRetrieveLevel, VR.CS, "IMAGE");
                keys.setString(Tag.StudyInstanceUID, VR.UI, report.getStudyInstanceUid());
                keys.setString(Tag.SeriesInstanceUID, VR.UI, report.getSeriesInstanceUid());
                keys.setString(Tag.SOPInstanceUID, VR.UI, report.getSopInstanceUid());

                // archive will send the pdf back to the configured AE for this - updated 6162026
                // this cmove only supports cuid, priority, keys, transfersyntax, and desinationae, tried to build
                // it around blocking response by accident like in cfind using 0 but that failed along with 1.
                // Those were messageID and responseMode for the other method whoops.
                DimseRSP response = association.cmove(
                        UID.StudyRootQueryRetrieveInformationModelMove,
                        Priority.NORMAL,
                        keys,
                        UID.ImplicitVRLittleEndian,
                        config.getRetrieveAeTitle()
                );

                while (response.next()) {
                        Attributes command = response.getCommand();
                        int status = command.getInt(Tag.Status, -1);

                        if (status == 0) {
                                return true;
                        }
                }

                return false;

        } catch (Exception ex) {
                ex.printStackTrace();
                return false;

        } finally {
                if (association != null && association.isReadyForDataTransfer()) {
                        try {
                                association.release();
                        } catch (Exception ignored) {
                        }
                }

                executorService.shutdown();
                scheduledExecutorService.shutdown();
        }
    }
}