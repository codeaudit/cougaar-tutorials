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
import org.cougaar.pizza.Constants;
import org.cougaar.planning.ldm.asset.Entity;

/**
 * Checks if entity likes meat or veg pizza by checking its role -
 * Carnivore or Vegetarian.
 */
public class PizzaPreferenceHelper {

  /**
   * Using self entity from blackboard, get its entity pg, and see if it
   * includes a role "carnivore"
   *
   * @return "meat" if carnivore, "veg" if herbivore
   */
  public String getPizzaPreference(LoggingService log, Entity entity) {
    boolean likeMeat = entity.getEntityPG().getRoles().contains(Constants.Roles.CARNIVORE);
    log.info("roles for entity " + entity + " are " +
             entity.getEntityPG().getRoles());

    if (likeMeat) {
      return Constants.MEAT_PIZZA;
    } else {
      return Constants.VEGGIE_PIZZA;
    }
  }
}
