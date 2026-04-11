package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.AdminBookRequest;
import seu.vcampus.model.BookInfo;
import seu.vcampus.model.BookInfoRequest;
public class LibraryManage1Controller implements Initializable {

    // ========== 1. FXML组件注入（与FXML中fx:id完全一致） ==========
    // 查询条件组件
    @FXML private TextField bookIdField;
    @FXML private TextField bookTitleField;
    @FXML private TextField authorField;
    @FXML private TextField publisherField;
    @FXML private TextField publishYearField;
    @FXML private ComboBox<String> locationComboBox;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> statusComboBox;

    // 操作按钮
    @FXML private Button searchButton;
    @FXML private Button resetButton;
    @FXML private Button addBookButton;
    @FXML private Button editBookButton;
    @FXML private Button deleteBookButton;

    // 表格与分页组件
    @FXML private TableView<Book> bookTableView;
    @FXML private TableColumn<Book, String> bookIdColumn;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, String> publisherColumn;
    @FXML private TableColumn<Book, String> publishYearColumn;
    @FXML private TableColumn<Book, String> categoryColumn;
    @FXML private TableColumn<Book, String> locationColumn;
    @FXML private TableColumn<Book, String> statusColumn;
    @FXML private Label resultCountLabel;
    @FXML private Button firstPageButton;
    @FXML private Button prevPageButton;
    @FXML private Label currentPageLabel;
    @FXML private Button nextPageButton;
    @FXML private Button lastPageButton;
    @FXML private ComboBox<Integer> pageSizeComboBox;

    // ========== 2. 核心数据与分页参数 ==========
    // 所有图书数据（从服务器获取）
    private List<Book> allBooks = new ArrayList<>();
    // 筛选后的图书数据（用于分页）
    private List<Book> filteredBooks = new ArrayList<>();
    // 分页参数
    private int currentPage = 1;    // 当前页码（默认第1页）
    private int pageSize = 10;      // 每页条数（默认10条）
    private int totalPages = 0;     // 总页数

