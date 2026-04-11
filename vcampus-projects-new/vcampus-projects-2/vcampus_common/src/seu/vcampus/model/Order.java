package seu.vcampus.model;


import java.util.ArrayList;
import java.util.List;
import seu.vcampus.model.OrderItem;

public class Order {
    private String id;
    private String date;
    private String status;
    private double total;
    private List<OrderItem> items;
    private String userid;
    
    public Order(String id, String date, String status, double total) {
        this.id = id;
        this.date = date;
        this.status = status;
        this.total = total;
        this.items = new ArrayList<>();
    }
    
    public void addItem(OrderItem item) {
        items.add(item);
    }
    
    // Getter方法
    public String getId() { return id; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
    public double getTotal() { return total; }
    public List<OrderItem> getItems() { return items; }
    
    // Setter方法
    public void setId(String id) { this.id = id; }
    public void setDate(String date) { this.date = date; }
    public void setStatus(String status) { this.status = status; }
    public void setTotal(double total) { this.total = total; }


	public void setUserId(String userid) {
		this.userid=userid;
	}

	public String getUserId() {
		return userid;
	}

}