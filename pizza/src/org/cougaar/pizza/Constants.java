package org.cougaar.pizza;

/**
 *  String constants for the pizza party application
 */
public class Constants {
  // Private constructor prevents instantiation
  private Constants() {}

  public static final String ORDER = "ORDER";

  /** the query to my friends */
  public static final String INVITATION_QUERY = "invitation-meat_or_veg";

  /* used when showing how long it takes for this number of friends to reply */
  public static final int EXPECTED_NUM_FRIENDS = 4;

  public static final String FIND_PROVIDERS = "FindProviders";
  public static final String PIZZA_PROVIDER = "PizzaProvider";
  public static final String CARNIVORE = "Carnivore";
  public static final String VEGETARIAN = "Vegetarian";
  public static final String PIZZA = "Pizza";
}
