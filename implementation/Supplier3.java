package implementation;

import java.util.ArrayList;

import Ontology.Ontology;
import elements.*;
import elements.SuppSell;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
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

public class Supplier3 extends Agent{

	private Codec codec = new SLCodec();
	private Ontology ontology = Ontology.getInstance();
	private int timeToDeliver = 3;
	private int[] prices = {150, 110, 95, 50, 30, 70, 35, 55, 60, 75, 0};
	private ArrayList<Order> orders;
	private AID tickerAgent;
	private int day = 0;
	private AID manufacturer;
	private int net = 0;
	private int deliverycount;
	private int acceptedcount;
	
	protected void setup() {
		
		orders = new ArrayList<Order>();
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Supplier");
		sd.setName(getLocalName() + "-supplier3-agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException e) {
			e.printStackTrace();
		}
		
		addBehaviour(new TickerWaiter(this));
	}
	
	private class DeliverOrders extends OneShotBehaviour {
		public DeliverOrders(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			ArrayList<Order> toRemove = new ArrayList<>();
			for (Order order : orders) {
				if (day >= order.getOrderedOn() + timeToDeliver) {
					toRemove.add(order);
					
					Deliver deliver = new Deliver();
					deliver.setDeliverTo(manufacturer);
					deliver.setOrder(order);
					
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.addReceiver(manufacturer);
					msg.setLanguage(codec.getName());
					msg.setOntology(ontology.getName());
					
					Action delivery = new Action();
					delivery.setAction(deliver);
					delivery.setActor(myAgent.getAID());
					
					try {
						getContentManager().fillContent(msg, delivery);
						send(msg);
						System.out.println(myAgent.getLocalName() + " has delivered order ID: " + order.getId());
						deliverycount++;
					}
					catch (CodecException ce) {
						ce.printStackTrace();
					}
					catch(OntologyException oe) {
						oe.printStackTrace();
					}
				}
			}
			orders.removeAll(toRemove);
		}
	}
	
