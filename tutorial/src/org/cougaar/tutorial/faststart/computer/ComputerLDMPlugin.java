package org.cougaar.tutorial.faststart.computer;

/**
  * Copyright 1997-1999 Defense Advanced Research Project Agency 
  * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
  * This software to be used in accordance with the COUGAAR license agreement.
**/

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.service.PrototypeRegistryService;
import org.cougaar.tutorial.faststart.computer.assets.*;
import java.util.Enumeration;
import java.util.Vector;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.*;

/**
 * Simple LDM Plugin to create instances of a given prototype and
 * tailor these instances, adding/changing properties
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ComputerLDMPlugin.java,v 1.2 2002-11-19 17:33:04 twright Exp $
 */
public class ComputerLDMPlugin  extends ComponentPlugin
{
  private DomainService domainService = null;
  private PlanningFactory ldmf = null;
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
   * Establish subscription for assets
   */
  public void setupSubscriptions()
  {
    // System.out.println("ComputerLDMPlugin::setupSubscriptions");
    ldmf = (PlanningFactory) getDomainService().getFactory("planning");
    if (ldmf == null) {
      throw new RuntimeException(
          "Unable to find \"planning\" domain factory");
    }

    // Register our new PropertyFactory so we can refer to properties by name
    ldmf.addPropertyGroupFactory(new org.cougaar.tutorial.faststart.computer.assets.PropertyGroupFactory());

    // Create the prototypes that will be used to create the computer assets
    createPrototypes();

    ComputerAsset asset = null;

    // Create an asset based on an existing prototype
    asset = (ComputerAsset) ldmf.createInstance("BUDGET_300_XT");

    // And change some properties in it, leaving the monitor property alone
    fillCPUPG(asset, 300, "XYZ");
    fillMarketPG(asset, 14, 900);
    fillMemoryPG(asset, 256);

    // And publish it
    getBlackboardService().publishAdd(asset);

    // make a couple computers using the unchanged prototype
    getBlackboardService().publishAdd(ldmf
				      .createInstance("BUDGET_300_XT"));
    getBlackboardService().publishAdd(ldmf
				      .createInstance("BUDGET_300_XT"));

    // Reference a new prototype
    // Create an instance of this prototype and publish it with no change
    asset = (ComputerAsset)ldmf
	.createInstance("TURBO_400");
    getBlackboardService().publishAdd(asset);

    // Create another and change the monitor on this one
    // And publish it
    asset = (ComputerAsset)ldmf
	.createInstance("TURBO_400");
    fillMonitorPG(asset, 25);
    fillMarketPG(asset, 3, 2500);
    getBlackboardService().publishAdd(asset);

    // Now, build an asset using another ASSET as a prototype
    ComputerAsset old_asset_as_prototype = asset;
    asset = (ComputerAsset)ldmf
	.createInstance(old_asset_as_prototype);

    // Change the memory and market info on this one
    fillMemoryPG(asset, 512);
    fillMarketPG(asset, 4, 2300);

    // And add another property to this asset : a CDPG
    fillCDPG(asset, 24, false);

    // And publish the new asset
    getBlackboardService().publishAdd(asset);


  }

  /**
   * Create and cache two computer prototypes that can be
   * used to create computer instances.
   */
  private void createPrototypes() {
    // Create a new prototype
    String new_proto_name = "TURBO_400";

    ComputerAsset new_prototype =
      (ComputerAsset)ldmf.createPrototype
      (org.cougaar.tutorial.faststart.computer.assets.ComputerAsset.class, new_proto_name);
    fillMemoryPG(new_prototype, 384);
    fillMonitorPG(new_prototype, 21);
    fillMarketPG(new_prototype, 10, 2000);
    fillCPUPG(new_prototype, 400, "TURBO");

    // Cache the prototype in the LDM : note this is not treated
    // As an asset that is available in subscriptions, but can
    // be used to build 'real' assets when asked for by prototype name
    getPrototypeRegistryService().cachePrototype(new_proto_name, new_prototype);

    // Create another prototype
    new_proto_name = "BUDGET_300_XT";

    new_prototype = (ComputerAsset)ldmf.createPrototype
      (org.cougaar.tutorial.faststart.computer.assets.ComputerAsset.class, new_proto_name);
    fillMemoryPG(new_prototype, 128);
    fillMonitorPG(new_prototype, 17);
    fillMarketPG(new_prototype, 7, 1000);
    fillCPUPG(new_prototype, 300, "BUDGET");

    getPrototypeRegistryService().cachePrototype(new_proto_name, new_prototype);
  }


  // Auxiliary Functions to fill in properties

  // Fill in MonitorPG on ComputerAsset
  private void fillMonitorPG(ComputerAsset asset, int screen_size) {
    NewMonitorPG monitor_pg =
      (NewMonitorPG)ldmf.createPropertyGroup("MonitorPG");
    monitor_pg.setScreenSize(screen_size);
    asset.setMonitorPG(monitor_pg);
  }

  // Fill in CPUPG on ComputerAsset
  private void fillCPUPG(ComputerAsset asset, 
			       int clock_speed, String manufacturer) {
    NewCPUPG cpu_pg = 
      (NewCPUPG)ldmf.createPropertyGroup("CPUPG");
    cpu_pg.setClockSpeed(clock_speed);
    cpu_pg.setManufacturer(manufacturer);
    asset.setCPUPG(cpu_pg);
  }

  // Fill in MarketPG on ComputerAsset
  private void fillMarketPG(ComputerAsset asset, 
			       int days_to_ship, int price) {
    NewMarketPG market_pg =
      (NewMarketPG)ldmf.createPropertyGroup("MarketPG");
    market_pg.setDaysToShip(days_to_ship);
    market_pg.setPrice(price);
    asset.setMarketPG(market_pg);
  }

  // Fill in MemoryPG on ComputerAsset
  private void fillMemoryPG(ComputerAsset asset, int RAM) {

    // Note : You can create a property by name or by class 
    //(if PropertyFactory is registered)
    NewMemoryPG memory_pg =
      (NewMemoryPG)ldmf.createPropertyGroup
      (org.cougaar.tutorial.faststart.computer.assets.MemoryPG.class);
    memory_pg.setRAM(RAM);
    asset.setMemoryPG(memory_pg);
  }

  // Add CD PG to computer Asset, replacing previous version if any
  private void fillCDPG(ComputerAsset asset, 
				int drive_speed, boolean dvd) {
    NewCDPG cd_pg =
      (NewCDPG)ldmf.createPropertyGroup("CDPG");
    cd_pg.setDriveSpeed(drive_speed);
    cd_pg.setDVD(dvd);
    asset.removeOtherPropertyGroup(org.cougaar.tutorial.faststart.computer.assets.CDPG.class);
    asset.addOtherPropertyGroup(cd_pg);
  }
  

  // This plugin doesn't do anything once it has created its objects
  public void execute() 
  {
  }
}


