package implementation;

import java.util.ArrayList;

import Ontology.Ontology;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgentSynchTicker extends Agent{
	private Codec codec = new SLCodec();
	private Ontology ontology = Ontology.getInstance();
	private int day = 0;
	public static final int NUM_DAYS = 90;
	@Override
	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("synch");
		sd.setName(getLocalName() + "-ticker-agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch(FIPAException e) {
			e.printStackTrace();
		}
		doWait(10000);
		addBehaviour(new SynchAgentsBehaviour(this));
	}
	
	@Override
	protected void takeDown() {
		try {
			DFService.deregister(this);
		}
		catch(FIPAException e) {
			e.printStackTrace();
		}
	}
	
	public class SynchAgentsBehaviour extends Behaviour {
		private int step = 0;
		private int numFinReceived = 0;
		private ArrayList<AID> simulationAgents = new ArrayList<>();
		private ArrayList<AID> suppliersList = new ArrayList<>();
		
		public SynchAgentsBehaviour(Agent a) {
			super(a);
		}
		
		@Override
		public void action() {
			switch(step) {
			case 0:
				DFAgentDescription cusTemplate = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("Customer");
				cusTemplate.addServices(sd);
				DFAgentDescription manuTemplate = new DFAgentDescription();
				ServiceDescription sd2 = new ServiceDescription();
				sd2.setType("Manufacturer");
				manuTemplate.addServices(sd2);
				DFAgentDescription supTemplate = new DFAgentDescription();
				ServiceDescription sd3 = new ServiceDescription();
				sd3.setType("Supplier");
				supTemplate.addServices(sd3);
				try {
					DFAgentDescription[] customers = DFService.search(myAgent, cusTemplate);
					for (int i = 0; i < customers.length; i++) {
						simulationAgents.add(customers[i].getName());
					}
					DFAgentDescription[] manufacturer = DFService.search(myAgent, manuTemplate);
					for (int i = 0; i < manufacturer.length; i++) {
						simulationAgents.add(manufacturer[i].getName());
					}
					DFAgentDescription[] suppliers = DFService.search(myAgent, supTemplate);
					for (int i = 0; i < suppliers.length; i++) {
						suppliersList.add(suppliers[i].getName());
					}
				}
				catch(FIPAException e) {
					e.printStackTrace();
				}
				ACLMessage tick = new ACLMessage(ACLMessage.INFORM);
				tick.setContent("new day");
				tick.setLanguage(codec.getName());
				tick.setOntology(ontology.getName());
				for (AID id : simulationAgents) {
					tick.addReceiver(id);
				}
				for (AID id : suppliersList) {
					tick.addReceiver(id);
				}
				step++;
				day++;
				System.out.println("Beginning of day " + day);
				myAgent.send(tick);
				break;
			case 1:
				MessageTemplate mt = MessageTemplate.MatchContent("done");
				ACLMessage msg = myAgent.receive(mt);
				if (msg != null) {
					numFinReceived++;
					if (numFinReceived == simulationAgents.size()) {
						//end of day
						ACLMessage message = new ACLMessage(ACLMessage.INFORM);
						for (AID supplier : suppliersList) {
							message.addReceiver(supplier);
						}
						message.setLanguage(codec.getName());
						message.setOntology(ontology.getName());
						message.setContent("done");
						myAgent.send(message);
						step++;
					}
				}
				else {
					block();
				}
			}
		}

		@Override
		public boolean done() {
			return step==2;
		}
		
		@Override
		public void reset() {
			super.reset();
			step = 0;
			simulationAgents.clear();
			suppliersList.clear();
			numFinReceived = 0;
			System.out.println("");
		}
		
		public int onEnd() {
			System.out.println("End of day " + day);
			if (day == NUM_DAYS) {
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setContent("terminate");
				for (AID agent : simulationAgents) {
					msg.addReceiver(agent);
				}
				for (AID id : suppliersList) {
					msg.addReceiver(id);
				}
				myAgent.send(msg);
				myAgent.doDelete();
			}
			else {
				reset();
				myAgent.addBehaviour(this);
			}
			return 0;
		}
	}
	
}
