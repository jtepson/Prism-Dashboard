package com.bms.processing.views.manage;

import com.bms.processing.entity.DicomConfigEntity;
import com.bms.processing.layouts.MainLayout;
import com.bms.processing.service.DicomConfigService;
import com.bms.processing.service.DicomService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@PageTitle("DICOM Configuration")
@PermitAll
@Route(value = "manage/dicom", layout = MainLayout.class)
public class ManageDicomView extends VerticalLayout {

    private final DicomConfigService dicomConfigService;
    private final DicomService dicomService;

    private DicomConfigEntity currentConfig;

    private final TextField configName = new TextField("Config Name");
    private final TextField remoteAeTitle = new TextField("Remote AE");
    private final TextField remoteHost = new TextField("Host");
    private final IntegerField remotePort = new IntegerField("Port");
    private final TextField localAeTitle = new TextField("Local AE");
    private final TextField retrieveAeTitle = new TextField("Retrieve AE");
    private final IntegerField retrievePort = new IntegerField("Retrieve Port");
    private final TextField storagePath = new TextField("Storage Path");
    private final Checkbox enabled = new Checkbox("Enabled");


    public ManageDicomView(
            DicomConfigService dicomConfigService,
            DicomService dicomService
    ) {
        this.dicomConfigService = dicomConfigService;
        this.dicomService = dicomService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle()
                .set("background", "#f8fafc");

        loadDefaultConfig();

        add(
                buildHeader(),
                buildCard()
        );
    }

    private Component buildHeader() {
        H2 title = new H2("DICOM Configuration");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "2rem")
                .set("font-weight", "800")
                .set("color", "#0f172a");

        VerticalLayout header = new VerticalLayout(title);
        header.setPadding(false);
        header.setSpacing(false);

