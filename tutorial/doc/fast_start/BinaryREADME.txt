		Simple Binary Search Fast Start Package

This package should contain everything you need to run the Binary Search single agent
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

5) In the terminal window, execute the Run script on the BinarySimpleNode configuration file
	Run BinarySimpleNode 
	** do not include the '.ini' on BinarySimpleNode

You should see the node start up, then read the Binary agent configuration file, then load  
and initialize the domain plugins. When the agent enters the run state, you will see a number
selected...this will be the target number the binary search is looking for.

The agent will continue to run in the window waiting for the next 'problem' to appear.  
Hit Ctrl-C to stop the agent and shut down the node.

How is it constructed?

This tutorial implements a binary search algorithm in the COUGAAR infrastructure.

The tutorial presents two different node/agent models for the solution. 
In the first, a single agent, BinarySimple runs which contains the following
Plugins:

	BinaryIteratorPlugin - Iterates on preferences to converge to solution
	BinaryResponderPlugin - Has solution value and responds to iterative 'guesses'
	UIPlugin - Standard COUGAAR Plugin to view log plan

In the second, we demonstrate what is involved in changing such a configuration
to a multi-agent society. This second society contains two clsuters, 
Binary and BinaryResponder with the following Plugins:

Binary:
	BinaryIteratorPlugin - Iterates on preferences to converge to solution
	BinaryDispatcherPlugin - Sends 'MANAGE' tasks to BinaryResponder
		support agent
BinaryResponder:
	BinaryResponderPlugin - Has solution value and responds to iterative 'guesses'
	TutorialHookupPlugin - An example Plugin that establishes inter-agent 
                support relations
both:
        SimpleServletComponent - Standard COUGAAR Component that here loads the
                PlanViewServlet to allow viewing the Blackboard

What to look for?

Users should look at the BinarySimpleNode.ini config file.  This files simply says that the 
node manager should create one agent, and use the BinarySimple.ini file to configure that 
agent.  The BinarySimple.ini config file lists the domain plugins that should be loaded in 
the agent to give it the 'behaviors' desired.  In this case, we are giving it the 
behaviors necessary to pick a number then guess that number using a binary search technique.

See the Hanoi package readme for more information on how to read the ini files.

Now you are ready to try the multi-agent version of the same problem.  In this version, 
follow the same instructions to start the node except use BinaryNode.ini instead of 
BinarySimpleNode.ini.  This file contains the following...

	cluster = Binary
	cluster = BinaryResponder

So it will start two agents.  The first will read Binary.ini for its configuration information
and the second will read BinaryResponder.ini for its configuration.

Binary.ini has in it a few of the plugs in from BinarySimple.ini, with the rest being put in 
BinaryResponder.ini.  You will notice that both .ini files have a SimpleServletComponent.  There 
is also another plugIn called TutorialHookupPlugin in both .ini files.  This plugIn allows 
the two nodes to establish a role relation in order to be able to talk.  

Check out the source code to see how this works.

Be sure to check out the planserver web inteface to the resulting plan:
	http://localhost:8800/$Binary/tasks
You should be able to see how each agent is holding some of the information.  In this case 
a plan is a poor representation, but you get the idea.

