/** 
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

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.plugin.PluginAlarm;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.relay.RSVPRelaySource;
import org.cougaar.pizza.plugin.util.PizzaPreferenceHelper;
import org.cougaar.planning.ldm.asset.Entity;

import org.cougaar.util.Arguments;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Sends a simple relay invitation to all "FriendsOfMark" (members of the community),
 * whose responses are automatically collected in the PizzaPreferences object.
 * <p/>
 * Waits for a set amount of time, WAIT_FOR_RSVP_DURATION, until it
 * publishes the pizza preference list to the blackboard.  While it's
 * waiting, replies come back from invitees and update the PizzaPreferences
 * object in memory.
 * <p>
 * It must wait because people may take a while to join the FriendsOfMark community,
 * and the PlaceOrderPlugin will place the orders as soon as the PizzaPreferences
 * object is published. 
 * <p>
 * An alternate way to do this instead of an alarm would be to tell this Plugin in
 * advance how many people to expect, or allow it to publishChange the
 * PizzaPreferences object.
 * @see PizzaPreferences
 * @see PlaceOrderPlugin
 */
public class InvitePlugin extends ComponentPlugin {

  private static final String ATTRIBUTE_TYPE  = "Role";
  private static final String ATTRIBUTE_VALUE = "Member";

  private LoggingService log;

  /**
   * the uid service gives me the uid for the relay with a unique id
   */
  private UIDService uids;

  /** initialize args to the empty instance */
  private Arguments args = Arguments.EMPTY_INSTANCE;
  
  /**
   * my subscription to Relays
   */
  private IncrementalSubscription relaySubscription;

  /**
   * my subscription to Entities
   */
  private IncrementalSubscription entitySubscription;

  /** 
   * A timer for recurrent events.  All access should be synchronized 
   * on timerLock 
   */
  private Alarm timer = null;

  /** Lock for accessing timer */
  private final Object timerLock = new Object();

  /**
   * my list of pizza preferences generated from RSVPs
   */
  protected PizzaPreferences pizzaPreferences;

  /**
   * How long to wait before publish preferences
   */
  protected long waitForRSVPDuration = 45000;

  /**
   * Have we published preferences
   */
  protected boolean publishedPreferences = false;

  /** "setParameter" is only called if a plugin has parameters */
  public void setParameter(Object o) {
    args = new Arguments(o);
  }  


  public void setLoggingService(LoggingService log) {
    this.log = log;
    // Note that by default all logging calls start with our agent name
  }

  public void setUIDService(UIDService uids) {
    this.uids = uids;
  }

  /**
   * Looks for argument to plugin like : "WAIT_FOR_RSVP_DURATION:60000"
   * <p/>
   * <pre>
   * <p/>
   * For example :
   * <p/>
   * <component
   *      name='org.cougaar.pizza.plugin.InvitePlugin'
   *       class='org.cougaar.pizza.plugin.InvitePlugin'
   *       priority='COMPONENT'
   *       insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'>
   *       <argument>
   *         WAIT_FOR_RSVP_DURATION:60000
   *       </argument>
   * </component>
   * <p/>
   * </pre>
   *
   * @return millis to wait
   */
  protected long getWaitParameter() {
    // get wait parameter
    long waitParam = 
      args.getLong("WAIT_FOR_RSVP_DURATION", waitForRSVPDuration);
    return waitParam;
  }

  /**
   * We have one subscription, to the relays (the invitation) we produce.
   * <p/>
   * Here we also publish the pizza preferences.  Initially it only holds
   * the preference for Alice, the inviting agent.
   * <p/>
   * Sets a timer that fires when we have waited long enough for responses
   * to return.
   */
  protected void setupSubscriptions() {
    if (log.isDebugEnabled())
      log.debug("setupSubscriptions");

    // get wait parameter
    waitForRSVPDuration = getWaitParameter();

    // create relay subscription
    relaySubscription = 
      (IncrementalSubscription) blackboard.subscribe(RELAYSOURCEPRED);

    // create entity subscription
    entitySubscription = 
      (IncrementalSubscription) blackboard.subscribe(ENTITYPRED);

    // If this agent moves or restarts, its possible we've already
    // invited people to the party. Get any pre-published PizzaPreferences.
    getPizzaPreferencesFromBB();

    // If we havent already published the pizzaPreferences,
    // then set a timer, to allow RSVPs to come in, before
    // we publish it
    if (!publishedPreferences) {
      // wait for a time for responses to get back
      if (log.isInfoEnabled()) {
	log.info(
		 "Waiting " + (waitForRSVPDuration / 1000) +
		 " seconds before publishing pizza prefs.");
      }
      
      startTimer(waitForRSVPDuration);
    } else {
      // Must have restarted when this plugins job was done
      if (log.isInfoEnabled())
	log.info("Restarting when RSVPs already collected and published.");
    }
  }

