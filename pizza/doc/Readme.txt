The Pizza Planner application is a simple 8 agent society that
exercises many key Cougaar modules, while staying relatively
simple. As such, it is a good test to ensure key pieces of Cougaar are
still working, and a good set of sample code for modelling custom
components off of.

This is a planning application. Alice has a "buddy list" for
FriendsOfMark (a community). She sends that list an invitation (a
Relay with an ABA target) to a Pizza Party. They each tell her if they eat Meat of Veggie
Pizza.

Meanwhile, there are 2 pizza places. Dominos serves all of
Massachusetts, so registers in the MA YellowPages community.
Joe's Local Pizza Shack serves only Cambridge, so registers in the
Cambridge YP.
Each pizza place may serve meat or veggie pizza, and maybe both.

Once Alice knows how many people are coming to her party, and if they
eat meat or veggie, she can order her pizza.

In the service-discovey application, she has to do a YP lookup to find a Pizza Provider
(using service discovery). Once she finds a provider, she sends them
an Order Task for meat and maybe another for veggie pizza. Alice is in
Cambridge, so the looks first in the Cambridge Yellow Pages (local is
better, right?). So she finds JoesLocalPizzaShack first. If the pizza place sells the
kind of pizza she wants, the Task is allocated to the pizza place's
Kitchen Asset, and the Task succeeds. If they don't (Joes only sells
meat, and she needs Veggie), then the Task fails.
When the Task fails, Alice must re-do Service Discovery to find
another provider, and send them her Order Task(s). So she looks beyond
Cambridge, and in the MA Yellow Pages, she sees Dominos, and retries
her entire order with them. Thankfully, Dominos has more kinds of pizza.

Once all her Orders are accepted, her party is planned.


In the non-service-discovery version of the applcation, Alice starts
up only having a relationship with one pizza provider - Joes. So when
Joes can't help her, she is out of luck!

