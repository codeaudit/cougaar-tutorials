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
ECHO Usage:  run.bat [-f] [-v] [-b] [NUMBER]
ECHO
ECHO  -f        force use of "execise#.jar"
ECHO  -v        verbose output
ECHI  -b        use jar bootstrapper
ECHO  NUMBER    an exercise number (1 to 11)
ECHO
ECHO Runs the numbered exercise.
ECHO
ECHOExample:
ECHO  > run exercise 1
ECHO  run.bat 1
GOTO L_END

:L_3
REM Handle the first parameter
SET FORCE=0
SET VERBOSE=0
SET BOOTSTRAP=0
IF "%1" == "-f" GOTO L_F_1
IF "%1" == "-v" GOTO L_V_1
IF "%1" == "-b" GOTO L_B_1

REM If we get here, it means that this arg was not a param. Treat as
REM an exercise number
SET EX=%1
GOTO L_6

:L_F_1
  SET FORCE=1
  GOTO L_4

:L_V_1
  SET VERBOSE=1
  GOTO L_4

:L_B_1
  SET BOOTSTRAP=1

:L_4
REM Handle the second parameter
IF "%2" == "-f" GOTO L_F_2
IF "%2" == "-v" GOTO L_V_2
IF "%2" == "-b" GOTO L_B_2

REM If we get here, it means that this arg was not a param. Treat as
REM an exercise number
SET EX=%2
GOTO L_6

:L_F_2
  SET FORCE=1
  GOTO L_5

:L_V_2
  SET VERBOSE=1
  GOTO L_5

:L_B_2
  SET BOOTSTRAP=1

:L_5
REM Handle the third parameter
IF "%3" == "-f" GOTO L_F_3
IF "%3" == "-v" GOTO L_V_3
IF "%3" == "-b" GOTO L_B_3

REM If we get here, it means that this arg was not a param. Treat as
REM an exercise number
SET EX=%3
GOTO L_7

:L_F_3
  SET FORCE=1
  GOTO L_6

:L_V_3
  SET VERBOSE=1
  GOTO L_6

:L_B_3
  SET BOOTSTRAP=1

:L_6
REM When we get here, it means we have not yet set the exercise number
SET EX=%4

:L_7
REM OK, we have all the parameters and the exercise number

SET DIR="%COUGAAR_INSTALL_PATH%\configs\exercises\exercise%EX%"
if EXISTS %DIR% GOTO :L_8
  echo Unknown exercise: %EX%
  echo Directory not found: %DIR%
  GOTO L_END

:L_8

SET JAR="%COUGAAR_INSTALL_PATH%\lib\exercise%EX%.jar"
if EXISTS %JAR% GOTO L_9
  REM The student per-exercise jar is not there. How about the solutions?
  SET SOLJAR="%COUGAAR_INSTALL_PATH%\lib\tutorial.jar"
  if EXISTS %SOLJAR% GOTO L_10
    REM Do Not have the solutions to use
    echo Could not find pre-built solutions jar %SOLJAR%
    echo And could not find exercise jar %JAR%
    echo Replace the solutions jar, and/or build the exercise jar
    GOTO L_END

:L_10
REM Have the pre-built solutions, not student solutions
    echo Tutorial exercise build not found: %JAR%
    if  "%FORCE%" == "" GOTO L_USE_SOL
      echo Please run: build.bat %EX%
      GOTO L_END

:L_USE_SOL
      echo Using tutorial solutions: %SOLJAR%
      SET JAR=%SOLJAR%

:L_9
REM Have a jar to run from

SET NODE="Exercise%EX%Node"
if EXISTS %DIR%\%NODE%.ini GOTO L_11
  echo Unable to find node script: %DIR%\%NODE%.ini
  GOTO L_END

REM FIXME!!!!
:L_11
SET LOG4J=log.props
SET LOG4J_PROP=
if [ -e %DIR%\%LOG% ] ||
  [ -e %COUGAAR_INSTALL_PATH%\configs\common\%LOG% ]; then
  SET LOG4J_PROP="-Dorg.cougaar.util.log.config=%LOG4J%"
fi

REM END FIXME

:L_SET_PATH
if "%BOOTSTRAP%" == "1" GOTO L_USE_B
  SET LIBPATHS="%COUGAAR_INSTALL_PATH%\lib\bootstrap.jar"
  SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\lib\core.jar"
  SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\lib\community.jar"
  SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\lib\glm.jar"
  SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\lib\planning.jar"
  SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\lib\util.jar"
  SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\lib\webserver.jar"
  SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\lib\webtomcat.jar"
  SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\sys\log4j.jar"
  SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\sys\servlet.jar"
  SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\sys\tomcat_40.jar"
  SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\sys\xml-apis.jar"
  SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\sys\xercesImpl.jar"
  SET LIBPATHS="%LIBPATHS%:%JAR%"
  SET BOOTCL=
  GOTO L_RUN

:L_USE_B
  SET LIBPATHS="%COUGAAR_INSTALL_PATH%\lib\bootstrap.jar"
  SET BOOTCL="org.cougaar.bootstrap.Bootstrapper"


:L_RUN
SET COMMAND="\
java \
  -Xbootclasspath/p:%COUGAAR_INSTALL_PATH%\lib\javaiopatch.jar \
  -classpath %LIBPATHS% \
  -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% \
  -Dorg.cougaar.node.name=%NODE% \
  -Dorg.cougaar.config.path=%DIR%\;%COUGAAR_INSTALL_PATH%\configs\common \
  %LOG4J_PROP% \
  %BOOTCL% \
  org.cougaar.core.node.Node \
  $*"

if "%VERBOSE%" == "1" echo %COMMAND%

%COMMAND% 
@echo ON

:L_END
