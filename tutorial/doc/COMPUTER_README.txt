COUGAAR Tutorial Problem - Computer Asset Creator

This tutorial is intended to provide a small demonstration of COUGAAR assets
and properties : how they are created and maintained in the software
development process, and how they are used in running software.

The computer society consists of a single node with a single cluster, Computer.
The cluster creates computer assets and prints them out. It consists of two
plugins:
	ComputerLDMPlugin : Creates computer Assets
	ComputerAssetListerPlugin : Prints new Assets out to stdout
	PlanServerPlugin - Standard COUGAAR Plugin to view log plan using a web 
                browser

Before running the society, set the environment variable COUGAAR_INSTALL_PATH
to the directory where the COUGAAR release was unpacked.  The COUGAAR_INSTALL_PATH
directory should contain the subdirectories "lib" and "doc" among others.

To run the tutorial, type '..\bin\Run ComputerNode' from the configs directory. The
standard output should reflect correct asset contents. Additionally, 
the debug UI in %COUGAAR_INSTALL_PATH%\bin\Debug can be used to view 
a tree/attribute view of the assets. [NOTE: The debug UI will be provided
in a later release]

To quit the demonstration, type Control-C in the Computer node window.

