package implementation;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jade.core.Runtime;

public class Main {

	public static void main(String[] args) {
		//jade setup
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		ContainerController myContainer = myRuntime.createMainContainer(myProfile);
		try {
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma",  null);
			rma.start();
		}
		catch (Exception e) {
			System.out.println("Exception starting agent: " + e.toString());
		}
		
		//create agents
		try {
			AgentController agentSynch = myContainer.createNewAgent("Synch",  AgentSynchTicker.class.getCanonicalName(), null);
			agentSynch.start();
			AgentController manufacturer = myContainer.createNewAgent("Manufacturer", Manufacturer.class.getCanonicalName(), null);
			manufacturer.start();
			AgentController customer1 = myContainer.createNewAgent("Customer1", Customer.class.getCanonicalName(), null);
			customer1.start();
			AgentController customer2 = myContainer.createNewAgent("Customer2", Customer.class.getCanonicalName(), null);
			customer2.start();
			AgentController customer3 = myContainer.createNewAgent("Customer3", Customer.class.getCanonicalName(), null);
			customer3.start();
			AgentController supplier3 = myContainer.createNewAgent("Supplier", Supplier3.class.getCanonicalName(), null);
			supplier3.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Exception starting agent: " + e.toString());;
		}
	}
}
