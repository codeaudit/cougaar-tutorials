		Computer Asset Creator Fast Start Package

This tutorial is intended to provide a small demonstration of ALP assets
and properties : how they are created and maintained in the software
development process, and how they are used in running software.

This package should contain everything you need to run the Computer Asset single agent
and multi-agent examples.  To run the single agent example, do the following...

1) Unzip into a working directory.
	For NT, if the working directory is c:\opt, then the 'unzip will create  		
		c:\opt\cougaar\bin, c:\opt\cougaar\lib and c:\opt\cougaar\src
	For LINUX/UNIX, if the working directory is /opt, then the 'unzip' will create
 		/opt/cougaar/bin, /opt/cougaar/lib and /opt/cougaar/src

2) Open a terminal window (Command Prompt Window on NT, xterm on LINUX/UNIX)

3) In the terminal window, set the environment ALP_INSTALL_PATH to the cougaar 
	directory, e.g. 
	for NT use:		set ALP_INSTALL_PATH=c:\opt\cougaar
	for LINUX/UNIX use:	set ALP_INSTALL_PATH=/opt/cougaar

4) In the terminal window, change directory to 'bin':
	cd c:\opt\cougaar\bin (on NT)
	cd /opt/cougaar/bin (on LINUX/UNIX)

5) In the terminal window, execute the Run script on the ComputerNode configuration file
	Run ComputerNode 
	** do not include the '.ini' on ComputerNode

You should see the node start up, then read the Computer agent configuration file, then load  
and initialize the domain plugins. As the agent is starting up, it will load a set of 
properties and assets relating to computers and computer peripherals.  Once loaded, the 
plugins will instantiate variants of the computers with appropriate property sets.  Another 
plugIn will allocate computers, in the form of orders, which will be printed to the 
screen as they are created and allocated.
The agent will continue to run in the window waiting for the next 'problem' to appear.  
Hit Ctrl-C to stop the agent and shut down the node.

Users should look at the ComputerNode.ini config file.  This files simply says that the 
node manager should create one agent, and use the Computer.ini file to configure that 
agent.  The Computer.ini config file lists the domain plugins that should be loaded in the 
agent to give it the 'behaviors' desired.  In this case, we are giving it the 
behaviors necessary to post and solve the Computer problem.  

The computer society consists of a single node with a single cluster, Computer.
The cluster creates computer assets and prints them out. It consists of two
plugins:
	ComputerLDMPlugIn : Creates computer Assets
	ComputerAssetListerPlugIn : Prints new Assets out to stdout
	PlanServerPlugIn - Standard ALP PlugIn to view log plan using a web 
                browser

These agents also load the plan server plugin which will allow you to 
drill into the live plan for the agent through your web browser.
Be sure to check out the planserver web inteface to the resulting plan:
	http://localhost:5555/alpine/demo/TASKS.PSP
Hit Search...drill around.  View the detailed log plan elements as XML.
You can look at Assets, Tasks, PlanElements and UniqueObjects.
Be sure to click on the tasks and see them expanded in the frame on the left.

To quit the demonstration, type Control-C in the Computer node window.