	private class TickerWaiter extends CyclicBehaviour {
		public TickerWaiter(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"),
					MessageTemplate.MatchContent("terminate"));
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (tickerAgent == null) {
					tickerAgent = msg.getSender();
				}
				if (msg.getContent().equals("new day")) {
					day++;
					ArrayList<Behaviour> cb = new ArrayList<>();
					CyclicBehaviour evaluateOrder = new EvaluateOrder(myAgent);
					cb.add(evaluateOrder);
					myAgent.addBehaviour(evaluateOrder);
					CyclicBehaviour receivePayments = new ReceivePayments(myAgent);
					cb.add(receivePayments);
					myAgent.addBehaviour(receivePayments);
					CyclicBehaviour cancelReceiver = new CancelReceiver(myAgent);
					cb.add(cancelReceiver);
					myAgent.addBehaviour(cancelReceiver);
					myAgent.addBehaviour(new DeliverOrders(myAgent));
					myAgent.addBehaviour(new EndDayListener(myAgent, cb));
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
	
	private class CancelReceiver extends CyclicBehaviour {
		public CancelReceiver(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			Order orderToRemove = new Order();
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
			ACLMessage msg = receive(mt);
			if (msg != null) {
				try {
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					if (ce instanceof Action) {
						Concept action = ((Action)ce).getAction();
						if (action instanceof Sell) {
							Sell sell = (Sell)action;
							Order order = sell.getOrder();
							for (int i = 0; i < orders.size(); i++) {
								if (order.getId() == orders.get(i).getId()) {
									orderToRemove = orders.get(i);
									acceptedcount--;
									////system.out.println("Order cancelled");
								}
							}
							orders.remove(orderToRemove);
						}
					}
				}
				catch (CodecException ce2) {
					ce2.printStackTrace();
				}
				catch(OntologyException oe) {
					oe.printStackTrace();
				}
			}
		}
	}
	
	private class ReceivePayments extends CyclicBehaviour {
		
		public ReceivePayments(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = receive(mt);
			if (msg != null) {
				if (!msg.getSender().equals(tickerAgent)) {
					try {
						ContentElement ce = null;
						ce = getContentManager().extractContent(msg);
						if (ce instanceof Action) {
							Concept action = ((Action)ce).getAction();
							if (action instanceof MakePayment) {
								MakePayment payment = (MakePayment)action;
								net = net + payment.getAmount();
								System.out.println("Supplier has received payment for order ID: " + payment.getID());
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
	
	private class EvaluateOrder extends CyclicBehaviour {
		
		public EvaluateOrder(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = receive(mt);
			if (msg != null) {
				try {
					manufacturer = msg.getSender();
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					if (ce instanceof Action) {
						Concept action = ((Action)ce).getAction();
						if(action instanceof Sell) {
							Sell sell = (Sell)action;
							Order order = sell.getOrder();
							//System.out.println("day " + day);
							if (order.getDeliveryDate() >= day + timeToDeliver) {
								orders.add(order);
								SuppSell supSell = new SuppSell();
								order.setTotalCost(getCost(order));
								////system.out.println(getCost(order));
								supSell.setOrder(order);
								supSell.setSupplier(myAgent.getAID());
								
								ACLMessage suppMsg = new ACLMessage(ACLMessage.AGREE);
								suppMsg.addReceiver(msg.getSender());
								suppMsg.setLanguage(codec.getName());
								suppMsg.setOntology(ontology.getName());
								
								Action suppSell = new Action();
								suppSell.setAction(supSell);
								suppSell.setActor(myAgent.getAID());
								try {
									getContentManager().fillContent(suppMsg, suppSell);
									send(suppMsg);
									System.out.println(myAgent.getLocalName() + " accepts order ID: " + order.getId());
									acceptedcount++;
								}
								catch(CodecException ce2) {
									ce2.printStackTrace();
								}
								
								
							}
							else {
								SuppSell supSell = new SuppSell();
								order.setTotalCost(getCost(order));
								supSell.setOrder(order);
								supSell.setSupplier(myAgent.getAID());
								
								ACLMessage suppMsg = new ACLMessage(ACLMessage.REFUSE);
								suppMsg.addReceiver(msg.getSender());
								suppMsg.setLanguage(codec.getName());
								suppMsg.setOntology(ontology.getName());
								
								Action suppSell = new Action();
								suppSell.setAction(supSell);
								suppSell.setActor(myAgent.getAID());
								
								try {
									getContentManager().fillContent(suppMsg, suppSell);
									send(suppMsg);
									System.out.println(myAgent.getLocalName() + " refuses order ID: " + order.getId());
								}
								catch(CodecException ce2) {
									ce2.printStackTrace();
								}
							}
						}
					}
				}
				catch (CodecException ce) {
					ce.printStackTrace();
				}
				catch (OntologyException oe) {
					oe.printStackTrace();
				}
				msg = null;
			}
		}

		private int getCost(Order order) {
			int ppu = 0; //price per unit
			int quantity = order.getQuantity();
			for (Part p : order.getParts()) {
				if (p instanceof LaptopCPU) {
					ppu = ppu + prices[0];
				}
				else if(p instanceof DesktopCPU) {
					ppu = ppu + prices[1];
				}
				else if (p instanceof LaptopMotherboard) {
					ppu = ppu + prices[2];
				}
				else if (p instanceof DesktopMotherboard) {
					ppu = ppu + prices[3];
				}
				else if (p instanceof RAM) {
					if (((RAM)p).getMemory() == 8) {
						ppu = ppu + prices[4];
					}
					else {
						ppu = ppu + prices[5];
					}
				}
				else if (p instanceof HDD) {
					if (((HDD)p).getSize() == 1) {
						ppu = ppu + prices[6];
					}
					else {
						ppu = ppu + prices[7];
					}
				}
				else if (p instanceof LScreen) {
					ppu = ppu + prices[8];
				}
				else if (p instanceof OS) {
					if (((OS)p).getOs() == "Windows") {
						ppu = ppu + prices[9];
					}
					else {
						ppu = ppu + prices[10];
					}
				}
			}
			return ppu * quantity;
		}
	}
	
	public class EndDayListener extends CyclicBehaviour {
		private ArrayList<Behaviour> toRemove;
		public EndDayListener(Agent a, ArrayList<Behaviour> toRemove) {
			super(a);
			this.toRemove = toRemove;
		}
		
		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchContent("done");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				for (Behaviour b : toRemove) {
					myAgent.removeBehaviour(b);
				}
				myAgent.removeBehaviour(this);
			}
			else {
				block();
			}
		}
	}
	
	protected void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch (FIPAException e) {
			e.printStackTrace();
		}
	}
	public int getTimeToDeliver() {
		return timeToDeliver;
	}
	
	public int getPrice(int index) {
		return prices[index];
	}
}
