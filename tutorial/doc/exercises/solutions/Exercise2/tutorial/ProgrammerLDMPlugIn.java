/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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
package tutorial;

import org.cougaar.core.plugin.*;
import tutorial.assets.*;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.domain.planning.ldm.asset.ItemIdentificationPG;

/**
 * This COUGAAR PlugIn creates and publishes ProgrammerAsset objects.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ProgrammerLDMPlugIn.java,v 1.2 2001-08-22 20:30:48 mthome Exp $
 */
public class ProgrammerLDMPlugIn extends SimplePlugIn {
  /**
   * Used for initialization to populate the PLAN with ProgrammerAsset objects
   */
protected void setupSubscriptions() {

    // Register our new PropertyFactory so we can refer to properties by name
    theLDMF.addPropertyGroupFactory(new tutorial.assets.PropertyGroupFactory());

    // Create the prototypes that will be used to create the programmer assets
    ProgrammerAsset new_prototype = (ProgrammerAsset)theLDMF.createPrototype
      (tutorial.assets.ProgrammerAsset.class, "programmer");
    new_prototype.setLanguagePG(makeALanguagePG(true, false) );  // knows Java but not JavaScript
    // Cache the prototype in the LDM : note this is not treated
    // As an asset that is available in subscriptions, but can
    // be used to build 'real' assets when asked for by prototype name
    theLDM.cachePrototype("programmer", new_prototype);

    // Create an asset based on an existing prototype
    ProgrammerAsset asset = (ProgrammerAsset) theLDMF.createInstance("programmer");
    asset.setLanguagePG(makeALanguagePG(true, true) );  // knows Java and JavaScript
    asset.setSkillsPG(makeASkillsPG(10, 100));    // 10 years experience, 100 SLOC/day
    asset.setItemIdentificationPG(makeAItemIdentificationPG("Bill Gates"));
    publishAdd(asset);

    // Create an asset based on an existing prototype
    ProgrammerAsset another_asset = (ProgrammerAsset) theLDMF.createInstance(asset);
    another_asset.setSkillsPG(makeASkillsPG(15, 150));   // 15 years experience, 150 SLOC/day
    another_asset.setItemIdentificationPG(makeAItemIdentificationPG("Linus Torvalds"));
    publishAdd(another_asset);

}

/**
 * Create and populate a Language property group
 */
private LanguagePG makeALanguagePG(boolean knowsJava, boolean knowsJavaScript) {
  NewLanguagePG new_language_pg = (NewLanguagePG)theLDMF.createPropertyGroup("LanguagePG");
  new_language_pg.setKnowsJava(knowsJava);
  new_language_pg.setKnowsJavaScript(knowsJavaScript);
  return new_language_pg;
}

/**
 * Create and populate a Skills property group
 */
private SkillsPG makeASkillsPG(int yearsExperience, int productivity) {
  NewSkillsPG new_skills_pg = (NewSkillsPG)theLDMF.createPropertyGroup("SkillsPG");
  new_skills_pg.setYearsExperience(yearsExperience);
  new_skills_pg.setSLOCPerDay(productivity);
  return new_skills_pg;
}

/**
 * Create and populate an ItemIdentification property group
 */
private ItemIdentificationPG makeAItemIdentificationPG(String name) {
  NewItemIdentificationPG new_item_id_pg = (NewItemIdentificationPG)theLDMF.createPropertyGroup("ItemIdentificationPG");
  new_item_id_pg.setItemIdentification(name);
  return new_item_id_pg;
}

/**
 * No subscriptions, so this method does nothing
 */
protected void execute () {}

}

