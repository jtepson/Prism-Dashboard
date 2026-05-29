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
import jakarta.annotation.security.PermitAll;

//user imports for keycloak
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@PermitAll
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

        //all things keycloak user card
        Authentication authentication =
                        SecurityContextHolder.getContext().getAuthentication();

                String displayName = "Unknown User";
                String email = "";
                String initials = "??";

                if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
                displayName = oidcUser.getFullName() != null
                        ? oidcUser.getFullName()
                        : oidcUser.getPreferredUsername();

                email = oidcUser.getEmail() != null
                        ? oidcUser.getEmail()
                        : "";

                initials = displayName != null && !displayName.isBlank()
                        ? displayName.substring(0, Math.min(2, displayName.length())).toUpperCase()
                        : "??";
        }

        //User placeholder
        Div userCard = new Div();
            Div avatar = new Div();
            avatar.setText(initials);
            avatar.getStyle()
                    .set("width", "36px")
                    .set("height", "36px")
                    .set("border-radius", "999px")
                    .set("background", "#dbeafe")
                    .set("color", "#1d4ed8")
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("justify-content", "center")
                    .set("font-weight", "800")
                    .set("flex-shrink", "0");

            Span userName = new Span(displayName);
            userName.getStyle()
                    .set("font-weight", "700")
                    .set("font-size", "0.9rem");

            Span userRole = new Span("PRISM USER");
            userRole.getStyle()
                    .set("color", "#64748b")
                    .set("font-size", "0.78rem");

            VerticalLayout userText = new VerticalLayout(userName, userRole);
            userText.setPadding(false);
            userText.setSpacing(false);

            HorizontalLayout userRow = new HorizontalLayout(avatar, userText);
            userRow.setAlignItems(Alignment.CENTER);
            userRow.setSpacing(true);

            userCard.removeAll();
            userCard.add(userRow);
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
