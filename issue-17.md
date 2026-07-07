Issue



Invoice Sent currently appears in multiple locations throughout the dashboard and does not behave consistently across views. Some instances are editable, some are read-only, and changes are not always reflected across the application.



This creates confusion regarding which control should be used and increases the risk of inconsistent billing records.



Expected Behavior



Invoice Sent should be represented by a single underlying field shared across the entire application.



Rather than storing a separate checkbox value, the system should use a single Invoice Sent Date field.



Behavior should be:



\* Invoice Sent Date is empty when an invoice has not been sent

\* Setting an Invoice Sent Date indicates the invoice was sent

\* All pages should display the same value

\* Changes made in one location should immediately be reflected everywhere the field is displayed



Permissions



The Invoice Sent Date should be editable by:



\* Administrators

\* BMS Users



All other roles should have read-only access.



User Interface



Any location that currently displays or edits Invoice Sent should reference the same underlying field and present a consistent experience throughout the dashboard.



Audit Requirements



Changes to the Invoice Sent Date should be recorded in the audit history, including:



\* User making the change

\* Previous value

\* New value

\* Date and time of modification



Goal



Provide a single source of truth for invoice tracking, eliminate conflicting controls, and ensure invoice status remains consistent throughout the dashboard.



