package org.cougaar.tutorial.faststart.computer;

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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.tutorial.faststart.computer.assets.*;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.*;

/**
 * Simple Plugin to list new instances of ComputerAssets
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ComputerAssetListerPlugin.java,v 1.2 2003-01-23 22:12:55 mbarger Exp $
 */
public class ComputerAssetListerPlugin extends ComponentPlugin
{

  private IncrementalSubscription allComputerAssets;
  private UnaryPredicate allComputerAssetsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof ComputerAsset;
    }
  };

  /**
   * Establish subscription for assets
   */
  public void setupSubscriptions() 
  {
    //    System.out.println("ComputerAssetListerPlugin::setupSubscriptions");

    allComputerAssets = 
      (IncrementalSubscription)getBlackboardService().subscribe(allComputerAssetsPredicate);
  }

  /**
   * Printout all assets as they come in with property-wise details
   **/
  public void execute() 
  {
    //    System.out.println("ComputerAssetListerPlugin::execute");

    for(Enumeration e = allComputerAssets.getAddedList(); e.hasMoreElements();) 
    {
      Object o = e.nextElement();
      ComputerAsset asset = (ComputerAsset)o;
      System.out.println(asset + " : " + Details(asset));
    }

  }

  // Get the CDPG (if any) from the 'other properties' of the asset
  private CDPG getCDPG(ComputerAsset asset) {
    CDPG cd_pg = null;
    for(Enumeration e = asset.getOtherProperties(); e.hasMoreElements();){
      PropertyGroup pg = (PropertyGroup)e.nextElement();
      if (pg instanceof CDPG) {
        cd_pg = (CDPG)pg;
        break;
      }
    }
    return cd_pg;
  }

  // Print details of the asset, including CD details, if any
  private String Details(ComputerAsset asset) {
    CDPG cd_pg = getCDPG(asset);
    String cd_string = "";
    if (cd_pg != null) {
      cd_string = "CD : " + cd_pg.getDriveSpeed() + " " + 
        cd_pg.getDVD();
    }
    return "Memory : " + 
      asset.getMemoryPG().getRAM() + " / " +
      "CPU : " + asset.getCPUPG().getClockSpeed() + " " +
      asset.getCPUPG().getManufacturer() + " / " +
      "Monitor : " +
      asset.getMonitorPG().getScreenSize() + " / " +
      "Market : " +
      asset.getMarketPG().getDaysToShip() + " " +
      asset.getMarketPG().getPrice() + " / " +
      cd_string;
  }
}
