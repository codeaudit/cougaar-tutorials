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
package org.cougaar.tutorial.exercise4;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.core.service.*;
import org.cougaar.planning.ldm.PlanningFactory;

/**
 * This COUGAAR Plugin creates and publishes "CODE" tasks
 */
public class ManagerPlugin extends ComponentPlugin {

  // The domainService acts as a provider of domain factory services
  private DomainService domainService = null;

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

  // Two assets to use as direct objects for the CODE tasks
  private Asset what_to_code, what_else_to_code;

  /**
   * Using setupSubscriptions to create the initial CODE tasks
   */
protected void setupSubscriptions() {
  // Get the PlanningFactory
  PlanningFactory factory = (PlanningFactory)getDomainService().getFactory("planning");
  // Create a task to code the next killer app
  what_to_code = factory.createPrototype("AbstractAsset", "The Next Killer App");
  NewItemIdentificationPG iipg = (NewItemIdentificationPG)factory.createPropertyGroup("ItemIdentificationPG");
  iipg.setItemIdentification("e-somthing");
  what_to_code.setItemIdentificationPG(iipg);
  getBlackboardService().publishAdd(what_to_code);
  getBlackboardService().publishAdd(makeTask(what_to_code));

  // Create a task to code something java
  what_else_to_code = factory.createInstance(what_to_code);
  iipg = (NewItemIdentificationPG)factory.createPropertyGroup("ItemIdentificationPG");
  iipg.setItemIdentification("something java");
  what_else_to_code.setItemIdentificationPG(iipg);
  getBlackboardService().publishAdd(what_else_to_code);
  getBlackboardService().publishAdd(makeTask(what_else_to_code));

  // Create a task to code something java
  what_else_to_code = factory.createInstance(what_to_code);
  iipg = (NewItemIdentificationPG)factory.createPropertyGroup("ItemIdentificationPG");
  iipg.setItemIdentification("something big java");
  what_else_to_code.setItemIdentificationPG(iipg);
  getBlackboardService().publishAdd(what_else_to_code);
  getBlackboardService().publishAdd(makeTask(what_else_to_code));

  // Create a task to code something java
  what_else_to_code = factory.createInstance(what_to_code);
  iipg = (NewItemIdentificationPG)factory.createPropertyGroup("ItemIdentificationPG");
  iipg.setItemIdentification("intelligent java agent");
  what_else_to_code.setItemIdentificationPG(iipg);
  getBlackboardService().publishAdd(what_else_to_code);
  getBlackboardService().publishAdd(makeTask(what_else_to_code));
}


/**
 * This Plugin has no subscriptions so this method does nothing
 */
protected void execute () {
}

/**
 * Create a CODE task.
 * @param what the direct object of the task
 */
protected Task makeTask(Asset what) {
    NewTask new_task = ((PlanningFactory)getDomainService().getFactory("planning")).newTask();

    // Set the verb as given
    new_task.setVerb(Verb.getVerb("CODE"));

    // Set the reality plan for the task
    new_task.setPlan(((PlanningFactory)getDomainService().getFactory("planning")).getRealityPlan());

    new_task.setDirectObject(what);

    return new_task;
}

}
