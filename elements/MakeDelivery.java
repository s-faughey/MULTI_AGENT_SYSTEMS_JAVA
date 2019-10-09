package elements;

import jade.core.AID;
import jade.content.AgentAction;

public class MakeDelivery implements AgentAction {
	private Order order;
	private AID buyer;
	public Order getOrder() {
		return order;
	}
	public void setOrder(Order order) {
		this.order = order;
	}
	public AID getBuyer() {
		return buyer;
	}
	public void setBuyer(AID buyer) {
		this.buyer = buyer;
	}
}
