/*
 * <copyright>
 *  Copyright 2002 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
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

package org.cougaar.pizza.plugin;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.util.RoleScorer;
import org.cougaar.planning.ldm.PlanningDomain;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Entity;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.description.ScoredServiceDescription;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceDescription;
import org.cougaar.servicediscovery.description.ServiceInfoScorer;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.TimeSpans;
import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

/**
 * Test implementation -  generates a MMQueryRequest for a spare part service
 */
public class SDClientPlugin extends ComponentPlugin {
  private IncrementalSubscription myLocalEntitySubscription;
  private IncrementalSubscription myMMRequestSubscription;
  private IncrementalSubscription myServiceContractRelaySubscription;
  private IncrementalSubscription myFindProvidersTaskSubscription;

  private LoggingService myLoggingService;
  private DomainService myDomainService;

  private SDFactory mySDFactory;
  private PlanningFactory myPlanningFactory;

  private UnaryPredicate myLocalEntityPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Entity) {
        Entity entity = (Entity) o;
        if (entity.isLocal()) {
          return true;
        }
      }
      return false;
    }
  };

  private UnaryPredicate myServiceContractRelayPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof ServiceContractRelay);
    }
  };


  private UnaryPredicate myMMRequestPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof MMQueryRequest);
    }
  };

  private UnaryPredicate myFindProvidersTaskPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        return ((Task) o).getVerb().equals(Constants.Verbs.FIND_PROVIDERS);
      } else {
        return false;
      }
    }
  };

  private static Calendar myCalendar = Calendar.getInstance();

  private static long DEFAULT_START_TIME = -1;
  private static long DEFAULT_END_TIME = -1;


  static {
    myCalendar.set(1990, 0, 1, 0, 0, 0);
    DEFAULT_START_TIME = myCalendar.getTime().getTime();

    myCalendar.set(2010, 0, 1, 0, 0, 0);
    DEFAULT_END_TIME = myCalendar.getTime().getTime();
  }


  /**
   * Used by the binding utility through reflection to set my DomainService
   */
  public void setDomainService(DomainService domainService) {
    myDomainService = domainService;
  }

  /**
   * Used by the binding utility through reflection to get my DomainService
   */
  public DomainService getDomainService() {
    return myDomainService;
  }

  public void load() {
    super.load();

    myLoggingService = (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);

    mySDFactory = (SDFactory) getDomainService().getFactory(SDDomain.SD_NAME);
    myPlanningFactory = (PlanningFactory) getDomainService().getFactory(PlanningDomain.PLANNING_NAME);
  }

  protected void setupSubscriptions() {
    myLocalEntitySubscription = (IncrementalSubscription) getBlackboardService().subscribe(myLocalEntityPredicate);
    myMMRequestSubscription = (IncrementalSubscription) getBlackboardService().subscribe(myMMRequestPredicate);
    myServiceContractRelaySubscription = (IncrementalSubscription) getBlackboardService().subscribe(myServiceContractRelayPredicate);
    myFindProvidersTaskSubscription = (IncrementalSubscription) getBlackboardService().subscribe(myFindProvidersTaskPredicate);
  }

  public void execute() {
    if (myFindProvidersTaskSubscription.hasChanged()) {
      Collection adds = myFindProvidersTaskSubscription.getAddedCollection();
      for (Iterator addIterator = adds.iterator(); addIterator.hasNext();) {
        Task findProviderTask = (Task) addIterator.next();
        Role taskRole = getRole(findProviderTask);
        myLoggingService.shout("execute: findProviders task = " + findProviderTask + " role = " + taskRole);
        queryServices(taskRole);
      }

      if (myLoggingService.isDebugEnabled()) {
        Collection changes = myFindProvidersTaskSubscription.getChangedCollection();
        Collection removes = myFindProvidersTaskSubscription.getRemovedCollection();
        if (!changes.isEmpty() || !removes.isEmpty()) {
          myLoggingService.debug("execute: ignoring changed/deleted FindProvider tasks - " + " changes = " + changes
                                 + ", removes = " + removes);
        }
      }
    }


    if (myMMRequestSubscription.hasChanged()) {
      for (Iterator iterator = myMMRequestSubscription.getChangedCollection().iterator(); iterator.hasNext();) {
        MMQueryRequest mmRequest = (MMQueryRequest) iterator.next();

        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug("execute: MMQueryRequest has changed." + mmRequest);
        }
        if (mmRequest.getResult() != null) {
          Collection services = mmRequest.getResult();


          // print all results
          if (myLoggingService.isDebugEnabled()) {

            myLoggingService.debug("Results for query " + mmRequest.getQuery().toString());
            for (Iterator serviceIterator = services.iterator(); serviceIterator.hasNext();) {
              ScoredServiceDescription serviceDescription = (ScoredServiceDescription) serviceIterator.next();
              myLoggingService.debug("Score " + serviceDescription.getScore());
              myLoggingService.debug("Provider name " + serviceDescription.getProviderName());
              myLoggingService.debug("*********");
            }
          }

          for (Iterator serviceIterator = services.iterator(); serviceIterator.hasNext();) {
            ScoredServiceDescription serviceDescription = (ScoredServiceDescription) serviceIterator.next();
            if (myLoggingService.isDebugEnabled()) {
              myLoggingService.debug(getAgentIdentifier() + " execute: - provider: " +
                                     serviceDescription.getProviderName() + " score: " + serviceDescription.getScore());
            }

            requestServiceContract(serviceDescription);
	    
            // only want one contract
            break;
          }

          // Done with the query so clean up
          //getBlackboardService().publishRemove(mmRequest);
        }
      }
    }

    if (myServiceContractRelaySubscription.hasChanged()) {
      Collection changedRelays = myServiceContractRelaySubscription.getChangedCollection();

      // Update disposition on FindProviders task
      if (changedRelays.size() > 0) {
        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(getAgentIdentifier() + " changedRelays.size = " + changedRelays.size() +
                                 ", updateFindProvidersTaskDispositions");
        }
        updateFindProvidersTaskDispositions(changedRelays);
      }
    }

  }

  /**
   * create & publish a relay with service request to the provider specified in the serviceDescription for the specified
   * time interval.
   */
  protected void requestServiceContract(ServiceDescription serviceDescription) {
    Role role = getRole(serviceDescription);
    if (role == null) {
      if (myLoggingService.isWarnEnabled()) {
        myLoggingService.warn(getAgentIdentifier() + ": error requesting service contract: a null role");
      }
    } else {
      TimeSpan timeSpan = 
	TimeSpans.getSpan(TimeSpan.MIN_VALUE + 1000, TimeSpan.MAX_VALUE - 1000);
      String providerName = serviceDescription.getProviderName();
      ServiceRequest request = mySDFactory.newServiceRequest(getLocalEntity(), role,
                                                             mySDFactory.createTimeSpanPreferences(timeSpan));

      ServiceContractRelay relay = mySDFactory.newServiceContractRelay(MessageAddress.getMessageAddress(providerName),
                                                                       request);
      getBlackboardService().publishAdd(relay);
    }
  }

  protected Entity getLocalEntity() {
    for (Iterator iterator = myLocalEntitySubscription.iterator(); iterator.hasNext();) {
      return (Entity) iterator.next();
    }
    return null;
  }

  protected Role getRole(ServiceDescription serviceDescription) {

    for (Iterator iterator = serviceDescription.getServiceClassifications().iterator(); iterator.hasNext();) {
      ServiceClassification serviceClassification = (ServiceClassification) iterator.next();
      if (serviceClassification.getClassificationSchemeName().equals(Constants.UDDIConstants.COMMERCIAL_SERVICE_SCHEME)) {
        Role role = Role.getRole(serviceClassification.getClassificationName());
        return role;
      }
    }
    return null;
  }

  private Collection getCurrentProviders(Role role) {
    Entity localEntity = getLocalEntity();

    RelationshipSchedule relSchedule = localEntity.getRelationshipSchedule();
    Collection providerRelationships = 
      relSchedule.getMatchingRelationships(role);

    Collection currentProviders = new ArrayList();

    for (Iterator iterator = providerRelationships.iterator(); iterator.hasNext();) {
      Relationship relationship = (Relationship) iterator.next();
      Entity other = (Entity) relSchedule.getOther(relationship);
      currentProviders.add(other.getItemIdentificationPG().getItemIdentification());
    }

    return currentProviders;
  }

  private Role getRole(Task findProvidersTask) {
    PrepositionalPhrase asPhrase = findProvidersTask.getPrepositionalPhrase(org.cougaar.planning.Constants.Preposition.AS);

    if (asPhrase == null) {
      return null;
    } else {
      return (Role) asPhrase.getIndirectObject();
    }
  }

  private void updateFindProvidersTaskDispositions(Collection changedServiceContractRelays) {
    if (myFindProvidersTaskSubscription.isEmpty()) {
      // Nothing to update
      return;
    }

    for (Iterator relayIterator = changedServiceContractRelays.iterator(); relayIterator.hasNext();) {
      ServiceContractRelay relay = (ServiceContractRelay) relayIterator.next();

      if (relay.getServiceContract() != null) {
        Role relayRole = relay.getServiceContract().getServiceRole();

        for (Iterator taskIterator = myFindProvidersTaskSubscription.iterator(); taskIterator.hasNext();) {
          Task findProvidersTask = (Task) taskIterator.next();

          Disposition disposition = (Disposition) findProvidersTask.getPlanElement();

          if (disposition == null) {
            Role taskRole = getRole(findProvidersTask);

            // Assuming only 1 open task per role.
            if (taskRole.equals(relayRole)) {
              AllocationResult estResult = PluginHelper.createEstimatedAllocationResult(findProvidersTask,
                                                                                        myPlanningFactory, 1.0, true);
              disposition = myPlanningFactory.createDisposition(findProvidersTask.getPlan(), findProvidersTask,
                                                                estResult);

              getBlackboardService().publishAdd(disposition);
            }
          }
        }
      }
    }
  }


  private void queryServices(Role role) {
    MMQueryRequest mmRequest;

    /*
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug("queryService  asking MatchMaker for : " + role);
    }
    */
    
    myLoggingService.shout("queryService  asking MatchMaker for : " + role);
    ServiceInfoScorer roleScorer = new RoleScorer(role, getCurrentProviders(role));
    MMRoleQuery roleQuery = new MMRoleQuery(role, roleScorer);
    mmRequest = mySDFactory.newMMQueryRequest(roleQuery);
    getBlackboardService().publishAdd(mmRequest);
  }
}










