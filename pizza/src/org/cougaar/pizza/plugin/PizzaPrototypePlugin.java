/*
 * <copyright>
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.pizza.plugin;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.service.PrototypeRegistryService;
import org.cougaar.pizza.asset.PizzaAsset;
//import org.cougaar.pizza.util.PGCreator;

/**
 * This COUGAAR Plugin creates and publishes Pizza Asset objects.
 */
public class PizzaPrototypePlugin extends ComponentPlugin {

  // The domainService acts as a provider of domain factory services
  private DomainService domainService = null;

  // The prototypeRegistryService acts as a provider of prototype rehistration services
  private PrototypeRegistryService prototypeRegistryService = null;

  /**
   * Used by the binding utility through reflection to set my DomainService
   */
  public void setDomainService(DomainService aDomainService) {
    domainService = aDomainService;
  }

  /**
   * Used by the binding utility through reflection to get my DomainService
   */
  public DomainService getDomainService() {
    return domainService;
  }

  /**
   * Used by the binding utility through reflection to set my PrototypeRegistryService
   */
  public void setPrototypeRegistryService(PrototypeRegistryService aPrototypeRegistryService) {
    prototypeRegistryService = aPrototypeRegistryService;
  }

  /**
   * Used by the binding utility through reflection to get my PrototypeRegistryService
   */
  public PrototypeRegistryService getPrototypeRegistryService() {
    return prototypeRegistryService;
  }

  /**
   * Used for initialization to register the PizzaAsset prototype 
   */
  protected void setupSubscriptions() {

    // Get the PlanningFactory
    PlanningFactory factory = (PlanningFactory)getDomainService().getFactory("planning");

    // Register our new PropertyFactory so we can refer to properties by name
    factory.addPropertyGroupFactory(new org.cougaar.pizza.asset.PropertyGroupFactory());

    // Create the prototypes that will be used to create the programmer assets
    PizzaAsset new_prototype = (PizzaAsset)factory.createPrototype
      (org.cougaar.pizza.asset.PizzaAsset.class, "pizza");
    // Cache the prototype in the LDM : note this is not treated
    // as an asset that is available in subscriptions, but can
    // be used to build 'real' assets when asked for by prototype name
    getPrototypeRegistryService().cachePrototype("pizza", new_prototype);

    // Create a Veggie Pizza Asset based on the existing pizza prototype
    //TODO: Lora put this in the invite plugin
    //PizzaAsset veggie_pizza_asset = (PizzaAsset) factory.createInstance("pizza");
    //veggie_pizza_asset.addOtherPropertyGroup(PGCreator.makeAVeggiePG(factory, true));
    //veggie_pizza_asset.setItemIdentificationPG(PGCreator.makeAItemIdentificationPG(factory, "Veggie Pizza"));
    //getBlackboardService().publishAdd(veggie_pizza_asset);

    // Create a Meat Pizza Asset based on the existing pizza prototype
    //PizzaAsset meat_pizza_asset = (PizzaAsset) factory.createInstance("pizza");
    //meat_pizza_asset.addOtherPropertyGroup(PGCreator.makeAMeatPG(factory, true));
    //meat_pizza_asset.setItemIdentificationPG(PGCreator.makeAItemIdentificationPG(factory, "Meat Pizza"));
    //getBlackboardService().publishAdd(meat_pizza_asset);
    // TODO: End of new stuff for invite plugin
  }

  /**
   * No subscriptions, so this method does nothing
   */
  protected void execute () {}

}

