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

# Script to compile tutorial project

# display usage info
if [ -z $1 ] || [ $1 = --help ]; then
  cat << EOF
Usage:  $0 [-v] [NUMBER]

  -v        verbose output
  NUMBER    an exercise number (1 to 11)

Compile exercise source from:
  \$CIP/exercises
Creates a new jar:
  \$CIP/lib/exercise\${NUMBER}.jar

Note that the solutions in "src" are compiled by the
"build.xml" ANT script.

Example:
  # compile Exercise1 to \$CIP/lib/exercise1.jar
  $0 1
EOF
  exit -1
fi

# take VERBOSE flag
VERBOSE=
if [ "$1" = "-v" ]; then
  VERBOSE=1
  shift 1
fi

# take exercise number
i=$1
shift 1

# make sure the $CIP is set
if [ -z $CIP ]; then
  CIP=$COUGAAR_INSTALL_PATH
  if [ -z $CIP ]; then
    echo "\$COUGAAR_INSTALL_PATH not set"
    exit -1
  fi
fi

# set config options
JAR="$CIP/lib/exercise${i}.jar"
BASE="$CIP/tutorial/exercises/org/cougaar/tutorial"
ASSET="$BASE/assets"
SRC="$BASE/exercise${i}"
TMP="$CIP/tutorial/tmp/exercise${i}"
ASSET_DEF=$ASSET/programmer_assets.def
PG_DEF=$ASSET/properties.def

if [ ! -d $SRC ]; then
  echo "Unknown exercise number: ${i}"
  exit -1
fi

# create property_group files
COMMAND="\
java \
  -classpath \
  $CIP/lib/core.jar:$CIP/clib/build.jar \
  org.cougaar.tools.build.PGWriter\
  $PG_DEF"
if [ ! -z $VERBOSE ]; then
  echo $COMMAND
fi
cd $SRC || exit -1
$COMMAND || exit -1
cd -

# create asset files
COMMAND="\
java \
  -classpath \
  $CIP/lib/core.jar:$CIP/clib/build.jar \
  org.cougaar.tools.build.AssetWriter\
  $PG_DEF \
  -Ptutorial.assets \
  $ASSET_DEF"
if [ ! -z $VERBOSE ]; then
  echo $COMMAND
fi
cd $SRC || exit -1
$COMMAND || exit -1
cd -

# create temp directory for class files
if [ -d $TMP ]; then
  rm -rf $TMP || exit -1
fi
mkdir -p $TMP || exit -1

# compile the code
LIBPATHS="$CIP/lib/bootstrap.jar"
LIBPATHS="$LIBPATHS:$CIP/lib/core.jar"
LIBPATHS="$LIBPATHS:$CIP/lib/planning.jar"
LIBPATHS="$LIBPATHS:$CIP/lib/util.jar"
LIBPATHS="$LIBPATHS:$CIP/clib/build.jar"
LIBPATHS="$LIBPATHS:$CIP/lib/glm.jar"
LIBPATHS="$LIBPATHS:$CIP/sys/servlet.jar"
COMMAND="\
javac \
  -deprecation \
  -d $TMP \
  -classpath $LIBPATHS \
  $ASSET/*.java \
  $SRC/*.java"
if [ ! -z $VERBOSE ]; then
  echo $COMMAND
fi
$COMMAND || exit -1

# create jar
COMMAND="\
jar \
  cf $JAR \
  -C $TMP \
  org"
if [ ! -z $VERBOSE ]; then
  echo $COMMAND
fi
$COMMAND || exit -1

if [ -e $CIP/lib/tutorial.jar ]; then
  echo "Warning: remove \$CIP/lib/tutorial.jar"
fi

echo "Successfully compiled \$CIP/lib/exercise${i}.jar"
