Issue



Upcoming cases need a way to be flagged when received imaging is incomplete, incorrect, or otherwise cannot move forward.



Currently, the Upcoming workflow assumes a case is either ready to be marked received or left untouched. There is no clear way to flag an issue, notify the right team, and keep the case visible while waiting for correction.



Expected Behavior



Add an Error button next to Mark Received on the Upcoming page.



When clicked, the Error button should open a dialog requiring an error note. Submitting the error should:



\* Create an active error record for the case

\* Trigger the CASE\_ERROR email notification to the BMS team and Prism/Processing team

\* Add an audit/activity entry with the error note, user, and timestamp

\* Display the patient on the Errors page

\* Keep the patient visible on the Upcoming page

\* Show an Error badge on the Upcoming row to indicate the case is waiting on correction



Error should behave as an overlay condition, not as a replacement workflow state. A patient should be able to be both Upcoming and Error at the same time.



Resolution Behavior



If a case has an active Upcoming error and the user later clicks Mark Received, the system should treat Mark Received as the resolution action.



When Mark Received is clicked on an errored Upcoming case, the system should:



\* Set the images received date/time

\* Set the error resolved date/time

\* Set the error resolved by user

\* Close the active error record

\* Remove the Error badge from Upcoming

\* Remove the patient from the Errors page

\* Move the case forward using the normal Mark Received workflow

\* Add an audit/activity entry noting that the error was resolved when images were marked received



Notes



There should not be a separate Resolved button for this workflow. Mark Received should resolve the active Upcoming error and continue the normal case progression.



