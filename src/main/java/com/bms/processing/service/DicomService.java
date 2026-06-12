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
import org.dcm4che3.data.VR;
import org.dcm4che3.net.Priority;

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
            String patientLastName,
            String patientId
    ) {

        if (config == null) {
            return List.of();
        }

        if (!isBlank(patientId)) {
            List<DicomStudyResult> results =
                    queryStudiesInternal(config, null, patientId.trim());

            if (!results.isEmpty()) {
                return results;
            }
        }

        String patientNameQuery = buildPatientNameQuery(patientLastName);

        if (!isBlank(patientNameQuery)) {
            return queryStudiesInternal(config, patientNameQuery, null);
        }

        return List.of();
    }

    private List<DicomStudyResult> queryStudiesInternal(
            DicomConfigEntity config,
            String patientNameQuery,
            String patientId
    ) {
        List<DicomStudyResult> results = new ArrayList<>();

        Association association = null;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        ScheduledExecutorService scheduledExecutorService =
                Executors.newSingleThreadScheduledExecutor();

        try {
            Device device = new Device("prism-dashboard-query-device");

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
                            UID.StudyRootQueryRetrieveInformationModelFind,
                            UID.ImplicitVRLittleEndian
                    )
            );

            association = localAe.connect(
                    localConnection,
                    remoteConnection,
                    request
            );

            Attributes keys = new Attributes();
            keys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");

            keys.setNull(Tag.PatientName, VR.PN);
            keys.setNull(Tag.PatientID, VR.LO);
            keys.setNull(Tag.StudyInstanceUID, VR.UI);
            keys.setNull(Tag.AccessionNumber, VR.SH);
            keys.setNull(Tag.StudyDate, VR.DA);
            keys.setNull(Tag.StudyDescription, VR.LO);

            if (!isBlank(patientId)) {
                keys.setString(Tag.PatientID, VR.LO, patientId);
            }

            if (!isBlank(patientNameQuery)) {
                keys.setString(Tag.PatientName, VR.PN, patientNameQuery);
            }

            //needed to change from response type to the specific transfer syntax that dcm wants. Added UID.... and changed null to 0 - updated 6122026
            DimseRSP response = association.cfind(
                    UID.StudyRootQueryRetrieveInformationModelFind,
                    Priority.NORMAL,
                    keys,
                    UID.ImplicitVRLittleEndian,
                    0
            );

            while (response.next()) {
                Attributes command = response.getCommand();
                int status = command.getInt(Tag.Status, -1);

                if (status == 0xFF00 || status == 0xFF01) {
                    Attributes data = response.getDataset();

                    if (data != null) {
                        DicomStudyResult result = new DicomStudyResult();
                        result.setPatientName(data.getString(Tag.PatientName));
                        result.setPatientId(data.getString(Tag.PatientID));
                        result.setStudyInstanceUid(data.getString(Tag.StudyInstanceUID));
                        result.setAccessionNumber(data.getString(Tag.AccessionNumber));
                        result.setStudyDate(data.getString(Tag.StudyDate));
                        result.setDescription(data.getString(Tag.StudyDescription));

                        results.add(result);
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();

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

        return results;
    }

    private String buildPatientNameQuery(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return "";
        }

        return lastName.trim().toUpperCase() + "*";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}