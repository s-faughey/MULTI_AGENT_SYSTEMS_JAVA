package elements;
import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;

public class Laptop extends PC {
	private LScreen screen;
	private LaptopCPU cpu;
	private LaptopMotherboard motherboard;

	@Slot (mandatory = true)
	public LScreen getScreen() {
		return screen;
	}

	public void setScreen(LScreen screen) {
		this.screen = screen;
	}

	@Slot(mandatory = true)
	public LaptopCPU getCpu() {
		return cpu;
	}

	public void setCpu(LaptopCPU cpu) {
		this.cpu = cpu;
	}

	@Slot(mandatory = true)
	public LaptopMotherboard getMotherboard() {
		return motherboard;
	}

	public void setMotherboard(LaptopMotherboard motherboard) {
		this.motherboard = motherboard;
	}
}
