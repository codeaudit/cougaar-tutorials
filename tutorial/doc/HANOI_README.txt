COUGAAR Tutorial Problem - Towers of Hanoi Algorithm

This tutorial implements a the 'Towers of Hanoi' algorithm in the ALP
infrastructure. The problem assumes three poles, and a set of ordered disks
on one pole. The goal is to devise a plan to move the disks from this pole
to another without ever putting a currently lower disk on top of a currently
higher one. There is a classic recursive solution to this problem given by
	Move(Pole1, Pole2, NUM_DISKS):
		if (NUM_DISKS = 1) 
			Move it from Pole1 to Pole2
		else
			Move(Pole1, Pole3, NUM_DISKS-1);
			Move(Pole1, Pole1, 1);
			Move(Pole3, Pole2, NUM_DISKS-1);

This society consists of a single cluster, Hanoi, containing the following
PlugIns:

	HanoiInitPlugIn - Takes an argument of the numbers of disks, and
		creates an initial task to move the disks
	HanoiPlugIn - Implements the above algorithm by expansion 
		and allocation
	HanoiMoverPlugIn - Manages the actual disk moves (when the 
		algorithm is down to NUM_DISKS = 1);
	PlanServerPlugIn - Standard COUGAAR PlugIn to view log plan using a web 
                browser

The society is configured with a PlanServerPlugin to 
allow for interactive viewing of the generated log plan.

Before running the society, set the environment variable COUGAAR_INSTALL_PATH
to the directory where the COUGAAR release was unpacked.  The COUGAAR_INSTALL_PATH
directory should contain the subdirectories "lib" and "doc" among others.
	
To run the society, type '..\bin\Run HanoiNode' from the configs directory. 
The batch file will start the HanoiNode.  There will be some debug output 
showing the movement of the disks.

To view the task expansion and allocations open this URL in a web browser: 
  http://localhost:5555/alpine/demo/TASKS.PSP
The web page allows viewing and navigating through the tasks that were 
generated in the plan.  The task named Hanoi/1 is the root of the task tree.
[NOTE: The TASKS.PSP will be provided in a later release]

To quit the demonstration, type Control-C in the Hanoi node window.

In the comments in the Java files, references are made to different
'Levels'. These refer to levels of complexity of the modelling of the problem
within ALP. 
	Level 1 - Base algorithm : Expand tasks recursively
	Level 2 - Add mover allocation capability
	Level 3 - Add start/end time preferences to the tasks
	Level 4 - Return allocation responses indicating perfect satisfaction
	Level 5 - Add time-wise constraints on workflows to enforce
				time-ordering of subtasks

This demo also includes a plan service provider (PSP) that shows the planned 
activity of the mover asset.  PSP_HanoiResults.java is an example of how a PSP
can respond to user requests by querying the log plan and writing HTML to the 
client.  To view the plan for the mover asset, open this URL in a web browser:
  http://localhost:5555/alpine/demo/HANOI.PSP
The mapping between the PSP class and the URL is established in hanoi.psps.xml
in this directory.
[NOTE: The HANOI.PSP will be provided in a later release]

