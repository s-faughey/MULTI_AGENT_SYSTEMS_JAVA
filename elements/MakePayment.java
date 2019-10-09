package elements;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;


public class MakePayment implements AgentAction{
	private int amount;
	private AID payer;
	private int id;
	
	@Slot(mandatory = true)
	public int getAmount() {
		return amount;
	}
	public void setAmount(int amount) {
		this.amount = amount;
	}
	@Slot(mandatory = true)
	public AID getPayer() {
		return payer;
	}
	public void setPayer(AID payer) {
		this.payer = payer;
	}
	
	public void setID(int id) {
		this.id = id;
	}
	@Slot(mandatory = true)
	public int getID() {
		return id;
	}
	
	
	
}
