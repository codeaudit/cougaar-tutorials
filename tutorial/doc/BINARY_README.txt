COUGAAR Tutorial Problem - Binary Search Algorithm

This tutorial implements a binary search algorithm in the COUGAAR infrastructure.
The tutorial presents two different node/cluster models for the solution. 

In the first, a single cluster, BinarySimple runs which contains the following
Plugins:
	BinaryIteratorPlugin - Iterates on preferences to converge to solution
	BinaryResponderPlugin - Has solution value and responds to iterative
		'guesses'
	UIPlugin - Standard COUGAAR Plugin to view log plan

In the second, we demonstrate what is involved in changing such a configuration
to a multi-cluster society. This second society contains two clsuters, 
Binary and BinaryResponder with the following Plugins:
Binary:
	BinaryIteratorPlugin - Iterates on preferences to converge to solution
	BinaryDispatcherPlugin - Sends 'MANAGE' tasks to BinaryResponder
		support cluster
BinaryResponder:
	BinaryResponderPlugin - Has solution value and responds to iterative
		'guesses'
	TutorialHookupPlugin - An example Plugin that establishes inter-cluster 
                support relations

both:
	PlanServerPlugin - Standard COUGAAR Plugin to view log plan using a web 
                browser

Before running the society, set the environment variable COUGAAR_INSTALL_PATH
to the directory where the COUGAAR release was unpacked.  The COUGAAR_INSTALL_PATH
directory should contain the subdirectories "lib" and "doc" among others.

To run the simple society, go to the configs directory and type 
'..\bin\Run BinarySimple'.  The batch file will start the single-cluster 
BinarySimple node. The output will show the convergance of the algorithm by 
iterative refinement of preferences between the plugins. 

To run the multi-cluster society, go to the configs directory and type 
'..\bin\Run Binary'. The same output will appear, showing the same dialog between 
the plugins, but this time these plugins are resident in different clusters.

To quit the demonstration, type Control-C in the Binary or BinarySimple node window.



