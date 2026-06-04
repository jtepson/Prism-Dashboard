package com.bms.processing.components;

import com.bms.processing.entity.SiteContactEntity;
import com.bms.processing.entity.SiteEntity;
import com.bms.processing.service.SiteContactService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;

import java.util.function.Consumer;

public class SiteContactDialog extends Dialog {

    public SiteContactDialog(
            SiteContactService siteContactService,
            SiteEntity site,
            SiteContactEntity existingContact,
            Consumer<SiteContactEntity> onContactSaved
    ) {
        boolean editMode = existingContact != null;

        setHeaderTitle(editMode ? "Edit Contact" : "Add Contact");
        setWidth("520px");

        TextField contactName = new TextField("Contact Name");
        contactName.setWidthFull();

        EmailField email = new EmailField("Email");
        email.setWidthFull();

        TextField phone = new TextField("Phone");
        phone.setWidthFull();

        if (editMode) {
            contactName.setValue(nullToEmpty(existingContact.getContactName()));
            email.setValue(nullToEmpty(existingContact.getEmail()));
            phone.setValue(nullToEmpty(existingContact.getPhone()));
        }

        FormLayout formLayout = new FormLayout(contactName, email, phone);
        formLayout.setWidthFull();

        Button cancelButton = new Button("Cancel", event -> close());

        Button saveButton = new Button(editMode ? "Save Changes" : "Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        saveButton.addClickListener(event -> {
            SiteContactEntity contact = editMode
                    ? existingContact
                    : new SiteContactEntity();

            contact.setSite(site);
            contact.setContactName(contactName.getValue().trim());
            contact.setEmail(email.getValue().trim());
            contact.setPhone(phone.getValue().trim());

            SiteContactEntity saved = siteContactService.save(contact);

            if (onContactSaved != null) {
                onContactSaved.accept(saved);
            }

            close();
        });

        add(formLayout);
        getFooter().add(cancelButton, saveButton);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}