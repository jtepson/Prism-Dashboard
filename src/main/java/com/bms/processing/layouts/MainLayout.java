package com.bms.processing.layouts;

import com.bms.processing.views.CompletedView;
import com.bms.processing.views.ErrorsView;
import com.bms.processing.views.ProcessedView;
import com.bms.processing.views.ProcessingView;
import com.bms.processing.views.SummaryView;
import com.bms.processing.views.UpcomingView;
import com.bms.processing.views.manage.ManagePatientsView;
import com.bms.processing.views.manage.ManageSitesView;
import com.bms.processing.views.manage.ManageNotificationsView;
import com.bms.processing.views.manage.ManageAuditLogView;
import com.bms.processing.views.manage.ManageDicomView;
import com.bms.processing.service.CurrentUserService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
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
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;

//user imports for keycloak
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.GrantedAuthority;
//vaadin auth context for logging out
import com.vaadin.flow.spring.security.AuthenticationContext;


@PermitAll
public class MainLayout extends AppLayout {

    private final AuthenticationContext authenticationContext;
    private final CurrentUserService currentUserService;

    public MainLayout(
                AuthenticationContext authenticationContext,
                CurrentUserService currentUserService
        ) {
                this.authenticationContext = authenticationContext;
                this.currentUserService = currentUserService;

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
                        .set("font-weight", "700")
                        .set("color", "#ffffff");

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
        
        //updated sidebar for dropdown - updated 6092026
        SideNavItem manage = new SideNavItem(
                "Manage",
                (Class<? extends Component>) null,
                new Icon(VaadinIcon.TOOLS)
        );

        SideNavItem patients = new SideNavItem("Patients", ManagePatientsView.class);
        SideNavItem sites = new SideNavItem("Sites", ManageSitesView.class);
        SideNavItem notifications = new SideNavItem("Notifications", ManageNotificationsView.class);
        SideNavItem dicomConfiguration = new SideNavItem("DICOM Configuration", ManageDicomView.class);
        SideNavItem auditLog = new SideNavItem("Audit Log", ManageAuditLogView.class);

        styleManageChild(patients);
        styleManageChild(sites);
        styleManageChild(notifications);
        styleManageChild(dicomConfiguration);
        styleManageChild(auditLog);

        //updated for use of auth via oauth2 grouping - 6302026
        manage.addItem(patients);
        manage.addItem(sites);

        if (currentUserService.isPrism()) {
                manage.addItem(notifications);
                manage.addItem(dicomConfiguration);

                //adding this in for admin only audit review page
                if (currentUserService.isAdmin()) {
                        manage.addItem(auditLog);
                }
        }

        manage.getStyle()
                .set("border-radius", "12px")
                .set("margin-top", "0.25rem");

        nav.addItem(manage);

        Scroller scroller = new Scroller(nav);
        scroller.setSizeFull();

        //all things keycloak user card
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

                String displayName = "Unknown User";
                String email = "";
                String initials = "??";
                String userRoleDisplay = "USER";

                if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
                        authentication.getAuthorities().forEach(a ->
                                System.out.println("ROLE FOUND: " + a.getAuthority())
                        );
                        displayName = oidcUser.getFullName() != null
                                ? oidcUser.getFullName()
                                : oidcUser.getPreferredUsername();

                        email = oidcUser.getEmail() != null
                                ? oidcUser.getEmail()
                                : "";

                Set<String> authorities = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(java.util.stream.Collectors.toSet());

                if (authorities.contains("ROLE_ADMIN")) {
                        userRoleDisplay = "ADMIN";
                } else if (authorities.contains("ROLE_USER")) {
                        userRoleDisplay = "USER";
                } else if (authorities.contains("ROLE_VIEWER")) {
                        userRoleDisplay = "VIEWER";
                }

                String[] nameParts = displayName.trim().split("\\s+");

                //replacing the first name initials code with first and last initials
                if (nameParts.length >= 2) {
                        initials =
                                nameParts[0].substring(0, 1).toUpperCase()
                                + nameParts[nameParts.length - 1].substring(0, 1).toUpperCase();
                } else if (!displayName.isBlank()) {
                        initials = displayName.substring(0, 1).toUpperCase();
                } else {
                        initials = "??";
                }
        }

