/*
 * <copyright>
 *
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 *
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * </copyright>
 */
package org.cougaar.pizza.plugin;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.pizza.Constants;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.util.UnaryPredicate;

/**
 * Plugin for ordering pizzas -- more documentation later
 */
public class PlaceOrderPlugin extends ComponentPlugin {
  private LoggingService logger;
  private DomainService domainService;

  public void load() {
    super.load();
    logger = getLoggingService(this);
  }

  private LoggingService getLoggingService(Object requestor) {
    return (LoggingService) getServiceBroker().getService(requestor, LoggingService.class, null);
  }

  protected void setupSubscriptions() {
    domainService = getDomainService();
  }
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

  protected void execute() {
  }

  private PlanningFactory getPlanningFactory() {
    PlanningFactory factory = null;
    if (domainService != null) {
      factory = (PlanningFactory) domainService.getFactory("planning");
    }
    return factory;
  }

  // TODO:  placeholder for now, may need to change significantly
  private Task makeTask() {
    PlanningFactory planningFactory = getPlanningFactory();
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(Verb.get(Constants.ORDER));
    newTask.setPlan(planningFactory.getRealityPlan());
    // TODO: update to real pizza asset
    newTask.setDirectObject(planningFactory.createAsset("Pizza"));
    return newTask;
  }

  /**
   * A predicate that filters for InviteList objects
   */
  static class TaskPredicate implements UnaryPredicate{
    public boolean execute(Object o) {
      //return (o instanceof InviteList);
      return false;
    }

    /**
     * A predicate that filters for allocations of "ORDER" tasks
     */
    static class AllocationPredicate implements UnaryPredicate{
      public boolean execute(Object o) {
        if (o instanceof Allocation) {
          Task t = ((Allocation)o).getTask();
          if (t != null) {
            return t.getVerb().equals(Verb.get(Constants.ORDER));
          }
        }
        return false;
      }
    }
  }
}