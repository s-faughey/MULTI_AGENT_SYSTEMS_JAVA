package elements;
import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;

public class HDD extends Part {
	private int size;

	@Slot (mandatory = true)
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

}