  /**
   * Query the Blackboard for a pre-existing PizzaPreferences object.
   * This might happen if the agent moved or restarted.
   * If it is found, record it and the fact it was already published. 
   * If not, check for an RSVPRelaySource off of which to grab the
   * (unpublished) PizzaPreferences.
   */
  private void getPizzaPreferencesFromBB() {
    Collection pprefs =  blackboard.query(new UnaryPredicate() {
	public boolean execute(Object o) {
	  return o instanceof PizzaPreferences;
	}
      });
    if (! pprefs.isEmpty()) {
      this.pizzaPreferences = (PizzaPreferences)pprefs.iterator().next();
      publishedPreferences = true;
    } else if (!relaySubscription.isEmpty()) {
      // If get here, haven't yet published the PizzaPreferences. But
      // have published a Relay with a PizzaPreferences object. Grab it back.
      RSVPRelaySource rSource = (RSVPRelaySource)relaySubscription.first();
      this.pizzaPreferences = rSource.getPizzaPrefs();
    }
  }

  /**
   * Onc we have the self org, publish the invite RSVPRelay if we haven't yet.
   * When the timer expires, assume all replies have come in, and publish the collected
   * PizzaPreferences object for the PlaceOrderPlugin.
   */
  protected void execute() {
    // When we have the self org, but havent yet created our 
    // reply catalog, we need to invite people to the party
    if (!entitySubscription.isEmpty() && (pizzaPreferences == null)) {
      // When we publish the relay, we create a pizza preference locally (not published).
      // That PizzaPreferences is auto-updated by the Relay responses. Publishing
      // it is just to let the PlaceOrderPlugin know it can start working
      publishRelay();
    }

    // Wait for everyone to register with the community,
    // so my ABA Relay gets to them, and then they reply, so it is safe to 
    // assume we have all the replies. Then publish the pizza preferences object so 
    // PlaceOrderPlugin can order the pizza.
    checkTimer();
  } // end of execute()

  /**
   * Create a PizzaPreferences object to collect local results, and 
   * publish my RSVP relay inviting people to the party. The relay itself
   * will update the PizzaPreferences object as replies come in.
   */
  protected void publishRelay() {
    // Create recipient addresses: a multicast address, going to all the
    // members of my community of friends. The infrastructure will take care
    // of ensuring that as people join the community, they get a copy of
    // any message sent to this ABA. You just have to wait for people
    // to join the community.
    MessageAddress target =
      AttributeBasedAddress.getAttributeBasedAddress(Constants.COMMUNITY, 
						     ATTRIBUTE_TYPE, 
						     ATTRIBUTE_VALUE);

    // create pizza preferences - updated as RSVPs arrive
    this.pizzaPreferences = new PizzaPreferences(uids.nextUID());

    // There may be several entities on the blackboard
    // we want the one that represents the inviting agent
    Entity selfEntity = getSelfEntity();

    if (selfEntity == null && log.isWarnEnabled()) {
      log.warn ("Could not find self entity. It's needed to find " + 
		"out my pizza preference.");
    }

    // Get the inviters preference
    String preference = PizzaPreferenceHelper.getPizzaPreference(log, selfEntity);

    // And record it.
    pizzaPreferences.addFriendToPizza(agentId.toString(), preference);

    // send invitation to the ABA
    Relay sourceRelay = new RSVPRelaySource(uids.nextUID(), 
					    target,
					    Constants.INVITATION_QUERY,
					    pizzaPreferences);
    
    log.shout("Sending '" + Constants.INVITATION_QUERY + "' to my Buddy list: " + Constants.COMMUNITY);
    
    blackboard.publishAdd(sourceRelay);
  }

