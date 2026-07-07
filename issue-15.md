Issue



Error handling is currently fragmented across multiple workflow areas. Vendor errors, processing errors, and general case errors are handled through separate controls and note fields, creating inconsistent behavior and making it difficult to determine whether a case is blocked, completed with limitations, or requires follow-up.



Examples of the current behavior include:



\* IMEKA Error prompting for an IMEKA-specific error note

\* Neuroreader Error prompting for a separate Neuroreader error note

\* General Error buttons moving cases to the Errors page

\* Error handling near Finalize moving studies to Errors

\* Internal processing concerns being tracked differently from vendor issues



This results in multiple disconnected error workflows and inconsistent visibility of important processing information.



Expected Behavior



Revamp error handling into a centralized issue tracking system that supports both blocking errors and non-blocking processing concerns.



Workflow status should remain separate from error status.



Examples:



\* Upcoming + Active Issue

\* Processing + Active Issue

\* Processed + Active Issue

\* Completed + Active Issue



Centralized Issue Dialog



All error creation, editing, and resolution should use a shared issue dialog.



The dialog should support:



\* Issue source

\* Issue note

\* Optional issue category

\* Active/resolved status

\* Blocking or non-blocking designation

\* Created by user

\* Created date/time

\* Resolved by user

\* Resolved date/time

\* Resolution note when applicable



Potential issue sources may include:



\* Acquisition / Upcoming

\* Internal Processing

\* IMEKA

\* Neuroreader

\* DuraMap

\* Administrative

\* Other



Blocking vs Non-Blocking Issues



The system should support both blocking and informational processing concerns.



Examples of blocking issues:



\* Missing imaging data

\* Corrupt DICOM files

\* IMEKA processing failure requiring alternate workflow

\* Missing required study information



Examples of non-blocking issues:



\* Study successfully processed with significant motion artifact

\* Braces causing reduced anterior brain data quality

\* Vendor limitations affecting report quality

\* Processing concerns that should be communicated to BMS or interpreting physicians



A case should be allowed to continue through the workflow when appropriate, even if an active non-blocking issue exists.



Vendor Issue Handling



When a third-party workflow status is set to Error, the system should create or update an active issue record using the centralized issue system.



Examples:



\* Setting IMEKA to Error creates or updates an IMEKA issue

\* Setting Neuroreader to Error creates or updates a Neuroreader issue

\* Setting DuraMap to Error creates or updates a DuraMap issue



Vendor-specific notes should be preserved but managed through the centralized issue workflow rather than separate disconnected note fields.



Workflow Behavior



Creating an issue should not automatically remove a case from its current workflow stage.



Users should be able to:



\* Create an issue while remaining in Processing

\* Continue processing when appropriate

\* Move a case forward while maintaining visibility of active issues

\* Resolve issues independently of workflow movement



The Errors page should function as a filtered view of cases with active issues rather than serving as a standalone workflow state.



Issue Badge Behavior



Cases with active issues should display an Issue/Error badge wherever they appear in the workflow.



Examples:



\* Upcoming with active issue

\* Processing with active issue

\* Processed with active issue

\* Completed with active issue



The badge should remain visible until the issue is resolved.



Email and Notification Integration



If active issues exist when a study is processed or completed, the relevant issue notes should be available for inclusion within notification templates.



This allows important processing context to be communicated through notifications such as:



\* Study Processed

\* Case Completed

\* Vendor Processing Updates



Examples:



\* IMEKA processing unavailable, study routed to DuraMap

\* Significant artifact present due to patient braces

\* Reduced image quality affecting interpretation confidence



Issue History



All issue activity should remain visible within the patient activity history, including:



\* Issue creation

\* Issue edits

\* Workflow changes while issue is active

\* Issue resolution

\* User responsible for each action

\* Date/time of each action



Goal



Create a unified issue management system that supports acquisition issues, processing concerns, vendor failures, and informational notes while preserving workflow flexibility and maintaining complete auditability.



