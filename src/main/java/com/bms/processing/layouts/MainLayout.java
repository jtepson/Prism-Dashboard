package com.bms.processing.layouts;

import com.bms.processing.views.CompletedView;
import com.bms.processing.views.ErrorsView;
import com.bms.processing.views.ProcessedView;
import com.bms.processing.views.ProcessingView;
import com.bms.processing.views.SummaryView;
import com.bms.processing.views.UpcomingView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class MainLayout extends AppLayout {

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        //commenting below out since it is kind of redudant, can do something with it later
        //addHeaderContent();
    }

    private void addHeaderContent() {
        Header header = new Header();
        header.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.BoxSizing.BORDER
        );

        Span title = new Span("Prism Dashboard");
        title.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD);

        header.add(title);
        addToNavbar(header);
    }

    private void addDrawerContent() {
        //image should be found in ...prism-dashboard\src\main\resources\META-INF\resources\graphics\ as Logo.png
        Image logo = new Image("graphics/Logo.png", "NO IMAGE FOUND");
            logo.setWidth("64px");
            logo.setHeight("64px");

            H2 appName = new H2("Prism Clinical Imaging");
            appName.getStyle()
                    .set("margin", "0")
                    .set("font-size", "1.15rem")
                    .set("font-weight", "700");

            HorizontalLayout branding = new HorizontalLayout(
                    logo,
                    appName
            );

            branding.setAlignItems(Alignment.CENTER);
            branding.setSpacing(true);
            branding.getStyle()
                    .set("padding", "1rem")
                    .set("border-bottom", "1px solid #e2e8f0");

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Summary", SummaryView.class, new Icon(VaadinIcon.DASHBOARD)));
        nav.addItem(new SideNavItem("Upcoming", UpcomingView.class, new Icon(VaadinIcon.CALENDAR)));
        nav.addItem(new SideNavItem("Processing", ProcessingView.class, new Icon(VaadinIcon.COG)));
        nav.addItem(new SideNavItem("Processed", ProcessedView.class, new Icon(VaadinIcon.CHECK_CIRCLE_O)));
        nav.addItem(new SideNavItem("Completed", CompletedView.class, new Icon(VaadinIcon.CHECK_CIRCLE)));
        nav.addItem(new SideNavItem("Errors", ErrorsView.class, new Icon(VaadinIcon.WARNING)));
        nav.addItem(new SideNavItem("Settings", SummaryView.class, new Icon(VaadinIcon.COG_O)));

        Scroller scroller = new Scroller(nav);
        scroller.setSizeFull();

        Div userCard = new Div();
            userCard.setText("Not Signed In");
            userCard.getStyle()
                    .set("margin", "1rem")
                    .set("padding", "0.75rem")
                    .set("border", "1px solid #dbe3ee")
                    .set("border-radius", "12px")
                    .set("background", "#f8fafc")
                    .set("font-size", "0.85rem")
                    .set("font-weight", "600");
            userCard.getStyle()
                    .set("cursor", "pointer");

        VerticalLayout drawerLayout =
            new VerticalLayout(branding, scroller, userCard);
        drawerLayout.setPadding(false);
        drawerLayout.setSpacing(false);
        drawerLayout.setSizeFull();

        addToDrawer(drawerLayout);
    }
}
