package org.cougaar.tutorial.faststart.hanoi;

/**
  * Copyright 1997-1999 Defense Advanced Research Project Agency 
  * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
  * This software to be used in accordance with the COUGAAR license agreement.
**/

import java.util.*;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.tutorial.faststart.*;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.*;

/**
 * Plugin to initialize the Hanoi processing by creating
 * an initial task based on number of disks read from command line
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: HanoiInitPlugin.java,v 1.2 2002-11-19 17:33:04 twright Exp $
 **/
public class HanoiInitPlugin extends ComponentPlugin
{
  // The domainService acts as a provider of domain factory services
  private DomainService domainService = null;
  private PlanningFactory ldmf = null;

  /**
   * Used by the binding utility through reflection to set my DomainService
   */
  public void setDomainService(DomainService aDomainService) {
    domainService = aDomainService;
  }

  /**
   * Used by the binding utility through reflection to get my DomainService
   */
  public DomainService getDomainService() {
    return domainService;
  }

  /**
   * Initialization code : get the count of disks for the problem
   * from the command line (default = 0) and publish a new task to
   * MANAGE the moving of those disks from pole 1 to pole2.
   **/
  public void setupSubscriptions() {
    System.out.println("HanoiInitPlugin::setupSubscriptions");
    ldmf = (PlanningFactory) getDomainService().getFactory("planning");
    if (ldmf == null) {
      throw new RuntimeException(
          "Unable to find \"planning\" domain factory");
    }

    int count = TutorialUtils.getNumericParameter(getParameters().iterator(), 0);

    // Publish a single task to manage the moving 'count' disks from 1 to 2
    getBlackboardService().publishAdd(HanoiUtils.createNewTask(1, 2, count,
					0.0, 1000000.0, // start and end times
					HanoiUtils.MANAGE_VERB, null, ldmf));
  }

  // Nothing required here
  public void execute() { }
}
