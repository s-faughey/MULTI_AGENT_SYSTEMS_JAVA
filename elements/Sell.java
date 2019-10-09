package elements;

import jade.content.AgentAction;
import jade.core.AID;

public class Sell implements AgentAction {
	private Order order;
	
	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}	
	
}
