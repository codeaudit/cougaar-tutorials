package org.cougaar.tutorial.faststart.binary;

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
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.tutorial.faststart.*;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;

/**
 * Plugin to dispatch tasks from one cluster to another
 * The Dispatcher looks at the allocation and makes sure that reported
 * results are copied to estimated, while the BinaryIterator waits for this
 * copy to take place before proceeding.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: BinaryDispatcherPlugin.java,v 1.5 2003-05-23 19:33:22 dmontana Exp $
 */
public class BinaryDispatcherPlugin extends ComponentPlugin
{

  // Subscribe to organizations that provide the 'BinaryProvider' role
  private IncrementalSubscription allSupportAssets = null;
  private UnaryPredicate allSupportAssetsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return TutorialUtils.isSupplierOrganization(o, BinaryUtils.BinaryServiceRole);
    }
  };

  // Subscription to 'MANAGE' tasks
  private IncrementalSubscription allManageTasks = null;
  private UnaryPredicate allManageTasksPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return TutorialUtils.isTaskWithVerb(o, BinaryUtils.MANAGE_VERB);
    }
  };

  // Subscription to 'MANAGE' Allocations
  private IncrementalSubscription allManageAllocations = null;
  private UnaryPredicate allManageAllocationsPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      return TutorialUtils.isAllocationWithVerb(o, BinaryUtils.MANAGE_VERB);
    }
  };

  private DomainService domainService;
  private PlanningFactory ldmf;
  public void setDomainService(DomainService value) {
    domainService=value;
  }

  public DomainService getDomainService() {
    return domainService;
  }

  /**
   * Initialize plugin by subscribing for support orgs and 
   * 'MANAGE' allocations.
   * NOTE : Wait to subscribe for 'MANAGE' tasks until we have the support
   * organization, so that we don't lose tasks
   **/
  public void setupSubscriptions() {
    //System.out.println("BinaryDispatcherPlugin::setupSubscriptions");
    ldmf = (PlanningFactory) getDomainService().getFactory("planning");
    if (ldmf == null) {
      throw new RuntimeException(
          "Unable to find \"planning\" domain factory");
    }

    // Subscribe to all support organizations
    allSupportAssets = (IncrementalSubscription)getBlackboardService()
       .subscribe(allSupportAssetsPredicate);

    // Subscribe to 'MANAGE' allocations
    allManageAllocations = (IncrementalSubscription)getBlackboardService()
       .subscribe(allManageAllocationsPredicate);
  }

  /**
   * Execute for Dispatcher 
   * Grab organization when it comes in
   * Allocate tasks to supporting cluster
   * Pass allocation results back from supporting cluster
   **/
  public void execute() {

    // Get support organization for allocating
    TutorialOrg mySupportOrganization;

    // System.out.println("BinaryDispatcherPlugin::execute...");

    // Grab the first support asset as the organization to whom we'll be
    // routing tasks
    mySupportOrganization = (TutorialOrg)
      TutorialUtils.getFirstObject(allSupportAssets.getAddedList());

    if ((mySupportOrganization != null) && (allManageTasks == null)) {
      // Subscribe to 'MANAGE' tasks, now that we're ready for them
      // NOTE : We don't have to subscribe from within setupSubscriptions
      allManageTasks = (IncrementalSubscription)getBlackboardService()
	  .subscribe(allManageTasksPredicate);
    }

    if (mySupportOrganization != null) {
      // Allocate all new tasks to support organization

      // Take all tasks and allocate it to the asset
      for(Enumeration e = allManageTasks.getAddedList();e.hasMoreElements();) 
      {
        Task task = (Task)e.nextElement();

        if (task.getPlanElement() == null) {
	    Allocation allocation = ldmf
                .createAllocation(task.getPlan(), task, 
                                  mySupportOrganization, 
                                  null, Role.AVAILABLE);
          //   System.out.println("Allocating task " + task + " to " + mySupportOrganization);
          getBlackboardService().publishAdd(allocation);
        }
      }
    }

    // Copy all reported allocation responses into estimates
    for(Enumeration e = allManageAllocations.getChangedList(); e.hasMoreElements();)
    {
      Allocation allocation = (Allocation)e.nextElement();
      if (allocation.getReportedResult() != allocation.getEstimatedResult()) {
        //	  System.out.println("Copying results to estimates");
        allocation.setEstimatedResult(allocation.getReportedResult());
        getBlackboardService().publishChange(allocation);
      }
    }
  }


}
