#! /bin/sh

# "<copyright>"
# " Copyright 2001-2003 BBNT Solutions, LLC"
# " under sponsorship of the Defense Advanced Research Projects Agency (DARPA)."
# ""
# " This program is free software; you can redistribute it and/or modify"
# " it under the terms of the Cougaar Open Source License as published by"
# " DARPA on the Cougaar Open Source Website (www.cougaar.org)."
# ""
# " THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS"
# " PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR"
# " IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF"
# " MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT"
# " ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT"
# " HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL"
# " DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,"
# " TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR"
# " PERFORMANCE OF THE COUGAAR SOFTWARE."
# "</copyright>"

# Script to run an exercise

if [ -z $1 ] || [ $1 = --help ]; then
  cat << EOF
Usage:  $0 [-f] [-v] [-b] [NUMBER]

  -f        force use of "execise#.jar"
  -v        verbose output
  -b        use jar bootstrapper
  NUMBER    an exercise number (1 to 11)

Runs the numbered exercise.

Example:
  # run exercise 1
  $0 1
EOF
  exit -1
fi

FORCE=
if [ "$1" = "-f" ]; then
  FORCE=1
  shift 1
fi

VERBOSE=
if [ "$1" = "-v" ]; then
  VERBOSE=1
  shift 1
fi

BOOTSTRAP=
if [ "$1" = "-b" ]; then
  BOOTSTRAP=1
  shift 1
fi

i=$1
shift 1

if [ -z $CIP ]; then
  CIP=$COUGAAR_INSTALL_PATH
  if [ -z $CIP ]; then
    echo "\$COUGAAR_INSTALL_PATH not set"
    exit -1
  fi
fi

DIR="$CIP/configs/exercises/exercise${i}"
if [ ! -d $DIR ]; then
  echo "Unknown exercise: $i"
  echo "Directory not found: $DIR"
  exit -1
fi

JAR="$CIP/lib/exercise${i}.jar"
if [ ! -e $JAR ]; then
  SOLJAR="$CIP/lib/tutorial.jar"
  if [ -e $SOLJAR ]; then
    echo "Tutorial exercise build not found: $JAR"
    if [ -z $FORCE ]; then
      echo "Using tutorial solutions: $SOLJAR"
      JAR=$SOLJAR
    else
      echo "Please run: build.sh ${i}"
      exit -1
    fi
  else
    echo "Unknown exercise $i"
    echo "Jar not found: $JAR"
    exit -1
  fi
fi

NODE="Exercise${i}Node"
if [ ! -e $DIR/${NODE}.ini ]; then
  echo "Unable to find node script: $DIR/${NODE}.ini"
  exit -1
fi

LOG4J=log.props
LOG4J_PROP=
if [ -e $DIR/$LOG ] ||
  [ -e $CIP/configs/common/$LOG ]; then
  LOG4J_PROP="-Dorg.cougaar.util.log.config=$LOG4J"
fi

if [ -z $BOOTSTRAP ]; then
  LIBPATHS="$CIP/lib/bootstrap.jar"
  LIBPATHS="$LIBPATHS:$CIP/lib/core.jar"
  LIBPATHS="$LIBPATHS:$CIP/lib/community.jar"
  LIBPATHS="$LIBPATHS:$CIP/lib/glm.jar"
  LIBPATHS="$LIBPATHS:$CIP/lib/planning.jar"
  LIBPATHS="$LIBPATHS:$CIP/lib/util.jar"
  LIBPATHS="$LIBPATHS:$CIP/lib/webserver.jar"
  LIBPATHS="$LIBPATHS:$CIP/lib/webtomcat.jar"
  LIBPATHS="$LIBPATHS:$CIP/sys/log4j.jar"
  LIBPATHS="$LIBPATHS:$CIP/sys/servlet.jar"
  LIBPATHS="$LIBPATHS:$CIP/sys/tomcat_40.jar"
  LIBPATHS="$LIBPATHS:$CIP/sys/xml-apis.jar"
  LIBPATHS="$LIBPATHS:$CIP/sys/xercesImpl.jar"
  LIBPATHS="$LIBPATHS:$JAR"
  BOOTCL=
else
  LIBPATHS="$CIP/lib/bootstrap.jar"
  BOOTCL="org.cougaar.bootstrap.Bootstrapper"
fi

COMMAND="\
java \
  -Xbootclasspath/p:$CIP/lib/javaiopatch.jar \
  -classpath $LIBPATHS \
  -Dorg.cougaar.install.path=$CIP \
  -Dorg.cougaar.node.name=$NODE \
  -Dorg.cougaar.config.path=$DIR\;$CIP/configs/common \
  $LOG4J_PROP \
  $BOOTCL \
  org.cougaar.core.node.Node \
  $*"

if [ ! -z $VERBOSE ]; then
  echo $COMMAND
fi

$COMMAND || exit -1
