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
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.core.service.*;
import org.cougaar.core.domain.RootFactory;

/**
 * This COUGAAR Plugin creates and publishes ProgrammerAsset objects.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ProgrammerLDMPlugin.java,v 1.2 2002-03-15 21:34:19 mbarger Exp $
 */
public class ProgrammerLDMPlugin extends ComponentPlugin {

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
   * Used for initialization to populate the Blackboard with ProgrammerAsset objects
   */
protected void setupSubscriptions() {

    // Get the RootFactory
    RootFactory factory = getDomainService().getFactory();

    // Register our new PropertyFactory so we can refer to properties by name
    factory.addPropertyGroupFactory(new tutorial.assets.PropertyGroupFactory());

    // Create the prototypes that will be used to create the programmer assets
    ProgrammerAsset new_prototype = (ProgrammerAsset)factory.createPrototype
      (tutorial.assets.ProgrammerAsset.class, "programmer");
    new_prototype.setLanguagePG(makeALanguagePG(true, false) );  // knows Java but not JavaScript
    // Cache the prototype in the LDM : note this is not treated
    // As an asset that is available in subscriptions, but can
    // be used to build 'real' assets when asked for by prototype name
    getPrototypeRegistryService().cachePrototype("programmer", new_prototype);

    // Create an asset based on an existing prototype
    ProgrammerAsset asset = (ProgrammerAsset) factory.createInstance("programmer");
    asset.setLanguagePG(makeALanguagePG(true, true) );  // knows Java and JavaScript
    asset.setSkillsPG(makeASkillsPG(10, 100));    // 10 years experience, 100 SLOC/day
    asset.setItemIdentificationPG(makeAItemIdentificationPG("Bill Gates"));
    getBlackboardService().publishAdd(asset);

    // Create an asset based on an existing prototype
    ProgrammerAsset another_asset = (ProgrammerAsset) factory.createInstance(asset);
    another_asset.setSkillsPG(makeASkillsPG(15, 150));   // 15 years experience, 150 SLOC/day
    another_asset.setItemIdentificationPG(makeAItemIdentificationPG("Linus Torvalds"));
    getBlackboardService().publishAdd(another_asset);

}

/**
 * Create and populate a Language property group
 */
private LanguagePG makeALanguagePG(boolean knowsJava, boolean knowsJavaScript) {
  NewLanguagePG new_language_pg = (NewLanguagePG)
              getDomainService().getFactory().createPropertyGroup("LanguagePG");
  new_language_pg.setKnowsJava(knowsJava);
  new_language_pg.setKnowsJavaScript(knowsJavaScript);
  return new_language_pg;
}

/**
 * Create and populate a Skills property group
 */
private SkillsPG makeASkillsPG(int yearsExperience, int productivity) {
  NewSkillsPG new_skills_pg = (NewSkillsPG)
          getDomainService().getFactory().createPropertyGroup("SkillsPG");
  new_skills_pg.setYearsExperience(yearsExperience);
  new_skills_pg.setSLOCPerDay(productivity);
  return new_skills_pg;
}

/**
 * Create and populate an ItemIdentification property group
 */
private ItemIdentificationPG makeAItemIdentificationPG(String name) {
  NewItemIdentificationPG new_item_id_pg = (NewItemIdentificationPG)
          getDomainService().getFactory().createPropertyGroup("ItemIdentificationPG");
  new_item_id_pg.setItemIdentification(name);
  return new_item_id_pg;
}

/**
 * No subscriptions, so this method does nothing
 */
protected void execute () {}

}

