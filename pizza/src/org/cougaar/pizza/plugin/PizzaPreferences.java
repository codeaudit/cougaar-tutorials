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
import java.util.Map;
import java.util.Set;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import org.cougaar.pizza.Constants;

/**
 * Stores mapping from friend to their pizza preference
 * <p/>
 * Counts meat and veg preferences.
 */

public class PizzaPreferences implements UniqueObject {
  private UID uid;
  private Map friendToPizza = new HashMap();

  private static Logger log = Logging.getLogger(PizzaPreferences.class);

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

  public void addFriendToPizza(String friend, String preference) {
    friendToPizza.put(friend, preference);

    if (preference.equals(Constants.MEAT_PIZZA))
      numMeat++;
    else if (preference.equals(Constants.VEGGIE_PIZZA))
      numVeg++;
    else
      log.warn("unknown preference " + preference + " for " + friend);
  }

  public String getPreferenceForFriend(String friend) {
    return (String) friendToPizza.get(friend);
  }

  public Set getFriends() {
    return friendToPizza.keySet();
  }

  public String getFriendNames() {
    return friendToPizza.keySet().toString();
  }

  public String getPreferenceValues() {
    return friendToPizza.values().toString();
  }

  public String getFriendToPreference() {
    return friendToPizza.toString();
  }

  public int getNumMeat() {
    return numMeat;
  }

  public int getNumVeg() {
    return numVeg;
  }

  public String toString() {
    return "Party guests pizza preferences : " + friendToPizza;
  }
}
