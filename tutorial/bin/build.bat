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

REM Script to compile tutorial project

REM display usage info
if [ -z %1 ] || [ %1 = --help ]; then
  cat << EOF
Usage:  build.bat [-v] [NUMBER]

  -v        verbose output
  NUMBER    an exercise number (1 to 11)

Compile exercise source from:
  \%COUGAAR_INSTALL_PATH%\exercises
Creates a new jar:
  \%COUGAAR_INSTALL_PATH%\lib\exercise\${NUMBER}.jar

Note that the solutions in "src" are compiled by the
"build.xml" ANT script.

Example:
  # compile Exercise1 to \%COUGAAR_INSTALL_PATH%\lib\exercise1.jar
  build.bat -jar 1
EOF
  exit -1
fi

REM take VERBOSE flag
SET VERBOSE=
if [ "%1" = "-v" ]; then
  SET VERBOSE=1
  shift 1
fi

REM take exercise number
SET i=%1
shift 1

REM make sure the %COUGAAR_INSTALL_PATH% is set
if [ -z %COUGAAR_INSTALL_PATH% ]; then
  SET CIP=%COUGAAR_INSTALL_PATH%
  if [ -z %COUGAAR_INSTALL_PATH% ]; then
    ECHO COUGAAR_INSTALL_PATH not set
    exit -1
  fi
fi

REM set config options
SET JAR="%COUGAAR_INSTALL_PATH%\lib\exercise%i.jar"
SET BASE="%COUGAAR_INSTALL_PATH%\tutorial\exercises\org\cougaar\tutorial"
SET ASSET="%BASE%\assets"
SET SRC="%BASE%\exercise%i"
SET TMP="%COUGAAR_INSTALL_PATH%\tutorial\tmp\exercise%i"
SET ASSET_DEF=%ASSET%\programmer_assets.def
SET PG_DEF=%ASSET%\properties.def

if [ ! -d $SRC ]; then
  ECHO Unknown exercise number: %i
  exit -1
fi

REM create property_group files
SET COMMAND="\
java \
  -classpath \
  %COUGAAR_INSTALL_PATH%\lib\core.jar:%COUGAAR_INSTALL_PATH%\clib\build.jar \
  org.cougaar.tools.build.PGWriter\
  %PG_DEF%"
if [ ! -z %VERBOSE% ]; then
  echo %COMMAND%
fi
cd $SRC || exit -1
%COMMAND% || exit -1
cd -

REM create asset files
SET COMMAND="\
java \
  -classpath \
  %COUGAAR_INSTALL_PATH%\lib\core.jar:%COUGAAR_INSTALL_PATH%\clib\build.jar \
  org.cougaar.tools.build.AssetWriter\
  %PG_DEF% \
  -Ptutorial.assets \
  %ASSET_DEF%"
if [ ! -z %VERBOSE% ]; then
  echo %COMMAND%
fi
cd $SRC || exit -1
%COMMAND% || exit -1
cd -

REM create temp directory for class files
if [ -d $TMP ]; then
  rm -rf $TMP || exit -1
fi
mkdir -p $TMP || exit -1

REM compile the code
SET LIBPATHS="%COUGAAR_INSTALL_PATH%\lib\bootstrap.jar"
SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\lib\core.jar"
SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\lib\planning.jar"
SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\lib\util.jar"
SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\clib\build.jar"
SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\lib\glm.jar"
SET LIBPATHS="%LIBPATHS%:%COUGAAR_INSTALL_PATH%\sys\servlet.jar"
SET COMMAND="\
javac \
  -deprecation \
  -d %TMP% \
  -classpath %LIBPATHS% \
  %ASSET%\*.java \
  %SRC%\*.java"
if [ ! -z %VERBOSE% ]; then
  echo %COMMAND%
fi
%COMMAND% || exit -1

REM create jar
SET COMMAND="\
jar \
  cf %JAR% \
  -C %TMP% \
  org"
if [ ! -z %VERBOSE% ]; then
  echo %COMMAND%
fi
%COMMAND% || exit -1

if [ -e %COUGAAR_INSTALL_PATH%\lib\tutorial.jar ]; then
  ECHO Warning: remove \%COUGAAR_INSTALL_PATH%\lib\tutorial.jar
fi

ECHO Successfully compiled \%COUGAAR_INSTALL_PATH%\lib\exercise%i.jar
