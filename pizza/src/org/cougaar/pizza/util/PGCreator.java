/*
 * <copyright>
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
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
package org.cougaar.pizza.util;

import org.cougaar.pizza.asset.MeatPG;
import org.cougaar.pizza.asset.NewMeatPG;
import org.cougaar.pizza.asset.NewVeggiePG;
import org.cougaar.pizza.asset.VeggiePG;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;

/**
 * This is a helper class that creates various types of
 * PropertyGroups (PGs) to place on assets.
 */
public class PGCreator {

  /**
   * Create and populate a Veggie property group
   * @param pFactory The planning factory to create a new PG
   * @param veggieOnly Fill in the boolean on the PG
   */
  public static VeggiePG makeAVeggiePG(PlanningFactory pFactory,
                                       boolean veggieOnly) {
    NewVeggiePG new_veggie_pg = (NewVeggiePG)
        pFactory.createPropertyGroup("VeggiePG");
    new_veggie_pg.setVeggieOnly(veggieOnly);
    return new_veggie_pg;
  }

  /**
   * Create and populate a Meat property group
   * @param pFactory The planning factory to create a new PG
   * @param meatOK Fill in the boolean on the PG
   */
  public static MeatPG makeAMeatPG(PlanningFactory pFactory,
                                   boolean meatOK) {
    NewMeatPG new_meat_pg = (NewMeatPG)
        pFactory.createPropertyGroup("MeatPG");
    new_meat_pg.setMeatOK(meatOK);
    return new_meat_pg;
  }

  /**
   * Create and populate an ItemIdentification property group
   * @param pFactory The planning factory to create a new PG
   * @param name Fill in the name of the asset on the ItemIDPG
   */
  public static ItemIdentificationPG
      makeAItemIdentificationPG(PlanningFactory pFactory, String name) {
    NewItemIdentificationPG new_item_id_pg = (NewItemIdentificationPG)
        pFactory.createPropertyGroup("ItemIdentificationPG");
    new_item_id_pg.setItemIdentification(name);
    return new_item_id_pg;
  }

}

