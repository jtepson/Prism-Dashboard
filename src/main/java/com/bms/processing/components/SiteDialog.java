package com.bms.processing.components;

import com.bms.processing.entity.SiteEntity;
import com.bms.processing.service.SiteService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;

import java.util.function.Consumer;

public class SiteDialog extends Dialog {

    public SiteDialog(
            SiteService siteService,
            SiteEntity existingSite,
            Consumer<SiteEntity> onSiteSaved
    ) {
        boolean editMode = existingSite != null;

        setHeaderTitle(editMode ? "Edit Site" : "Add Site");
        setWidth("700px");

        TextField facilityName = new TextField("Facility Name");
        
        //Updated address entry for sites 6042026
        TextField addressLine1 = new TextField("Address Line 1");
        addressLine1.setWidthFull();

        TextField addressLine2 = new TextField("Address Line 2");
        addressLine2.setWidthFull();

        TextField city = new TextField("City");
        city.setWidthFull();

        ComboBox<String> state = new ComboBox<>("State");
        state.setWidthFull();

        state.setItems(
                "AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA",
                "HI","ID","IL","IN","IA","KS","KY","LA","ME","MD",
                "MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ",
                "NM","NY","NC","ND","OH","OK","OR","PA","RI","SC",
                "SD","TN","TX","UT","VT","VA","WA","WV","WI","WY"
        );

        TextField zipCode = new TextField("ZIP Code");
        zipCode.setWidthFull();

        TextField primaryContact = new TextField("Primary Contact");
        TextField transferMethod = new TextField("Transfer Method");
        Checkbox imekaCertified = new Checkbox("IMEKA Certified");
        TextField scannerBrand = new TextField("Scanner Brand");
        TextField magnetStrength = new TextField("Magnet Strength");

        facilityName.setWidthFull();
        addressLine1.setWidthFull();
        primaryContact.setWidthFull();
        transferMethod.setWidthFull();
        scannerBrand.setWidthFull();
        magnetStrength.setWidthFull();

        if (editMode) {
            facilityName.setValue(nullToEmpty(existingSite.getFacilityName()));
            addressLine1.setValue(nullToEmpty(existingSite.getAddressLine1()));
            addressLine2.setValue(nullToEmpty(existingSite.getAddressLine2()));
            city.setValue(nullToEmpty(existingSite.getCity()));
            state.setValue(existingSite.getState());
            zipCode.setValue(nullToEmpty(existingSite.getZipCode()));
            primaryContact.setValue(nullToEmpty(existingSite.getPrimaryContact()));
            transferMethod.setValue(nullToEmpty(existingSite.getTransferMethod()));
            imekaCertified.setValue(Boolean.TRUE.equals(existingSite.getImekaCertified()));
            scannerBrand.setValue(nullToEmpty(existingSite.getScannerBrand()));
            magnetStrength.setValue(nullToEmpty(existingSite.getMagnetStrength()));
        }

        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        formLayout.add(
                facilityName,
                addressLine1,
                addressLine2,
                city,
                state,
                zipCode,
                primaryContact,
                transferMethod,
                imekaCertified,
                scannerBrand,
                magnetStrength
        );

        Button cancelButton = new Button("Cancel", e -> close());

        Button saveButton = new Button(editMode ? "Save Changes" : "Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        saveButton.addClickListener(event -> {
            String name = facilityName.getValue().trim();

            if (name.isEmpty()) {
                showError("Facility name is required.");
                return;
            }

            Long currentId = editMode ? existingSite.getId() : null;

            if (siteService.existsForOtherSite(name, currentId)) {
                showError("A site with this facility name already exists.");
                return;
            }

            SiteEntity site = editMode ? existingSite : new SiteEntity();

            site.setFacilityName(name);
            site.setAddressLine1(addressLine1.getValue().trim());
            site.setAddressLine2(addressLine2.getValue().trim());
            site.setCity(city.getValue().trim());
            site.setState(state.getValue());
            site.setZipCode(zipCode.getValue().trim());
            site.setPrimaryContact(primaryContact.getValue().trim());
            site.setTransferMethod(transferMethod.getValue().trim());
            site.setImekaCertified(imekaCertified.getValue());
            site.setScannerBrand(scannerBrand.getValue().trim());
            site.setMagnetStrength(magnetStrength.getValue().trim());

            if (site.getActive() == null) {
                site.setActive(true);
            }

            SiteEntity saved = siteService.save(site);

            if (onSiteSaved != null) {
                onSiteSaved.accept(saved);
            }

            close();
        });

        add(formLayout);
        getFooter().add(cancelButton, saveButton);
    }

    public SiteDialog(
            SiteService siteService,
            Consumer<SiteEntity> onSiteSaved
    ) {
        this(siteService, null, onSiteSaved);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 3500, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}