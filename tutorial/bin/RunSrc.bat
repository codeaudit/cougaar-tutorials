@echo OFF

REM "<copyright>"
REM " Copyright 2001-2003 BBNT Solutions, LLC"
REM " under sponsorship of the Defense Advanced Research Projects Agency (DARPA)."
REM ""
REM " This program is free software; you can redistribute it and/or modify"
REM " it under the terms of the Cougaar Open Source License as published by"
REM " DARPA on the Cougaar Open Source Website (www.cougaar.org)."
REM ""
REM " THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS"
REM " PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR"
REM " IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF"
REM " MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT"
REM " ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT"
REM " HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL"
REM " DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,"
REM " TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR"
REM " PERFORMANCE OF THE COUGAAR SOFTWARE."
REM "</copyright>"

REM Script to run tutorials


REM Make sure that COUGAAR_INSTALL_PATH is specified
IF NOT "%COUGAAR_INSTALL_PATH%" == "" GOTO L_2

REM Unable to find cougaar-install-path
ECHO COUGAAR_INSTALL_PATH not set!
GOTO L_END
:L_2

REM IF asked for help or did not supply at least one arg, print help
IF "%1" == "" GOTO L_HELP
IF "%1" == "--help" GOTO L_HELP
IF "%1" == "-?" GOTO L_HELP
GOTO L_3

:L_HELP
ECHO Usage:  run.bat [NUMBER]
ECHO  NUMBER is an exercise number (1 to 11)
ECHO  Runs the numbered exercise.
ECHO  Example:
ECHO  > run exercise 1
ECHO  run.bat 1
GOTO L_END

:L_3
REM Handle the first parameter
SET EX=%1
GOTO L_4

:L_4
REM Define where we are running from
SET CONFIGS=%COUGAAR_INSTALL_PATH%\tutorial\configs\exercises\exercise%EX%;%COUGAAR_INSTALL_PATH%\configs\common
SET NODE="Exercise%EX%Node.ini"
GOTO L_6

:L_6
  set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\bootstrap.jar
  set MYPROPERTIES= -Dorg.cougaar.system.path=%COUGAAR_INSTALL_PATH%\sys -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -Dorg.cougaar.config.path=%CONFIGS% -Dorg.cougaar.core.servlet.enable=true -Dorg.cougaar.lib.web.scanRange=100 -Dorg.cougaar.lib.web.http.port=8800 -Dorg.cougaar.lib.web.https.port=-1 -Dorg.cougaar.lib.web.https.clientAuth=true -Xbootclasspath/p:%COUGAAR_INSTALL_PATH%\lib\javaiopatch.jar
  set MYMEMORY=
  set MYCLASSES=org.cougaar.bootstrap.Bootstrapper org.cougaar.core.node.Node
  set MYARGUMENTS= -c -n 
GOTO L_RUN

:L_RUN

@echo ON
java.exe %MYPROPERTIES% %MYMEMORY% -classpath %LIBPATHS% %MYCLASSES% %MYARGUMENTS% %NODE%


:L_END
