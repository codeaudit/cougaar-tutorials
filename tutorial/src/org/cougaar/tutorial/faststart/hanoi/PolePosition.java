/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.tutorial.faststart.hanoi;

import java.io.Serializable;

/**
 * Holds the pole identifier number for the Tower of Hanoi demo.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: PolePosition.java,v 1.2 2001-04-05 19:28:50 mthome Exp $
 */
public class PolePosition implements Serializable {

  /**
   * Create a PolePosition with the identifier provided.
   * @param number the pole's number.
   */
  public PolePosition(int number) {
    setPosition(number);
  }

  /**
   * Set the identifier for the pole.
   * @param number the pole's number.
   */
  public void setPosition(int newPosition) {
    position = newPosition;
  }

  /**
   * Get the identifier for the pole.
   * @return the pole's number.
   */
  public int getPosition() {
    return position;
  }
  private int position;
} 