package com.bms.processing.service;

import com.bms.processing.entity.DicomConfigEntity;
import com.bms.processing.model.DicomStudyResult;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DimseRSP;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
public class DicomService {

    public boolean testEcho(DicomConfigEntity config) {
        if (config == null) {
            return false;
        }

        Association association = null;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        ScheduledExecutorService scheduledExecutorService =
                Executors.newSingleThreadScheduledExecutor();

        try {
            Device device = new Device("prism-dashboard-device");

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
                            UID.Verification,
                            UID.ImplicitVRLittleEndian
                    )
            );

            association = localAe.connect(
                    localConnection,
                    remoteConnection,
                    request
            );

            DimseRSP response = association.cecho();

            if (response.next()) {
                Attributes command = response.getCommand();
                int status = command.getInt(Tag.Status, -1);
                return status == 0;
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

    public String testQuery(DicomConfigEntity config) {
        if (config == null) {
            return "No configuration selected.";
        }

        return "Query test placeholder successful.";
    }

    public String testRetrieve(DicomConfigEntity config) {
        if (config == null) {
            return "No configuration selected.";
        }

        return "Retrieve test placeholder successful.";
    }

    public List<DicomStudyResult> queryStudies(
            DicomConfigEntity config,
            String patientName,
            String patientId
    ) {

        List<DicomStudyResult> results = new ArrayList<>();

        /*
        * Real C-FIND implementation next.
        * Returning empty list for now.
        */

        return results;
    }
}