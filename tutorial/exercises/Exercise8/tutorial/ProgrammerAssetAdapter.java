/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package tutorial;

import alp.ldm.asset.Asset;

/**
 * This ALP Asset class serves as a base class for the ProgrammerAsset class.
 * The ProgrammerAsset class is generated using the AssetWriter utility
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: ProgrammerAssetAdapter.java,v 1.1 2000-12-15 20:18:59 mthome Exp $
 */
public class ProgrammerAssetAdapter extends Asset {
  private tutorial.Schedule schedule = new Schedule();

  /**
   * Create a new ProgrammerAssetAdapter
   */
  public ProgrammerAssetAdapter() {
    super();
  }

  /**
   * Create a new ProgrammerAssetAdapter
   * @param prototype the Asset's prototype
   */
  public ProgrammerAssetAdapter(Asset prototype) {
    super(prototype);
  }

  /**
   * Get the schedule of assignments for this programmer
   * @return this programmer's schedule
   */
  public tutorial.Schedule getSchedule() {
    return schedule;
  }

  /**
   * Set the schedule of assignments for this programmer
   * @param this programmer's new schedule
   */
  public void setSchedule(tutorial.Schedule newSchedule) {
    schedule = newSchedule;
  }

}