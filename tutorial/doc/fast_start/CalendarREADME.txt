		Single and Multi Agent Calendar Scheduler

This package should contain everything you need to run the Calendar single agent
and multi-agent examples.  To run the single agent example, do the following...

1) Unzip into a working directory.
	For NT, if the working directory is c:\opt, then the 'unzip will create  		
		c:\opt\cougaar\bin, c:\opt\cougaar\lib and c:\opt\cougaar\src
	For LINUX/UNIX, if the working directory is /opt, then the 'unzip' will create
 		/opt/cougaar/bin, /opt/cougaar/lib and /opt/cougaar/src

2) Open a terminal window (Command Prompt Window on NT, xterm on LINUX/UNIX)

3) In the terminal window, set the environment COUGAAR_INSTALL_PATH to the cougaar 
	directory, e.g. 
	for NT use:		set COUGAAR_INSTALL_PATH=c:\opt\cougaar
	for LINUX/UNIX use:	set COUGAAR_INSTALL_PATH=/opt/cougaar

4) In the terminal window, change directory to 'bin':
	cd c:\opt\cougaar\bin (on NT)
	cd /opt/cougaar/bin (on LINUX/UNIX)

5) In the terminal window, execute the Run script on the CalendarSimpleNode configuration file
	Run CalendarSimpleNode 
	** do not include the '.ini' on BinarySimpleNode

You should see the node start up, then read the Calendar agent configuration file, then load  
and initialize the domain plugins. You will see two UI windows pop up.  One is to add appointments
to your calendar, the other to request vacation days.  Press each a couple of times.
You will see that as you add either your calendar is printed out.  If you request an appointment
and there is a conflict, the point will move to the next available day.  If you request 
a vacation day and there is a conflict, the vacation day wins and dynamic replanning of 
the appointment is performed.  

The agent will continue to run in the window waiting for the next appointment to appear.  
Hit Ctrl-C to stop the agent and shut down the node.

To run the multi-agent version, use the CalendarNode.ini config file.

How is it constructed?

This tutorial contains a simple calendar scheduler problem intended
to show Cougaar approaches towards execution monitoring and dynamic replanning.

The tutorial society consists of two clusters in a single node, 
CalendarRequester and CalendarManager, which request scheduling of events 
and manage the scheduling of events respectively. When running, the society 
pops up two GUI buttons to allow for creating new tasks, as well as 
creating vacations, which in turn force the requester to replan his scheduled
task.  Click on the UI buttons to create tasks and force replanning of those tasks due
to 'execution' changes to availability due to vacation. The output stream
of the process contains debug statements indicating the state of the 
calendar 'datebook' and the need to replan tasks.



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
 

Check out the source code to see how this works.

These agents also load the plan server plugin which will allow you to 
drill into the live plan for the agent through your web browser.
Be sure to check out the planserver web inteface to the resulting plan:
	http://localhost:5555/alpine/demo/TASKS.PSP
Hit Search...drill around.  View the detailed log plan elements as XML.
You can look at Assets, Tasks, PlanElements and UniqueObjects.
Be sure to click on the tasks and see them expanded in the frame on the left.

You should be able to see how each agent is holding some of the information.  In this case 
a plan is a poor representation, but you get the idea.  Also, be sure to view the plan, then 
add some appointments and view it again.  Try to understand how and why it changed.  Check
the source code to see what was done in response to adding each appointment.  Now grab
a vacation day and see its effect.
