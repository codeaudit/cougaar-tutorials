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
package org.cougaar.tutorial.faststart.hanoi;

import java.io.Serializable;

/**
 * Holds the pole identifier number for the Tower of Hanoi demo.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: PolePosition.java,v 1.3 2001-08-22 20:30:52 mthome Exp $
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