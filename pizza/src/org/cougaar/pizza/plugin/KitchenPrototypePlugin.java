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
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.service.PrototypeRegistryService;

import org.cougaar.pizza.asset.*;

/**
 * This COUGAAR Plugin creates and publishes the Pizza Provider Kitchen Asset object
 * which identifies what kind of pizza that provider's kitchen can make.
 */
public class KitchenPrototypePlugin extends ComponentPlugin {

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
   * Variables for storing plugin arguments that determine whether the kitchen assets
   * are capable of creating meat and/or veggie pizzas.
   * Default is to create both.
   */
  private boolean createVeggie = true;
  private boolean createMeat = true;

  /**
   * Used for initialization to populate the Blackboard with ProgrammerAsset objects
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
      kitchen_asset.addOtherPropertyGroup(makeAVeggiePG(true));
    }
    // Check the plugin arguments to see if this plugin should create a
    // Kitchen asset that can make meat pizza.
    if (createMeat) {
      kitchen_asset.addOtherPropertyGroup(makeAMeatPG(true));
    }
    kitchen_asset.setItemIdentificationPG(makeAItemIdentificationPG("Pizza Kitchen"));
    getBlackboardService().publishAdd(kitchen_asset);

  }

  /**
   * Create and populate a Veggie property group
   */
  private VeggiePG makeAVeggiePG(boolean veggieOnly) {
    NewVeggiePG new_veggie_pg = (NewVeggiePG)
      ((PlanningFactory)getDomainService().getFactory("planning")).createPropertyGroup("VeggiePG");
    new_veggie_pg.setVeggieOnly(veggieOnly);
    return new_veggie_pg;
  }

  /**
   * Create and populate a Meat property group
   */
  private MeatPG makeAMeatPG(boolean meatOK) {
    NewMeatPG new_meat_pg = (NewMeatPG)
      ((PlanningFactory)getDomainService().getFactory("planning")).createPropertyGroup("MeatPG");
    new_meat_pg.setMeatOK(meatOK);
    return new_meat_pg;
  }

  /**
   * Create and populate an ItemIdentification property group
   */
  private ItemIdentificationPG makeAItemIdentificationPG(String name) {
    NewItemIdentificationPG new_item_id_pg = (NewItemIdentificationPG)
      ((PlanningFactory)getDomainService().getFactory("planning")).createPropertyGroup("ItemIdentificationPG");
    new_item_id_pg.setItemIdentification(name);
    return new_item_id_pg;
  }

  /**
   * No subscriptions, so this method does nothing
   */
  protected void execute () {}

}

