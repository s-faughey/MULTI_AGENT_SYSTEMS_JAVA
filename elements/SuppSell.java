package elements;

import jade.core.AID;

public class SuppSell extends Sell {
	private AID supplier;
	public AID getSupplier() {
		return supplier;
	}
	public void setSupplier(AID supplier) {
		this.supplier = supplier;
	}
}
