package elements;

import jade.content.AgentAction;
import jade.core.AID;

public class Place implements AgentAction {
	private AID seller;
	private Order order;
	
	public AID getSeller() {
		return seller;
	}
	
	public void setSeller(AID seller) {
		this.seller = seller;
	}
	
	public Order getOrder() {
		return order;
	}
	
	public void setOrder(Order order) {
		this.order = order;
	}
	
}
