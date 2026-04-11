package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.BookInfo;
import seu.vcampus.model.BookInfoRequest;
import seu.vcampus.model.BorrowRequest;

public class LibraryBorrowController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button clearButton;
    @FXML private Label resultCountLabel;
    @FXML private VBox booksContainer;
    @FXML private Label statusLabel;

    private List<BookInfo> allBooks = new ArrayList<>();
    private String userid; // 当前登录用户ID

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 绑定按钮事件
        searchButton.setOnAction(e -> handleSearch());
        clearButton.setOnAction(e -> handleClear());

        // 初始加载所有可借图书
        loadAllAvailableBooks();
    }

    // 加载所有可借图书
 // 在 LibraryBorrowController.java 的 loadAllAvailableBooks 方法中修改
    private void loadAllAvailableBooks() {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 构建图书查询请求
                BookInfoRequest req = new BookInfoRequest();
                req.setSearchType("全量查询");
                out.writeUTF("BookInfoRequest");
                out.writeUTF(new Gson().toJson(req));
                out.flush();

                // 接收响应
                String response = in.readUTF();
                System.out.println("图书查询响应: " + response);

                if (response.startsWith("SUCCESS|")) {
                    String jsonStr = response.substring(8);
                    System.out.println("图书查询JSON数据: '" + jsonStr + "'");

                    // 确保jsonStr不是空字符串
                    if (jsonStr.trim().isEmpty()) {
                        jsonStr = "[]"; // 如果为空，设置为空数组
                    }

                    List<BookInfo> bookList = new Gson().fromJson(jsonStr,
                        new TypeToken<List<BookInfo>>(){}.getType());

                    Platform.runLater(() -> {
                        allBooks.clear();
                        if (bookList != null) {
                            allBooks.addAll(bookList);
                        }
                        displayBooks(allBooks);
                        resultCountLabel.setText("找到 " + allBooks.size() + " 本图书");
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("错误", "获取图书数据失败: " + response.substring(6));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("错误", "网络连接失败: " + e.getMessage());
                });
            }
        }).start();
    }

    // 搜索图书
    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            displayBooks(allBooks);
            resultCountLabel.setText("找到 " + allBooks.size() + " 本图书");
            return;
        }

        List<BookInfo> matchedBooks = new ArrayList<>();
        for (BookInfo book : allBooks) {
            if (book.getTitle().toLowerCase().contains(keyword) ||
                book.getAuthor().toLowerCase().contains(keyword)) {
                matchedBooks.add(book);
            }
        }

        displayBooks(matchedBooks);
        resultCountLabel.setText("找到 " + matchedBooks.size() + " 本相关书籍");
    }

    // 清空搜索
    @FXML
    private void handleClear() {
        searchField.clear();
        displayBooks(allBooks);
        resultCountLabel.setText("找到 " + allBooks.size() + " 本图书");
    }

    // 显示图书列表
    private void displayBooks(List<BookInfo> books) {
        booksContainer.getChildren().clear();

        for (BookInfo book : books) {
            HBox bookCard = new HBox(20);
            bookCard.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-radius: 5; -fx-background-radius: 5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

            // 书名
            Label titleLabel = new Label("书名：" + book.getTitle());
            titleLabel.setFont(new Font(14));
            titleLabel.setStyle("-fx-font-weight: bold; -fx-pref-width: 200px;");

            // 作者
            Label authorLabel = new Label("作者：" + book.getAuthor());
            authorLabel.setStyle("-fx-pref-width: 150px;");

            // 出版社
            Label publisherLabel = new Label("出版社：" + book.getPublisher());
            publisherLabel.setStyle("-fx-pref-width: 180px;");

            // 状态
            Label statusLabel = new Label("状态：" + book.getStatus());
            statusLabel.setStyle("-fx-pref-width: 80px;");
            if ("可借".equals(book.getStatus())) {
                statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-pref-width: 80px;");
            } else {
                statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-pref-width: 80px;");
            }

            // 借书按钮
            Button borrowBtn = new Button("借书");
            borrowBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-pref-width: 80px;");
            borrowBtn.setDisable(!"可借".equals(book.getStatus()));  // 不可借时禁用

            // 绑定借书事件
            borrowBtn.setOnAction(e -> handleBorrowBook(book));

            // 将组件添加到卡片
            bookCard.getChildren().addAll(titleLabel, authorLabel, publisherLabel, statusLabel, borrowBtn);
            booksContainer.getChildren().add(bookCard);
        }
    }

    // 处理借书请求
    private void handleBorrowBook(BookInfo book) {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 构建借阅请求
                BorrowRequest req = new BorrowRequest();
                req.setUserId(userid);
                req.setCallNumber(book.getCallNumber());
                req.setOperationType("BORROW");

                out.writeUTF("BorrowRequest");
                out.writeUTF(new Gson().toJson(req));
                out.flush();

                // 接收响应
                String response = in.readUTF();
                if (response.startsWith("SUCCESS|")) {
                    Platform.runLater(() -> {
                        // 更新本地图书状态
                        book.setStatus("借出");

                        // 显示借书成功信息
                        LocalDate returnDate = LocalDate.now().plusDays(30);
                        statusLabel.setText("《" + book.getTitle() + "》借阅成功，应还日期：" +
                                           returnDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

                        // 刷新显示
                        handleSearch();
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("借阅失败", response.substring(6));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("错误", "借阅请求失败: " + e.getMessage());
                });
            }
        }).start();
    }

    // 显示提示框
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

	public void setUserId(String studentid) {
		this.userid=studentid;

	}
}