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
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.pizza.Constants;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.servicediscovery.description.MMRoleQuery;
import org.cougaar.servicediscovery.description.ScoredServiceDescriptionImpl;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceClassificationImpl;
import org.cougaar.servicediscovery.description.ServiceInfo;
import org.cougaar.servicediscovery.service.RegistryQueryService;
import org.cougaar.servicediscovery.transaction.MMQueryRequest;
import org.cougaar.servicediscovery.transaction.MMQueryRequestImpl;
import org.cougaar.servicediscovery.transaction.RegistryQuery;
import org.cougaar.servicediscovery.transaction.RegistryQueryImpl;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

/**
 *
 * Query the YellowPages for possible service providers
 *
 */
public class MatchmakerPlugin extends ComponentPlugin {
  private static int WARNING_SUPPRESSION_INTERVAL = 2;
  private long myWarningCutoffTime = 0;
  private static final String QUERY_GRACE_PERIOD_PROPERTY = 
                "org.cougaar.pizza.plugin.MatchmakerQueryGracePeriod";

  private LoggingService myLoggingService;
  private RegistryQueryService myRegistryQueryService;
  private IncrementalSubscription myClientRequestSubscription;

  // outstanding RQ are those which have been issued but have not yet returned
  private ArrayList myOutstandingRQs = new ArrayList();
  // pending RQs are returned RQ which haven't been consumed by the plugin yet
  private ArrayList myPendingRQs = new ArrayList();

  // Outstanding alarms (any means non-quiescent)
  private int myOutstandingAlarms = 0; 

