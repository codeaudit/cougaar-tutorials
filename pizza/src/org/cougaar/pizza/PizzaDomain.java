/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.pizza;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.domain.DomainAdapter;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DomainService;
import org.cougaar.pizza.asset.AssetFactory;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.service.LDMService;

import java.util.Collection;
import java.util.Collections;


/**
 * PizzaDomain definition -
 * Required to ensure that Roles and Assets specific to the pizza
 * application are initialized correctly. PizzaDomain does not include any
 * domain specific LogicProviders. It only initializes our Constants
 * and loads our Asset Factory.
 *
 * Other applications might have custom objects to transmit between
 * agents, and therefore need their own LogicProviders. Similarly,
 * they might want their own XPlan, to have custom-tuned lookup
 * methods for objects (the Planning LogPlan for example looks up
 * PlanElements by Task UID).
 **/
public class PizzaDomain extends DomainAdapter {

  // Note the constant for the domain name, with the constant
  // following the <domain>_NAME variable naming pattern
  public static final String PIZZA_NAME = "pizza";

  private DomainService domainService;
  private LDMService ldmService;

  public String getDomainName() {
    return PIZZA_NAME;
  }

  public PizzaDomain() {
    super();
  }

  // Note the use of the introspection-based service retrieving
  // methods. These guarantee that the component
  // will not load if the services are not available.

  public void setDomainService(DomainService domainService) {
    this.domainService = domainService;
  }

  public void setLDMService(LDMService ldmService) {
    this.ldmService = ldmService;
  }

  public void initialize() {
    super.initialize();

    // Domain is loaded before any plugins. Call to Constants.Role.init() 
    // creates all Roles specific to the Pizza domain before they are 
    // accessed by application code. This insures that that the Roles and
    // their converses are defined consistently.
    Constants.Roles.init();    // Insure that our Role constants are initted
  }

  public void unload() {
    // Unload any services we loaded earlier
    ServiceBroker sb = getServiceBroker();
    if (ldmService != null) {
      sb.releaseService(
          this, LDMService.class, ldmService);
      ldmService = null;
    }
    if (domainService != null) {
      sb.releaseService(
          this, DomainService.class, domainService);
      domainService = null;
    }
    super.unload();
  }

  public Collection getAliases() {
    return Collections.EMPTY_LIST;
  }

  protected void loadFactory() {
    LDMServesPlugin ldm = ldmService.getLDM();
    PlanningFactory ldmf = (PlanningFactory) ldm.getFactory("planning");
    if (ldmf == null) {
      throw new RuntimeException("Missing \"planning\" factory!");
    }

    // Adding pizza specific AssetFactory and PropertyGroupFactory allows 
    // pizza application code to use ldmf.createAsset(<asset class name>) for
    // the assets defined in org.cougaar.pizza.asset. For example, 
    // ldmf.createAsset("KitchenAsset")
    ldmf.addAssetFactory(new AssetFactory());
    ldmf.addPropertyGroupFactory(new org.cougaar.pizza.asset.PropertyGroupFactory());
  }

  protected void loadXPlan() {
    // no Pizza specific XPlan
  }

  protected void loadLPs() {
    // no Pizza specific LPs
  }

}


