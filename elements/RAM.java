package elements;
import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;

public class RAM extends Part {
	private int memory;
	
	@Slot(mandatory = true)
	public int getMemory() {
		return memory;
	}
	
	public void setMemory(int memory) {
		this.memory = memory;
	}
}
