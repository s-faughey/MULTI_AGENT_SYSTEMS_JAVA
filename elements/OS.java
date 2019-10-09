package elements;
import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;

public class OS extends Part{
	private String os;

	@Slot (mandatory = true)
	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}
	
}
