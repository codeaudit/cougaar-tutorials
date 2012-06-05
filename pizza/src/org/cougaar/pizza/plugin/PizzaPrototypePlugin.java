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
import org.cougaar.pizza.Constants;
import org.cougaar.pizza.asset.PizzaAsset;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.service.PrototypeRegistryService;

/**
 * This Plugin creates and registers the Pizza Prototype asset. Other plugins
 * will create instances of Pizza Assets as needed based on this prototype. This
 * plugin does not require any inputs, it simply creates and registers the
 * prototype. Note that this plugin will only run once since there are no
 * inputs(subscriptions) that will cause it to run.
 */
public class PizzaPrototypePlugin
      extends ComponentPlugin {

   // The domainService provides domain factory services
   private DomainService domainService = null;

   // The prototypeRegistryService provides prototype registration services
   private PrototypeRegistryService prototypeRegistryService = null;

   // The planning factory we use to create planning objects.
   private PlanningFactory planningFactory;

   /**
    * Used by the binding utility through introspection to set my DomainService
    * Services that are required for plugin usage should be set through
    * reflection instead of explicitly getting each service from your
    * ServiceBroker in the load method. The setter methods are called after the
    * component is constructed but before the state methods such as initialize,
    * load, setupSubscriptions, etc. If the service is not available at that
    * time the component will be unloaded.
    */
   public void setDomainService(DomainService aDomainService) {
      domainService = aDomainService;
   }

   /**
    * Used by the binding utility through introspection to set my
    * PrototypeRegistryService
    */
   public void setPrototypeRegistryService(PrototypeRegistryService aPrototypeRegistryService) {
      prototypeRegistryService = aPrototypeRegistryService;
   }

   /**
    * Generally used to initalize plugin subscriptions. But in this case, we
    * will just use it to call our method that will create the prototype.
    */
   @Override
   protected void setupSubscriptions() {
      planningFactory = (PlanningFactory) domainService.getFactory("planning");
      // unload the domain service since we only need it to get the planning
      // factory
      getServiceBroker().releaseService(this, DomainService.class, domainService);

      // Create our prototype
      createPizzaPrototype();
   }

   /**
    * Create and register the pizza prototype.
    */
   private void createPizzaPrototype() {
      PizzaAsset new_prototype = (PizzaAsset) planningFactory.createPrototype(PizzaAsset.class, Constants.PIZZA);
      // Cache the prototype in the LDM so that other plugins can create Pizza
      // instances using the new prototype.
      prototypeRegistryService.cachePrototype(Constants.PIZZA, new_prototype);
   }

   /**
    * No subscriptions to process so this method does nothing.
    */
   @Override
   protected void execute() {
   }
}
