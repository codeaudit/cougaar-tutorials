package org.cougaar.tutorial.faststart.calendar;

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

import java.util.*;

/**
 * A really simple calendar : every day is just tagged as a number,
 * and can hold up to one thing for that day.
 *
 * NOTE : An Asset doesn't need to be generated automatically by AssetWriter
 * 
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: CalendarAsset.java,v 1.3 2003-01-23 22:12:54 mbarger Exp $
 */
public class CalendarAsset extends org.cougaar.planning.ldm.asset.Asset
{
  public CalendarAsset() {
    my_datebook = new Hashtable();
  }

  // Set assignment for given day (previous assignment will be overridden
  public void setAssignment(int day, Object obj) {
    my_datebook.put(new Integer(day), obj);
    Dump();
  }

  // Is any object assigned to this day? If so, return it, else return null.
  public Object getAssignment(int day) {
    return my_datebook.get(new Integer(day));
  }

  // Enumeration of all assigned days as Integers
  public Enumeration getAssignedDays() { 
    return my_datebook.keys();
  }

  // Enumeration of all assignments
  public Enumeration getAssignments() {
    return my_datebook.elements();
  }

  // Clear all assignments
  public void clear() {
    System.out.println("Clearing datebook of CalendarAsset");
    my_datebook.clear();
  }

  // Dump current contents of datebook to stdout
  private void Dump() {
    System.out.println("Current contents of CalendarAsset datebook");
    for(Enumeration e = my_datebook.keys();e.hasMoreElements();) 
      {
	Integer key = (Integer)e.nextElement();
	Object obj = my_datebook.get(key);
	System.out.println(key + " => " + obj);
      }
  }

  // Hold all appointments as a hashtable mapping Integer(day) => Allocation or other Object
  private Hashtable my_datebook;
}
