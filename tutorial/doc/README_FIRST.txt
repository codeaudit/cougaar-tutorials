COUGAAR Tutorial Package

This package contains the COUGAAR tutorial packages. 

This package contains four demonstrations:
	* The Towers of Hanoi (see HANOI_README.txt)
	* A binary search example (see BINARY_README.txt)
	* An example of COUGAAR assets  (see COMPUTER_README.txt)
	* A simple calendar scheduler (see CALENDAR_README.txt)

There are six directories in this package:
	* doc     -- readme files and javadoc for the tutorials
	* bin     -- scripts for running and building the tutorials
	* configs -- COUGAAR configuration files for the tutorials
	* data    -- data files used to build and run the tutorials
	* lib     -- contains a JAR file containing the tutorial class files
	* src     -- the Java source code for the tutorials

To install the tutorials, the tutorial.zip file must be unzipped into a directory
and the COUGAAR core distribution must be available.

Before running or rebuilding the code, set the environment variable COUGAAR_INSTALL_PATH
to the directory where the COUGAAR core release was unpacked.  The COUGAAR_INSTALL_PATH
directory should contain the subdirectories "lib" and "doc" among others.

To recompile the code and remake the tutorial.jar file:
1) Make sure that the java 1.2 compiler and interpreter (javac and java) are in your 
PATH and COUGAAR_INSTALL_PATH is set.
2) In the bin directory, type makeall

See each individual tutorial's README in this directory for running instructions.

