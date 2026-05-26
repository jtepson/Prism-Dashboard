package com.bms.processing.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class DashboardMetricCard extends Div {

    public DashboardMetricCard(
            String label,
            long count,
            String subtitle,
            String color
    ) {
        addClassName("dashboard-metric-card");

        getStyle()
                .set("background", "#ffffff")
                .set("border", "1px solid #dbe3ee")
                .set("border-radius", "16px")
                .set("padding", "1rem")
                .set("box-shadow", "0 4px 12px rgba(15, 23, 42, 0.06)")
                .set("border-bottom", "3px solid " + color)
                .set("min-width", "180px");

        Span countText = new Span(String.valueOf(count));
        countText.getStyle()
                .set("font-size", "1.8rem")
                .set("font-weight", "800")
                .set("color", "#0f172a");

        Span labelText = new Span(label);
        labelText.getStyle()
                .set("font-size", "0.95rem")
                .set("font-weight", "700")
                .set("color", "#1e293b");

        Span subtitleText = new Span(subtitle);
        subtitleText.getStyle()
                .set("font-size", "0.78rem")
                .set("color", "#64748b");

        VerticalLayout layout = new VerticalLayout(countText, labelText, subtitleText);
        layout.setPadding(false);
        layout.setSpacing(false);

        add(layout);
    }
}