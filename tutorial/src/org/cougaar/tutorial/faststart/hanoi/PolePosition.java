package org.cougaar.tutorial.faststart.hanoi;

import java.io.Serializable;

/**
 * Holds the pole identifier number for the Tower of Hanoi demo.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: PolePosition.java,v 1.1 2000-12-15 20:19:04 mthome Exp $
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