package com.bms.processing.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public String getUsername() {
        Authentication authentication = getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            return "system";
        }

        return authentication.getName();
    }

    public boolean isBms() {
        return hasAuthority("GROUP_BMS") || hasAuthority("ROLE_BMS");
    }

    public boolean isPrism() {
        return hasAuthority("GROUP_PRISM") || hasAuthority("ROLE_PRISM");
    }

    public boolean isAdmin() {
        return hasAuthority("ROLE_ADMIN");
    }

    public boolean isUser() {
        return hasAuthority("ROLE_USER");
    }

    public boolean isViewer() {
        return hasAuthority("ROLE_VIEWER");
    }

    public boolean canEdit() {
        return isAdmin() || isUser();
    }

    public boolean canDownloadFiles() {
        return isAdmin() || isUser() || isViewer();
    }

    public boolean canManageNotifications() {
        return isAdmin() && isPrism();
    }

    public boolean canManageDicom() {
        return isAdmin() && isPrism();
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = getAuthentication();

        if (authentication == null) {
            return false;
        }

        authentication.getAuthorities()
            .forEach(a -> System.out.println("AUTHORITY: " + a.getAuthority()));

        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority ->
                        authority.equals(grantedAuthority.getAuthority())
                );
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}