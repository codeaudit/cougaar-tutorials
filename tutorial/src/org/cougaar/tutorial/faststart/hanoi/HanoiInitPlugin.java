package org.cougaar.tutorial.faststart.hanoi;

/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

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
 * @version $Id: HanoiInitPlugin.java,v 1.3 2003-01-23 22:12:55 mbarger Exp $
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
