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


# Regenerate and recompile all property/asset files
if [ -a makeassets.sh ]; then
  chmod +x ./makeassets.sh
  ./makeassets.sh;
fi

# compile the code
#if not exist bin mkdir bin
LIBPATHS="$COUGAAR_INSTALL_PATH/lib/bootstrap.jar"
LIBPATHS="$LIBPATHS:$COUGAAR_INSTALL_PATH/lib/core.jar"
LIBPATHS="$LIBPATHS:$COUGAAR_INSTALL_PATH/lib/util.jar"
LIBPATHS="$LIBPATHS:$COUGAAR_INSTALL_PATH/lib/build.jar"
LIBPATHS="$LIBPATHS:$COUGAAR_INSTALL_PATH/lib/glm.jar"
LIBPATHS="$LIBPATHS:$COUGAAR_INSTALL_PATH/sys/servlet.jar"

FILES=tutorial/*.java
if [ -d tutorial/assets ]; then
FILES="$FILES tutorial/assets/*.java";
fi

mkdir -p bin/tutorial/asset
javac -deprecation -d bin -classpath $LIBPATHS $FILES

if [ $? = 0 ]; then
jar cf $COUGAAR_INSTALL_PATH/sys/course.jar -C bin ./tutorial;
fi
