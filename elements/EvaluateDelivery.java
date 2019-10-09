package elements;

import jade.core.AID;
import jade.content.AgentAction;

public class EvaluateDelivery implements AgentAction {
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
