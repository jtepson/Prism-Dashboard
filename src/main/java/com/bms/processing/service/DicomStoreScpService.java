package com.bms.processing.service;

import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DimseRQHandler;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.springframework.stereotype.Service;

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

    public boolean startListener(
            String aeTitle,
            int port,
            String storagePath
    ) {
        try {
            Files.createDirectories(Path.of(storagePath));

            // listener state is real now, but C-STORE handling comes next - 6152026
            device = new Device("prism-dashboard-store-scp");
            applicationEntity = new ApplicationEntity(aeTitle);

            DicomServiceRegistry serviceRegistry =
                    new DicomServiceRegistry();

            serviceRegistry.addDicomService(
                    new BasicCEchoSCP()
            );

            serviceRegistry.addDicomService(
                    new BasicCStoreSCP("*")
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