#!/bin/sh

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

# Script to run tutorial exercise

if [ -z $CIP ]; then
  CIP=$COUGAAR_INSTALL_PATH
  if [ -z $CIP ]; then
    echo "\$COUGAAR_INSTALL_PATH not set"
    exit -1
  fi
fi

SCRIPT=$CIP/tutorial/bin/run.sh
if [ ! -e $SCRIPT ]; then
  echo "Unable to find run script: $SCRIPT"
fi

$SCRIPT -f $*