  /**
   * Returns the Entity representing the agent.  Checks the entity subscription and
   * returns the first element.  In this example, there should be only one self entity.
   * @return local Entity, null if none yet
   */
  protected Entity getSelfEntity() {
    // See if we have any yet -- we should
    if (entitySubscription.isEmpty()) {
      log.error("Entity subscription is empty, this should not happen!!!!");
      return null;
    }

    // Look for the local Entity
    for (Enumeration entities=entitySubscription.elements(); entities.hasMoreElements(); ) {
      Entity ent = (Entity)entities.nextElement();
      if (ent.isLocal())
	return ent;
    }
    return null;
  }

  /**
   * If the timer has expired, assume all the members of the community have joined,
   * gotten the ABA targeted RSVP Relay, and replied. So publish the 
   * PizzaPreferences object, so the PlaceOrderPlugin can begin.
   */
  protected void checkTimer () {
    if (timerExpired()) {
      Collection relays = relaySubscription.getCollection();
      if (publishedPreferences) {
	if (log.isDebugEnabled()) {
	  log.debug("We already published the invite list, nothing to do.");
	  // Note that the relaySubscription should be empty too, since
	  // we remove the relay when we're done
	}
      } else {
	if (relays.isEmpty()) {
	  log.error("Expecting an RSVPrelay, since the timer has expired.");
	  return;
	}

	// Get our (single) published relay
        RSVPRelaySource sourceRelay = (RSVPRelaySource) relays.iterator().next();

	if (log.isInfoEnabled()) {
	  log.info("We've waited " + (waitForRSVPDuration / 1000) +
		   " seconds, so we're publishing the preference list.");

	  log.info("Removing source relay: " + sourceRelay +
		   ", and adding pizza preferences: " + pizzaPreferences);
	}

	// We're done with the relay -- and any agent which starts up late
	// and joins the community late is too late to come to the party.
	// So remove the relay. In other applications, we could leave it.
        blackboard.publishRemove(sourceRelay);

	// Publish our final set of attendees with their pizza preferences,
	// so the PlaceOrderPlugin knows what to get
        blackboard.publishAdd(pizzaPreferences);

	log.shout("RSVP time is up. Got " + pizzaPreferences);

	// Note that we've finished.
        publishedPreferences = true;
      }
    } else {
      if (log.isInfoEnabled()) {
	log.info("Timer not expired so not publishing Preferences yet. " +
		 "Now: " + new Date() +
		 ", Timer expiration time is: " + 
		 new Date(getTimerExpirationTime()));
      }
    }
  }

  /**
   * Schedule a update wakeup after some interval of time
   * @param delay how long to delay before the timer expires.
   */
  protected void startTimer(long delay) {
    synchronized (timerLock) {
      //     if (logger.isDebugEnabled()) logger.debug("Starting timer " + delay);
      if (getBlackboardService() == null && 
          log != null && 
          log.isWarnEnabled()) {
        log.warn(
                    "Started service alarm before the blackboard service"+
                    " is available");
      }
      timer = createAlarm(System.currentTimeMillis()+delay);
      getAlarmService().addRealTimeAlarm(timer);
    }
  }

  private Alarm createAlarm(long time) {
    return new PluginAlarm(time) {
      public BlackboardService getBlackboardService() {
        if (blackboard == null) {
          if (log != null && log.isWarnEnabled()) {
            log.warn(
              "Alarm to trigger at "
                + (new Date(getExpirationTime()))
                + " has expired,"
                + " but the blackboard service is null.  Plugin "
                + " model state is "
                + getModelState());
          }
        }
        return blackboard;
      }
    };
  }

  /**
   * Test if the timer has expired.
   * @return false if the timer is not running or has not yet expired
   */
  protected boolean timerExpired() {
    synchronized (timerLock) {
      return timer != null && timer.hasExpired();
    }
  }

  /** When will (has) the timer expire(d)? */
  protected long getTimerExpirationTime() {
    synchronized (timerLock) {
      if (timer != null) {
        return timer.getExpirationTime();
      } else {
        return 0;
      }
    }
  }

  /**
   * Single static predicate that matches RSVPRelaySource objects. 
   * Use a static singleton since we only need one.
   */
  private static final UnaryPredicate RELAYSOURCEPRED = new UnaryPredicate() {
      public boolean execute(Object o) {
	return (o instanceof RSVPRelaySource);
      }
    };

  /**
   * Subscribe to Entities, in order to find the self Entity.
   */
  private static final UnaryPredicate ENTITYPRED = new UnaryPredicate() {
      public boolean execute(Object o) {
	return o instanceof Entity;
      }
    };
}
