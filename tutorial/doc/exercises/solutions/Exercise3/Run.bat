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


if "%COUGAAR_INSTALL_PATH%"=="" goto AIP_ERROR
if "%1"=="" goto ARG_ERROR

set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\bootstrap.jar

REM pass in "NodeName" to run a specific named Node

set MYPROPERTIES= -Dorg.cougaar.system.path=%COUGAAR_INSTALL_PATH%\sys -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -Xbootclasspath/p:%COUGAAR_INSTALL_PATH%\lib\javaiopatch.jar

set MYMEMORY=
set MYCLASSES=org.cougaar.bootstrap.Bootstrapper org.cougaar.core.node.Node
set MYARGUMENTS= -c -n "%1"


@ECHO ON

java.exe %MYPROPERTIES% %MYMEMORY% -classpath %LIBPATHS% %MYCLASSES% %MYARGUMENTS% %2 %3
goto QUIT

:AIP_ERROR
echo Please set COUGAAR_INSTALL_PATH
goto QUIT

:ARG_ERROR
echo Run requires an argument  eg: Run ExerciseOneNode
goto QUIT

:QUIT
