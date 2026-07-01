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

                grid.setItems(
                        auditEventService.getRecentAuditEvents()
                );

                grid.setSizeFull();

                add(grid);
        }
}