Issue



The Patient Files upload area currently suggests that users can upload general files, but the actual upload behavior is limited and unclear. Users may attempt to drop unsupported file types without understanding what is allowed or where the files are stored.



Originally, Patient Files was intended mainly for IMEKA report PDFs. The dashboard should now support a broader patient file workflow for case-related documents.



Expected Behavior



Patient Files should support uploading approved patient-related documents such as:



\* IMEKA reports

\* Invoices

\* Patient forms

\* Supporting documentation

\* Other approved file types as defined by system configuration



The upload area should clearly indicate what file types are allowed.



Examples:



\* "Drop patient files here"

\* "Allowed file types: PDF, DOCX, XLSX, CSV, JPG, PNG"

\* "Maximum file size: \[configured limit]"



Validation Requirements



If a user attempts to upload an unsupported file type, the system should display a clear error message explaining why the upload failed.



The system should validate:



\* File type

\* File size

\* Case association

\* Storage success



Storage and Access



Uploaded files should be stored in the dashboard patient file storage location and remain accessible from the Patient Files section of the patient dialog.



Users should be able to:



\* Upload files

\* View supported file types in browser when possible

\* Download files

\* See file metadata such as original filename, file type, upload date, and uploading user



Goal



Make Patient Files a clear and reliable case document area instead of a PDF-only upload control with unclear messaging.



