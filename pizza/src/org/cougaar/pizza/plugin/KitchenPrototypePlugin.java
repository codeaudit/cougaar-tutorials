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
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.service.PrototypeRegistryService;
import org.cougaar.pizza.asset.KitchenAsset;
import org.cougaar.pizza.asset.PropertyGroupFactory;
import org.cougaar.pizza.util.PGCreator;
import org.cougaar.util.Arguments;

/**
 * This COUGAAR Plugin creates and publishes the Pizza Provider Kitchen
 * Asset object which identifies what kind of pizza that provider's
 * kitchen can make.
 */
public class KitchenPrototypePlugin extends ComponentPlugin {

  //The logging service acts as a central logger for each component
  private LoggingService logger;

  // The domainService acts as a provider of domain factory services
  private DomainService domainService = null;

  // The prototypeRegistryService acts as a provider of prototype
  // registration services
  private PrototypeRegistryService prototypeRegistryService = null;

  // The planning factory we use to create planning objects.
  private PlanningFactory factory;

  // Initialize my plugin args to the empty instance
  private Arguments args = Arguments.EMPTY_INSTANCE;

  /**
   * Variables for storing plugin arguments that determine whether the
   * kitchen assets are capable of creating meat and/or veggie pizzas.
   * Default is to create both.
   */
  private boolean createVeggie = true;
  private boolean createMeat = true;
  private final String PIZZA_TYPE_PARAM = "PIZZA_TYPES_PROVIDED";

  /**
   * setParameter is only called by the infrastructure
   * if a plugin has parameters
   */
  public void setParameter(Object o) {
    args = new Arguments(o);
    super.setParameter(o);
  }

  /**
   * Initialize the plugin with the plugin parameters and get our
   * services.
   */
  public void load() {
    super.load();
    // get services
    domainService = (DomainService)
        getServiceBroker().getService(this, DomainService.class, null);
    prototypeRegistryService = (PrototypeRegistryService)
        getServiceBroker().getService(this, PrototypeRegistryService.class,
                                      null);
    logger = (LoggingService)
        getServiceBroker().getService(this, LoggingService.class, null);
    factory = (PlanningFactory)domainService.getFactory("planning");
    // Get the plugin params and initialize the createVeggie and
    // createMeat booleans
    String pizzaTypeValue = args.getString(PIZZA_TYPE_PARAM);
    setKitchenParameters(pizzaTypeValue);
  }

  /**
   * Used for initialization to populate the Blackboard with Kitchen
   * prototype and Asset objects
   */
  protected void setupSubscriptions() {
    // create the kitchen prototypes and assets
    createKitchenAssets();
  }

  /**
   * Create the Kitchen prototype and asset instances with the appropriate
   * veggie and/org meat PGs based on the existing kitchen prototype
   * and the plugin arguments.
   */
  private void createKitchenAssets() {
    // Register our PropertyGroupFactory
    factory.addPropertyGroupFactory(new PropertyGroupFactory());

    KitchenAsset new_prototype = (KitchenAsset)factory.
        createPrototype(KitchenAsset.class, "kitchen");
    // Cache the prototype in the LDM so that it can be used to create
    // instances of Kitchen assets when asked for by prototype name
    prototypeRegistryService.cachePrototype("kitchen", new_prototype);
    KitchenAsset kitchen_asset = (KitchenAsset)
        factory.createInstance("kitchen");
    // Check the plugin arguments to see if this plugin should create a
    // Kitchen asset that can make veggie pizza.
    if (createVeggie) {
      kitchen_asset.
          addOtherPropertyGroup(PGCreator.makeAVeggiePG(factory, true));
    }
    // Check the plugin arguments to see if this plugin should create a
    // Kitchen asset that can make meat pizza.
    if (createMeat) {
      kitchen_asset.
          addOtherPropertyGroup(PGCreator.makeAMeatPG(factory, true));
    }
    kitchen_asset.setItemIdentificationPG(
        PGCreator.makeAItemIdentificationPG(factory, "Pizza Kitchen"));
    getBlackboardService().publishAdd(kitchen_asset);
  }

  /**
   * No subscriptions, so this method does nothing
   */
  protected void execute () {}

  /**
   * Set the createVeggie and createMeat plugin variables according to
   * the plugin param that was read in.
   */
  private void setKitchenParameters(String paramValue) {
    if (logger.isDebugEnabled()) {
      logger.debug("Found " + PIZZA_TYPE_PARAM + " param of: " +
                   paramValue.trim());
    }
    if (paramValue.trim().equals("all")) {
      createVeggie = true;
      createMeat = true;
    } else if (paramValue.trim().equals("veggie_only")) {
      createVeggie = true;
      createMeat = false;
    } else if (paramValue.trim().equals("meat_only")) {
      createVeggie = false;
      createMeat = true;
    } 
  }

}

