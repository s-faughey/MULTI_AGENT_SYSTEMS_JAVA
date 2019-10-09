package elements;

import jade.content.Predicate;
import jade.core.AID;

public class PlacedOrder implements Predicate{
	private AID customer;
	private Order order;
	
	public AID getCustomer() {
		return customer;
	}
	
	public void setCustomer(AID customer) {
		this.customer = customer;
	}
	
	public Order getOrder() {
		return order;
	}
	
	public void setOrder(Order order) {
		this.order = order;
	}
}
