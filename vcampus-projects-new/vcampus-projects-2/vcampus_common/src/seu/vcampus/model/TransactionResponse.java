package seu.vcampus.model;

import java.util.List;

public class TransactionResponse {
    private List<Transaction> transactions; // 交易记录列表

    // Getter和Setter
    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
}