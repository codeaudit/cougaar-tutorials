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

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import org.cougaar.pizza.Constants;
import org.cougaar.pizza.servlet.HistoryServletFriendly;

/**
 * Local accumulation of replies to invite, marking who has replied and the kinds
 * of pizza they want. Automatically updated by the {@link RSVPRelaySource}. Published
 * by the InvitePlugin so that the PlaceOrderPlugin knows to start.
 * @see org.cougaar.pizza.plugin.InvitePlugin
 * @see org.cougaar.pizza.plugin.PlaceOrderPlugin
 */
public class PizzaPreferences implements UniqueObject, HistoryServletFriendly {
  private UID uid;
  // Map of accumulated results
  private Map friendToPizza = new HashMap();

  // Note this static method of getting a logger within an object.
  private static Logger log = Logging.getLogger(PizzaPreferences.class);

  // Total meat and veg to order
  private int numMeat = 0;
  private int numVeg = 0;

  public PizzaPreferences (UID uid) {
    this.uid = uid;
  }

  // for UniqueObject interface
  public UID getUID() {
    return uid;
  }

  public void setUID(UID uid) {
    this.uid = uid;
  }

  /**
   * The given friend has replied, requesting the given pizza, so store
   * their response.
   * 
   * @param friend The agent name responding
   * @param preference The kind of meat they like (using a pizza Constant)
   */
  public void addFriendToPizza(String friend, String preference) {
    if (friend == null || friend.equals(""))
      return;

    friendToPizza.put(friend, preference);

    if (preference.equals(Constants.MEAT_PIZZA))
      numMeat++;
    else if (preference.equals(Constants.VEGGIE_PIZZA))
      numVeg++;
    else
      log.warn("Unknown preference " + preference + " for " + friend);
  }

  /**
   * What kind of pizza does the given friend want?
   * @param friend to look up
   * @return kind of pizza they want
   */
  public String getPreferenceForFriend(String friend) {
    return (String) friendToPizza.get(friend);
  }

  public Set getFriends() {
    return friendToPizza.keySet();
  }

  /** @return printable list of the friends we have preferences for */
  public String getFriendNames() {
    return friendToPizza.keySet().toString();
  }

  /** @return printable list of just the preferences collected */
  public String getPreferenceValues() {
    return friendToPizza.values().toString();
  }

  /** @return printable list of the friend-to-preference mapping */
  public String getFriendToPreference() {
    return friendToPizza.toString();
  }

  /** @return Number of meat pizzas wanted */
  public int getNumMeat() {
    return numMeat;
  }

  /** @return Number of veggie pizzas wanted */
  public int getNumVeg() {
    return numVeg;
  }

  /**
   * Print an HTML-friendly description of the object, for use in the HistoryServlet.
   * @param type Not-used here, is the transaction type
   * @return HTML text to display this object
   */
  public String toHTML (int type) {
    StringBuffer buf = new StringBuffer();
    buf.append("<table border=1 align=center>");
    buf.append("<tr>");
    buf.append("<th>");
    buf.append("Friend");
    buf.append("</th>");
    buf.append("<th>");
    buf.append("Preference");
    buf.append("</th>");
    buf.append("</tr>");
    for (Iterator iter = new TreeSet(getFriends()).iterator(); iter.hasNext(); ) {
      buf.append("<tr>");

      buf.append("<td>");
      String friend = (String) iter.next();
      buf.append(friend);
      buf.append("</td>");

      buf.append("<td>");
      String preference = getPreferenceForFriend(friend);
      buf.append(preference);
      buf.append("</td>");

      buf.append("</tr>");
    }
    buf.append("</table>");
    return buf.toString();
  }

  public String toString() {
    return "Party guests pizza preferences: " + friendToPizza;
  }
}
