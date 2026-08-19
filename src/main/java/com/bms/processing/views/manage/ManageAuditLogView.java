package com.bms.processing.views.manage;

import com.bms.processing.entity.AuditEventEntity;
import com.bms.processing.service.AuditEventService;
import com.bms.processing.service.CurrentUserService;
import com.bms.processing.layouts.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.component.combobox.ComboBox;
import java.util.List;
import java.time.LocalDateTime;

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

                grid.addColumn(event -> formatTimestamp(event.getCreatedAt()))
                        .setHeader("Timestamp")
                        .setWidth("180px")
                        .setFlexGrow(0)
                        .setSortable(true);

                grid.addColumn(AuditEventEntity::getCreatedBy)
                        .setHeader("User")
                        .setWidth("150px")
                        .setFlexGrow(0);

                grid.addColumn(AuditEventEntity::getCaseRecordId)
                        .setHeader("Case ID")
                        .setWidth("100px")
                        .setFlexGrow(0);

                grid.addColumn(AuditEventEntity::getEventType)
                        .setHeader("Event")
                        .setWidth("220px")
                        .setFlexGrow(0);

                grid.addColumn(AuditEventEntity::getMessage)
                        .setHeader("Details")
                        .setFlexGrow(1);

                grid.addColumn(AuditEventEntity::getOldValue)
                        .setHeader("Previous")
                        .setWidth("180px")
                        .setFlexGrow(0);

                grid.addColumn(AuditEventEntity::getNewValue)
                        .setHeader("New")
                        .setWidth("180px")
                        .setFlexGrow(0);

                List<AuditEventEntity> events = auditEventService.getRecentAuditEvents();

                ListDataProvider<AuditEventEntity> dataProvider =
                        new ListDataProvider<>(events);

                TextField searchField = new TextField();
                searchField.setPlaceholder("Search audit log...");
                searchField.setClearButtonVisible(true);
                searchField.setWidth("350px");

                ComboBox<String> eventFilter = new ComboBox<>();
                eventFilter.setPlaceholder("Filter event");
                eventFilter.setClearButtonVisible(true);
                eventFilter.setWidth("220px");

                eventFilter.setItems(
                        events.stream()
                                .map(AuditEventEntity::getEventType)
                                .filter(java.util.Objects::nonNull)
                                .distinct()
                                .sorted()
                                .toList()
                );

        Runnable applyFilters = () -> {
                String searchTerm = searchField.getValue() == null
                        ? ""
                        : searchField.getValue().trim().toLowerCase();

                String selectedEvent = eventFilter.getValue();

                dataProvider.setFilter(auditEvent -> {
                        boolean matchesSearch =
                                searchTerm.isBlank()
                                        || contains(auditEvent.getCreatedBy(), searchTerm)
                                        || contains(auditEvent.getEventType(), searchTerm)
                                        || contains(auditEvent.getMessage(), searchTerm)
                                        || contains(auditEvent.getOldValue(), searchTerm)
                                        || contains(auditEvent.getNewValue(), searchTerm)
                                        || contains(
                                                auditEvent.getCaseRecordId() != null
                                                        ? auditEvent.getCaseRecordId().toString()
                                                        : null,
                                                searchTerm
                                        );

                        boolean matchesEvent =
                                selectedEvent == null
                                        || selectedEvent.equals(auditEvent.getEventType());

                        return matchesSearch && matchesEvent;
                });
        };        

        searchField.addValueChangeListener(event -> applyFilters.run());
        eventFilter.addValueChangeListener(event -> applyFilters.run());

        grid.setItems(dataProvider);

                grid.setSizeFull();

                HorizontalLayout filters = new HorizontalLayout(
                        searchField,
                        eventFilter
                );

                filters.setAlignItems(
                        com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END
                );

                add(filters, grid);
        }

        private boolean contains(String value, String searchTerm) {
                return value != null
                        && value.toLowerCase().contains(searchTerm);
        }

        private String formatTimestamp(LocalDateTime value) {
                if (value == null) {
                        return "";
                }

                int hour = value.getHour();
                int minute = value.getMinute();

                int displayHour = hour % 12;
                if (displayHour == 0) {
                        displayHour = 12;
                }

                String amPm = hour >= 12 ? "PM" : "AM";

                return String.format(
                        "%02d-%02d-%04d %02d:%02d %s",
                        value.getMonthValue(),
                        value.getDayOfMonth(),
                        value.getYear(),
                        displayHour,
                        minute,
                        amPm
                );
        }
}