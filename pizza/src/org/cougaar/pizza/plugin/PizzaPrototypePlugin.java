/*
 * <copyright>
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency
 *  (DARPA).
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
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.asset.PizzaAsset;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.service.PrototypeRegistryService;

/**
 * This COUGAAR Plugin creates and publishes Pizza Asset objects.
 */
public class PizzaPrototypePlugin extends ComponentPlugin {

  // The domainService acts as a provider of domain factory services
  private DomainService domainService = null;

  // The prototypeRegistryService acts as a provider of prototype
  // registration services
  private PrototypeRegistryService prototypeRegistryService = null;

  // The planning factory we use to create planning objects.
  private PlanningFactory factory;


  /**
   * Set up the services and factories we need.
   */
  public void load() {
    super.load();

    // get services
    domainService = (DomainService)
        getServiceBroker().getService(this, DomainService.class, null);
    prototypeRegistryService = (PrototypeRegistryService)
        getServiceBroker().getService(this, PrototypeRegistryService.class,
                                      null);
    factory = (PlanningFactory)domainService.getFactory("planning");
  }


  /**
   * Initialize our plugin. Since we don't have any subscriptions,
   * create the Pizza prototype right away.
   */
  protected void setupSubscriptions() {
    createPizzaPrototype();
  }

  private void createPizzaPrototype() {
    // Register our new PropertyGroupFactory
    factory.addPropertyGroupFactory(
        new org.cougaar.pizza.asset.PropertyGroupFactory());

    PizzaAsset new_prototype = (PizzaAsset)factory.createPrototype
        (PizzaAsset.class, Constants.PIZZA);
    // Cache the prototype in the LDM so that other plugins can create
    // Pizza instances using the new prototype.
    prototypeRegistryService.cachePrototype(Constants.PIZZA,
                                            new_prototype);
  }

  /**
   * No subscriptions, so this method does nothing
   */
  protected void execute () {}
}

