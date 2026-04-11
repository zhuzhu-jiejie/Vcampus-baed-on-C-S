package seu.vcampus.model;

public class BookInfoRequest {
    private String searchType;    // 搜索类型（全部/按条件）
    private String keyword;       // 关键词
    private String category;      // 分区筛选
    private String location;      // 馆藏位置筛选
    private String title;         // 书名筛选
    private String author;        // 作者筛选
    private String publisher;     // 出版社筛选

    // Getter和Setter方法
    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    // 新增字段的Getter和Setter
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }
}
