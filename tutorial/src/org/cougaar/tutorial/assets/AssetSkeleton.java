/*
 * <copyright>
 *  
 *  Copyright 1997-2012 Raytheon BBN Technologies
 *  under partial sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

/* @generated Wed Jun 06 07:10:20 EDT 2012 from /Users/djw/unbackedup/cougaar-tmp/repo/tutorials/tutorial/src/org/cougaar/tutorial/assets/properties.def - DO NOT HAND EDIT */
/** Abstract Asset Skeleton implementation
 * Implements default property getters, and additional property
 * lists.
 * Intended to be extended by org.cougaar.planning.ldm.asset.Asset
 **/

package org.cougaar.tutorial.assets;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;


import java.io.Serializable;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;

public abstract class AssetSkeleton extends org.cougaar.planning.ldm.asset.Asset {

  protected AssetSkeleton() {}

  protected AssetSkeleton(AssetSkeleton prototype) {
    super(prototype);
  }

  /**                 Default PG accessors               **/

  /** Search additional properties for a SkillsPG instance.
   * @return instance of SkillsPG or null.
   **/
  public SkillsPG getSkillsPG()
  {
    SkillsPG _tmp = (SkillsPG) resolvePG(SkillsPG.class);
    return (_tmp==SkillsPG.nullPG)?null:_tmp;
  }

  /** Test for existence of a SkillsPG
   **/
  public boolean hasSkillsPG() {
    return (getSkillsPG() != null);
  }

  /** Set the SkillsPG property.
   * The default implementation will create a new SkillsPG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setSkillsPG(PropertyGroup aSkillsPG) {
    if (aSkillsPG == null) {
      removeOtherPropertyGroup(SkillsPG.class);
    } else {
      addOtherPropertyGroup(aSkillsPG);
    }
  }

  /** Search additional properties for a LanguagePG instance.
   * @return instance of LanguagePG or null.
   **/
  public LanguagePG getLanguagePG()
  {
    LanguagePG _tmp = (LanguagePG) resolvePG(LanguagePG.class);
    return (_tmp==LanguagePG.nullPG)?null:_tmp;
  }

  /** Test for existence of a LanguagePG
   **/
  public boolean hasLanguagePG() {
    return (getLanguagePG() != null);
  }

  /** Set the LanguagePG property.
   * The default implementation will create a new LanguagePG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setLanguagePG(PropertyGroup aLanguagePG) {
    if (aLanguagePG == null) {
      removeOtherPropertyGroup(LanguagePG.class);
    } else {
      addOtherPropertyGroup(aLanguagePG);
    }
  }

}
