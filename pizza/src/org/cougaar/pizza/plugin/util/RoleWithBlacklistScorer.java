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

package org.cougaar.pizza.plugin.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.pizza.Constants;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceInfo;
import org.cougaar.servicediscovery.description.ServiceInfoScorer;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * ServiceDiscovery Service scoring function using <code>Role</code> name and an
 * exclusion list. 
 * <br><pre>
 * Uses 2 criteria -  
 *   service role must match scorer role (in Commercial Service Scheme)
 *   service provider name must not match one of the names on the blacklist.
 *</pre><p>
 * All passing descriptions get a score of 1, all failing descriptions get
 * a score of -1.
 * <p>
 * <code>SDClientPlugin</code> creates the <code>RoleWithBlacklistScorer</code> and attaches it to the <code>MMQueryRequest</code>.
 * <code>MatchmakerPlugin</code> uses the <code>RoleWithBlacklistScorer</code> to evaluate service descriptions
 * returned from the yellow pages. All passing service descriptions are added
 * to the <code>MMQueryRequest</code> results field.
 *<p>
 * This Scorer is a simple variation on the RoleScorer in the ServiceDiscovery module, as an example
 * of how to modify this.
 *
 * @see org.cougaar.servicediscovery.util.RoleScorer
 */
public class RoleWithBlacklistScorer implements ServiceInfoScorer, Serializable {
  // Note this is how a non-component can get a Logger
  private static Logger logger = Logging.getLogger(RoleWithBlacklistScorer.class);
  private Role myRole; // The role we want
  private Collection myBlacklist; // providers to exclude -- for example, those we've already tried

  public RoleWithBlacklistScorer(Role role, Collection blacklist) {
    myRole = role;
    myBlacklist = blacklist;
  }

  /**
   * What Role is being requested?
   * @return the Role required for this request
   **/
  public Role getRole() {
    return myRole;
  }
  

  /**
   * Which providers are not acceptable?
   * @return the Collection of excluded provider names (Strings)
   **/
  public Collection getBlacklist() {
    return myBlacklist;
  }

  /**
   * Will be called by MatchmakerPlugin for each ServiceInfo. Returned score will
   * be added to the ScoredServiceDescription associated with the Service.
   * 
   * @param serviceInfo The ServiceInfo returned by the YP for which we want a score
   * @return int representing score. Client responsible for 
   * understanding the precise value. Current usage assumes lowest value >= 0
   * is the best. Values less than 0 indicate the provider is not suitable.
   * 
   */
  public int scoreServiceInfo(ServiceInfo serviceInfo) {
    // If the blacklist scoring says the provider is no good, its no good
    if (getBlacklistScore(serviceInfo) < 0) {
      return -1;
    }

    // Otherwise, we just use the role score
    int roleScore = getRoleScore(serviceInfo);

    return roleScore;
  }

  /** 
   * Score the role portion -- lowest non-negative score is best. 
   * Note that it assumes that the Role is in the Commercial Service Scheme.
   */
  private int getRoleScore(ServiceInfo serviceInfo) {
    String serviceRole = null;

    // Find the correct service classification code
    for (Iterator iterator = serviceInfo.getServiceClassifications().iterator();
	 iterator.hasNext();) {
      ServiceClassification classification =
	(ServiceClassification) iterator.next();
      // Here we assume / require where the Role will be classified
      if (classification.getClassificationSchemeName().equals(Constants.UDDIConstants.COMMERCIAL_SERVICE_SCHEME)) {

	serviceRole = classification.getClassificationCode();
	break;
      }
    }

    if (serviceRole == null) {
      if (logger.isInfoEnabled()) {
	logger.info("Ignoring service (score is -1) with a bad service role for provider: " + 
		    serviceInfo.getProviderName());
      }
      return -1;
    } else if (!serviceRole.equals(myRole.toString())) {
      // If this is not the role we're looking for, score it down
      if (logger.isInfoEnabled()) {
	logger.info("Ignoring service (score is -1) with (wrong) Role of: " +
		    serviceRole + ", was looking for role " + myRole + 
		    " for provider: " + serviceInfo.getProviderName());
      }
      return -1;
    } else {
      // This is the role we're looking for!
      if (logger.isInfoEnabled()) {
	logger.info("Found good service (score is 0) with matching Role of: " +
		    serviceRole + 
		    " on provider: " + serviceInfo.getProviderName());
      }
      return 0;
    }
  }

  /** Score the service provider relative to the blacklist - if it's blacklisted, it gets -1 */
  private int getBlacklistScore(ServiceInfo serviceInfo) {
    // Look for the serviceInfo's ProviderName on the blacklist
    for (Iterator iterator = myBlacklist.iterator();
	 iterator.hasNext();) {
      String blacklistedProvider = (String) iterator.next();

      if (serviceInfo.getProviderName().equals(blacklistedProvider)) {
	if (logger.isInfoEnabled()) {
	  logger.info("Ignoring service (score is -1) from blacklisted provider - " + 
		      serviceInfo.getProviderName() +
		      ". Provider on the blacklist - " + myBlacklist);
	}
	// -1 means don't use this provider
	return -1;
      }
    } // loop over blacklist entries

    // Didn't find it on the Blacklist -- its good
    return 0;
  }

  public String toString() {
    return "<RoleWithBlacklistScorer Role: " + myRole + ", Blacklist: " + myBlacklist + ">";
  }

}






