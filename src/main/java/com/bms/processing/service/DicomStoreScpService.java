package com.bms.processing.service;

import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DimseRQHandler;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.net.service.DicomServiceException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
public class DicomStoreScpService {

    private volatile boolean running = false;
    private volatile int listeningPort = -1;
    private volatile String listeningAeTitle;

    private Device device;
    private ApplicationEntity applicationEntity;
    private Connection connection;
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;
    private String storagePath;

    public boolean startListener(
            String aeTitle,
            int port,
            String storagePath
    ) {
        try {
            Files.createDirectories(Path.of(storagePath));
            this.storagePath = storagePath;

            // listener state is real now, but C-STORE handling comes next - 6152026
            device = new Device("prism-dashboard-store-scp");
            applicationEntity = new ApplicationEntity(aeTitle);

            DicomServiceRegistry serviceRegistry =
                    new DicomServiceRegistry();

            serviceRegistry.addDicomService(
                    new BasicCEchoSCP()
            );

            serviceRegistry.addDicomService(
                    createStoreScp()
            );

            applicationEntity.setDimseRQHandler(serviceRegistry);

            connection = new Connection();

            connection.setPort(port);

            executorService = Executors.newCachedThreadPool();
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

            device.setExecutor(executorService);
            device.setScheduledExecutor(scheduledExecutorService);

            device.addConnection(connection);
            device.addApplicationEntity(applicationEntity);
            applicationEntity.addConnection(connection);

            running = true;
            listeningPort = port;
            listeningAeTitle = aeTitle;

            return true;

        } catch (Exception ex) {
            ex.printStackTrace();

            running = false;
            listeningPort = -1;
            listeningAeTitle = null;

            return false;
        }
    }

    private BasicCStoreSCP createStoreScp() {

        return new BasicCStoreSCP("*") {

            @Override
            protected void store(
                    Association association,
                    org.dcm4che3.net.pdu.PresentationContext pc,
                    Attributes request,
                    PDVInputStream data,
                    Attributes response
            ) throws IOException {

                String sopInstanceUid =
                        request.getString(Tag.AffectedSOPInstanceUID);

                Path incomingDir =
                        Path.of(storagePath, "incoming-dicom");

                Files.createDirectories(incomingDir);

                Path dicomFile =
                        incomingDir.resolve(
                                sopInstanceUid + ".dcm"
                        );

                Files.copy(
                        data,
                        dicomFile,
                        StandardCopyOption.REPLACE_EXISTING
                );

                // successful rec lands here before future pdf extractions and wiring together with pt file records - updated 6152026
                System.out.println(
                        "Received DICOM object: " +
                                dicomFile
                );

                response.setInt(
                        Tag.Status,
                        org.dcm4che3.data.VR.US,
                        Status.Success
                );
            }
        };
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