  private UnaryPredicate myQueryRequestPredicate =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MMQueryRequest) {
          MMQueryRequest qr = (MMQueryRequest) o;
          return (qr.getQuery() instanceof MMRoleQuery);
        }
        return false;
      }
    };

  public void load() {
    super.load();

    myLoggingService = (LoggingService)
      getServiceBroker().getService(this, 
				    LoggingService.class, 
				    null);
    if (myLoggingService == null) {
      myLoggingService = LoggingService.NULL;
    }

    myRegistryQueryService = (RegistryQueryService)
      getServiceBroker().getService(this,
				    RegistryQueryService.class,
				    null);
    if (myRegistryQueryService == null)
      throw new RuntimeException("Unable to obtain RegistryQuery service");
  }

  public void unload() {
    if (myRegistryQueryService != null) {
      getServiceBroker().releaseService(this,
					RegistryQueryService.class,
					myRegistryQueryService);
      myRegistryQueryService = null;
    }

    if ((myLoggingService != null) && 
	(myLoggingService != LoggingService.NULL)) {
      getServiceBroker().releaseService(this, 
					LoggingService.class,
					myLoggingService);
      myLoggingService = null;
    }
    super.unload();
  }

  protected void setupSubscriptions() {
    myClientRequestSubscription = 
      (IncrementalSubscription) getBlackboardService().subscribe(myQueryRequestPredicate);
  }

 
  protected void execute() {
    if (myClientRequestSubscription.hasChanged()) {

      for (Iterator i = myClientRequestSubscription.getAddedCollection().iterator();
	   i.hasNext();) {
        MMQueryRequest queryRequest =  (MMQueryRequest) i.next();
        MMRoleQuery query = (MMRoleQuery) queryRequest.getQuery();
        RegistryQuery rq = new RegistryQueryImpl();
	RQ r;

        // Find all service providers for specifed Role
        ServiceClassification roleSC =
	  new ServiceClassificationImpl(query.getRole().toString(),
					query.getRole().toString(),
					Constants.UDDIConstants.COMMERCIAL_SERVICE_SCHEME);
        rq.addServiceClassification(roleSC);
	myLoggingService.debug("RegistryQuery = " +rq + " " +
			       rq.getServiceClassifications());
	r = new RQ(queryRequest, query, rq);

        postRQ(r);
      }
    }


    RQ r;
    while ((r = getPendingRQ()) != null) {
      if (r.exception != null) {
	handleException(r);
      } else {
	handleResponse(r);
      }
    }
  }

  protected void handleException(RQ r) {
    retryErrorLog(r, ": Exception querying registry for " +
      r.query.getRole().toString() +
      ", try again later.", r.exception);
    r.exception = null;
  }

  private void retryErrorLog(RQ r, String message) {
    retryErrorLog(r, message, null);
  }

  // When an error occurs, but we'll be retrying later, treat it as a DEBUG
  // at first. After a while it becomes an error.
  private void retryErrorLog(RQ r, String message, Throwable e) {
    int rand = (int)(Math.random()*10000) + 1000;
    QueryAlarm alarm =
      new QueryAlarm(r, getAlarmService().currentTimeMillis() + rand);
    getAlarmService().addAlarm(alarm);
    // Alarms silently make us non-quiescent -- so keep track of when we have any
    myOutstandingAlarms++;

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(":retryErrorLog - adding a QueryAlarm for r.query.getRole()" + 
		" alarm - " + alarm);
    }

    if(System.currentTimeMillis() > getWarningCutoffTime()) {
      if (e == null)
	myLoggingService.error(message);
      else
	myLoggingService.error(message, e);
    } else if (myLoggingService.isDebugEnabled()) {
      if (e == null)
	myLoggingService.debug(message);
      else
	myLoggingService.debug(message, e);
    }
  }

  protected void handleResponse(RQ r) {
    MMQueryRequest queryRequest = r.queryRequest;
    MMRoleQuery query = r.query;

    if (query.getObsolete()) {
      if (myLoggingService.isDebugEnabled()) {
	myLoggingService.debug(": handleResponse - ignoring registry query result for obsolete request - " +
			       r.query);
      }
      return;
    }

    Collection services = r.services;

    
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(": handleResponse - registry query result size is : " + 
			     services.size() + " for query: " + 
			     query.getRole().toString());
    }

    ArrayList scoredServiceDescriptions = new ArrayList();
    for (Iterator iter = services.iterator(); iter.hasNext(); ) {
      ServiceInfo serviceInfo = (ServiceInfo) iter.next();
      int score = query.getServiceInfoScorer().scoreServiceInfo(serviceInfo);

      if (score >= 0) {
	scoredServiceDescriptions.add(new ScoredServiceDescriptionImpl(score,
								       serviceInfo));
	if(myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(": execute: adding Provider name - " + 
				 serviceInfo.getProviderName() +
				 " Service name: " + 
				 serviceInfo.getServiceName() +
				 " Service score: " + score);
	}
      } else {
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(": execute: ignoring Provider name - " + 
				 serviceInfo.getProviderName() +
				 " Service name: " + 
				 serviceInfo.getServiceName() +
				 " Service score: " + score);
	}
      }
    }

    if (scoredServiceDescriptions.isEmpty()) {
      if (!r.getNextContextFailed) {
	  if (myLoggingService.isDebugEnabled()) {
	    myLoggingService.debug(":execute - no matching provider for " + 
				   query.getRole() +
				   " in " + r.currentYPContext +
				   " retrying in next context.");
	  }
	  postRQ(r);
      } else {
	// Couldn't find another YPServer to search
	retryErrorLog(r, 
		      ": unable to find provider for " + 
		      query.getRole() +
		      ", publishing empty query result. " +
		      "Will try query again later.");

      }
    } else {
      Collections.sort(scoredServiceDescriptions);
    }


    ((MMQueryRequestImpl) queryRequest).setResult(scoredServiceDescriptions);
    ((MMQueryRequestImpl) queryRequest).setQueryCount(queryRequest.getQueryCount() + 1);
    getBlackboardService().publishChange(queryRequest);

    if(myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(": publishChanged query");
    }
  }

  protected long getWarningCutoffTime() {
    if (myWarningCutoffTime == 0) {
      WARNING_SUPPRESSION_INTERVAL = 
	Integer.getInteger(QUERY_GRACE_PERIOD_PROPERTY,
			   WARNING_SUPPRESSION_INTERVAL).intValue();
      myWarningCutoffTime = System.currentTimeMillis() + 
	WARNING_SUPPRESSION_INTERVAL*60000;
    }

    return myWarningCutoffTime;
  }
  
  protected void resetWarningCutoffTime() {
    myWarningCutoffTime = -1;
  }

  private class RQ {
    MMQueryRequest queryRequest;
    MMRoleQuery query;
    RegistryQuery rq;

    Collection services;
    Exception exception;
    boolean complete = false;
    Object previousYPContext = null;
    Object currentYPContext = null;
    boolean getNextContextFailed = false;


    RQ(MMQueryRequest queryRequest, MMRoleQuery query, RegistryQuery rq) {
      this.queryRequest = queryRequest;
      this.query = query;
      this.rq = rq;
    }
  }

  // issue a async request
  private void postRQ(final RQ r) {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(": postRQ " + r + 
			     " (" + r.rq + ")" );
    }
    synchronized (myOutstandingRQs) {
      myOutstandingRQs.add(r);
    }

    findServiceWithDistributedYP(r);
  }

  // note an async response and wake the plugin
  private void pendRQ(RQ r) {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(": pendRQ " + r + " (" + r.rq + ")");
    }
    r.complete = true;
    synchronized (myOutstandingRQs) {
      myOutstandingRQs.remove(r);
    }
    synchronized (myPendingRQs) {
      myPendingRQs.add(r);
    }
    // tell the plugin to wake up
    getBlackboardService().signalClientActivity();
  }

  // get a pending RQ (or null) so that we can deal with it
  private RQ getPendingRQ() {
    RQ r = null;
    synchronized (myPendingRQs) {
      if (!myPendingRQs.isEmpty()) {
        r = (RQ) myPendingRQs.remove(0); // treat like a fifo
        if (myLoggingService.isDebugEnabled()) {
          myLoggingService.debug(": getPendingRQ " + r + " (" + r.rq + ")");
        }
      }
    }
    return r;
  }

  private void findServiceWithDistributedYP(final RQ r) {
    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug(": findServiceWithDistributedYP - " +
			     " using YPCommunity search.");
    }
    
    myRegistryQueryService.findServiceAndBinding(r.currentYPContext, 
						 r.rq,
						 new RegistryQueryService.CallbackWithContext() {
      public void invoke(Object result) {
	r.services = (Collection) result;
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(": results = " + result + 
				 " for " + r.currentYPContext);
	}
	flush();
      }
      
      public void handle(Exception e) {
	r.exception = e;
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(": failed during query of " +
				 r.queryRequest + 
				 " context =  " + r.currentYPContext, e);
	}
	flush();
      }
      
      public void setNextContext(Object context){
	if (myLoggingService.isDebugEnabled()) {
	  myLoggingService.debug(": previous YPContext " +
				 r.currentYPContext + 
				 " current YPContext " + context);
	}
	r.previousYPContext = r.currentYPContext;
	r.currentYPContext = context;
	
	if (context == null) {
	  r.getNextContextFailed = true;
	}
      }
      
      private void flush() {
	pendRQ(r);
      }
    });
  }

  public class QueryAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;
    private RQ rq = null;

    public QueryAlarm (RQ rq, long expirationTime) {
      expiresAt = expirationTime;
      this.rq = rq;
    }
    public long getExpirationTime() { return expiresAt; }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
	rq.complete = false;
	postRQ(rq);
	--myOutstandingAlarms;
      }
    }

    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired = true;
      --myOutstandingAlarms;
      return was;
    }
    public String toString() {
      return "<QueryAlarm " + expiresAt +
        (expired ? "(Expired) " : " ") +
	rq.query.getRole() + " " +
        "for MatchmakerPlugin at " + getAgentIdentifier() + ">";
    }
  }
}













