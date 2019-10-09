package implementation;

import Ontology.Ontology;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class Supplier2 extends Agent{
	private Codec codec = new SLCodec();
	private Ontology ontology = Ontology.getInstance();
	private int timeToDeliver = 3;
	private int[] prices = {175, 130, 115, 60, 40, 80, 45, 65, 80, 75, 0};
	
	protected void setup() {
	
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Supplier-agent");
		sd.setName(getLocalName() + "-supplier2-agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException e) {
			e.printStackTrace();
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