        //User card design and formatting - updated 6092026 with dark glass style
        Div userCard = new Div();
            Div avatar = new Div();
            avatar.setText(initials);
            avatar.getStyle()
                        .set("width", "38px")
                        .set("height", "38px")
                        .set("border-radius", "999px")
                        .set("background", "linear-gradient(135deg, #f4f7ff, #dbeafe)")
                        .set("color", "#2563eb")
                        .set("display", "flex")
                        .set("align-items", "center")
                        .set("justify-content", "center")
                        .set("font-weight", "800")
                        .set("flex-shrink", "0")
                        .set("box-shadow", "0 2px 10px rgba(0,0,0,0.20)");

            Span userName = new Span(displayName);
            userName.getStyle()
                        .set("color", "#ffffff")
                        .set("font-weight", "700")
                        .set("font-size", "0.86rem")
                        .set("white-space", "nowrap");

            Span userRole = new Span(userRoleDisplay);
            userRole.getStyle()
                        .set("color", "#a7f3d0")
                        .set("font-size", "0.72rem")
                        .set("font-weight", "600");

            VerticalLayout userText = new VerticalLayout(userName, userRole);
            userText.setPadding(false);
            userText.setSpacing(false);
            userText.getStyle().set("min-width", "0");

            HorizontalLayout userRow = new HorizontalLayout(avatar, userText);
            userRow.setAlignItems(Alignment.CENTER);
            userRow.setSpacing(true);

            Button logoutButton = new Button("Logout");
                logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                logoutButton.getStyle()
                        .set("color", "#ff8f8f")
                        .set("font-size", "0.86rem")
                        .set("font-weight", "600")
                        .set("cursor", "pointer")
                        .set("transition", "all 0.15s ease");

                logoutButton.addClassName("sidebar-logout-button");

                logoutButton.addClickListener(event ->
                        authenticationContext.logout()
        );

        HorizontalLayout userTopRow = new HorizontalLayout(avatar, userText);
        userTopRow.setAlignItems(Alignment.CENTER);
        userTopRow.setWidthFull();
        userTopRow.setSpacing(true);
        userTopRow.setPadding(false);
        userTopRow.getStyle().set("min-width", "0");

        Hr divider = new Hr();
        divider.getStyle()
                .set("margin", "0.55rem 0")
                .set("border", "none")
                .set("height", "1px")
                .set("background",
                        "linear-gradient(90deg, transparent, rgba(255,255,255,0.18), transparent)");

        HorizontalLayout logoutRow = new HorizontalLayout(logoutButton);
        logoutRow.setWidthFull();
        logoutRow.setPadding(false);
        logoutRow.setSpacing(false);

        userCard.removeAll();
        userCard.add(userTopRow, divider, logoutRow);

        userCard.setWidth("90%");
        userCard.setMaxWidth("215px");

        userCard.getStyle()
                .set("background",
                        "linear-gradient(#0f4a4f, #0f4a4f) padding-box, " +
                        "linear-gradient(135deg, rgba(94,234,212,0.95), rgba(255,255,255,0.18), rgba(20,184,166,0.25)) border-box")
                .set("border", "1px solid transparent")
                .set("border-radius", "18px")
                .set("padding", "0.75rem")
                .set("margin-bottom", "18px")
                .set("box-shadow", "0 10px 26px rgba(0,0,0,0.22)")
                .set("color", "#ffffff")
                .set("box-sizing", "border-box");

        VerticalLayout drawerLayout =
            new VerticalLayout(branding, scroller, userCard);
        drawerLayout.setPadding(false);
        drawerLayout.setSpacing(false);
        drawerLayout.setSizeFull();

        drawerLayout.setDefaultHorizontalComponentAlignment(
                Alignment.CENTER
        );

        addToDrawer(drawerLayout);
    }

    private void styleManageChild(SideNavItem item) {
        item.getStyle()
                .set("margin-left", "1rem")
                .set("border-radius", "12px")
                .set("font-weight", "500")
                .set("padding", "0.15rem 0.25rem");
    }
}
