package com.bms.processing.views.manage;

import com.bms.processing.entity.AuditEventEntity;
import com.bms.processing.service.AuditEventService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "manage/audit")
@PageTitle("Audit Log")
public class ManageAuditLogView extends VerticalLayout {

    public ManageAuditLogView(
            AuditEventService auditEventService
    ) {

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