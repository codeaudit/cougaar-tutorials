/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
package org.cougaar.tutorial.exercise2;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.service.PrototypeRegistryService;
import org.cougaar.tutorial.assets.LanguagePG;
import org.cougaar.tutorial.assets.NewLanguagePG;
import org.cougaar.tutorial.assets.NewSkillsPG;
import org.cougaar.tutorial.assets.ProgrammerAsset;
import org.cougaar.tutorial.assets.SkillsPG;


/**
 * This COUGAAR Plugin creates and publishes ProgrammerAsset objects.
 */

 // todo:  add code to make this a subclass
public class ProgrammerLDMPlugin {

  // todo:  Add attributes and methods for access to the DomainService and the
  //        PrototypeRegistryService

  /**
   * Used for initialization to populate the Blackboard with ProgrammerAsset objects
   */
protected void setupSubscriptions() {

    // todo: Register our new PropertyFactory so we can refer to properties by name

    // todo: Create the prototypes that will be used to create the programmer assets

    // todo: Create an asset for programmer 'Bill Gates' based on an existing prototype

    // todo: Create an asset for programmer 'Linus Torvalds' based on an existing prototype

}

/**
 * todo: Create and populate a Language property group
 */

/**
 * todo: Create and populate a Skills property group
 */

/**
 * todo: Create and populate an ItemIdentification property group
 */


/**
 * No subscriptions, so this method does nothing
 */
protected void execute () {}

}