    // Gson实例用于JSON解析
    private Gson gson = new Gson();
    

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. 初始化表格列绑定
        initTableColumns();
        // 2. 初始化下拉框选项
        initComboBoxData();
        // 3. 绑定表格行选中监听（控制编辑/删除按钮状态）
        initTableSelectionListener();
        // 4. 绑定按钮点击事件
        bindButtonEvents();
        // 5. 从服务器加载图书数据
        loadBooksFromServer();
    }

    // ========== 3. 初始化辅助方法 ==========
    /**
     * 初始化表格列与Book实体类属性绑定
     */
    private void initTableColumns() {
        bookIdColumn.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        publisherColumn.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        publishYearColumn.setCellValueFactory(new PropertyValueFactory<>("publishYear"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    /**
     * 初始化下拉框选项
     */
    private void initComboBoxData() {
        // 分类选项（包含"全部"）
        ObservableList<String> categoryItems = FXCollections.observableArrayList(
                "全部", "计算机", "文学", "历史", "经济", "心理", "哲学", "法律", "语言", "艺术", "数学", "医学"
        );
        categoryComboBox.setItems(categoryItems);
        categoryComboBox.setValue("全部");  // 默认选中"全部"

        // 位置选项（包含"全部"）
        ObservableList<String> locationItems = FXCollections.observableArrayList(
                "全部", "一楼大厅", "二楼文学区", "三楼科技区", "四楼社会科学区", "五楼特藏区", "六楼外文区"
        );
        locationComboBox.setItems(locationItems);
        locationComboBox.setValue("全部");  // 默认选中"全部"

        // 状态选项（包含"全部"）
        ObservableList<String> statusItems = FXCollections.observableArrayList(
                "全部", "可借", "借出", "维护中"
        );
        statusComboBox.setItems(statusItems);
        statusComboBox.setValue("全部");  // 默认选中"全部"
    }

    /**
     * 初始化表格行选中监听：选中行时启用编辑/删除按钮，未选中则禁用
     */
    private void initTableSelectionListener() {
        bookTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelected = newVal != null;
            editBookButton.setDisable(!hasSelected);
            deleteBookButton.setDisable(!hasSelected);
        });
    }

    /**
     * 绑定所有按钮的点击事件
     */
    private void bindButtonEvents() {
        searchButton.setOnAction(e -> handleSearch());       // 查询
        resetButton.setOnAction(e -> handleReset());         // 重置
        addBookButton.setOnAction(e -> handleAddBook());     // 添加图书
        editBookButton.setOnAction(e -> handleEditBook());   // 编辑图书
        deleteBookButton.setOnAction(e -> handleDeleteBook());// 删除图书
        firstPageButton.setOnAction(e -> goToFirstPage());   // 首页
        prevPageButton.setOnAction(e -> goToPrevPage());     // 上一页
        nextPageButton.setOnAction(e -> goToNextPage());     // 下一页
        lastPageButton.setOnAction(e -> goToLastPage());     // 末页
        // 每页条数变更时刷新分页
        pageSizeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            pageSize = newVal;
            currentPage = 1;  // 条数变更后回到第一页
            refreshPagination();
        });
    }

    /**
     * 从服务器加载图书数据
     */
    private void loadBooksFromServer() {
        new Thread(() -> {
            try {
                SocketManager socketManager = SocketManager.getInstance();
                DataOutputStream out = socketManager.getOut();
                DataInputStream in = socketManager.getIn();

                if (out == null || in == null) {
                    Platform.runLater(() -> showAlert("错误", "网络连接异常，请检查网络设置"));
                    return;
                }

                // 构建请求
                BookInfoRequest request = new BookInfoRequest();
                request.setSearchType("全量查询"); // 获取所有图书

                // 发送请求
                out.writeUTF("BookInfoRequest");
                out.writeUTF(gson.toJson(request));
                out.flush();

                // 接收响应
                String response = in.readUTF();
                if (response.startsWith("SUCCESS|")) {
                    String jsonStr = response.substring(8);
                    Type bookListType = new TypeToken<List<BookInfo>>(){}.getType();
                    List<BookInfo> bookInfoList = gson.fromJson(jsonStr, bookListType);

                    // 转换为本地Book对象
                    allBooks.clear();
                    for (BookInfo info : bookInfoList) {
                        allBooks.add(new Book(
                            info.getCallNumber(),
                            info.getTitle(),
                            info.getAuthor(),
                            info.getPublisher(),
                            info.getPublishYear(),
                            info.getCategory(),
                            info.getLocation(),
                            info.getStatus(),
                            info.getPdfUrl()
                        ));
                    }

                    Platform.runLater(() -> {
                        filteredBooks.clear();
                        filteredBooks.addAll(allBooks);
                        currentPage = 1;
                        refreshPagination();
                        resultCountLabel.setText(String.format("(共找到 %d 本图书)", filteredBooks.size()));
                    });
                } else {
                    Platform.runLater(() -> showAlert("错误", "获取图书数据失败: " + response));
                }
            } catch (IOException e) {
                Platform.runLater(() -> showAlert("错误", "网络通信异常: " + e.getMessage()));
            } catch (JsonSyntaxException e) {
                Platform.runLater(() -> showAlert("错误", "数据解析异常: " + e.getMessage()));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("错误", "获取数据时发生未知错误: " + e.getMessage()));
            }
        }).start();
    }

    // ========== 4. 核心功能：图书增删改查 ==========
    /**
     * 处理图书查询（根据筛选条件过滤数据）
     */
    private void handleSearch() {
        // 1. 获取筛选条件（去空格，不区分大小写）
        String bookId = bookIdField.getText().trim().toLowerCase();
        String title = bookTitleField.getText().trim().toLowerCase();
        String author = authorField.getText().trim().toLowerCase();
        String publisher = publisherField.getText().trim().toLowerCase();
        String publishYear = publishYearField.getText().trim();
        String category = categoryComboBox.getValue();
        String location = locationComboBox.getValue();
        String status = statusComboBox.getValue();
        // 2. 筛选数据
        filteredBooks.clear();
        for (Book book : allBooks) {
            // 图书ID筛选（空则不过滤）
            boolean matchId = bookId.isEmpty() || book.getBookId().toLowerCase().contains(bookId);
            // 书名筛选
            boolean matchTitle = title.isEmpty() || book.getTitle().toLowerCase().contains(title);
            // 作者筛选
            boolean matchAuthor = author.isEmpty() || book.getAuthor().toLowerCase().contains(author);
            // 出版社筛选
            boolean matchPublisher = publisher.isEmpty() || book.getPublisher().toLowerCase().contains(publisher);
            // 出版年份筛选
            boolean matchPublishYear = publishYear.isEmpty() || book.getPublishYear().contains(publishYear);
            // 分类筛选（"全部"则不过滤）
            boolean matchCategory = "全部".equals(category) || book.getCategory().equals(category);
            // 位置筛选（"全部"则不过滤）
            boolean matchLocation = "全部".equals(location) || book.getLocation().equals(location);
            // 状态筛选（"全部"则不过滤）
            boolean matchStatus = "全部".equals(status) || book.getStatus().equals(status);

            // 所有条件都满足则加入筛选结果
            if (matchId && matchTitle && matchAuthor && matchPublisher &&
                matchPublishYear && matchCategory && matchLocation && matchStatus) {
                filteredBooks.add(book);
            }
        }

        // 3. 刷新分页和结果计数
        currentPage = 1;  // 查询后回到第一页
        refreshPagination();
        resultCountLabel.setText(String.format("(共找到 %d 本图书)", filteredBooks.size()));
    }

    /**
     * 处理表单重置（清空所有筛选条件）
     */
    private void handleReset() {
        bookIdField.clear();
        bookTitleField.clear();
        authorField.clear();
        publisherField.clear();
        publishYearField.clear();
        categoryComboBox.setValue("全部");
        locationComboBox.setValue("全部");
        statusComboBox.setValue("全部");

        filteredBooks.clear();
        filteredBooks.addAll(allBooks);
        bookTableView.setItems(FXCollections.observableArrayList(filteredBooks));
        resultCountLabel.setText(String.format("(共找到 %d 本图书)", filteredBooks.size()));
        currentPage = 1;
        refreshPagination();
    }

    /**
     * 处理添加图书（弹出表单对话框）
     */
    private void handleAddBook() {
        // 1. 创建对话框
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle("添加图书");
        dialog.setHeaderText("请输入图书信息");

        // 2. 设置对话框按钮（确认/取消）
        ButtonType confirmBtnType = new ButtonType("确认添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmBtnType, ButtonType.CANCEL);

        // 3. 创建表单布局（GridPane）
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        // 表单控件
        TextField idField = new TextField();
        idField.setPromptText("图书ID（索书号）");
        TextField titleField = new TextField();
        titleField.setPromptText("书名");
        TextField authorField = new TextField();
        authorField.setPromptText("作者");
        TextField publisherField = new TextField();
        publisherField.setPromptText("出版社");
        TextField yearField = new TextField();
        yearField.setPromptText("出版年（如2023）");
        ComboBox<String> categoryBox = new ComboBox<>(FXCollections.observableArrayList(
                "计算机", "文学", "历史", "经济", "心理", "哲学", "法律", "语言", "艺术", "数学", "医学"
        ));
        categoryBox.setPromptText("分类");
        
        // 文件上传控件
        Label fileLabel = new Label("未选择文件");
        Button uploadButton = new Button("选择PDF文件");
        AtomicReference<File> selectedFile = new AtomicReference<>();
        
        uploadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择PDF文件");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );
            File file = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (file != null) {
                selectedFile.set(file);
                fileLabel.setText(file.getName());
            }
        });
        
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList(
                "可借", "借出", "维护中"
        ));
        statusBox.setPromptText("状态");
        ComboBox<String> locationBox = new ComboBox<>(FXCollections.observableArrayList(
                "一楼大厅", "二楼文学区", "三楼科技区", "四楼社会科学区", "五楼特藏区", "六楼外文区"
        ));
        locationBox.setPromptText("位置");

        // 添加控件到网格
        grid.add(new Label("图书ID*:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("书名*:"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(new Label("作者*:"), 0, 2);
        grid.add(authorField, 1, 2);
        grid.add(new Label("出版社:"), 0, 3);
        grid.add(publisherField, 1, 3);
        grid.add(new Label("出版年:"), 0, 4);
        grid.add(yearField, 1, 4);
        grid.add(new Label("分类*:"), 0, 5);
        grid.add(categoryBox, 1, 5);
        grid.add(new Label("PDF文件*:"), 0, 6);
        grid.add(uploadButton, 1, 6);
        grid.add(fileLabel, 2, 6);
        grid.add(new Label("位置*:"), 0, 7);
        grid.add(locationBox, 1, 7);
        grid.add(new Label("状态*:"), 0, 8);
        grid.add(statusBox, 1, 8);

        dialog.getDialogPane().setContent(grid);

        // 4. 对话框结果转换
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmBtnType) {
                try {
                    String bookId = idField.getText().trim();
                    String title = titleField.getText().trim();
                    String author = authorField.getText().trim();
                    String publisher = publisherField.getText().trim();
                    String year = yearField.getText().trim();
                    String category = categoryBox.getValue();
                    String location = locationBox.getValue();
                    String status = statusBox.getValue();
                    File pdfFile = selectedFile.get();

                    // 验证必填
                    if (bookId.isEmpty() || title.isEmpty() || author.isEmpty() ||
                        category == null || location == null || status == null || pdfFile == null) {
                        showAlert("错误", "带*的字段为必填项！");
                        return null;
                    }

                    // 上传文件并获取服务器返回的文件名
                    String serverFileName = uploadFileToServer(pdfFile);
                    if (serverFileName == null) {
                        showAlert("错误", "文件上传失败！");
                        return null;
                    }

                    String completeURL = "http://192.168.107.23:8085/books/" + serverFileName;
                    
                    // 返回新创建的图书对象
                    return new Book(bookId, title, author, publisher, year, category, location, status, completeURL);
                } catch (Exception e) {
                    showAlert("错误", "操作失败: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        // 5. 处理对话框结果
        dialog.showAndWait().ifPresent(newBook -> {
            sendAdminBookRequest("ADD", newBook);
        });
    }

    /**
     * 处理编辑图书（弹出表单对话框，预填选中图书信息）
     */
    private void handleEditBook() {
        // 获取选中的图书
        Book selectedBook = bookTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert("提示", "请先选中要编辑的图书！");
            return;
        }

        // 1. 创建对话框（逻辑与添加类似，预填数据）
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle("编辑图书");
        dialog.setHeaderText("修改图书信息（*为必填项）");

        ButtonType confirmBtnType = new ButtonType("确认修改", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmBtnType, ButtonType.CANCEL);

        // 2. 创建表单并预填选中图书信息
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField idField = new TextField(selectedBook.getBookId());
        idField.setEditable(false);  // 图书ID不可修改
        TextField titleField = new TextField(selectedBook.getTitle());
        TextField authorField = new TextField(selectedBook.getAuthor());
        TextField publisherField = new TextField(selectedBook.getPublisher());
        TextField yearField = new TextField(selectedBook.getPublishYear());
        ComboBox<String> categoryBox = new ComboBox<>(FXCollections.observableArrayList(
                "计算机", "文学", "历史", "经济", "心理", "哲学", "法律", "语言", "艺术", "数学", "医学"
        ));
        categoryBox.setValue(selectedBook.getCategory());
        ComboBox<String> locationBox = new ComboBox<>(FXCollections.observableArrayList(
                "一楼大厅", "二楼文学区", "三楼科技区", "四楼社会科学区", "五楼特藏区", "六楼外文区"
        ));
        locationBox.setValue(selectedBook.getLocation());
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList(
                "可借", "借出", "维护中"
        ));
        statusBox.setValue(selectedBook.getStatus());

        // 添加控件到网格
        grid.add(new Label("图书ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("书名*:"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(new Label("作者*:"), 0, 2);
        grid.add(authorField, 1, 2);
        grid.add(new Label("出版社:"), 0, 3);
        grid.add(publisherField, 1, 3);
        grid.add(new Label("出版年:"), 0, 4);
        grid.add(yearField, 1, 4);
        grid.add(new Label("分类*:"), 0, 5);
        grid.add(categoryBox, 1, 5);
        grid.add(new Label("位置*:"), 0, 6);
        grid.add(locationBox, 1, 6);
        grid.add(new Label("状态*:"), 0, 7);
        grid.add(statusBox, 1, 7);

        dialog.getDialogPane().setContent(grid);

        // 3. 对话框结果转换（确认时返回修改后的图书）
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmBtnType) {
                try {
                    String title = titleField.getText().trim();
                    String author = authorField.getText().trim();
                    String publisher = publisherField.getText().trim();
                    String year = yearField.getText().trim();
                    String category = categoryBox.getValue();
                    String location = locationBox.getValue();
                    String status = statusBox.getValue();

                    // 验证必填项
                    if (title.isEmpty() || author.isEmpty() || category == null ||
                        location == null || status == null) {
                        showAlert("错误", "带*的字段为必填项！");
                        return null;
                    }

                    // 更新选中图书的信息（图书ID不变）
                    selectedBook.setTitle(title);
                    selectedBook.setAuthor(author);
                    selectedBook.setPublisher(publisher);
                    selectedBook.setPublishYear(year);
                    selectedBook.setCategory(category);
                    selectedBook.setLocation(location);
                    selectedBook.setStatus(status);

                    return selectedBook;
                } catch (NumberFormatException e) {
                    showAlert("错误", "库存需输入数字！");
                    return null;
                }
            }
            return null;
        });

        // 4. 处理结果（刷新表格）
        dialog.showAndWait().ifPresent(updatedBook -> {
            // 发送请求到服务器更新图书信息
            sendAdminBookRequest("EDIT", updatedBook);
        });
    }

    /**
     * 处理删除图书（弹出确认对话框）
     */
    private void handleDeleteBook() {
        Book selectedBook = bookTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert("提示", "请先选中要删除的图书！");
            return;
        }

        // 1. 创建确认对话框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除图书《" + selectedBook.getTitle() + "》吗？删除后不可恢复！");

        // 2. 处理确认结果
        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                // 发送请求到服务器删除图书
                sendAdminBookRequest("DELETE", selectedBook);
            }
        });
    }

    /**
     * 发送管理员图书操作请求到服务器
     */
    private void sendAdminBookRequest(String operationType, Book book) {
        new Thread(() -> {
            try {
                SocketManager socketManager = SocketManager.getInstance();
                DataOutputStream out = socketManager.getOut();
                DataInputStream in = socketManager.getIn();

                if (out == null || in == null) {
                    Platform.runLater(() -> showAlert("错误", "网络连接异常，请检查网络设置"));
                    return;
                }

                // 将Book对象转换为BookInfo对象
                BookInfo bookInfo = new BookInfo();
                bookInfo.setCallNumber(book.getBookId());
                bookInfo.setTitle(book.getTitle());
                bookInfo.setAuthor(book.getAuthor());
                bookInfo.setPublisher(book.getPublisher());
                bookInfo.setPublishYear(book.getPublishYear());
                bookInfo.setCategory(book.getCategory());
                bookInfo.setLocation(book.getLocation());
                bookInfo.setStatus(book.getStatus());
                bookInfo.setPdfUrl(book.getURL());
                // 构建请求
                AdminBookRequest request = new AdminBookRequest();
                request.setOperationType(operationType);
                request.setBookInfo(bookInfo);

                // 发送请求
                out.writeUTF("AdminBookRequest");
                out.writeUTF(gson.toJson(request));
                out.flush();

             // 接收响应
                String response = in.readUTF();
                if (response.startsWith("SUCCESS|")) {
                    Platform.runLater(() -> {
                        showAlert("成功", response.substring(8));
                        // 重新加载图书数据
                        loadBooksFromServer();
                    });
                } else {
                    Platform.runLater(() -> {
                        // 更详细的错误信息
                        String errorMsg = response.substring(6);
                        if (errorMsg.contains("Duplicate entry")) {
                            showAlert("错误", "图书ID已存在，请使用不同的ID");
                        } else if (errorMsg.contains("cannot be null")) {
                            showAlert("错误", "必填字段不能为空");
                        } else {
                            showAlert("错误", errorMsg);
                        }
                    });
                }
            } catch (IOException e) {
                Platform.runLater(() -> showAlert("错误", "网络通信异常: " + e.getMessage()));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("错误", "操作图书时发生未知错误: " + e.getMessage()));
            }
        }).start();
    }

    // ========== 5. 分页控制方法 ==========
    /**
     * 刷新分页状态（计算总页数、更新表格数据、更新分页标签）
     */
    private void refreshPagination() {
        // 1. 计算总页数（筛选后的数据为空则总页数为0）
        if (filteredBooks.isEmpty()) {
            totalPages = 0;
            bookTableView.setItems(FXCollections.observableArrayList());
            currentPageLabel.setText("第 0 页 / 共 0 页");
            disableAllPageButtons(true);  // 禁用所有分页按钮
            return;
        }

        // 计算总页数（向上取整）
        totalPages = (int) Math.ceil((double) filteredBooks.size() / pageSize);
        // 确保当前页码不超过总页数
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        // 确保当前页码不小于1
        if (currentPage < 1) {
            currentPage = 1;
        }

        // 2. 计算当前页要显示的数据（起始索引和结束索引）
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, filteredBooks.size());
        List<Book> currentPageBooks = filteredBooks.subList(startIndex, endIndex);

        // 3. 更新表格数据
        bookTableView.setItems(FXCollections.observableArrayList(currentPageBooks));

        // 4. 更新分页标签和按钮状态
        currentPageLabel.setText(String.format("第 %d 页 / 共 %d 页", currentPage, totalPages));
        disablePageButtons();
    }

    /**
     * 禁用/启用分页按钮（根据当前页码和总页数）
     */
    private void disablePageButtons() {
        // 首页和上一页：当前页为1时禁用
        firstPageButton.setDisable(currentPage == 1);
        prevPageButton.setDisable(currentPage == 1);
        // 下一页和末页：当前页为总页数时禁用
        nextPageButton.setDisable(currentPage == totalPages);
        lastPageButton.setDisable(currentPage == totalPages);
    }

    /**
     * 批量禁用所有分页按钮（无数据时）
     */
    private void disableAllPageButtons(boolean disable) {
        firstPageButton.setDisable(disable);
        prevPageButton.setDisable(disable);
        nextPageButton.setDisable(disable);
        lastPageButton.setDisable(disable);
    }

    // 分页跳转方法
    private void goToFirstPage() {
        currentPage = 1;
        refreshPagination();
    }

    private void goToPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            refreshPagination();
        }
    }

    private void goToNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            refreshPagination();
        }
    }

    private void goToLastPage() {
        currentPage = totalPages;
        refreshPagination();
    }
    
    
    /**
     * 上传文件到服务器
     */
    private String uploadFileToServer(File file) {
        try {
            SocketManager socketManager = SocketManager.getInstance();
            DataOutputStream out = socketManager.getOut();
            DataInputStream in = socketManager.getIn();

            // 发送文件上传请求
            out.writeUTF("FileUploadRequest");
            out.writeUTF(file.getName());
            out.writeLong(file.length());
            
            // 发送文件内容
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            fis.close();
            
            // 获取服务器响应
            String response = in.readUTF();
            if (response.startsWith("SUCCESS|")) {
                return response.substring(8); // 返回服务器保存的文件名
            } else {
                return null;
            }
        } catch (IOException e) {
            showAlert("错误", "文件上传失败: " + e.getMessage());
            return null;
        }
    }

    // ========== 6. 辅助工具：弹窗提示 ==========
    /**
     * 显示弹窗提示（信息/错误）
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ========== 7. 图书实体类（与表格列属性对应） ==========
    public static class Book {
        private String bookId;        // 图书ID（索书号）
        private String title;         // 书名
        private String author;        // 作者
        private String publisher;     // 出版社
        private String publishYear;   // 出版年
        private String category;      // 分类
        private String location;      // 位置
        private String status;        // 状态（可借/借出/维护中）
        private String URL;
        // 构造方法
        public Book(String bookId, String title, String author, String publisher,
                   String publishYear, String category, String location, String status,String URL) {
            this.bookId = bookId;
            this.title = title;
            this.author = author;
            this.publisher = publisher;
            this.publishYear = publishYear;
            this.category = category;
            this.location = location;
            this.status = status;
            this.URL=URL;
        }

        // Getter和Setter（表格绑定和编辑功能需要）
        public String getBookId() { return bookId; }
        public void setBookId(String bookId) { this.bookId = bookId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public String getPublisher() { return publisher; }
        public void setPublisher(String publisher) { this.publisher = publisher; }

        public String getPublishYear() { return publishYear; }
        public void setPublishYear(String publishYear) { this.publishYear = publishYear; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getURL() { return URL; }
        public void setURL(String URL) { this.URL = URL; }
    }
}