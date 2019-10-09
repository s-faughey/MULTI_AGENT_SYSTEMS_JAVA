package implementation;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;

import Ontology.Ontology;
import elements.*;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.lang.Codec.CodecException;

public class Manufacturer extends Agent{
	private Codec codec = new SLCodec();
	private Ontology ontology = Ontology.getInstance();
	private AID tickerAgent;
	private ArrayList<Order> orders = new ArrayList<>();
	private ArrayList<Order> acceptedOrders = new ArrayList<>();
	private ArrayList<Order> waitingOrders = new ArrayList<>();
	private ArrayList<Order> refusedOrders = new ArrayList<>();
	private ArrayList<Order> deliveredOrders = new ArrayList<>();
	private ArrayList<Order> warehouse = new ArrayList<>();
	private ArrayList<AID> customers = new ArrayList<>();
	private ArrayList<AID> suppliers = new ArrayList<>();
	private ArrayList<Integer> profits = new ArrayList<>();
	private int custRespCount = 0;
	private int day = 0;
	private int idCounter = 1;
	private int net = 0;
	private int dailyTakings = 0;
	
	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Manufacturer");
		sd.setName(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException e) {
			e.printStackTrace();
		}
		addBehaviour(new TickerWaiter(this));
	}
	
	protected void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch (FIPAException e) {
			e.printStackTrace();
		}
	}
	
	public class TickerWaiter extends CyclicBehaviour {
		
		public TickerWaiter(Agent a) {
			super(a);
		}
		
		public void action() {
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"),
					MessageTemplate.MatchContent("terminate"));
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (tickerAgent == null) {
					tickerAgent = msg.getSender();
					////System.out.println("(manufacturer)found ticker agent");
				}
				if (msg.getContent().equals("new day")) {
					day++;
					myAgent.addBehaviour(new FindCustomers(myAgent));
					myAgent.addBehaviour(new FindSuppliers(myAgent));
					ArrayList<Behaviour> cb = new ArrayList<>();
					
					myAgent.addBehaviour(new CollectOrders(myAgent));
					myAgent.addBehaviour(new QuerySupplier(myAgent));
					CyclicBehaviour collectAccepted = new CollectAccepted(myAgent);
					cb.add(collectAccepted);
					myAgent.addBehaviour(collectAccepted);
					CyclicBehaviour collectRefused = new CollectRefused(myAgent);
					cb.add(collectRefused);
					myAgent.addBehaviour(collectRefused);
					CyclicBehaviour respondCustomers = new RespondCustomers(myAgent);
					cb.add(respondCustomers);
					myAgent.addBehaviour(respondCustomers);
					CyclicBehaviour informListener = new InformListener(myAgent);
					cb.add(informListener);
					myAgent.addBehaviour(informListener);
					myAgent.addBehaviour(new DeliverOrders(myAgent));
					myAgent.addBehaviour(new EndDay(myAgent, cb));
				}
				else {
					myAgent.doDelete();
				}
			}
			else {
				block();
			}
		}
	}
	

	
	
	public class FindCustomers extends OneShotBehaviour {
		
		public FindCustomers(Agent a) {
			super(a);
		}
		
		@Override
		public void action() {
			doWait(1000);
			DFAgentDescription customerTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("customer");
			customerTemplate.addServices(sd);
			try {
				customers.clear();
				DFAgentDescription[] customerAgents = DFService.search(myAgent, customerTemplate);
				for (int i = 0; i < customerAgents.length; i++) {
					customers.add(customerAgents[i].getName());
				}
			}
			catch(FIPAException e) {
				e.printStackTrace();
			}
		}
	}
	
	public class FindSuppliers extends OneShotBehaviour {
		public FindSuppliers(Agent a) {
			super(a);
		}
		
		@Override
		public void action() {
			DFAgentDescription supplierTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("supplier");
			supplierTemplate.addServices(sd);
			try {
				suppliers.clear();
				DFAgentDescription[] supplierAgents = DFService.search(myAgent, supplierTemplate);
				for (int i = 0; i < supplierAgents.length; i++) {
					suppliers.add(supplierAgents[i].getName());
				}
			}
			catch(FIPAException e) {
				e.printStackTrace();
			}
		}
	}
	
	public class RespondCustomers extends CyclicBehaviour {
		public RespondCustomers(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			if (acceptedOrders.size() + refusedOrders.size() == customers.size()) {
				for (int i = 0; i < acceptedOrders.size(); i++) {
	
					Order order = acceptedOrders.get(i);
					AID customerAID = order.getCustomer();
					
					Sell sell = new Sell();
					sell.setOrder(order);
					
					ACLMessage msg = new ACLMessage(ACLMessage.AGREE);
					msg.addReceiver(customerAID);
					msg.setLanguage(codec.getName());
					msg.setOntology(ontology.getName());
					
					Action custSell = new Action();
					custSell.setAction(sell);
					custSell.setActor(myAgent.getAID());
					try {
						getContentManager().fillContent(msg, custSell);
						send(msg);
						custRespCount++;
						System.out.println(myAgent.getLocalName() + " Agree to " + customerAID.getLocalName() + " order " + order.getId());
						waitingOrders.add(acceptedOrders.get(i));
					}
					catch(CodecException ce) {
						ce.printStackTrace();
					}
					catch(OntologyException oe) {
						oe.printStackTrace();
					}
				}
				acceptedOrders.clear();
				for (int i = 0; i < refusedOrders.size(); i++) {
					Order order = refusedOrders.get(i);
					AID customerAID = order.getCustomer();
					System.out.println(myAgent.getLocalName() + " refused " + customerAID.getLocalName() + "'s order ID: " + order.getId());
					
					Sell sell = new Sell();
					sell.setOrder(order);
					
					ACLMessage msg = new ACLMessage(ACLMessage.REFUSE);
					msg.addReceiver(customerAID);
					msg.setLanguage(codec.getName());
					msg.setOntology(ontology.getName());
					
					Action custSell = new Action();
					custSell.setAction(sell);
					custSell.setActor(myAgent.getAID());
					try {
						getContentManager().fillContent(msg, custSell);
						send(msg);
						custRespCount++;
					}
					catch(CodecException ce) {
						ce.printStackTrace();
					}
					catch(OntologyException oe) {
						oe.printStackTrace();
					}
				}
				refusedOrders.clear();
				myAgent.removeBehaviour(this);
			}
		}
	}
	
	public class QuerySupplier extends OneShotBehaviour {
		
		public QuerySupplier(Agent a) {
			super(a);
		}

		@Override
		public void action() {
				for (Order o : orders) {
					System.out.println(myAgent.getLocalName() + " querying " + suppliers.get(0).getLocalName() + " order ID: " + o.getId());
					Sell sell = new Sell();
					sell.setOrder(o);
					
					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					msg.addReceiver(suppliers.get(0)); //once the rest of the suppliers are added need to cange back to 2
					msg.setLanguage(codec.getName());
					msg.setOntology(ontology.getName());
					
					Action order = new Action();
					order.setAction(sell);
					order.setActor(suppliers.get(0));
					
					try {
						getContentManager().fillContent(msg, order);
						send(msg);
						//System.out.println("Sending order to supplier " + msg.getContent());
						//System.out.println("(manufacturer)supplier queried...");
					}
					catch(CodecException ce) {
						ce.printStackTrace();
					}
					catch(OntologyException oe) {
						oe.printStackTrace();
					}
			}
		}	
	}
	
	public class InformListener extends CyclicBehaviour {
		
		public InformListener(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (!msg.getSender().equals(tickerAgent)) {
					try {
						ContentElement ce = null;
						ce = getContentManager().extractContent(msg);
						if (ce instanceof Action) {
							Concept action = ((Action)ce).getAction();
							if(action instanceof Deliver) {
								Deliver deliver = (Deliver) action;
								Order delivery = deliver.getOrder();
								for(Order order : waitingOrders) {
									if (order.getId() == delivery.getId()) {
										warehouse.add(order);
										waitingOrders.remove(order);
										System.out.println("Order ID: " + order.getId() + " is in the warehouse.");
										break;
									}
								}
							}
							
							else if (action instanceof MakePayment) {
								MakePayment payment = (MakePayment)action;
								net = net + payment.getAmount();
								System.out.println("Manufacturer has received payment for order ID: " + payment.getID());
							}
							
						}
					}
					catch(CodecException ce2) {
						ce2.printStackTrace();
					}
					catch(OntologyException oe) {
						oe.printStackTrace();
					}
				}
			}
			else {
				block();
			}
		}
	}
	
	public class CollectAccepted extends CyclicBehaviour {

		public CollectAccepted(Agent myAgent) {
			super(myAgent);
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				ContentElement ce = null;
				try {
					ce = getContentManager().extractContent(msg);
					if (ce instanceof Action) {
						Concept action = ((Action)ce).getAction();
						if (action instanceof SuppSell) {
							SuppSell order = (SuppSell) action;
							Order custOrder = order.getOrder();
							
							//System.out.println("got here");
							if (!priceEval(custOrder)) {
								System.out.println(myAgent.getLocalName() + " refused due to price " + custOrder.getCustomer().getLocalName() + "'s order ID: " + custOrder.getId());
								refusedOrders.add(custOrder);
								////////System.out.println("(manufacturer)cancel customer order");
								AID supplier = order.getSupplier();
								Sell sell = new Sell();
								sell.setOrder(custOrder);
								
								ACLMessage cancel = new ACLMessage(ACLMessage.CANCEL);
								cancel.addReceiver(supplier);
								cancel.setLanguage(codec.getName());
								cancel.setOntology(ontology.getName());
								
								Action suppSell = new Action();
								suppSell.setAction(sell);
								suppSell.setActor(supplier);
								try {
									getContentManager().fillContent(cancel, suppSell);
									send(cancel);
									System.out.println(myAgent.getLocalName() + " cancel order " + custOrder.getId() + " with supplier");
								}
								catch(CodecException ce2) {
									ce2.printStackTrace();
								}
							}
							else {
								acceptedOrders.add(custOrder);
								ACLMessage payment = new ACLMessage(ACLMessage.INFORM);
								payment.addReceiver(msg.getSender());
								payment.setLanguage(codec.getName());
								payment.setOntology(ontology.getName());
								
								MakePayment makePayment = new MakePayment();
								makePayment.setAmount(custOrder.getTotalCost());
								makePayment.setPayer(myAgent.getAID());
								makePayment.setID(custOrder.getId());
								
								Action sendPayment = new Action();
								sendPayment.setAction(makePayment);
								sendPayment.setActor(myAgent.getAID());
								getContentManager().fillContent(payment, sendPayment);
								send(payment);
							}
							
							ArrayList<Integer> ordersToRemove = new ArrayList<>();
							for (Order order1 : orders) {
								if (order1.getId() == custOrder.getId()) {
									ordersToRemove.add(custOrder.getId());
								}
							}
							for (int i = 0; i < ordersToRemove.size(); i++) {
								for (int j = 0; j < orders.size(); j++) {
									if (orders.get(j).getId() == ordersToRemove.get(i)) {
										orders.remove(j);
										break;
									}
								}
							}
						}
					}
				} catch (CodecException e) {
					e.printStackTrace();
				} catch (OntologyException e) {
					e.printStackTrace();
				}
			}
			else {
				block();
			}
		}
		
		private boolean priceEval(Order order) {
			if (order.getPrice() > order.getTotalCost()) {
				return true;
			}
			else {
				return false;
			}
		}
		
	}
	
	public class CollectRefused extends CyclicBehaviour {

		public CollectRefused(Agent myAgent) {
			super(myAgent);
		}

		@Override
		public void action() {
			ArrayList<Integer> ordersToRemove = new ArrayList<>();
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REFUSE);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				ContentElement ce = null;
				try {
					ce = getContentManager().extractContent(msg);
					if (ce instanceof Action) {
						Concept action = ((Action)ce).getAction();
						if (action instanceof SuppSell) {
							SuppSell order = (SuppSell) action;
							Order custOrder = order.getOrder();
							for (Order order1 : orders) {
								if (order1.getId() == custOrder.getId()) {
									refusedOrders.add(custOrder);
									ordersToRemove.add(custOrder.getId());
									//System.out.println(myAgent.getLocalName() + " refused " + order1.getCustomer().getLocalName() + "'s order ID: " + order1.getId());
								}
							}
							for (int i = 0; i < ordersToRemove.size(); i++) {
								for (int j = 0; j < orders.size(); j++) {
									if (orders.get(j).getId() == ordersToRemove.get(i)) {
										orders.remove(j);
										break;
									}
								}
							}
						}
					}
				} catch (CodecException e) {
					e.printStackTrace();
				} catch (OntologyException e) {
					e.printStackTrace();
				}
			}
			else {
				block();
			}
		}
		
		
	}
	
	private class CollectOrders extends Behaviour {
		
		public CollectOrders(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			for (AID customer : customers) {
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
				ACLMessage msg = myAgent.receive(mt);
				//listen for message
				if(msg != null) {
					try {
						ContentElement ce = null;
						ce = getContentManager().extractContent(msg);
						if (ce instanceof Action) {
							Concept action = ((Action)ce).getAction();
							if (action instanceof Sell) { //if the customer wants to buy
								Sell order = (Sell) action;
								Order custOrder = order.getOrder(); //and the order has been fiolled in
								////System.out.println("(manufacturer)received an order");
								if (custOrder != null) {
									//add order to order queue
									custOrder.setId(idCounter++);
									System.out.println(myAgent.getLocalName() + " received order number " + custOrder.getId());
									orders.add(custOrder);
								}
							}
						}
					}
					catch(CodecException coE) {
						coE.printStackTrace();
					}
					catch(OntologyException oe) {
						oe.printStackTrace();
					}
				}
				else {
					block();
				}
			}
		}

		@Override
		public boolean done() {
			return false;
		}
	}
	
	private class DeliverOrders extends OneShotBehaviour {
		public DeliverOrders(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			ArrayList<Order> toRemove = new ArrayList<>();
			for (Order order : warehouse) {
				if (day >= order.getDeliveryDate()) {
					toRemove.add(order);
					
					Deliver deliver = new Deliver();
					deliver.setDeliverTo(order.getCustomer());
					deliver.setOrder(order);
					
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.addReceiver(order.getCustomer());
					msg.setLanguage(codec.getName());
					msg.setOntology(ontology.getName());
					
					Action delivery = new Action();
					delivery.setAction(deliver);
					delivery.setActor(myAgent.getAID());
					
					try {
						getContentManager().fillContent(msg, delivery);
						send(msg);
						System.out.println(myAgent.getLocalName() + " has delivered order ID: " + order.getId());
						dailyTakings = dailyTakings + order.getPrice();
					}
					catch (CodecException ce) {
						ce.printStackTrace();
					}
					catch(OntologyException oe) {
						oe.printStackTrace();
					}
				}
			}
			deliveredOrders.addAll(toRemove);
			warehouse.removeAll(toRemove);
		}
	}
		
	public class EndDay extends CyclicBehaviour {
		private ArrayList<Behaviour> toRemove;
		public EndDay(Agent a, ArrayList<Behaviour> toRemove) {
			super(a);
			this.toRemove = toRemove;
		}

		@Override
		public void action() 
		{
			if (custRespCount == customers.size()) {
				custRespCount = 0;
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(tickerAgent);
				msg.setLanguage(codec.getName());
				msg.setOntology(ontology.getName());
				msg.setContent("done");
				myAgent.send(msg);
				System.out.println();
				System.out.println("Profit for day " + day + " is £" + singleDayProfit());
				dailyTakings = 0;
				for (Behaviour b : toRemove) {
					myAgent.removeBehaviour(b);
				}
				if(day == 90) {
					System.out.println("Profit made this simulation is: £" + totalProfit());
				}
				myAgent.removeBehaviour(this);
			}
		}
	}
	
	private int totalProfit() {
		int totalProfit = 0;
		for (int i = 0; i < profits.size(); i++) {
			totalProfit = totalProfit + profits.get(i);
		}
		return totalProfit;
	}
	
	private int singleDayProfit() {
		int singleDayProfit = totalValueShipped() - warehouseStorage() - suppliesPurchased();
		profits.add(singleDayProfit);
		return singleDayProfit;
	}
	private int totalValueShipped() {
		return dailyTakings;
	}
	private int warehouseStorage() {
		int cpp = 5;
		int total = 0;
		for (Order order : warehouse) {
			for (int i = 0; i < order.getParts().size(); i++) {
				total = total + cpp;
			}
		}
		return total;
	}
	private int suppliesPurchased() {
		int total = 0;
		for (Order order : acceptedOrders) {
			total = total + order.getTotalCost();
		}
		return total;
	}
}
