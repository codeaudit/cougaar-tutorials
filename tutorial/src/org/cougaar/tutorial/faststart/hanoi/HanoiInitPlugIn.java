package org.cougaar.tutorial.faststart.hanoi;

/**
  * Copyright 1997-1999 Defense Advanced Research Project Agency 
  * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
  * This software to be used in accordance with the COUGAAR license agreement.
**/

import java.util.*;
import org.cougaar.domain.planning.ldm.plan.Verb;
import org.cougaar.tutorial.faststart.*;

/**
 * Plugin to initialize the Hanoi processing by creating
 * an initial task based on number of disks read from command line
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: HanoiInitPlugIn.java,v 1.1 2000-12-15 20:19:04 mthome Exp $
 **/
public class HanoiInitPlugIn extends org.cougaar.core.plugin.SimplePlugIn
{
  /**
   * Initialization code : get the count of disks for the problem
   * from the command line (default = 0) and publish a new task to
   * MANAGE the moving of those disks from pole 1 to pole2.
   **/
  public void setupSubscriptions() {
    //    System.out.println("HanoiInitPlugin::setupSubscriptions");

    int count = TutorialUtils.getNumericParameter(getParameters().elements(), 0);

    // Publish a single task to manage the moving 'count' disks from 1 to 2
    publishAdd(HanoiUtils.createNewTask(1, 2, count,
					0.0, 1000000.0, // start and end times
					HanoiUtils.MANAGE_VERB, null, theLDMF));
  }

  // Nothing required here
  public void execute() { }
}
