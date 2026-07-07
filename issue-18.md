Issue



The current study linking workflow can associate a DICOM study with the wrong dashboard patient when patient identifiers are incomplete, reused, or incorrectly selected. In addition, linking a study may unintentionally update patient demographic information without sufficient validation.



Study linking should provide stronger patient verification safeguards and prevent accidental linkage to the wrong patient record.



Expected Behavior



Study search should follow a confidence-based workflow:



1\. Search by Patient ID

2\. If no Patient ID match is found, search by Last Name

3\. Present all potential matches to the user for review



The study selection dialog should provide enough information for users to confidently identify the correct study, including:



\* Last Name

\* First Name

\* Patient ID

\* Date of Birth

\* Sex

\* Study Date

\* Study UID

\* Additional study information when available



Validation Requirements



After a study is selected, the dashboard should compare the selected DICOM patient information against the existing dashboard patient information.



Potential mismatches should include:



\* Patient ID

\* Last Name

\* First Name

\* Date of Birth

\* Sex



If any mismatch is detected, the user should be presented with a warning dialog identifying the conflicting values and requiring confirmation before the study can be linked.



Example:



"Selected study information does not match the current dashboard patient record. Are you sure you want to continue linking this study?"



Duplicate Study UID Protection



A Study UID should only be linked to a single dashboard case unless an authorized user explicitly confirms an override.



If a Study UID is already linked, the user should be warned and required to confirm the action before proceeding.



Data Integrity Requirements



Study linking should never silently overwrite patient demographic information.



Any patient demographic updates resulting from a study link should require explicit user confirmation and be recorded in the audit history.



Goal



Reduce accidental patient mismatches, prevent incorrect demographic updates, and ensure Study UID linkage remains accurate and traceable.



