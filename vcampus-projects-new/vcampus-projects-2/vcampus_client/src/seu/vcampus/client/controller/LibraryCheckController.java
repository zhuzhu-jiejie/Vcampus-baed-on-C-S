package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.BookInfo;
import seu.vcampus.model.BookInfoRequest;

/**
 * 馆藏查询页面控制器
 * 功能：处理查询条件输入、结果过滤、表格展示、分页控制
 * 与 librarycheck.fxml 控件完全绑定，无空指针风险
 */
public class LibraryCheckController implements Initializable {

    // ====================== 1. FXML控件绑定（与FXML的fx:id完全一致）======================
    // 【查询条件区域】控件
    @FXML private TextField bookTitleField;       // 书名输入框
    @FXML private TextField authorField;          // 作者输入框
    @FXML private TextField publisherField;       // 出版社输入框
    @FXML private ComboBox<String> categoryComboBox; // 分区下拉框
    @FXML private ComboBox<String> locationComboBox; // 馆藏位置下拉框
    @FXML private Button searchButton;            // 查询按钮
    @FXML private Button resetButton;             // 重置按钮

    // 【查询结果区域】控件
    @FXML private Label resultCountLabel;         // 结果数量提示（共X条记录）
    @FXML private TableView<Book> resultTable;    // 结果表格
    // 表格列（8列，与FXML中TableColumn的fx:id一一对应）
    @FXML private TableColumn<Book, Void> previewColumn;  // 新增预览按钮列
    @FXML private TableColumn<Book, String> callNumberColumn;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, String> publisherColumn;
    @FXML private TableColumn<Book, String> publishYearColumn;
    @FXML private TableColumn<Book, String> categoryColumn;
    @FXML private TableColumn<Book, String> locationColumn;
    @FXML private TableColumn<Book, String> statusColumn;

    // 【分页控制区域】控件
    @FXML private Button firstPageButton;         // 首页按钮
    @FXML private Button prevPageButton;          // 上一页按钮
    @FXML private Label currentPageLabel;         // 当前页码（第X页/共Y页）
    @FXML private Button nextPageButton;          // 下一页按钮
    @FXML private Button lastPageButton;          // 末页按钮
    @FXML private ComboBox<Integer> pageSizeComboBox; // 每页显示条数下拉框
    // ====================== 2. 数据与分页变量（初始化避免空指针）=====================
    private ObservableList<Book> allBookData = FXCollections.observableArrayList(); // 所有查询结果数据
    private ObservableList<Book> currentPageData = FXCollections.observableArrayList(); // 当前页显示数据
    private int currentPage = 1;                  // 当前页码（默认第1页）
    private int pageSize = 10;                    // 每页显示条数（默认10条）
    private int totalPages = 0;                   // 总页数

    // ====================== 新增: 性能优化变量 ======================
    private ExecutorService pdfLoadingExecutor; // PDF加载线程池
    private long lastPdfOpenTime = 0; // 上次打开PDF的时间(用于防止快速连续点击)
    private static final long MIN_PDF_OPEN_INTERVAL = 1000; // 最小打开PDF间隔(毫秒)

