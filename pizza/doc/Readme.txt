The Pizza Planner application is a simple 8 agent society that  
exercises many key Cougaar modules, while staying relatively
simple. As such, it is a good test to ensure key pieces of Cougaar are
still working, and a good set of sample code for modelling custom
components off of.

To run this application, you run the 2 Cougaar Nodes that make up the
system:
[In a command line window]
cd COUGAAR_INSTALL_PATH/pizza/configs/pizzaparty
Cougaar SDPizzaNode1.xml

[Start another window]
cd COUGAAR_INSTALL_PATH/pizza/configs/pizzaparty
Cougaar SDPizzaNode2.xml

You should see something like:
COUGAAR 11.4 built on Tue Dec 07 07:04:16 GMT 2004
Repository: HEAD on Tue Dec 07 07:00:08 GMT 2004
VM: JDK 1.4.2_06-b03 (mixed mode)
OS: Windows 2000 (5.0)
16:52:11,680 SHOUT - XMLComponentInitializerServiceProvider - Initializing node "SDPizzaNo
de1" from XML file "SDPizzaNode1.xml"
16:52:20,622 WARN  - ExecutionTimer - Multi-node societies will have execution-time clock
skew: Set org.cougaar.core.society.startTime or society.timeOffset to avoid this problem.
16:52:20,622 WARN  - ExecutionTimer - Starting Time set to Wed Aug 10 00:05:00 GMT 2005 of
fset=21107559388ms
2004-12-08 04:52:26,331 SHOUT [DOTS] -
+-+-+-.+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-....+----
16:53:12,607 SHOUT - SDPlaceOrderPlugin - Alice: Pizza Order Task FAILED
16:53:12,607 SHOUT - SDPlaceOrderPlugin - Alice:      JoesLocalPizzaShack could handle Ord
er for 2.0 Meat Pizza
16:53:12,617 SHOUT - SDPlaceOrderPlugin - Alice:      JoesLocalPizzaShack could NOT handle
 Order for 2.0 Veggie Pizza
16:53:12,617 SHOUT - SDPlaceOrderPlugin - Alice: Initial Expansion FAILed. Redo Service Di
scovery.
2004-12-08 04:53:12,617 SHOUT [DOTS] - +
2004-12-08 04:53:12,617 SHOUT [DOTS] - +-+-+-+-+-+-+-+-+-+-+-+-
16:53:12,757 SHOUT - SDPlaceOrderPlugin - Alice: Pizza Order Task SUCCEEDED
16:53:12,757 SHOUT - SDPlaceOrderPlugin - Alice:      Dominos could handle Order for 2.0 M
eat Pizza
16:53:12,757 SHOUT - SDPlaceOrderPlugin - Alice:      Dominos could handle Order for 2.0 V
eggie Pizza

------------------------------------

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

