package elements;

import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;

public class Desktop extends PC {
	private DesktopCPU cpu;
	private DesktopMotherboard motherboard;
	public DesktopCPU getCpu() {
		return cpu;
	}
	public void setCpu(DesktopCPU cpu) {
		this.cpu = cpu;
	}
	public DesktopMotherboard getMotherboard() {
		return motherboard;
	}
	public void setMotherboard(DesktopMotherboard motherboard) {
		this.motherboard = motherboard;
	}
}
