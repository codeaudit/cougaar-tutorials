COUGAAR Tutorial Problem - Calendar Scheduler

This tutorial contains a simple calendar scheduler problem intended
to show COUGAAR approaches towards execution monitoring and dynamic replanning.

The tutorial society consists of two clusters in a single node, 
CalendarRequester and CalendarManager, which request scheduling of events 
and manage the scheduling of events respectively. When running, the society 
pops up two GUI buttons to allow for creating new tasks, as well as 
creating vacations, which in turn force the requester to replan his scheduled
task.

The clusters contain the following plugins:
CalendarRequester:
	CalendarRequesterPlugIn : Create requests based on UI response
	CalendarAllocatorPlugIn : Allocate new request tasks to Manager cluster

CalendarManager:
	CalendarManagerPlugIn : Attempts to schedule all request tasks
	CalendarAssessorPlugIn : Notices inconsistences in schedule 
		and allocation structure and forces replanning
	CalendarPerturbingPlugIn : Grabs vacation from some scheduled time
		based on UI response
	TutorialHookupPlugIn - An example PlugIn that establishes inter-cluster 
                support relations

both:
	PlanServerPlugIn - Standard COUGAAR PlugIn to view log plan using a web 
                browser


Before running the society, set the environment variable COUGAAR_INSTALL_PATH
to the directory where the COUGAAR release was unpacked.  The COUGAAR_INSTALL_PATH
directory should contain the subdirectories "lib" and "doc" among others.

To run the tutorial, type '..\bin\Run CalendarNode' from the configs directory. 
The batch file will start the Calendar node.  Click on the UI buttons to create 
tasks and force replanning of those tasks due to 'execution' changes to 
availability due to vacation. The output stream of the process contains debug 
statements indicating the state of the calendar 'datebook' and the 
need to replan tasks.

To quit the demonstration, type Control-C in the Calendar node window.

