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
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.service.PrototypeRegistryService;
import org.cougaar.pizza.asset.KitchenAsset;
import org.cougaar.pizza.util.PGCreator;

import java.util.Collection;
import java.util.Iterator;

/**
 * This COUGAAR Plugin creates and publishes the Pizza Provider Kitchen Asset object
 * which identifies what kind of pizza that provider's kitchen can make.
 */
public class KitchenPrototypePlugin extends ComponentPlugin {

  //The logging service acts as a central logger for each component
  private LoggingService logger;

  // The domainService acts as a provider of domain factory services
  private DomainService domainService = null;

  // The prototypeRegistryService acts as a provider of prototype rehistration services
  private PrototypeRegistryService prototypeRegistryService = null;

  /**
   * Used by the binding utility through reflection to get my LoggingService
   */
  public LoggingService getLoggingService(Object requestor) {
    LoggingService ls = (LoggingService)
        getServiceBroker().getService(requestor, LoggingService.class, null);
    return LoggingServiceWithPrefix.add(ls, getAgentIdentifier() + ": ");
  }

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
   * Variables for storing plugin arguments that determine whether the kitchen assets
   * are capable of creating meat and/or veggie pizzas.
   * Default is to create both.
   */
  private boolean createVeggie = true;
  private boolean createMeat = true;

  /**
   * Used to initialize the plugin with the plugin parameters
   */
  public void load() {
    super.load();
    logger = getLoggingService(this);
    // The readParameters method will initialize the createVeggie and createMeat booleans
    readKitchenParameters();
  }

  /**
   * Used for initialization to populate the Blackboard with Kitchen prototype and Asset objects
   */
  protected void setupSubscriptions() {

    // Get the PlanningFactory
    PlanningFactory factory = (PlanningFactory)getDomainService().getFactory("planning");

    // Register our new PropertyFactory so we can refer to properties by name
    factory.addPropertyGroupFactory(new org.cougaar.pizza.asset.PropertyGroupFactory());

    // Create the prototype that will be used to create the kitchen assets
    KitchenAsset new_prototype = (KitchenAsset)factory.createPrototype
      (org.cougaar.pizza.asset.KitchenAsset.class, "kitchen");
    // Cache the prototype in the LDM : note this is not treated
    // as an asset that is available in subscriptions, but can
    // be used to build 'real' assets when asked for by prototype name
    getPrototypeRegistryService().cachePrototype("kitchen", new_prototype);

    // Create a Kitchen Asset with the appropriate veggie and/or meat PGs based on the
    // existing kitchen prototype and the plugin arguments
    KitchenAsset kitchen_asset = (KitchenAsset) factory.createInstance("kitchen");
    // Check the plugin arguments to see if this plugin should create a
    // Kitchen asset that can make veggie pizza.
    if (createVeggie) {
      kitchen_asset.addOtherPropertyGroup(PGCreator.makeAVeggiePG(factory, true));
    }
    // Check the plugin arguments to see if this plugin should create a
    // Kitchen asset that can make meat pizza.
    if (createMeat) {
      kitchen_asset.addOtherPropertyGroup(PGCreator.makeAMeatPG(factory, true));
    }
    kitchen_asset.setItemIdentificationPG(PGCreator.makeAItemIdentificationPG(factory, "Pizza Kitchen"));
    getBlackboardService().publishAdd(kitchen_asset);

  }

  /**
   * No subscriptions, so this method does nothing
   */
  protected void execute () {}

  /**
   * Reads the plugin parameters.
   */
  private void readKitchenParameters() {
    Collection params = getParameters();
    if (params.isEmpty()) {
      if (logger.isInfoEnabled()) {
        logger.info("No parameters. Assuming that this kitchen provides both meat and veggie pizzas");
      }
      return;
    }
    //Walk through the plugin params and find the one we are interested in.
    int index;
    for (Iterator i = params.iterator(); i.hasNext();) {
      String s = (String) i.next();
      if ((index = s.indexOf('=')) != -1) {
        String paramName = new String(s.substring(0, index));
        if (paramName.trim().equals("PIZZA_TYPES_PROVIDED")) {
          setKitchenParameters(new String(s.substring(index + 1, s.length())));
          return;
        }
      }
    }
  }

  /**
   * Set the createVeggie and createMeat plugin variables according to the plugin param that was read in.
   */
  private void setKitchenParameters(String paramValue) {
    if (logger.isDebugEnabled()) {
      logger.debug("Found PIZZA_TYPES_PROVIDED Plugin param of: " + paramValue.trim());
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
    } else if (paramValue.trim().equals("none")) {
      createVeggie = false;
      createMeat = false;
    }
  }

}

