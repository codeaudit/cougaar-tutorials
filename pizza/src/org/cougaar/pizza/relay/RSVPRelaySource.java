/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.pizza.relay;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.plugin.PizzaPreferences;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import java.util.HashSet;
import java.util.Set;

/**
 * A source-side {@link Relay} for RSVPs.
 * <p/>
 * As responses come back from remote agents, the updateResponse
 * method is called and entries are added to the {@link PizzaPreferences} 
 * object.
 * @see org.cougaar.pizza.plugin.InvitePlugin
 * @see org.cougaar.pizza.plugin.PizzaPreferences
 */
public class RSVPRelaySource implements Relay.Source, UniqueObject {
  private final Object query; // The query in our RSVP -- meat or veg?
  private final UID uid;

  private final long creationTime = System.currentTimeMillis();

  /** 
   * The set of target addresses - in this case, only one, the ABA 
   * @see org.cougaar.multicast.AttributeBasedAddress
   */
  private Set targets = new HashSet();

  /** The source-side accumulation of RSVPs, so we can automatically accumulate responses */
  private PizzaPreferences pizzaPreferences;

  /**
   * A static logger for all rsvp relay source objects.
   * Note that this is a nice way to use the Cougaar logger from an object that
   * is not a Component, and therefore can't easily get an instace of the LoggingService.
   */
  private static Logger classLogger = 
    Logging.getLogger(RSVPRelaySource.class);

  public RSVPRelaySource(UID uid,
                         MessageAddress target,
                         Object query,
                         PizzaPreferences pizzaPreferences) {
    this.uid = uid; 
    this.query = query;
    this.pizzaPreferences = pizzaPreferences;
    targets.add(target);
  }

  /**
   * Who gets this invitation?
   *
   * @return set of targets (For us, just one member - the AttributeBasedAddress)
   */
  public Set getTargets() {
    return targets;
  }

  /**
   * The query in the relay.
   *
   * @return the query in the RSVP - meat or veg?
   */
  public Object getContent() {
    return query;
  }

  /**
   * Get the Factory for RSVPTargets. 
   * <p/>
   * Make sure on the target side we create a target relay.
   * The target relay has just 
   * a source, and no target address, so the the relay won't be propagated
   * at the target.
   * <p/>
   * An alternative would be to have the relay implement both source and
   * target interfaces, but this could lead to endless pinging in this case
   * where the target address is an ABA broadcast to all members of the
   * community.
   *
   * @return target factory that makes a target that has no target addresses
   */
  public TargetFactory getTargetFactory() {
    return RSVPTargetRelayFactory.getTargetFactory();
  }

  /**
   * Record responses from remote agents as they come in on the pizza
   * preferences object.
   * <p/>
   * Note that we assume the response will be a {@link RSVPReply}.
   * <p/>
   * If INFO logging is on, tells how long we had to wait until all responses
   * came back.
   *
   * @return Relay.RESPONSE_CHANGE - since every time we get a response, we
   *         want to examine it
   */
  public int updateResponse(MessageAddress target, Object response) {
    RSVPReply rsvpReply = (RSVPReply) response;

    // Accumulate the response automatically
    pizzaPreferences.addFriendToPizza(rsvpReply.getFriend(),
                                      rsvpReply.getPizzaPreference());

    // INFO logging of what we have so far.
    if (classLogger.isInfoEnabled()) {
      classLogger.info("RSVPRelaySource - pizza prefs now : " + pizzaPreferences);
      
      if ((pizzaPreferences.getNumMeat() + pizzaPreferences.getNumVeg()) ==
	  Constants.EXPECTED_NUM_FRIENDS) {
	classLogger.info("Waited " + ((System.currentTimeMillis() - creationTime) / 1000) +
			 " seconds to get responses back from " +
			 Constants.EXPECTED_NUM_FRIENDS +
			 " friends (including party planner).");
      }
    }
    
    return Relay.RESPONSE_CHANGE;
  }

  /**
   * @return the PizzaPreferences object in which our answers are collected
   */
  public PizzaPreferences getPizzaPrefs() {
    return pizzaPreferences;
  }

  /** 
   * Implemented for UniqueObject interface.
   * @return the UID of this UniqueObject.
   **/
  public UID getUID() { return uid; }

  /** 
   * Does nothing - not allowed to reset UID.
   * Implemented for UniqueObject interface.
   **/
  public void setUID(UID uid) {}

  public String toString() {
    return "RSVPRelaySource: " +
        " query=" + getContent() +
        " target=" + targets +
        " preferences=" + pizzaPreferences;
  }
}
