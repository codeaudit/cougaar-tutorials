package org.cougaar.tutorial.faststart.computer;

/**
 * Copyright 1997-1999 Defense Advanced Research Project Agency 
 * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
 * This software to be used in accordance with the COUGAAR license agreement.
 */

import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.asset.*;
import org.cougaar.tutorial.faststart.computer.assets.*;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Simple Plugin to list new instances of ComputerAssets
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ComputerAssetListerPlugIn.java,v 1.1 2000-12-15 20:19:04 mthome Exp $
 */
public class ComputerAssetListerPlugIn extends org.cougaar.core.plugin.SimplePlugIn 
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
    //    System.out.println("ComputerAssetListerPlugIn::setupSubscriptions");

    allComputerAssets = 
      (IncrementalSubscription)subscribe(allComputerAssetsPredicate);
  }

  /**
   * Printout all assets as they come in with property-wise details
   **/
  public void execute() 
  {
    //    System.out.println("ComputerAssetListerPlugIn::execute");

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
