package seu.vcampus.model;

public class AdminBookRequest {
    private String operationType; // ADD/EDIT/DELETE
    private BookInfo bookInfo;

    // Getter和Setter方法
    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public BookInfo getBookInfo() {
        return bookInfo;
    }

    public void setBookInfo(BookInfo bookInfo) {
        this.bookInfo = bookInfo;
    }
}