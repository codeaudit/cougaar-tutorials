package org.cougaar.tutorial.faststart.calendar;

/**
  * Copyright 1997-1999 Defense Advanced Research Project Agency 
  * and ALPINE (A Raytheon Systems Company and BBN Corporation Consortium). 
  * This software to be used in accordance with the COUGAAR license agreement.
**/

import java.util.*;

/**
 * A really simple calendar : every day is just tagged as a number,
 * and can hold up to one thing for that day.
 *
 * NOTE : An Asset doesn't need to be generated automatically by AssetWriter
 * 
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: CalendarAsset.java,v 1.1 2000-12-15 20:19:04 mthome Exp $
 */
public class CalendarAsset extends org.cougaar.domain.planning.ldm.asset.Asset
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
