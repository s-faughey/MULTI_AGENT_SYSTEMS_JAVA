package elements;

import java.util.Date;
import java.util.List;

import jade.content.Concept;
import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

public class Order implements Concept {
	private List<Part> parts;
	private int orderNum;
	private boolean assembled;
	private boolean delivered;
	private int quantity;
	private int deliveryDate;
	private int price;
	private int totalCost;
	private AID customer;
	private int id;
	private int orderedOn;
	
	@AggregateSlot(cardMin = 5, cardMax = 6)
	public List<Part> getParts() {
		return parts;
	}
	public void setParts(List<Part> parts) {
		this.parts = parts;
	}
	
	@Slot(mandatory = true)
	public int getOrderNum() {
		return orderNum;
	}
	public void setOrderNum(int orderNum) {
		this.orderNum = orderNum;
	}
	
	@Slot(mandatory = true)
	public boolean isAssembled() {
		return assembled;
	}
	public void setAssembled(boolean assembled) {
		this.assembled = assembled;
	}
	
	@Slot(mandatory = true)
	public boolean isDelivered() {
		return delivered;
	}
	public void setDelivered(boolean delivered) {
		this.delivered = delivered;
	}
	@Slot(mandatory = true)
	public int getQuantity() {
		return quantity;
	}
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	@Slot (mandatory = true)
	public int getDeliveryDate() {
		return deliveryDate;
	}
	public void setDeliveryDate(int date) {
		this.deliveryDate = date;
	}
	@Slot(mandatory = true)
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	@Slot(mandatory = true)
	public AID getCustomer() {
		return customer;
	}
	public void setCustomer(AID customer) {
		this.customer = customer;
	}
	public int getTotalCost() {
		return totalCost;
	}
	public void setTotalCost(int totalCost) {
		this.totalCost = totalCost;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	@Slot(mandatory = true)
	public int getOrderedOn() {
		return orderedOn;
	}
	public void setOrderedOn(int orderedOn) {
		this.orderedOn = orderedOn;
	}
	
}
