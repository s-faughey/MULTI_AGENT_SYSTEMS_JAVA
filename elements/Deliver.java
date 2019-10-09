package elements;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

public class Deliver implements AgentAction {
	private Order order;
	private AID deliverTo;
	
	@Slot(mandatory = true)
	public Order getOrder() {
		return order;
	}
	public void setOrder(Order order) {
		this.order = order;
	}
	@Slot(mandatory = true)
	public AID getDeliverTo() {
		return deliverTo;
	}
	public void setDeliverTo(AID deliverTo) {
		this.deliverTo = deliverTo;
	}
	
}
