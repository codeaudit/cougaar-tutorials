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
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.pizza.Constants;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.*;
import org.cougaar.planning.ldm.asset.Entity;
import org.cougaar.planning.ldm.asset.EntityPG;

import java.util.*;

/**
 * Plugin for ordering pizzas -- more documentation later
 */
public class PlaceOrderPlugin extends ComponentPlugin {
  private LoggingService logger;
  private DomainService domainService;
  private IncrementalSubscription selfOrgSub;
  private IncrementalSubscription inviteSub;
  private IncrementalSubscription taskSub;
  private IncrementalSubscription allocationSub;
  private Entity selfOrg = null;

  public void load() {
    super.load();
    logger = getLoggingService(this);
  }

  private LoggingService getLoggingService(Object requestor) {
    return (LoggingService) getServiceBroker().getService(requestor, LoggingService.class, null);
  }

  protected void setupSubscriptions() {
    domainService = getDomainService();
    selfOrgSub = (IncrementalSubscription) blackboard.subscribe(selfOrgPred);
    inviteSub = (IncrementalSubscription) blackboard.subscribe(inviteListPred);
    allocationSub = (IncrementalSubscription) blackboard.subscribe(allocationPred);

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
    if (! selfOrgSub.getAddedCollection().isEmpty()) {
      selfOrg = (Entity) selfOrgSub.getAddedList().nextElement();
    } else {
      //cannot do anything until our self org is set
      return;
    }


    if (inviteSub.getAddedList().hasMoreElements()) {

    }

    makeFindProvidersTask();
    createOrderTaskAndExpand();
    allocateTasks();

  }

  private void allocateTasks() {
    //To change body of created methods use File | Settings | File Templates.
  }

  private void createOrderTaskAndExpand() {
    //To change body of created methods use File | Settings | File Templates.
  }

  private PlanningFactory getPlanningFactory() {
    PlanningFactory factory = null;
    if (domainService != null) {
      factory = (PlanningFactory) domainService.getFactory("planning");
    }
    return factory;
  }

  private Task makeFindProvidersTask() {
    /** That's fine. I was going to propose the following structure for FindProviders task -
     verb == FindProviders
     AS prep phrase
     indirect object == required role
     direct object == self entity (planning level version of the self org)
     */
    PlanningFactory planningFactory = getPlanningFactory();
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(Verb.get(Constants.FIND_PROVIDERS));
    newTask.setPlan(planningFactory.getRealityPlan());
    newTask.setDirectObject(selfOrg);
    NewPrepositionalPhrase pp = getPlanningFactory().newPrepositionalPhrase();
    pp.setPreposition("AS");
    pp.setIndirectObject(Role.getRole(Constants.PIZZA_PROVIDER));
    newTask.setPrepositionalPhrases(pp);
    return newTask;
  }

  // TODO:  placeholder for now, may need to change significantly
  private Task makeTask() {
    /** That's fine. I was going to propose the following structure for FindProviders task -
     verb == FindProviders
     AS prep phrase
     indirect object == required role
     direct object == self entity (planning level version of the self org)
     */
    PlanningFactory planningFactory = getPlanningFactory();
    NewTask newTask = planningFactory.newTask();
    newTask.setVerb(Verb.get(Constants.ORDER));
    newTask.setPlan(planningFactory.getRealityPlan());
    // TODO: update to real pizza asset
    newTask.setDirectObject(planningFactory.createAsset("Pizza"));
    return newTask;
  }



  public Collection getProviderOrgAssets() {
    TimeSpan timeSpan = TimeSpans.getSpan(TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE);
    RelationshipSchedule relSched = selfOrg.getRelationshipSchedule();
    Collection relationships = relSched.getMatchingRelationships(Role.getRole(Constants.PIZZA_PROVIDER), timeSpan);
    ArrayList providers = new ArrayList();
    for (Iterator iterator = relationships.iterator(); iterator.hasNext();) {
      Relationship r = (Relationship) iterator.next();
      providers.add(relSched.getOther(r));
    }
    return providers;
  }

  private static UnaryPredicate selfOrgPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Entity) {
        return ((Entity) o).isSelf();
      }
      return false;
    }
  };

  /**
   * A predicate that filters for InviteList objects
   */
  private static UnaryPredicate inviteListPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      //return (o instanceof InviteList);
      return false;
    }
  };
  /**
   * A predicate that filters for allocations of "ORDER" tasks
   */
  private static UnaryPredicate allocationPred = new UnaryPredicate (){
    public boolean execute(Object o) {
      if (o instanceof Allocation) {
        Task t = ((Allocation)o).getTask();
        if (t != null) {
          return t.getVerb().equals(Verb.get(Constants.ORDER));
        }
      }
      return false;
    }
  };
}
