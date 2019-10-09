package elements;

import jade.content.Predicate;
import jade.core.AID;

public class DeliveryBy implements Predicate {
	private int deliverBy;

	public int getDeliverBy() {
		return deliverBy;
	}

	public void setDeliverBy(int deliverBy) {
		this.deliverBy = deliverBy;
	}
}
