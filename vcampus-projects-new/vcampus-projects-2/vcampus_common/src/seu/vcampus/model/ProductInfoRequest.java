package seu.vcampus.model;

public class ProductInfoRequest {
    private String category;
    private String keyword;
    
    public ProductInfoRequest() {}
    
    public ProductInfoRequest(String category, String keyword) {
        this.category = category;
        this.keyword = keyword;
    }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}