        return header;
    }

    private Component buildCard() {
        H3 cardTitle = new H3("Edit DICOM Configuration");
        cardTitle.getStyle()
                .set("margin", "0")
                .set("font-size", "1.25rem")
                .set("font-weight", "800")
                .set("color", "#0f172a");

        Div dividerOne = divider();

        FormLayout form = new FormLayout();
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );

        configureFields();

        form.add(
                configName,
                remoteAeTitle,
                remoteHost,
                remotePort,
                localAeTitle,
                retrieveAeTitle,
                retrievePort,
                storagePath,
                enabled
        );

        Div dividerTwo = divider();

        HorizontalLayout testButtons = new HorizontalLayout(
                buildTestEchoButton(),
                buildTestQueryButton(),
                buildTestRetrieveButton()
        );
        testButtons.setSpacing(true);
        testButtons.setPadding(false);

        Div dividerThree = divider();

        Button saveButton = new Button("Save", new Icon(VaadinIcon.DOWNLOAD_ALT));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle()
                .set("background", "#008b8b")
                .set("color", "#ffffff")
                .set("border-radius", "10px")
                .set("font-weight", "700")
                .set("padding", "0 1.5rem");
        saveButton.addClickListener(event -> saveConfig());

        HorizontalLayout footer = new HorizontalLayout(saveButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout cardContent = new VerticalLayout(
                cardTitle,
                dividerOne,
                form,
                dividerTwo,
                testButtons,
                dividerThree,
                footer
        );

        cardContent.setPadding(false);
        cardContent.setSpacing(true);
        cardContent.setWidthFull();

        Div card = new Div(cardContent);
        card.setWidthFull();
        card.getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #e2e8f0")
                .set("border-radius", "14px")
                .set("box-shadow", "0 12px 30px rgba(15, 23, 42, 0.08)")
                .set("padding", "1.5rem")
                .set("box-sizing", "border-box")
                .set("max-width", "1180px");

        return card;
    }

    private void configureFields() {
        configName.setWidthFull();
        remoteAeTitle.setWidthFull();
        remoteHost.setWidthFull();
        remotePort.setWidthFull();
        localAeTitle.setWidthFull();
        retrieveAeTitle.setWidthFull();
        retrievePort.setWidthFull();
        storagePath.setWidthFull();

        configName.setPlaceholder("Archive Name");
        remoteAeTitle.setPlaceholder("BMS_CACHE");
        remoteHost.setPlaceholder("172.31.36.63");
        remotePort.setPlaceholder("11112");
        localAeTitle.setPlaceholder("PRISM_DASHBOARD");
        retrieveAeTitle.setPlaceholder("PRISM_DASHBOARD");
        retrievePort.setPlaceholder("11113");
        storagePath.setPlaceholder("/data/prism/files");

        if (currentConfig != null) {
            configName.setValue(nullSafe(currentConfig.getConfigName()));
            remoteAeTitle.setValue(nullSafe(currentConfig.getRemoteAeTitle()));
            remoteHost.setValue(nullSafe(currentConfig.getRemoteHost()));

            if (currentConfig.getRemotePort() != null) {
                remotePort.setValue(currentConfig.getRemotePort());
            }

            localAeTitle.setValue(nullSafe(currentConfig.getLocalAeTitle()));
            retrieveAeTitle.setValue(nullSafe(currentConfig.getRetrieveAeTitle()));
            retrievePort.setValue(currentConfig.getRetrievePort());
            storagePath.setValue(nullSafe(currentConfig.getStoragePath()));
            enabled.setValue(Boolean.TRUE.equals(currentConfig.getEnabled()));
        } else {
            configName.setValue("Archive Name");
            remoteAeTitle.setValue("BMS_CACHE");
            remoteHost.setValue("172.31.36.63");
            remotePort.setValue(11112);
            localAeTitle.setValue("PRISM_DASHBOARD");
            retrieveAeTitle.setValue("PRISM_DASHBOARD");
            retrievePort.setValue(11113);
            storagePath.setValue("/data/prism/files");
            enabled.setValue(true);
        }
    }

    private Button buildTestEchoButton() {
        Button button = new Button("Test C-ECHO", new Icon(VaadinIcon.CONNECT));
        styleTestButton(button);

        button.addClickListener(event -> {
            DicomConfigEntity config = buildConfigFromForm();
            boolean success = dicomService.testEcho(config);

            if (success) {
                showSuccess("C-ECHO test successful.");
            } else {
                showError("C-ECHO test failed.");
            }
        });

        return button;
    }

    private Button buildTestQueryButton() {
        Button button = new Button("Test Query", new Icon(VaadinIcon.SEARCH));
        styleTestButton(button);

        button.addClickListener(event -> {
            DicomConfigEntity config = buildConfigFromForm();
            showSuccess(dicomService.testQuery(config));
        });

        return button;
    }

    private Button buildTestRetrieveButton() {
        Button button = new Button("Test Retrieve", new Icon(VaadinIcon.DOWNLOAD));
        styleTestButton(button);

        button.addClickListener(event -> {
            DicomConfigEntity config = buildConfigFromForm();
            showSuccess(dicomService.testRetrieve(config));
        });

        return button;
    }

    private void styleTestButton(Button button) {
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        button.getStyle()
                .set("border", "1px solid #99d6d6")
                .set("color", "#007c7c")
                .set("border-radius", "10px")
                .set("font-weight", "700")
                .set("padding", "0.65rem 1.25rem")
                .set("background", "#ffffff");
    }

    private void saveConfig() {
        try {
            DicomConfigEntity config = buildConfigFromForm();
            currentConfig = dicomConfigService.save(config);
            showSuccess("DICOM configuration saved.");
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private DicomConfigEntity buildConfigFromForm() {
        DicomConfigEntity config =
                currentConfig != null
                        ? currentConfig
                        : new DicomConfigEntity();

        config.setConfigName(configName.getValue());
        config.setRemoteAeTitle(remoteAeTitle.getValue());
        config.setRemoteHost(remoteHost.getValue());
        config.setRemotePort(remotePort.getValue());
        config.setLocalAeTitle(localAeTitle.getValue());
        config.setRetrieveAeTitle(retrieveAeTitle.getValue());
        config.setRetrievePort(retrievePort.getValue());
        config.setStoragePath(storagePath.getValue());
        config.setEnabled(enabled.getValue());

        return config;
    }

    private void loadDefaultConfig() {
        currentConfig = dicomConfigService.findDefaultEnabled().orElse(null);
    }

    private Div divider() {
        Div divider = new Div();
        divider.getStyle()
                .set("border-top", "1px solid #e2e8f0")
                .set("width", "100%")
                .set("margin", "0.5rem 0");
        return divider;
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3500, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}