package seu.vcampus.model;

public class Transaction {
    private String id;
    private String date;
    private String category;
    private double amount;
    private String type; // income 或 expense
    private String description;
    private String status; // success, pending, failed
    
    public Transaction(String id, String date, String category, double amount, 
                      String type, String description, String status) {
        this.id = id;
        this.date = date;
        this.category = category;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.status = status;
    }
    
    // Getter方法
    public String getId() { return id; }
    public String getDate() { return date; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    
    // Setter方法
    public void setId(String id) { this.id = id; }
    public void setDate(String date) { this.date = date; }
    public void setCategory(String category) { this.category = category; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setType(String type) { this.type = type; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
}