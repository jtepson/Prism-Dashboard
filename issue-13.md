Issue



The dashboard does not currently provide a simple way for administrators or users to handle account access issues such as password resets or account recovery.



User management is handled through Keycloak, which should remain the source of truth for users, roles, groups, and authentication. However, common account recovery actions should be accessible from the dashboard so administrators do not need to manually navigate Keycloak for routine support.



Expected Behavior



Add a User Management or Account Access section to the dashboard focused on password reset and account recovery workflows.



This section should allow authorized administrators to:



\* Search for dashboard users

\* View basic account status

\* Trigger a password reset or recovery action

\* Confirm whether a user account is enabled or disabled

\* Direct users to the appropriate Keycloak-managed recovery flow



Scope



This should not recreate the full Keycloak admin console inside the dashboard.



Keycloak should remain responsible for:



\* Authentication

\* Password storage

\* Password reset enforcement

\* Group and role assignment

\* Account security policies



The dashboard should provide a simplified administrative access point for common account recovery needs.



