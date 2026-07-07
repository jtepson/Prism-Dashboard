Issue



Patient files currently remain in storage indefinitely and there is no mechanism for retention management, cleanup, or deletion tracking.



As the dashboard grows, long-term file retention introduces concerns around storage utilization, patient privacy, operational policies, and regulatory requirements. Different file types may require different retention periods, and administrators need visibility into when files are removed from the system.



Expected Behavior



Add patient file lifecycle management capabilities to support future retention and cleanup policies.



Initial functionality should include:



\* Manual deletion of patient files by authorized users

\* Confirmation dialog prior to deletion

\* Audit logging for all file deletions

\* Tracking of user, date, time, and file details when a deletion occurs



Future Enhancements



Support configurable retention and cleanup jobs that can automatically remove files based on administrator-defined policies.



Potential future retention criteria may include:



\* File source

\* File type

\* File age

\* Workflow status

\* Manual retention flags



All automated cleanup actions should generate audit records to maintain traceability of file removal activities.



Notes



This issue is intended to establish the framework for patient file lifecycle management. Specific retention periods and cleanup policies will be defined in a future implementation effort.



