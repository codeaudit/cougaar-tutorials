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

import org.cougaar.core.plugin.*;
import org.cougaar.tutorial.assets.*;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.core.service.*;
import org.cougaar.planning.service.*;
import org.cougaar.planning.ldm.PlanningFactory;

/**
 * This COUGAAR Plugin creates and publishes ProgrammerAsset objects.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ProgrammerLDMPlugin.java,v 1.1 2003-12-15 16:07:01 twright Exp $
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
 * No subscriptions, so this method does nothing
 */
protected void execute () {}

}

