The Pizza Planner application is a simple 8 agent society that
exercises many key Cougaar modules, while staying relatively
simple. As such, it is a good test to ensure key pieces of Cougaar are
still working, and a good set of sample code for modelling custom
components off of.

This is a planning application. Alice has a "buddy list" for
FriendsOfMark (a community). She sends that list an invitation (a
relay) to a Pizza Party. They each tell her if they eat Meat of Veggie
Pizza.

Meanwhile, there are 2 pizza places. Dominos serves all of
Massachusetts, so registers in the MA YellowPages community.
Joe's Local Pizza Shack serves only Cambridge, so registers in the
Cambridge YP.
Each pizza place may serve meat or veggie pizza, and maybe both.

Once Alice knows how many people are coming to her party, and if they
eat meat or veggie, she has to do a YP lookup to find a Pizza Provider
(using service discovery). Once she finds a provider, she sends them
an Order Task for meat or veggie pizza. If the pizza place sells the
kind of pizza she wants, the Task is allocated to the pizza place's
Pizza Asset, and the Task succeeds. If they don't (say they only sell
meat, and she needs Veggie), then the Task fails.
When the Task fails, Alice must re-do Service Discovery to find
another provider, and send them her Order Task.

Once all her Orders are accepted, her party is planned.
