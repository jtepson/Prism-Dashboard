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
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.Component;
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
//vaadin auth context for logging out
import com.vaadin.flow.spring.security.AuthenticationContext;


@PermitAll
public class MainLayout extends AppLayout {

    private final AuthenticationContext authenticationContext;

    public MainLayout(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
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
        
        //updated sidebar for dropdown - updated 6092026
        SideNavItem manage = new SideNavItem(
                "Manage",
                (Class<? extends Component>) null,
                new Icon(VaadinIcon.TOOLS)
        );

        SideNavItem patients = new SideNavItem("Patients", ManagePatientsView.class);
        SideNavItem sites = new SideNavItem("Sites", ManageSitesView.class);
        SideNavItem notificationGroups = new SideNavItem("Notification Groups", ManageNotificationsView.class);

        styleManageChild(patients);
        styleManageChild(sites);
        styleManageChild(notificationGroups);

        manage.addItem(patients);
        manage.addItem(sites);
        manage.addItem(notificationGroups);

        manage.getStyle()
                .set("border-radius", "12px")
                .set("margin-top", "0.25rem");

        nav.addItem(manage);

        Scroller scroller = new Scroller(nav);
        scroller.setSizeFull();

        //all things keycloak user card
        Authentication authentication =
                        SecurityContextHolder.getContext().getAuthentication();
                        //role check
                        if (authentication != null) {
                                authentication.getAuthorities()
                                        .forEach(authority ->
                                                System.out.println("ROLE FOUND: " + authority.getAuthority())
                                        );
                                }

                String displayName = "Unknown User";
                String email = "";
                String initials = "??";
                String userRoleDisplay = "USER";

                if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
                        displayName = oidcUser.getFullName() != null
                                ? oidcUser.getFullName()
                                : oidcUser.getPreferredUsername();

                        email = oidcUser.getEmail() != null
                                ? oidcUser.getEmail()
                                : "";

                //debug chain just so i can see if it is pulling role - please delete once verified PLEASE
                Object realmAccess = oidcUser.getClaims().get("realm_access");

                if (realmAccess instanceof java.util.Map<?, ?> accessMap) {
                        Object rolesObject = accessMap.get("roles");

                        if (rolesObject instanceof java.util.List<?> roles) {
                                if (roles.contains("ADMIN")) {
                                        userRoleDisplay = "ADMIN";
                                } else if (roles.contains("PRISM_USER")) {
                                        userRoleDisplay = "PRISM USER";
                                } else if (roles.contains("BMS_USER")) {
                                        userRoleDisplay = "BMS USER";
                                }
                        }
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
                    .set("color", "#ffffff")
                    .set("font-weight", "700");

            Span userRole = new Span(userRoleDisplay);
            userRole.getStyle()
                    .set("color", "#a7f3d0")
                    .set("font-size", "0.78rem")
                    .set("font-weight", "600");

            VerticalLayout userText = new VerticalLayout(userName, userRole);
            userText.setPadding(false);
            userText.setSpacing(false);

            HorizontalLayout userRow = new HorizontalLayout(avatar, userText);
            userRow.setAlignItems(Alignment.CENTER);
            userRow.setSpacing(true);

            Button logoutButton = new Button("Logout");
                logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                logoutButton.getStyle()
                        .set("font-size", "0.8rem")
                        .set("font-weight", "700")
                        .set("color", "#fca5a5")
                        .set("padding", "0");

                logoutButton.addClickListener(event ->
                        authenticationContext.logout()
                );

            userCard.removeAll();
            userCard.add(userRow, logoutButton);
            userCard.setWidth("190px");
            userCard.getStyle()
                .set("background", "rgba(255, 255, 255, 0.08)")
                .set("border", "1px solid rgba(255, 255, 255, 0.14)")
                .set("border-radius", "14px")
                .set("padding", "0.9rem")
                .set("margin-bottom", "16px")
                .set("color", "#e6fffb");
            userCard.getStyle()
                .set("cursor", "pointer");

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
