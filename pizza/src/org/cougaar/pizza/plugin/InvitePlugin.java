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

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
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
import org.cougaar.planning.ldm.asset.Entity;

import org.cougaar.util.Arguments;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

/**
 * Sends a simple relay invitation to all "FriendsOfMark".
 * <p/>
 * Waits for a set amount of time, WAIT_FOR_RSVP_DURATION, until it
 * publishes the pizza preference list to the blackboard.  While it's
 * waiting, replies come back from invitees and update the PizzaPreferences
 * object.
 *
 * Linewrap detector:
 *3456789012345678901234567890123456789012345678901234567890123456789012345
 *       1         2         3         4         5         6         7   75  
 */
public class InvitePlugin extends ComponentPlugin {

  private static final String COMMUNITY = "FriendsOfMark-COMM";
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
   * my subscription to relays
   */
  private IncrementalSubscription relaySubscription;

  /**
   * my subscription to entities
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
    log = LoggingServiceWithPrefix.add(log, agentId + ": ");

    // get wait parameter
    waitForRSVPDuration = getWaitParameter();
	
    log.debug("plugin loaded, services found");
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
    log.debug("setupSubscriptions");

    // create relay subscription
    relaySubscription = 
      (IncrementalSubscription) blackboard.subscribe(new RelaySourcePred());

    // create entity subscription
    entitySubscription = 
      (IncrementalSubscription) blackboard.subscribe(new EntityPred());

    // wait for a time for responses to get back
    if (log.isInfoEnabled()) {
      log.info(
	       "Waiting " + (waitForRSVPDuration / 1000) +
	       " seconds before publishing pizza prefs.");
    }

    startTimer(waitForRSVPDuration);
  }

  /**
   * When a relay changes, this method gets called
   * <p/>
   * Also, when the timer expires, this method gets called.
   */
  protected void execute() {
    if (log.isInfoEnabled()) {
      log.info(" execute --------------- ");
    }

    // observe changed entity to see when it changes
    if (log.isInfoEnabled()) {
      for (Iterator iter = entitySubscription.getChangedCollection().iterator(); iter.hasNext();) {
	Entity sr = (Entity) iter.next();
	log.info("observe changed " + sr);
      }
    }

    if (!entitySubscription.isEmpty() && (pizzaPreferences == null)) {
      // when we publish the relay, we create a pizza preference
      // we use this to know only to do this once
      publishRelay (entitySubscription.getCollection());
    }

    // observe changed relay to see when it changes
    if (log.isInfoEnabled()) {
      for (Iterator iter = relaySubscription.getChangedCollection().iterator(); iter.hasNext();) {
	Relay sr = (Relay) iter.next();
	log.info("observe changed " + sr);
      }
    }

    if (log.isDebugEnabled()) {
      // removed relays
      for (Iterator iter = relaySubscription.getRemovedCollection().iterator(); iter.hasNext();) {
        Relay.Source sr = (Relay.Source) iter.next();
        log.debug("observe removed " + sr);
      }
    }

    // if we waited long enough, publish pizza preferences so 
    // OrderPlugin can order the pizza.
    checkTimer();
  }

  protected void publishRelay (Collection entities) {
    // Create recipient addresses 
    MessageAddress target =
      AttributeBasedAddress.getAttributeBasedAddress(COMMUNITY, 
						     ATTRIBUTE_TYPE, 
						     ATTRIBUTE_VALUE);

    // create pizza preferences - update as RSVPs arrive
    this.pizzaPreferences = new PizzaPreferences(uids.nextUID());

    // record inviter's preference
    // there will be several entities on the blackboard
    // we want the one that represents the inviting agent
    Entity selfEntity = null;
    for (Iterator iter = entities.iterator(); iter.hasNext(); ) {
      Entity entity = (Entity) iter.next();

      // if this entity is myself
      String entityItemId = 
	entity.getItemIdentificationPG().getItemIdentification();
      if (entityItemId.equals(getAgentIdentifier().toString())) {
	selfEntity = entity;
	break;
      }
    }

    if (selfEntity == null) {
      log.warn ("Could not find self entity. It's needed to find " + 
		"out my pizza preference.");
    }

    PizzaPreferenceHelper prefHelper = new PizzaPreferenceHelper();
    String preference = prefHelper.getPizzaPreference(log, selfEntity);
    pizzaPreferences.addFriendToPizza(agentId.toString(), preference);

    // send invitation
    Relay sourceRelay = new RSVPRelaySource(uids.nextUID(), 
					    target,
                                            Constants.INVITATION_QUERY,
                                            pizzaPreferences);

    if (log.isInfoEnabled()) {
      log.info(" Sending " + sourceRelay);
    }

    blackboard.publishAdd(sourceRelay);
  }

  /**
   * If the timer has expired, publish the pizza preferences.
   */
  protected void checkTimer () {
    if (timerExpired()) {
      Collection relays = relaySubscription.getCollection();
      if (publishedPreferences) {
	if (log.isInfoEnabled()) {
	  log.info("We published the invite list already, " +
		   "so there are no relays in our collection.");
	}
      } else {
	if (relays.isEmpty()) {
	  log.warn ("Expecting a relay since the timer has expired.");
	}

	// the timer could only have been started if we 
	// published the relay, so we are guaranteed it will be in the
	// collection
	
        Object sourceRelay = relays.iterator().next();

	if (log.isInfoEnabled()) {
	  log.info("We've waited " + (waitForRSVPDuration / 1000) +
		   " seconds, so we're publishing the preference list.");

	  log.info("\nremoving source relay        : " + sourceRelay +
		   "\nand adding pizza preferences : " + pizzaPreferences);
	}

        blackboard.publishRemove(sourceRelay);
        blackboard.publishAdd(pizzaPreferences);
        publishedPreferences = true;
      }
    } else {
      if (log.isInfoEnabled()) {
	log.info("Timer not expired so not publishing..." +
		 "\nvs now             : " + new Date() +
		 "\nExpiration time is : " + 
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
   * else return true.
   */
  protected boolean timerExpired() {
    synchronized (timerLock) {
      return timer != null && timer.hasExpired();
    }
  }

  /** When will (has) the timer expire */
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
   * My subscription predicate, which matches RSVPRelaySource objects
   */
  private static class RelaySourcePred implements UnaryPredicate {
    public boolean execute(Object o) {
      return (o instanceof RSVPRelaySource);
    }
  }

  private static class EntityPred implements UnaryPredicate {
    public boolean execute(Object o) {
      return (o instanceof Entity);
    }
  }
}
