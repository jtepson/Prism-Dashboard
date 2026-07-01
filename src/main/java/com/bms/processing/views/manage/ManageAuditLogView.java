package com.bms.processing.views.manage;

import com.bms.processing.entity.AuditEventEntity;
import com.bms.processing.service.AuditEventService;
import com.bms.processing.service.CurrentUserService;
import com.bms.processing.layouts.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import java.util.List;

@Route(value = "manage/audit", layout = MainLayout.class)
@PageTitle("Audit Log")
@PermitAll
public class ManageAuditLogView extends VerticalLayout {

        private final CurrentUserService currentUserService;

        public ManageAuditLogView(
                AuditEventService auditEventService,
                CurrentUserService currentUserService
        ) {
                this.currentUserService = currentUserService;

                if (!currentUserService.isPrism() || !currentUserService.isAdmin()) {
                        getUI().ifPresent(ui -> ui.navigate(""));
                        return;
                }

                setSizeFull();

                Grid<AuditEventEntity> grid = new Grid<>(AuditEventEntity.class, false);

                grid.addColumn(AuditEventEntity::getCreatedAt)
                        .setHeader("Timestamp")
                        .setAutoWidth(true);

                grid.addColumn(AuditEventEntity::getCreatedBy)
                        .setHeader("User")
                        .setAutoWidth(true);

                grid.addColumn(AuditEventEntity::getEventType)
                        .setHeader("Event")
                        .setAutoWidth(true);

                grid.addColumn(AuditEventEntity::getMessage)
                        .setHeader("Details")
                        .setFlexGrow(1);

                List<AuditEventEntity> events = auditEventService.getRecentAuditEvents();

                ListDataProvider<AuditEventEntity> dataProvider =
                        new ListDataProvider<>(events);

                TextField searchField = new TextField();
                searchField.setPlaceholder("Search audit log...");
                searchField.setClearButtonVisible(true);
                searchField.setWidth("350px");

                searchField.addValueChangeListener(event -> {
                String searchTerm = event.getValue() == null
                        ? ""
                        : event.getValue().trim().toLowerCase();

                dataProvider.setFilter(auditEvent -> {
                        if (searchTerm.isBlank()) {
                        return true;
                        }

                        return contains(auditEvent.getCreatedBy(), searchTerm)
                                || contains(auditEvent.getEventType(), searchTerm)
                                || contains(auditEvent.getMessage(), searchTerm);
                });
        });

        grid.setItems(dataProvider);

                grid.setSizeFull();

                add(searchField, grid);
        }

        private boolean contains(String value, String searchTerm) {
                return value != null
                        && value.toLowerCase().contains(searchTerm);
        }
}