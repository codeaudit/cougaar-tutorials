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
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.service.PrototypeRegistryService;
import org.cougaar.pizza.asset.KitchenAsset;
import org.cougaar.pizza.asset.PropertyGroupFactory;
import org.cougaar.util.Arguments;

/**
 * This COUGAAR Plugin creates and publishes the Pizza Provider Kitchen
 * Asset object which identifies what kind of pizza that provider's
 * kitchen can make.
 */
public class KitchenPrototypePlugin extends ComponentPlugin {

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
    * Used by the binding utility through introspection to set my DomainService
    * Services that are required for plugin usage should be set through reflection instead of explicitly
    * getting each service from your ServiceBroker in the load method. The setter methods are called after
    * the component is constructed but before the state methods such as initialize, load, setupSubscriptions, etc.
    * If the service is not available at that time the component will be unloaded.
    */
   public void setDomainService(DomainService aDomainService) {
     domainService = aDomainService;
   }

  /**
   * Used by the binding utility through introspection to set my PrototypeRegistryService
   */
  public void setPrototypeRegistryService(PrototypeRegistryService aPrototypeRegistryService) {
    prototypeRegistryService = aPrototypeRegistryService;
  }

  /**
   * Get the plugin parameters and our factory when our component is loaded.
   * If we want additional services that are not required for the plugin, we should get them in load.
   * However, if the component gets a service in load, it should override the unload() method and
   * release the services.
   */
  public void load() {
    super.load();
    factory = (PlanningFactory)domainService.getFactory("planning");
    // Process the plugin params
    args = new Arguments(getParameter());
    setKitchenParameters();
  }

  /**
   * Used for initialization to populate the Blackboard with Kitchen prototype and Asset objects
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
    KitchenAsset new_prototype = (KitchenAsset)factory.createPrototype(KitchenAsset.class, "kitchen");
    // Cache the prototype in the LDM so that it can be used to create
    // instances of Kitchen assets when asked for by prototype name
    prototypeRegistryService.cachePrototype("kitchen", new_prototype);
    KitchenAsset kitchen_asset = (KitchenAsset) factory.createInstance("kitchen");
    // Check the plugin arguments to see if this plugin should create a Kitchen asset that can make veggie pizza.
    if (createVeggie) {
      kitchen_asset.addOtherPropertyGroup(PropertyGroupFactory.newVeggiePG());
    }
    // Check the plugin arguments to see if this plugin should create a Kitchen asset that can make meat pizza.
    if (createMeat) {
      kitchen_asset.addOtherPropertyGroup(PropertyGroupFactory.newMeatPG());
    }
    NewItemIdentificationPG itemIDPG = PropertyGroupFactory.newItemIdentificationPG();
    itemIDPG.setItemIdentification("Pizza Kitchen");
    kitchen_asset.setItemIdentificationPG(itemIDPG);
    getBlackboardService().publishAdd(kitchen_asset);
  }

  /**
   * No subscriptions, so this method does nothing
   */
  protected void execute () {}

  /**
   * Set the createVeggie and createMeat plugin variables according to the plugin param that was read in.
   */
  private void setKitchenParameters() {
    String paramValue = args.getString(PIZZA_TYPE_PARAM);
    if (paramValue != null) {
      if ("all".equals(paramValue)) {
        createVeggie = true;
        createMeat = true;
      } else if ("veggie_only".equals(paramValue)) {
        createVeggie = true;
        createMeat = false;
      } else if ("meat_only".equals(paramValue)) {
        createVeggie = false;
        createMeat = true;
      }
    }
  }

}

