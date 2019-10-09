package implementation;

import Ontology.Ontology;
import elements.*;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class Customer extends Agent{
	private Codec codec = new SLCodec();
	private Ontology ontology = Ontology.getInstance();
	private ArrayList<AID> manufacturers = new ArrayList<>();
	private AID manuAID;
	private AID tickerAgent;
	private ArrayList<Order> ordersWaiting = new ArrayList<>();
	private ArrayList<Order> ordersDelivered = new ArrayList<>();
	private boolean response = false;
	private int day = 0;
	
	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Customer");
		sd.setName(getLocalName() + "-customer-agent");
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

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"),
					MessageTemplate.MatchContent("terminate"));
			ACLMessage msg = myAgent.receive(mt); 
			if(msg != null) {
				if(tickerAgent == null) {
					tickerAgent = msg.getSender();
				}
				if (msg.getContent().equals("new day")) {
					day++;
					myAgent.addBehaviour(new FindManufacturer(myAgent));
					myAgent.addBehaviour(new GenerateOrderBehaviour(myAgent));
					
				//	SequentialBehaviour dailyActivity = new SequentialBehaviour();
				//	dailyActivity.addSubBehaviour(new FindManufacturer(myAgent));
				//	dailyActivity.addSubBehaviour(new GenerateOrderBehaviour(myAgent));
					CyclicBehaviour manuListener = new ManufacturerListener(myAgent);
					CyclicBehaviour deliveryListener = new DeliveryListener(myAgent);
					myAgent.addBehaviour(manuListener);
					myAgent.addBehaviour(deliveryListener);
					ArrayList<Behaviour> cb = new ArrayList<>();
					cb.add(manuListener);
					cb.add(deliveryListener);
					myAgent.addBehaviour(new EndDay(myAgent, cb));
			//		dailyActivity.addSubBehaviour(new EndDay(myAgent, MLWrapper));
			//		myAgent.addBehaviour(dailyActivity);
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
	
	public class DeliveryListener extends CyclicBehaviour {
		public DeliveryListener(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = receive(mt);
			if (msg != null) {
				if(!msg.getSender().equals(tickerAgent)) {
					try {
						ContentElement ce = null;
						ce = getContentManager().extractContent(msg);
						if (ce instanceof Action) {
							Concept action = ((Action)ce).getAction();
							if(action instanceof Deliver) {
								Deliver delivery = (Deliver)action;
								Order order = delivery.getOrder();
								ordersDelivered.add(order);
								System.out.println(myAgent.getLocalName() + " has received order ID: " + order.getId());
								ArrayList<Order> temp = new ArrayList<>();
								for (Order WOrder : ordersWaiting) {
									if (WOrder.getId() == order.getId()) {
										temp.add(WOrder);
									}
								}
								ordersWaiting.removeAll(temp);
								
								ACLMessage payment = new ACLMessage(ACLMessage.INFORM);
								payment.addReceiver(msg.getSender());
								payment.setLanguage(codec.getName());
								payment.setOntology(ontology.getName());
								
								MakePayment makePayment = new MakePayment();
								makePayment.setAmount(order.getPrice());
								makePayment.setPayer(myAgent.getAID());
								makePayment.setID(order.getId());
								
								Action sendPayment = new Action();
								sendPayment.setAction(makePayment);
								sendPayment.setActor(myAgent.getAID());
								getContentManager().fillContent(payment, sendPayment);
								send(payment);
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
		}
	}
	
	public class ManufacturerListener extends CyclicBehaviour {
		public ManufacturerListener (Agent a) {
			super(a);
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.AGREE), MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
			ACLMessage msg = receive(mt);
			if (msg != null) {
				try {
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					if (ce instanceof Action) {
						Concept action = ((Action)ce).getAction();
						if (action instanceof Sell) {
							if (msg.getPerformative() == 1) {
								Sell sell = (Sell)action;
								Order order = sell.getOrder();
								ordersWaiting.add(order);
								response = true;
							}
							else {
								response = true;
							}
						}
					}
				}
				catch (CodecException ce) {
					ce.printStackTrace();
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
	
	public class FindManufacturer extends OneShotBehaviour {
		
		public FindManufacturer(Agent a) {
			super(a);
		}
		
		@Override
		public void action() {
			DFAgentDescription manuTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("manufacturer");
			manuTemplate.addServices(sd);
			try {
				manufacturers.clear();
				DFAgentDescription[] manuAgents = DFService.search(myAgent, manuTemplate);
				for (int i = 0; i < manuAgents.length; i++) {
					manufacturers.add(manuAgents[i].getName());
				}
			}
			catch(FIPAException e) {
				e.printStackTrace();
			}
			manuAID = manufacturers.get(0);
		}
	}
	
	//i suspect this should be part of a sequential behaviour
	private class GenerateOrderBehaviour extends OneShotBehaviour {
		
		public GenerateOrderBehaviour(Agent a) {
			super(a);
		}
		
		@Override
		public void action() {
			Order order = new Order();
			
			List<Part> parts = new ArrayList<Part>();
			
			RAM ram = new RAM();
			HDD hdd = new HDD();
			OS os = new OS();
			
			if (Math.round(Math.random()) < 0.5) {
				DesktopCPU cpu = new DesktopCPU();
				parts.add(cpu);
				DesktopMotherboard mbd = new DesktopMotherboard();
				parts.add(mbd);
			}
			else {
				LaptopCPU cpu = new LaptopCPU();
				parts.add(cpu);
				LaptopMotherboard mbd = new LaptopMotherboard();
				parts.add(mbd);
				LScreen screen = new LScreen();
				parts.add(screen);
			}
			if(Math.round(Math.random()) < 0.5) {
				ram.setMemory(8);
			}
			else {
				ram.setMemory(16);
			}

			if(Math.round(Math.random()) < 0.5) {
				hdd.setSize(1);
			}
			else {
				hdd.setSize(2);
			}

			if (Math.round(Math.random()) < 0.5) {
				os.setOs("Windows");
			}
			else {
				os.setOs("Linux");
			}
			parts.add(ram);
			parts.add(hdd);
			parts.add(os);
			
			int quantity = floor(1+50 * Math.random());
			int price = quantity * floor(600+200*Math.random());
			int dueInDays = floor(1 + 10*Math.random());
			
			order.setQuantity(quantity);
			order.setPrice(price);
			order.setDeliveryDate(day + dueInDays);
			order.setParts(parts);
			order.setCustomer(myAgent.getAID());
			order.setOrderedOn(day);
			
			Sell custOrder = new Sell();
			custOrder.setOrder(order);
			
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.addReceiver(manuAID);
			msg.setLanguage(codec.getName());
			msg.setOntology(ontology.getName());
			
			Action placeOrder = new Action();
			placeOrder.setAction(custOrder);
			placeOrder.setActor(manuAID);
			try {
				getContentManager().fillContent(msg, placeOrder);
				send(msg);
				System.out.println(myAgent.getLocalName() + " sent order");
			}
			catch(CodecException ce) {
				ce.printStackTrace();
			}
			catch (OntologyException oe) {
				oe.printStackTrace();
			}	
		}
	}
	
	private int floor(double num) {
		return (int)num;
	}
	
	public class EndDay extends CyclicBehaviour {
		private ArrayList<Behaviour> toRemove;
		public EndDay(Agent a, ArrayList<Behaviour> toRemove) {
			super(a);
			this.toRemove = toRemove;
		}

		@Override
		public void action() {
			if (response) {
				response = false;
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(tickerAgent);
				msg.setLanguage(codec.getName());
				msg.setOntology(ontology.getName());
				msg.setContent("done");
				myAgent.send(msg);
				for (Behaviour b : toRemove) {
					myAgent.removeBehaviour(b);
				}
				myAgent.removeBehaviour(this);
			}
		}
	}
	
}
