/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.PlanningDomain;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Entity;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.ServiceContract;
import org.cougaar.servicediscovery.description.ServiceContractImpl;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.transaction.ProviderServiceContractRelay;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

/**
 * SDProviderPlugin responds to ServiceContractRelays. This version says 
 * yes to all requests - generating a contract which exactly matches
 * the request.
 */
public class SDProviderPlugin extends ComponentPlugin {
  private static Integer START_TIME_KEY = new Integer(AspectType.START_TIME);
  private static Integer END_TIME_KEY = new Integer(AspectType.END_TIME);

  private IncrementalSubscription myLocalEntitySubscription;
  private IncrementalSubscription myServiceContractRelaySubscription;
  
  private String myAgentName;

  protected LoggingService myLoggingService;
  protected DomainService myDomainService;
  protected SDFactory mySDFactory;
  protected PlanningFactory myPlanningFactory;

  private UnaryPredicate myServiceContractRelayPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ProviderServiceContractRelay) {
	ServiceContractRelay relay = (ServiceContractRelay) o;
	return (relay.getProviderName().equals(myAgentName));
      } else {
	return false;
      }
    }
  };

  private UnaryPredicate myLocalEntityPred = new UnaryPredicate() {
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

    mySDFactory = (SDFactory) getDomainService().getFactory(SDDomain.SD_NAME);
    myPlanningFactory = (PlanningFactory) getDomainService().getFactory(PlanningDomain.PLANNING_NAME);

    myLoggingService =
      (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);

    myAgentName = getAgentIdentifier().toString();
  }

  protected void setupSubscriptions() {
    myServiceContractRelaySubscription = 
      (IncrementalSubscription) getBlackboardService().subscribe(myServiceContractRelayPred);
    myLocalEntitySubscription = 
      (IncrementalSubscription) getBlackboardService().subscribe(myLocalEntityPred);
  }

  public void execute() {
    if (myServiceContractRelaySubscription.hasChanged()) {

      Collection addedRelays =
	myServiceContractRelaySubscription.getAddedCollection();
      for (Iterator adds = addedRelays.iterator(); adds.hasNext();) {
	ProviderServiceContractRelay relay = 
	  (ProviderServiceContractRelay) adds.next();

	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() +
				 ": execute() new ServiceContractRelay - " +
				 relay);
	}
        handleServiceContractRelay(relay);
      }

      Collection changedRelays = 
	myServiceContractRelaySubscription.getChangedCollection();
      for (Iterator changes = changedRelays.iterator(); changes.hasNext();) {
	ProviderServiceContractRelay relay = 
	  (ProviderServiceContractRelay) changes.next();

	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() +
				 ": execute() ignoring modifed ServiceContractRelay - " +
				 relay);
	}
      }


      Collection removedRelays = 
	myServiceContractRelaySubscription.getRemovedCollection();
      for (Iterator removes = removedRelays.iterator(); removes.hasNext();) {
	ProviderServiceContractRelay relay = 
	  (ProviderServiceContractRelay) removes.next();

	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(getAgentIdentifier() +
				 ": execute() ignoring removed ServiceContractRelay - " +
				 relay);
	}
      }
    }
  }

  protected void handleServiceContractRelay(ProviderServiceContractRelay relay){
    ServiceRequest serviceRequest = relay.getServiceRequest();
    ServiceContract serviceContract = relay.getServiceContract();

    HashMap contractPreferences = 
      copyPreferences(serviceRequest.getServicePreferences());

    boolean contractExists = (serviceContract != null);

    if (contractExists) { 
      myLoggingService.debug(getAgentIdentifier() +
			     ": handleServiceContractRelay() relay = " + relay + 
			     " " + relay.getUID() +
			     " already has a contract. Not handling changed/removed requests.");

      return;
    }
     
    // Replace start/end time preferences
    // Construct start/end pref by making request agree with capability avail
    // schedule
    TimeSpan requestedTimeSpan = 
      mySDFactory.getTimeSpanFromPreferences(serviceRequest.getServicePreferences());

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(getAgentIdentifier() +
			     ": handleServiceContractRelay() relay = " + relay + 
			     " " + relay.getUID() +
			     " requestedTimeSpan = " + 
			     new Date(requestedTimeSpan.getStartTime()) + " " +
			     new Date(requestedTimeSpan.getEndTime()));
    }

    if (requestedTimeSpan == null) {
      if (myLoggingService.isInfoEnabled()) {
	myLoggingService.info(getAgentIdentifier() + 
			      ": handleServiceContractRelay() unable to handle service request - " + 
			      relay.getServiceRequest() +
			      " - does not have start and/or end time preferences.");
      }   

      // Remove start/end preferences since provider can't meet request.
      contractPreferences.remove(START_TIME_KEY);
      contractPreferences.remove(END_TIME_KEY);
    }

    if (myLoggingService.isInfoEnabled()) {
      myLoggingService.info(getAgentIdentifier() + 
			    ": added new ServiceContract on a relay from " + 
			    relay.getUID() + " for the role " + 
			    serviceRequest.getServiceRole());
    }
    serviceContract =
      mySDFactory.newServiceContract(getLocalEntity(),
				     serviceRequest.getServiceRole(),
				     contractPreferences.values());
    relay.setServiceContract(serviceContract);

    getBlackboardService().publishChange(relay);
    
  }

  protected Entity getLocalEntity() {
    for (Iterator iterator = myLocalEntitySubscription.iterator();
	 iterator.hasNext();) {
      return (Entity) iterator.next();
    }

    return null;
  }

  private HashMap copyPreferences(Collection preferences) {
    HashMap preferenceMap = new HashMap(preferences.size());

    for (Iterator iterator = preferences.iterator();
	 iterator.hasNext();) {
      Preference original = (Preference) iterator.next();
      
      Preference copy =
	myPlanningFactory.newPreference(original.getAspectType(),
					original.getScoringFunction(),
					original.getWeight());
      preferenceMap.put(new Integer(copy.getAspectType()), copy);
    }

    return preferenceMap;
  }
}
