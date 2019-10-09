package elements;
import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class PC implements Concept {
	private RAM ram;
	private HDD hdd;
	private OS os;
	
	public RAM getRam() {
		return ram;
	}
	
	public void setRam(RAM ram) {
		this.ram = ram;
	}
	
	public HDD getHDD() {
		return hdd;
	}
	public void setHDD(HDD hdd) {
		this.hdd = hdd;
	}
	public OS getOS() {
		return os;
	}
	public void setOS(OS os) {
		this.os = os;
	}
}
