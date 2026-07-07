Issue



Cases currently follow a one-direction workflow and cannot be moved back to a previous stage when corrections, missing information, or workflow mistakes are discovered.



Operationally, there are situations where a case may need to be returned from Completed to Processing, Processed to Upcoming, or any other workflow state to correct data, repeat processing, address errors, or re-enter the normal workflow.



Expected Behavior



Authorized users should be able to move a case to any workflow state regardless of its current location.



Supported destinations should include:



\* Upcoming

\* Processing

\* Processed

\* Completed

\* Error



Workflow movement should not be restricted to moving only one step backward or forward.



Audit Requirements



Every workflow state change must create an audit log entry including:



\* User performing the action

\* Previous workflow state

\* New workflow state

\* Date and time of the change

\* Optional reason or notes provided by the user



This audit history should remain visible within the patient activity log.



Open Design Consideration



Workflow rollback introduces questions regarding workflow timestamps such as Acquired Date, Processing Date, Processed Date, and Completed Date.



Historical workflow dates should be preserved until a formal rollback timestamp policy is defined. Future implementation may require distinguishing between original workflow dates and current workflow state dates.



