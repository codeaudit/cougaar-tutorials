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

package org.cougaar.pizza.util;

import java.util.Iterator;

import org.cougaar.pizza.Constants;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceInfo;
import org.cougaar.servicediscovery.description.ServiceInfoScorer;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

public class RoleScorer implements ServiceInfoScorer {
  private static Logger logger = Logging.getLogger(RoleScorer.class);
  Role myRole;
  String myAgentName = null;

  public RoleScorer(Role role) {
    myRole = role;
  }

  public void setRole(Role role) {
    if (myRole != null) {
      logger.warn("setRole: ignoring attempt to change Role from " + 
		  myRole + " to " + role);
    } else {
      myRole = role;
    }
  }

  public Role getRole() {
    return myRole;
  }
  

  /**
   * Will be called by Matchmaker for each ServiceInfo. Returned score will
   * be added to the ScoredServiceDescription associated with the Service.
   * 
   * @return int representing score. Client responsible for 
   * understanding the precise value. Current usage assumes lowest value >= 0
   * is the best. Values less than 0 are not suitable.
   * 
   */
  public int scoreServiceInfo(ServiceInfo serviceInfo) {
    int roleScore = getRoleScore(serviceInfo);

    if (logger.isDebugEnabled()) {
      logger.debug("scoreServiceProvider: Role score " + roleScore);
    }

    return roleScore;
  }

  protected int getRoleScore(ServiceInfo serviceInfo) {
    String serviceRole = null;

    for (Iterator iterator = serviceInfo.getServiceClassifications().iterator();
	 iterator.hasNext();) {
      ServiceClassification classification =
	(ServiceClassification) iterator.next();
      if (classification.getClassificationSchemeName().equals(Constants.UDDIConstants.COMMERCIAL_SERVICE_SCHEME)) {

	serviceRole = classification.getClassificationCode();
	break;
      }
    }

    if (serviceRole == null) {
      if (logger.isInfoEnabled()) {
	logger.info(myAgentName + 
		    ": Ignoring service with a bad service role for provider: " + 
		    serviceInfo.getProviderName());
      }
      return -1;
    } if (!serviceRole.equals(myRole.toString())) {
      if (logger.isInfoEnabled()) {
	logger.info(myAgentName + ": Ignoring service with Role of : " +
		    serviceRole + 
		    " for provider: " + serviceInfo.getProviderName());
      }
      return -1;
    } else {
      return 0;
    }
  }

  public boolean equals(Object o) {
    if (o instanceof RoleScorer) {
      RoleScorer scorer = (RoleScorer) o;

      return (scorer.getRole().equals(getRole()));
    } else {
      return false;
    }
  }

  public String toString() {
    return "Role: " + myRole;
  }

}






