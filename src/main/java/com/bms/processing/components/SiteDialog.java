package com.bms.processing.components;

import com.bms.processing.entity.SiteEntity;
import com.bms.processing.service.SiteService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;

import java.util.function.Consumer;

public class SiteDialog extends Dialog {

    public SiteDialog(
            SiteService siteService,
            Consumer<SiteEntity> onSiteSaved
    ) {
        setHeaderTitle("Add Site");
        setWidth("700px");

        TextField facilityName = new TextField("Facility Name");
        facilityName.setWidthFull();

        TextField address = new TextField("Address");
        address.setWidthFull();

        TextField primaryContact = new TextField("Primary Contact");
        primaryContact.setWidthFull();

        TextField transferMethod = new TextField("Transfer Method");
        transferMethod.setWidthFull();

        Checkbox imekaCertified = new Checkbox("IMEKA Certified");

        TextField scannerBrand = new TextField("Scanner Brand");
        scannerBrand.setWidthFull();

        TextField magnetStrength = new TextField("Magnet Strength");
        magnetStrength.setWidthFull();

        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        formLayout.add(
                facilityName,
                address,
                primaryContact,
                transferMethod,
                imekaCertified,
                scannerBrand,
                magnetStrength
        );

        Button cancelButton = new Button("Cancel", e -> close());

        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        saveButton.addClickListener(event -> {
            if (facilityName.getValue().trim().isEmpty()) {
                showError("Facility name is required.");
                return;
            }

            if (siteService.exists(facilityName.getValue().trim())) {
                showError("A site with this facility name already exists.");
                return;
            }

            SiteEntity site = new SiteEntity();
            site.setFacilityName(facilityName.getValue().trim());
            site.setAddress(address.getValue().trim());
            site.setPrimaryContact(primaryContact.getValue().trim());
            site.setTransferMethod(transferMethod.getValue().trim());
            site.setImekaCertified(imekaCertified.getValue());
            site.setScannerBrand(scannerBrand.getValue().trim());
            site.setMagnetStrength(magnetStrength.getValue().trim());

            SiteEntity saved = siteService.save(site);

            if (onSiteSaved != null) {
                onSiteSaved.accept(saved);
            }

            close();
        });

        add(formLayout);
        getFooter().add(cancelButton, saveButton);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 3500, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}