// BorrowInfo.java
package seu.vcampus.model;

import java.util.Date;

public class BorrowInfo {
    private String borrowId;
    private String userId;
    private String callNumber;
    private Date borrowDate;
    private Date dueDate;
    private Date returnDate;
    private int renewCount;
    private String status; // 借阅中/已归还/逾期
    private String bookTitle;
    private String author;
    private String publisher;
    
    // 添加getter和setter
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public String getBorrowId() { return borrowId; }
    public void setBorrowId(String borrowId) { this.borrowId = borrowId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getCallNumber() { return callNumber; }
    public void setCallNumber(String callNumber) { this.callNumber = callNumber; }
    
    public Date getBorrowDate() { return borrowDate; }
    public void setBorrowDate(Date borrowDate) { this.borrowDate = borrowDate; }
    
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    
    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }
    
    public int getRenewCount() { return renewCount; }
    public void setRenewCount(int renewCount) { this.renewCount = renewCount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getPublisher() {

		return publisher;
	}
	public void setPublisher(String string) {
		this.publisher=string;
		
	}

}