/* 
 * <copyright>
 * 
 *  Copyright 2004 BBNT Solutions, LLC
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

import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.pizza.Constants;
import org.cougaar.pizza.plugin.util.RoleWithBlacklistScorer;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.servicediscovery.description.ServiceClassification;
import org.cougaar.servicediscovery.description.ServiceDescription;
import org.cougaar.servicediscovery.description.ServiceInfoScorer;
import org.cougaar.servicediscovery.plugin.SimpleSDClientPlugin;

import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * ServiceDiscovery ClientPlugin, responsible for initiating queries for services, 
 * taking the resulting service pointer, and sending a Relay to the Provider requesting service. 
 *<p>
 * Service Discovery is initiated by receiving a Find Providers task, and looking for the single Role
 * indicated on the AS Prepositional Phrase, possibly excluding the Providers listed in the NOT 
 * Prepositional Phrase. Sends a Disposition on the FindProviders Task when it gets a Relationship
 * with a provider for the service.
 *<p>
 * This is an extension of the SimpleSDClientPlugin that uses the pizza Domain constants and
 * specically forbids using providers listed in the Find Providers Task. Use this as an example
 * of how you can extend the SimpleSDClientPlugin for your own application.
 *<p>
 * Limitations: not quiescent aware, doesn't handle time-phased relationships, 
 * not guaranteed to work with persistence and restarts, and assumes that providers
 * will not revoke relationships or otherwise change their mind.
 */
public class SDClientPlugin extends SimpleSDClientPlugin {
  /////////
  // Methods to get the query to the Matchmaker...

  /**
   * Return Collection of the Entity in the indirect Object of the {@link Constants.Prepositions.NOT} Prep Phrases,
   * indicating a Provider not to return from Service Discovery.
   *
   * @param findProvidersTask Task requesting Service Discovery
   * @return Collection of Provider names not to use in Service Discovery, possibly empty
   */
  private Collection getExcludedProviders(Task findProvidersTask) {
    Enumeration phrases = findProvidersTask.getPrepositionalPhrases();
    ArrayList providers = new ArrayList();
    // Loop over Prepositional Phrases on the Task
    while (phrases.hasMoreElements()) {
      PrepositionalPhrase nPhrase = (PrepositionalPhrase)phrases.nextElement();
      // If this Phrase's preposition is NOT
      if (nPhrase != null && org.cougaar.pizza.Constants.Prepositions.NOT.equals(nPhrase.getPreposition())) {
	// Then add the Item ID of the Indirect Object (ie String name of the Provider) to the collection
	providers.add(((Asset)nPhrase.getIndirectObject()).getItemIdentificationPG().getItemIdentification());
      }
    }

    // The resulting collection (possibly empty) is the providers not to use
    return providers;
  }

  /**
   * Create a scoring function to weigh different services registered in the YP, for use
   * by the Matchmaker. 
   * In this case, create a function that requires the given role, and forbids any providers
   * named by the given Task.
   *<p>
   * Extenders of this plugin may over-ride this method to use a different function.
   *
   * @param role the Role to look for
   * @param fpTask The FindProviders task this will satisfy
   * @return the scoring function to hand the Matchmaker
   */
  protected ServiceInfoScorer getServiceInfoScorer(Role role, Task fpTask) {
    // Create a Service Scorer that looks for a provider with the given role,
    // excluding any providers specifically excluded on the FindProviders Task.
    // Note that another way to do this would be to exclude any providers currently supplying this Role
    Collection excludes = getExcludedProviders(fpTask);
    if (myLoggingService.isInfoEnabled())
      myLoggingService.info("Will excluded providers: " + excludes);
    return new RoleWithBlacklistScorer(role, excludes);
  }

  ///////////
  // Support methods for tasking Matchmaker response and requesting the service from the Provider

  /**
   * Retrieve the Role that the Provider is offering out of the ServiceDescription. 
   * Note that this assumes the role is under the CommercialServiceScheme, using the 
   * Domain's Constants file. Extenders will over-ride this method.
   *
   * @param serviceDescription The Description of the Service as advertised in the YP.
   * @return the Role advertised by the Provider, or null if none found.
   */
  protected Role getRole(ServiceDescription serviceDescription) {
    // A serviceDescription may have multiple Classifications
    for (Iterator iterator = serviceDescription.getServiceClassifications().iterator(); 
	 iterator.hasNext();) {
      ServiceClassification serviceClassification = (ServiceClassification) iterator.next();
      // We have placed the Roles providers are registering in the CommercialServiceScheme
      // (see pizza/data/taxonomies)
      if (serviceClassification.getClassificationSchemeName().equals(org.cougaar.pizza.Constants.UDDIConstants.COMMERCIAL_SERVICE_SCHEME)) {
        Role role = Role.getRole(serviceClassification.getClassificationName());
        return role;
      }
    }
    return null;
  }

  ////////////////
  // Now the Unary Predicates for use with subscriptions.

  /**
   * Predicate to get the FindProviders Tasks. In the off-chance that your verb is 
   * slightly different, you can over-ride this method to point to your
   * domain-specific Verb Constant.
   *<b>
   * Note that it is not a constant, just so it can be over-ridden.
   */ 
  protected UnaryPredicate getFindProvidersPredicate() {
    return new UnaryPredicate() {
	public boolean execute(Object o) {
	  if (o instanceof Task) {
	    return ((Task) o).getVerb().equals(org.cougaar.pizza.Constants.Verbs.FIND_PROVIDERS);
	  } else {
	    return false;
	  }
	}
      };
  }
}