    // ====================== 3. 初始化方法（页面加载时自动执行）======================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTableColumns();
        initComboBoxData();
        initPagination();
        bindButtonEvents();
        initPerformanceOptimizations(); // 初始化性能优化
        loadBookDataFromServer(); // 初始化时从服务器加载全量数据
    }

    // ====================== 新增: 性能优化初始化 ======================
    private void initPerformanceOptimizations() {
        // 创建专用线程池处理PDF加载
        pdfLoadingExecutor = Executors.newFixedThreadPool(2);

        System.out.println("性能优化已初始化: 线程池已启用");
    }

    // ====================== 4. 核心功能方法实现 ======================

    /**
     * 1. 初始化表格列：添加预览按钮列
     */
    private void initTableColumns() {
        // 预览按钮列
        previewColumn.setCellFactory(new Callback<TableColumn<Book, Void>, TableCell<Book, Void>>() {
            @Override
            public TableCell<Book, Void> call(TableColumn<Book, Void> param) {
                return new TableCell<>() {
                    private final Button previewButton = new Button("预览");

                    {
                        previewButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                        previewButton.setOnAction(event -> {
                            Book book = getTableView().getItems().get(getIndex());
                            if (book != null && book.getPdfUrl() != null && !book.getPdfUrl().isEmpty()) {
                                openPdfInBrowser(book.getPdfUrl());
                            } else {
                                showAlert("提示", "该图书暂无预览资源");
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(previewButton);
                        }
                    }
                };
            }
        });

        titleColumn.setCellValueFactory(cellData -> cellData.getValue().titleProperty());
        authorColumn.setCellValueFactory(cellData -> cellData.getValue().authorProperty());
        publisherColumn.setCellValueFactory(cellData -> cellData.getValue().publisherProperty());
        publishYearColumn.setCellValueFactory(cellData -> cellData.getValue().publishYearProperty());
        categoryColumn.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
        locationColumn.setCellValueFactory(cellData -> cellData.getValue().locationProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        callNumberColumn.setCellValueFactory(cellData -> cellData.getValue().callNumberProperty());

        // 状态列样式优化（"可借"绿色，"借出"红色）
        statusColumn.setCellFactory(column -> new TableCell<Book, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("可借".equals(item)) {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                    } else if ("借出".equals(item)) {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #6c757d;");
                    }
                }
            }
        });
    }

    /**
     * 在浏览器中打开PDF - 使用系统默认浏览器
     */
    private void openPdfInBrowser(String pdfUrl) {
        // 防止快速连续点击
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPdfOpenTime < MIN_PDF_OPEN_INTERVAL) {
            System.out.println("操作过于频繁，请稍后再试");
            return;
        }
        lastPdfOpenTime = currentTime;

        // 使用线程池异步打开PDF
        pdfLoadingExecutor.submit(() -> {
            try {
                // 检查URL格式
                String finalPdfUrl;
                if (!pdfUrl.startsWith("http://") && !pdfUrl.startsWith("https://")) {
                    finalPdfUrl = "http://10.203.147.229:8088/books/" + pdfUrl;
                } else {
                    finalPdfUrl = pdfUrl;
                }

                System.out.println("Opening PDF in browser: " + finalPdfUrl);

                // 使用Desktop类打开系统默认浏览器
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                    if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                        desktop.browse(new URI(finalPdfUrl));
                    } else {
                        Platform.runLater(() -> {
                            showAlert("错误", "您的系统不支持浏览器打开操作");
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        showAlert("错误", "您的系统不支持桌面操作");
                    });
                }

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("错误", "无法打开PDF文件: " + e.getMessage());
                });
                e.printStackTrace();
            }
        });
    }

    /**
     * 2. 初始化下拉框数据：加载分区、馆藏位置、每页条数选项
     */
    private void initComboBoxData() {
        // 分区下拉框
        ObservableList<String> categoryOptions = FXCollections.observableArrayList(
                "全部", "文学", "科技", "历史", "哲学", "经济", "法律", "计算机", "医学", "艺术"
        );
        categoryComboBox.setItems(categoryOptions);
        categoryComboBox.getSelectionModel().selectFirst();

        // 馆藏位置下拉框
        ObservableList<String> locationOptions = FXCollections.observableArrayList(
                "全部", "一楼大厅", "二楼文学区", "三楼科技区", "四楼社会科学区", "五楼特藏区", "六楼外文区"
        );
        locationComboBox.setItems(locationOptions);
        locationComboBox.getSelectionModel().selectFirst();

        // 每页显示条数下拉框
        if (pageSizeComboBox.getItems().isEmpty()) {
            ObservableList<Integer> pageSizeOptions = FXCollections.observableArrayList(10, 20, 30, 50);
            pageSizeComboBox.setItems(pageSizeOptions);
        }
        pageSizeComboBox.getSelectionModel().select(Integer.valueOf(pageSize));
        pageSize = pageSizeComboBox.getValue();
    }

    /**
     * 3. 初始化分页控件：设置初始状态（禁用无效按钮）
     */
    private void initPagination() {
        updatePageLabel();
        firstPageButton.setDisable(true);
        prevPageButton.setDisable(true);
        nextPageButton.setDisable(true);
        lastPageButton.setDisable(true);
    }

    /**
     * 4. 绑定按钮事件：处理所有用户交互逻辑
     * 核心逻辑：查询按钮优先本地过滤（已加载服务器数据时），重置按钮重新请求服务器全量数据
     */
    private void bindButtonEvents() {
        // 4.1 查询按钮：本地条件过滤（基于已加载的服务器数据）
        searchButton.setOnAction(e -> {
            currentPage = 1; // 过滤后重置为第1页
            filterBookDataByCondition(); // 本地过滤
            updateCurrentPageData(); // 更新当前页数据
            updateResultCount(); // 更新结果计数
            updatePageLabel(); // 更新页码
            updatePaginationButtonStatus(); // 更新按钮状态
        });

        // 4.2 重置按钮：清空条件+重新请求服务器全量数据
        resetButton.setOnAction(e -> {
            // 清空输入框
            bookTitleField.clear();
            authorField.clear();
            publisherField.clear();
            // 恢复下拉框默认值
            categoryComboBox.getSelectionModel().selectFirst();
            locationComboBox.getSelectionModel().selectFirst();
            pageSizeComboBox.getSelectionModel().select(Integer.valueOf(10));
            pageSize = 10;
            // 重新请求服务器数据
            currentPage = 1;
            loadBookDataFromServer();
        });

        // 4.3 分页按钮逻辑（首页/上一页/下一页/末页）
        firstPageButton.setOnAction(e -> {
            if (currentPage != 1) {
                currentPage = 1;
                updateCurrentPageData();
                updatePageLabel();
                updatePaginationButtonStatus();
            }
        });

        prevPageButton.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                updateCurrentPageData();
                updatePageLabel();
                updatePaginationButtonStatus();
            }
        });

        nextPageButton.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                updateCurrentPageData();
                updatePageLabel();
                updatePaginationButtonStatus();
            }
        });

        lastPageButton.setOnAction(e -> {
            if (currentPage != totalPages) {
                currentPage = totalPages;
                updateCurrentPageData();
                updatePageLabel();
                updatePaginationButtonStatus();
            }
        });

        // 4.4 每页条数变更：重置页码并更新数据
        pageSizeComboBox.setOnAction(e -> {
            pageSize = pageSizeComboBox.getValue();
            currentPage = 1;
            updateCurrentPageData();
            updatePageLabel();
            updatePaginationButtonStatus();
        });
    }

    /**
     * 5. 核心方法1：从服务器加载图书数据（支持全量/条件请求）
     * 逻辑：新线程执行网络请求 → 构建请求参数 → 发送请求 → 解析响应 → UI线程更新数据
     */
    private void loadBookDataFromServer() {
        // 开启新线程（避免阻塞JavaFX UI线程）
        new Thread(() -> {
            DataOutputStream out = null;
            DataInputStream in = null;
            try {
                // 1. 获取Socket连接（确保SocketManager已初始化）
                SocketManager socketManager = SocketManager.getInstance();
                out = socketManager.getOut();
                in = socketManager.getIn();
                if (out == null || in == null) {
                    throw new IOException("Socket连接未初始化，请先登录");
                }

                // 2. 构建请求参数（携带当前查询条件，支持"全量查询"和"条件查询"）
                BookInfoRequest req = new BookInfoRequest();
                // 若所有条件为空，按"全量查询"；否则按"条件查询"
                boolean isFullQuery = bookTitleField.getText().trim().isEmpty()
                        && authorField.getText().trim().isEmpty()
                        && publisherField.getText().trim().isEmpty()
                        && "全部".equals(categoryComboBox.getValue())
                        && "全部".equals(locationComboBox.getValue());
                req.setSearchType(isFullQuery ? "全量查询" : "条件查询");
                req.setTitle(bookTitleField.getText().trim()); // 书名条件
                req.setAuthor(authorField.getText().trim()); // 作者条件
                req.setPublisher(publisherField.getText().trim()); // 出版社条件
                req.setCategory(categoryComboBox.getValue()); // 分区条件
                req.setLocation(locationComboBox.getValue()); // 馆藏位置条件
                req.setKeyword(""); // 预留关键词字段（若服务器支持关键词搜索可扩展）

                // 3. 发送请求到服务器（请求类型+JSON参数）
                out.writeUTF("BookInfoRequest"); // 固定请求类型标识
                out.writeUTF(new Gson().toJson(req)); // 请求参数JSON化
                out.flush(); // 强制刷新流，确保数据立即发送

                // 4. 接收服务器响应并解析
                String response = in.readUTF();
                if (response.startsWith("SUCCESS|")) {
                    // 解析成功响应（截取"SUCCESS|"后的JSON数据）
                    String jsonStr = response.substring(8);
                    Type bookListType = new TypeToken<List<BookInfo>>() {}.getType();
                    List<BookInfo> serverBookList = new Gson().fromJson(jsonStr, bookListType);

                 // 在 LibraryCheckController.java 的 loadBookDataFromServer 方法中修改
                 // 5. UI线程更新数据（JavaFX UI操作必须在应用线程）
                 Platform.runLater(() -> {
                     allBookData.clear(); // 清空旧数据

                     // 确保serverBookList不为null
                     if (serverBookList != null) {
                         // 转换Server的BookInfo为本地Book实体
                         for (BookInfo info : serverBookList) {
                             allBookData.add(new Book(
                                     info.getCallNumber(),
                                     info.getTitle(),
                                     info.getAuthor(),
                                     info.getPublisher(),
                                     info.getPublishYear(),
                                     info.getCategory(),
                                     info.getLocation(),
                                     info.getStatus(),
                                     info.getPdfUrl() // 新增：传递PDF URL
                             ));
                         }
                     } else {
                         System.out.println("[Info] 服务器返回的图书列表为空");
                     }

                     // 更新分页和UI显示
                     updateCurrentPageData();
                     updateResultCount();
                     updatePageLabel();
                     updatePaginationButtonStatus();
                 });
                } else if (response.startsWith("ERROR|")) {
                    // 解析错误响应（截取"ERROR|"后的错误信息）
                    String errorMsg = response.substring(6);
                    Platform.runLater(() -> showAlert("服务器错误", "获取数据失败：" + errorMsg));
                } else {
                    // 未知响应格式
                    Platform.runLater(() -> showAlert("格式错误", "服务器返回无效响应：" + response));
                }

            } catch (JsonSyntaxException e) {
                // JSON解析异常（服务器返回格式错误）
                Platform.runLater(() -> showAlert("解析错误", "数据格式异常：" + e.getMessage()));
            } catch (IOException e) {
                // 网络异常（连接超时、断开等）
                Platform.runLater(() -> showAlert("网络错误", "连接服务器失败：" + e.getMessage()));
            } catch (Exception e) {
                // 其他未知异常
                Platform.runLater(() -> showAlert("系统异常", "加载数据时出错：" + e.getMessage()));
            } finally {
                // 无需关闭流（SocketManager统一管理连接）
            }
        }).start();
    }

    /**
     * 6. 核心方法2：本地条件过滤（基于已加载的allBookData）
     * 逻辑：多条件组合过滤 → 不区分大小写 → 关键词包含匹配 → 分类/位置精确匹配（"全部"除外）
     */
    private void filterBookDataByCondition() {
        // 1. 获取并处理查询条件（去空格+转小写，便于不区分大小写匹配）
        String titleCond = bookTitleField.getText().trim().toLowerCase();
        String authorCond = authorField.getText().trim().toLowerCase();
        String publisherCond = publisherField.getText().trim().toLowerCase();
        String categoryCond = categoryComboBox.getValue();
        String locationCond = locationComboBox.getValue();

        // 2. 空数据防护（避免allBookData为空导致遍历异常）
        if (allBookData == null || allBookData.isEmpty()) {
            showAlert("提示", "暂无数据可过滤，请先加载服务器数据");
            return;
        }

        // 3. 遍历所有数据，筛选符合条件的书籍
        ObservableList<Book> filteredData = FXCollections.observableArrayList();
        for (Book book : allBookData) {
            boolean isMatch = true; // 标记是否符合所有条件

            // 3.1 书名过滤：条件非空时，判断书名是否包含关键词（不区分大小写）
            if (!titleCond.isEmpty()) {
                if (!book.getTitle().toLowerCase().contains(titleCond)) {
                    isMatch = false;
                }
            }

            // 3.2 作者过滤：条件非空时，判断作者是否包含关键词（不区分大小写）
            if (!authorCond.isEmpty() && isMatch) {
                if (!book.getAuthor().toLowerCase().contains(authorCond)) {
                    isMatch = false;
                }
            }

            // 3.3 出版社过滤：条件非空时，判断出版社是否包含关键词（不区分大小写）
            if (!publisherCond.isEmpty() && isMatch) {
                if (!book.getPublisher().toLowerCase().contains(publisherCond)) {
                    isMatch = false;
                }
            }

            // 3.4 分区过滤：条件非"全部"时，精确匹配分区
            if (!"全部".equals(categoryCond) && isMatch) {
                if (!book.getCategory().equals(categoryCond)) {
                    isMatch = false;
                }
            }

            // 3.5 馆藏位置过滤：条件非"全部"时，精确匹配位置
            if (!"全部".equals(locationCond) && isMatch) {
                if (!book.getLocation().equals(locationCond)) {
                    isMatch = false;
                }
            }

            // 3.6 符合所有条件则加入过滤结果
            if (isMatch) {
                filteredData.add(book);
            }
        }

        // 4. 更新数据并提示结果
        allBookData = filteredData;
        if (filteredData.isEmpty()) {
            showAlert("提示", "未找到符合条件的书籍");
        } else {
            showAlert("提示", "过滤成功，共找到" + filteredData.size() + "条符合条件的记录");
        }
    }

    /**
     * 7. 更新当前页数据：根据页码和每页条数截取数据（避免越界）
     */
    private void updateCurrentPageData() {
        currentPageData.clear(); // 清空旧数据
        if (allBookData.isEmpty()) {
            totalPages = 0;
        } else {
            // 计算总页数（向上取整：总数据量+每页条数-1 再除以每页条数）
            totalPages = (allBookData.size() + pageSize - 1) / pageSize;
            // 确保当前页在有效范围（避免页码越界）
            currentPage = Math.max(1, Math.min(currentPage, totalPages));
            // 计算截取范围（左闭右开区间，避免数组越界）
            int startIdx = (currentPage - 1) * pageSize;
            int endIdx = Math.min(startIdx + pageSize, allBookData.size());
            // 截取当前页数据
            currentPageData.addAll(allBookData.subList(startIdx, endIdx));
        }
        // 设置表格数据
        resultTable.setItems(currentPageData);
    }

    /**
     * 8. 更新结果计数标签：显示"共X条记录"
     */
    private void updateResultCount() {
        resultCountLabel.setText(String.format("(共找到 %d 条记录)", allBookData.size()));
    }

    /**
     * 9. 更新页码标签：显示"第X页 / 共Y页"
     */
    private void updatePageLabel() {
        currentPageLabel.setText(String.format("第 %d 页 / 共 %d 页", currentPage, totalPages));
    }

    /**
     * 10. 更新分页按钮状态：根据当前页和总页数启用/禁用按钮（避免无效点击）
     */
    private void updatePaginationButtonStatus() {
        boolean hasMultiPages = totalPages > 1; // 是否有多页数据
        // 首页/上一页：只有多页且当前页>1时启用
        firstPageButton.setDisable(!hasMultiPages || currentPage <= 1);
        prevPageButton.setDisable(!hasMultiPages || currentPage <= 1);
        // 下一页/末页：只有多页且当前页<总页数时启用
        nextPageButton.setDisable(!hasMultiPages || currentPage >= totalPages);
        lastPageButton.setDisable(!hasMultiPages || currentPage >= totalPages);
    }

    /**
     * 11. 统一弹窗工具：避免重复代码，规范弹窗样式
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // 隐藏头部文本
        alert.setContentText(content);
        alert.setResizable(false); // 禁止调整弹窗大小
        alert.showAndWait(); // 阻塞等待用户关闭
    }

    /**
     * 12. 新增: 性能优化方法 - 清理资源
     */
    public void cleanup() {
        if (pdfLoadingExecutor != null && !pdfLoadingExecutor.isShutdown()) {
            pdfLoadingExecutor.shutdown();
        }
    }


    // ====================== 5. 书籍实体类（表格数据源，支持数据绑定）======================
    public static class Book {
        private final SimpleStringProperty callNumber;  // 索书号
        private final SimpleStringProperty title;       // 书名
        private final SimpleStringProperty author;      // 作者
        private final SimpleStringProperty publisher;   // 出版社
        private final SimpleStringProperty publishYear; // 出版年
        private final SimpleStringProperty category;    // 分区
        private final SimpleStringProperty location;    // 馆藏位置
        private final SimpleStringProperty status;      // 状态（可借/借出）
        private final SimpleStringProperty pdfUrl;      // 新增：PDF URL

     // 构造方法
        public Book(String callNumber, String title, String author, String publisher,
                   String publishYear, String category, String location, String status, String pdfUrl) {
            this.callNumber = new SimpleStringProperty(callNumber);
            this.title = new SimpleStringProperty(title);
            this.author = new SimpleStringProperty(author);
            this.publisher = new SimpleStringProperty(publisher);
            this.publishYear = new SimpleStringProperty(publishYear);
            this.category = new SimpleStringProperty(category);
            this.location = new SimpleStringProperty(location);
            this.status = new SimpleStringProperty(status);
            this.pdfUrl = new SimpleStringProperty(pdfUrl);
        }

        // 普通Getters（获取字符串值）
        public String getPdfUrl() { return pdfUrl.get(); }
        public SimpleStringProperty pdfUrlProperty() { return pdfUrl; }
        public String getCallNumber() { return callNumber.get(); }
        public String getTitle() { return title.get(); }
        public String getAuthor() { return author.get(); }
        public String getPublisher() { return publisher.get(); }
        public String getPublishYear() { return publishYear.get(); }
        public String getCategory() { return category.get(); }
        public String getLocation() { return location.get(); }
        public String getStatus() { return status.get(); }

        // Property Getters（用于表格绑定，支持UI联动）
        public SimpleStringProperty callNumberProperty() { return callNumber; }
        public SimpleStringProperty titleProperty() { return title; }
        public SimpleStringProperty authorProperty() { return author; }
        public SimpleStringProperty publisherProperty() { return publisher; }
        public SimpleStringProperty publishYearProperty() { return publishYear; }
        public SimpleStringProperty categoryProperty() { return category; }
        public SimpleStringProperty locationProperty() { return location; }
        public SimpleStringProperty statusProperty() { return status; }
    }
}