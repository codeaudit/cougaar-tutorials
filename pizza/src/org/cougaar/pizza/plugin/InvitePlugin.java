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

import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.relay.RSVPRelaySource;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.UnaryPredicate;


/**
 * Sends a simple relay invitation to all "FriendsOfMark".
 *
 * Waits for a set amount of time, WAIT_FOR_RSVP_DURATION, until it publishes the 
 * pizza preference list to the blackboard.  While it's waiting, replies come back
 * from invitees and update the PizzaPreferences object.
 *
 */
public class InvitePlugin extends ServiceUserPlugin {
  private LoggingService log;

  /** the uid service gives me the uid for the relay with a unique id */
  private UIDService uids;

  /** my subscription to relays */
  private IncrementalSubscription sub;

  /** my list of pizza preferences generated from RSVPs */
  protected PizzaPreferences pizzaPreferences;

  /** How long to wait before publish preferences */
  protected long WAIT_FOR_RSVP_DURATION = 45000;

  /** Have we published preferences */
  protected boolean publishedPreferences = false;

  public InvitePlugin() {
    super(new Class[0]);
  }

  /**
   * Set up the services we need - logging and the uid service
   */
  public void load() {
    super.load();

    // get services
    ServiceBroker sb = getServiceBroker();
    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    uids = (UIDService)
      sb.getService(this, UIDService.class, null);

    // prefix all logging calls with our agent name
    log = LoggingServiceWithPrefix.add(log, agentId+": ");

    log.debug("loaded");
  }

  /**
   * We have one subscription, to the relays (the invitation) we produce.
   *
   * Here we also publish the pizza preferences, although they are empty initially.
   *
   * Sets a timer that fires when we have waited long enough for responses to return.
   */
  protected void setupSubscriptions() {
    log.debug("setupSubscriptions");

    // create relay subscription
    sub = (IncrementalSubscription) blackboard.subscribe(new MyPred());

    // Create recipient addresses 
    MessageAddress source = getAgentIdentifier(); 
    MessageAddress target = 
      AttributeBasedAddress.getAttributeBasedAddress("FriendsOfMark-COMM", "Role", "Member"); 

    UID uid = uids.nextUID();

    // create pizza preferences - update as RSVPs arrive
    pizzaPreferences = new PizzaPreferences ();
    pizzaPreferences.setUID (uids.nextUID());

    // record inviter's preference
    PizzaPreferenceHelper prefHelper = new PizzaPreferenceHelper();
    String preference = (prefHelper.iLikeMeat(log, blackboard)) ? "meat" : "veg";
    pizzaPreferences.addFriendToPizza (agentId.toString(), preference);

    // send invitation
    Relay sourceRelay = new RSVPRelaySource(log, target, Constants.INVITATION_QUERY, pizzaPreferences);
    sourceRelay.setUID (uid);

    log.info (getAgentIdentifier () + " - Sending "+sourceRelay);

    blackboard.publishAdd(sourceRelay);

    // get wait parameter
    WAIT_FOR_RSVP_DURATION = getWaitParameter ();
	
    // wait for a time for responses to get back
    log.info ("Waiting " + (WAIT_FOR_RSVP_DURATION/1000) + " seconds before publishing pizza prefs.");
    resetTimer (WAIT_FOR_RSVP_DURATION);
  }

  /**
   * Look argument to plugin like : "WAIT_FOR_RSVP_DURATION:45000"
   */
  protected long getWaitParameter () {
    // get wait parameter
    long waitParam = WAIT_FOR_RSVP_DURATION;
    for (Iterator iter = getParameters().iterator(); iter.hasNext(); ) {
      String param = (String) iter.next();
      String [] keyAndValue = param.split(":");

      if (keyAndValue.length == 2) {
	if (keyAndValue[0].equals("WAIT_FOR_RSVP_DURATION")) {
	  try { 
	    waitParam = Integer.parseInt(keyAndValue[1]); 
	    log.info ("We will wait " + (WAIT_FOR_RSVP_DURATION/1000) + 
		      " seconds before publishing pizza prefs.");

	  } catch (Exception e) {
	    log.warn ("Could not parse wait param <" + keyAndValue[1] + ">"); 
	  }
	}
	else {
	  log.info ("ignoring " + keyAndValue);
	}
      }
      else {
	log.info ("ignoring " + keyAndValue);
      }
    }

    return waitParam;
  }

  /**
   * When a relay changes, this method gets called
   * 
   * Also, when the timer expires, this method gets called.
   */
  protected void execute() {
    log.info(" execute --------------- ");

    // if we waited long enough, publish pizza preferences so 
    // OrderPlugin can order the pizza.
    if (timerExpired()) {
      Collection relays = sub.getCollection();

      if (publishedPreferences) {
	log.info ("We published the invite list already, " + 
		  "so there are no relays in our collection.");
      }
      else if (!publishedPreferences) {
	Object sourceRelay = relays.iterator().next();

	log.info ("We've waited " + (WAIT_FOR_RSVP_DURATION/1000) + 
		  " seconds, so we're publishing the preference list.");

	log.info ("\nremoving source relay        : " + sourceRelay + 
		  "\nand adding pizza preferences : " + pizzaPreferences);

	blackboard.publishRemove(sourceRelay); 
	blackboard.publishAdd(pizzaPreferences);
	publishedPreferences = true;
      }
    }
    else {
      log.info("Timer not expired so not publishing..." + 
	       "\nvs now             : " + new Date() +
	       "\nExpiration time is : " + new Date(getTimerExpirationTime()));
    }

    // observe changed relay to see when it changes
    for (Enumeration en = sub.getChangedList(); en.hasMoreElements();) {
      Relay sr = (Relay) en.nextElement();
      log.info("observe changed "+sr);
    }

    if (log.isDebugEnabled()) {
      // removed relays
      for (Enumeration en = sub.getRemovedList(); en.hasMoreElements();) {
        Relay.Source sr = (Relay.Source) en.nextElement();
        log.debug("observe removed "+sr);
      }
    }
  }

  /**
   * My subscription predicate, which matches SimpleRelays
   */ 
  private class MyPred implements UnaryPredicate {
    public boolean execute(Object o) {
      if (o instanceof Relay.Source) {
	return true;
      }
      else if (o instanceof Relay) {
	log.debug ("\nignored : " + o +
		   "\nclass   : " + o.getClass());
      }
      return false;
    }
  }
}
