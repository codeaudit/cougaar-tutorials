package org.cougaar.tutorial.faststart;

import org.cougaar.domain.planning.ldm.*;
import org.cougaar.domain.planning.ldm.asset.*;
import org.cougaar.core.cluster.*;
import org.cougaar.domain.planning.ldm.plan.*;

/**
 * An COUGAAR Asset class that represents a cluster.  Allocation to
 * one of these assets triggers the inter-cluster communication.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: Organization.java,v 1.2 2001-03-29 21:51:51 mthome Exp $
 */
public class Organization extends org.cougaar.domain.planning.ldm.asset.Asset {

  /**
   * Constants that represent the relationships between clusters.
   */
  public static String CUSTOMER_RELATIONSHIP = "Customer";
  public static String SUPPLIER_RELATIONSHIP = "Supplier";
  public static String SELF_RELATIONSHIP = "Self";

  /**
   * A zero-argument constructor is required by Asset.clone()
   */
  public Organization() {
  }

  /**
   * Create a new Organization and populate its ClusterPG with the name given.
   * @param name the name of the organization.
   * @param theFactory used to vreate the ClusterPG property group.
   */
  public Organization(String name, RootFactory theFactory) {
    NewClusterPG cpg = (NewClusterPG)theFactory.createPropertyGroup(ClusterPGImpl.class);
    cpg.setClusterIdentifier(new ClusterIdentifier(name));
    this.setClusterPG(cpg);
  }

  private Role[] roles;
  private String relationship;

  /**
   * Get a list of the roles that this cluster can take.
   * @return the array of roles.
   */
  public Role[] getRoles() {
    return roles;
  }

  /**
   * Set the list of roles that this cluster can take.
   * @param newRoles the array of roles for this organization.
   */
  public void setRoles(Role[] newRoles) {
    roles = newRoles;
  }

  /**
   * Set the relationship between this cluster and the local cluster.
   * @param newRelationship the relationship.
   * @see CUSTOMER_RELATIONSHIP
   * @see SUPPLIER_RELATIONSHIP
   * @see SELF_RELATIONSHIP
   */
  public void setRelationship(String newRelationship) {
    relationship = newRelationship;
  }

  /**
   * Set the relationship between this cluster and the local cluster.
   * @return the relationship.
   */
  public String getRelationship() {
    return relationship;
  }

  /**
   * Make a copy of this object
   */
  public Object clone() throws java.lang.CloneNotSupportedException {

    Organization ret = (Organization) super.clone();
    ret.setRoles(getRoles());
    ret.setRelationship(getRelationship());

    return ret;
  }


}
