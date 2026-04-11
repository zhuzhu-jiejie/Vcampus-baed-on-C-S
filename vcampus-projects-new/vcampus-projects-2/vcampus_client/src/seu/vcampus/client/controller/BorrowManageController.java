package seu.vcampus.client.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
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
import javafx.scene.control.Tooltip;
import javafx.util.Callback;
import seu.vcampus.client.network.SocketManager;
import seu.vcampus.model.BorrowInfo;
import seu.vcampus.model.BorrowInfoRequest;
import seu.vcampus.model.BorrowRequest;

/**
 * 借阅管理页面控制器（修复模块化反射权限问题）
 * 核心变更：用显式绑定替代 PropertyValueFactory，避免反射访问
 */
public class BorrowManageController implements Initializable {

    // ====================== 1. FXML控件绑定（与FXML的fx:id完全一致）======================
    @FXML private Label totalBorrowLabel;     // 总借阅数
    @FXML private Label expiringSoonLabel;   // 即将到期数（3天内）
    @FXML private Label overdueLabel;        // 已逾期数
    @FXML private TableView<BorrowRecord> borrowTable; // 借阅记录表格
    // 表格列（无索书号列）
    @FXML private TableColumn<BorrowRecord, String> titleColumn;
    @FXML private TableColumn<BorrowRecord, String> authorColumn;
    @FXML private TableColumn<BorrowRecord, String> publisherColumn;
    @FXML private TableColumn<BorrowRecord, String> borrowDateColumn;
    @FXML private TableColumn<BorrowRecord, String> dueDateColumn;
    @FXML private TableColumn<BorrowRecord, String> statusColumn;
    @FXML private TableColumn<BorrowRecord, String> renewCountColumn; // 续借次数列
    @FXML private TableColumn<BorrowRecord, Void> renewColumn; // 续借操作列
    // 分页控件
    @FXML private Button firstPageBtn;       // 首页按钮
    @FXML private Button prevPageBtn;        // 上一页按钮
    @FXML private Label pageInfoLabel;       // 页码信息
    @FXML private Button nextPageBtn;        // 下一页按钮
    @FXML private Button lastPageBtn;        // 末页按钮
    @FXML private ComboBox<Integer> pageSizeCombo; // 每页显示条数

    // ====================== 2. 数据与分页变量 ======================
    private ObservableList<BorrowRecord> allBorrowRecords; // 所有借阅记录
    private ObservableList<BorrowRecord> currentPageRecords; // 当前页记录
    private int currentPage = 1;              // 当前页码（默认第1页）
    private int pageSize = 10;                // 每页显示条数（默认10条）
    private int totalPages = 0;               // 总页数
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // 日期格式化
    private final int RENEW_DAYS = 30;        // 续借延长天数（默认30天）
    private final int EXPIRING_SOON_DAYS = 3; // 即将到期阈值（3天内）
    private String userid=null;

    // ====================== 3. 初始化方法 =======================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //初始化不调用，在setid后初始化
    }

    void setUserId(String userid) {
    	this.userid=userid;
        allBorrowRecords = FXCollections.observableArrayList();
        // 给pageSizeCombo设置默认值（避免后续getUser()返回null）
        pageSizeCombo.setValue(10);
        pageSize = 10; // 同步默认页大小

        initTableColumns();          // 初始化表格列（不依赖数据）
        loadBorrowDataFromServer();  // 加载数据（异步，后续会更新统计和分页）
        bindPaginationEvents();      // 绑定分页事件（不依赖数据）
    }

    // ====================== 4. 核心功能修改：显式绑定表格列 =======================
    /**
     * 初始化表格列：用显式Callback绑定（替代PropertyValueFactory，无反射）
     */
    private void initTableColumns() {
        // 1. 书名列：显式调用 titleProperty()
        titleColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<BorrowRecord, String>, javafx.beans.value.ObservableValue<String>>() {
            @Override
            public javafx.beans.value.ObservableValue<String> call(TableColumn.CellDataFeatures<BorrowRecord, String> param) {
                return param.getValue().titleProperty();
            }
        });

        // 2. 作者列：显式调用 authorProperty()
        authorColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<BorrowRecord, String>, javafx.beans.value.ObservableValue<String>>() {
            @Override
            public javafx.beans.value.ObservableValue<String> call(TableColumn.CellDataFeatures<BorrowRecord, String> param) {
                return param.getValue().authorProperty();
            }
        });

        // 3. 出版社列：显式调用 publisherProperty()
        publisherColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<BorrowRecord, String>, javafx.beans.value.ObservableValue<String>>() {
            @Override
            public javafx.beans.value.ObservableValue<String> call(TableColumn.CellDataFeatures<BorrowRecord, String> param) {
                return param.getValue().publisherProperty();
            }
        });

        // 4. 借阅时间列：显式调用 borrowDateProperty()
        borrowDateColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<BorrowRecord, String>, javafx.beans.value.ObservableValue<String>>() {
            @Override
            public javafx.beans.value.ObservableValue<String> call(TableColumn.CellDataFeatures<BorrowRecord, String> param) {
                return param.getValue().borrowDateProperty();
            }
        });

        // 5. 应还时间列：显式调用 dueDateProperty()
        dueDateColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<BorrowRecord, String>, javafx.beans.value.ObservableValue<String>>() {
            @Override
            public javafx.beans.value.ObservableValue<String> call(TableColumn.CellDataFeatures<BorrowRecord, String> param) {
                return param.getValue().dueDateProperty();
            }
        });

        // 6. 状态列：显式调用 statusProperty() + 样式区分
        statusColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<BorrowRecord, String>, javafx.beans.value.ObservableValue<String>>() {
            @Override
            public javafx.beans.value.ObservableValue<String> call(TableColumn.CellDataFeatures<BorrowRecord, String> param) {
                return param.getValue().statusProperty();
            }
        });
        // 状态列样式（正常绿色/即将到期橙色/已逾期红色）
        statusColumn.setCellFactory(column -> new TableCell<BorrowRecord, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "正常":
                            setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                            break;
                        case "即将到期":
                            setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                            break;
                        case "已逾期":
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: #6c757d;");
                    }
                }
            }
        });

        // 7. 续借次数列：显式调用 renewCountProperty()
        renewCountColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<BorrowRecord, String>, javafx.beans.value.ObservableValue<String>>() {
            @Override
            public javafx.beans.value.ObservableValue<String> call(TableColumn.CellDataFeatures<BorrowRecord, String> param) {
                return param.getValue().renewCountProperty().asString();
            }
        });

        // 8. 续借操作列
     // 在 renewColumn 的 cell factory 中，加强续借按钮的禁用逻辑
        renewColumn.setCellFactory(new Callback<TableColumn<BorrowRecord, Void>, TableCell<BorrowRecord, Void>>() {
            @Override
            public TableCell<BorrowRecord, Void> call(TableColumn<BorrowRecord, Void> param) {
                return new TableCell<>() {
                    private final Button renewBtn = new Button("续借");
                    {
                        renewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 3; -fx-padding: 3 8;");
                        renewBtn.setOnAction(e -> {
                            BorrowRecord record = getTableView().getItems().get(getIndex());
                            System.out.println("用户点击续借按钮，书籍: " + record.getTitle() +
                                             ", 索书号: " + record.getCallNumber() +
                                             ", 当前状态: " + record.getStatus() +
                                             ", 续借次数: " + record.getRenewCount());
                            handleRenew(record);
                        });
                    }
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            BorrowRecord record = getTableView().getItems().get(getIndex());
                            // 已逾期书籍或已达到最大续借次数(3次)的书籍禁用续借
                            boolean disable = "已逾期".equals(record.getStatus()) || record.getRenewCount() >= 3;
                            renewBtn.setDisable(disable);
                            if (disable) {
                                if ("已逾期".equals(record.getStatus())) {
                                    renewBtn.setTooltip(new Tooltip("已逾期书籍不能续借"));
                                } else if (record.getRenewCount() >= 3) {
                                    renewBtn.setTooltip(new Tooltip("已达到最大续借次数(3次)"));
                                }
                            } else {
                                renewBtn.setTooltip(null);
                            }
                            setGraphic(renewBtn);
                        }
                    }
                };
            }
        });
    }

    // ====================== 5. 其他功能方法（无修改，保持原逻辑）======================
    /**
     * 加载模拟借阅数据（实际项目替换为数据库查询）
     */
    private void loadBorrowDataFromServer() {
        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                BorrowInfoRequest req = new BorrowInfoRequest();
                req.setUserId(userid);

                out.writeUTF("BorrowInfoRequest");
                out.writeUTF(new Gson().toJson(req));
                out.flush();

                String response = in.readUTF();
                if (response.startsWith("SUCCESS|")) {
                    String jsonStr = response.substring(8);
                    List<BorrowInfo> borrowList = new Gson().fromJson(jsonStr,
                        new TypeToken<List<BorrowInfo>>(){}.getType());

                    Platform.runLater(() -> {
                        allBorrowRecords.clear();

                        // 检查服务器返回的数据是否为null或空
                        if (borrowList != null && !borrowList.isEmpty()) {
                            for (BorrowInfo info : borrowList) {
                                // 添加空值检查，防止日期为null
                                String borrowDateStr = info.getBorrowDate() != null ?
                                    sdf.format(info.getBorrowDate()) : "未知";
                                String dueDateStr = info.getDueDate() != null ?
                                    sdf.format(info.getDueDate()) : "未知";
                                String status = calculateStatus(dueDateStr);

                                allBorrowRecords.add(new BorrowRecord(
                                    info.getCallNumber(), // 添加索书号
                                    info.getBookTitle(),
                                    info.getAuthor(),
                                    info.getPublisher(),
                                    borrowDateStr,
                                    dueDateStr,
                                    status,
                                    info.getRenewCount() // 添加续借次数
                                ));
                            }
                            updateBorrowStats();
                            initPagination();
                            updateCurrentPageRecords();
                        } else {
                            // 服务器返回空数据，显示提示信息
                            showAlert("提示", "您当前没有借阅记录或服务器返回空数据");
                            updateBorrowStats(); // 更新统计信息（显示0）
                            initPagination(); // 初始化分页
                            updateCurrentPageRecords(); // 更新当前页数据
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("错误", "获取借阅数据失败: " + response.substring(6));
                        loadSampleBorrowData(); // 加载示例数据
                        updateBorrowStats();
                        initPagination();
                        updateCurrentPageRecords();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("错误", "网络连接失败: " + e.getMessage());
                    loadSampleBorrowData(); // 加载示例数据
                    updateBorrowStats();
                    initPagination();
                    updateCurrentPageRecords();
                });
            }
        }).start();
    }

    private void loadSampleBorrowData() {
        allBorrowRecords.clear(); // 不再重新赋值，直接清空已有数据
        Calendar cal = Calendar.getInstance();
        String today = sdf.format(cal.getTime());

        // 记录1：正常（应还日期3天后）
        cal.add(Calendar.DAY_OF_MONTH, 3);
        allBorrowRecords.add(new BorrowRecord("TP311.56/JAVA", "Java编程思想（第4版）", "Bruce Eckel", "机械工业出版社", today, sdf.format(cal.getTime()), "正常", 0));

        // 记录2：即将到期（应还日期2天后）
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, 2);
        allBorrowRecords.add(new BorrowRecord("I247.55/SAN", "三体", "刘慈欣", "重庆出版社", today, sdf.format(cal.getTime()), "即将到期", 1));

        // 记录3：已逾期（应还日期2天前）
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, -2);
        allBorrowRecords.add(new BorrowRecord("K209/ZGT", "中国通史", "吕思勉", "中华书局", today, sdf.format(cal.getTime()), "已逾期", 2));

        // 补充更多模拟数据
        allBorrowRecords.add(new BorrowRecord("F0/JJX", "经济学原理", "曼昆", "北京大学出版社", today, sdf.format(cal.getTime()), "已逾期", 0));
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, 5);
        allBorrowRecords.add(new BorrowRecord("TP311.12/SJG", "数据结构", "严蔚敏", "清华大学出版社", today, sdf.format(cal.getTime()), "正常", 1));
        cal.add(Calendar.DAY_OF_MONTH, 10);
        allBorrowRecords.add(new BorrowRecord("R322/RTJ", "人体解剖学", "Gray", "人民卫生出版社", today, sdf.format(cal.getTime()), "正常", 0));
        cal.add(Calendar.DAY_OF_MONTH, 1);
        allBorrowRecords.add(new BorrowRecord("D923.4/MF", "民法典解读", "王利明", "中国人民大学出版社", today, sdf.format(cal.getTime()), "即将到期", 3)); // 已达到最大续借次数
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, 4);
        allBorrowRecords.add(new BorrowRecord("I561.33/HM", "哈姆雷特", "莎士比亚", "人民文学出版社", today, sdf.format(cal.getTime()), "正常", 0));
        cal.add(Calendar.DAY_OF_MONTH, -1);
        allBorrowRecords.add(new BorrowRecord("N02/KXZ", "科学哲学导论", "波普尔", "中国美术学院出版社", today, sdf.format(cal.getTime()), "即将到期", 1));
        cal.add(Calendar.DAY_OF_MONTH, -5);
        allBorrowRecords.add(new BorrowRecord("TP181/JQ", "机器学习", "周志华", "清华大学出版社", today, sdf.format(cal.getTime()), "已逾期", 2));
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, 7);
        allBorrowRecords.add(new BorrowRecord("I313.45/NW", "挪威的森林", "村上春树", "上海译文出版社", today, sdf.format(cal.getTime()), "正常", 0));
        cal.add(Calendar.DAY_OF_MONTH, 2);
        allBorrowRecords.add(new BorrowRecord("J209.2/ZG", "中国绘画史", "王伯敏", "高等教育出版社", today, sdf.format(cal.getTime()), "正常", 0));
    }

    /**
     * 处理续借操作
     */
    private void handleRenew(BorrowRecord record) {
        // 前置检查：如果续借次数已经达到1次，则直接提示并返回
        if (record.getRenewCount() >= 1) {
            Platform.runLater(() -> {
                showAlert("续借失败", "已达到最大续借次数(1次)，无法继续续借");
            });
            return;
        }

        new Thread(() -> {
            try {
                DataOutputStream out = SocketManager.getInstance().getOut();
                DataInputStream in = SocketManager.getInstance().getIn();

                // 构建续借请求
                BorrowRequest req = new BorrowRequest();
                req.setUserId(userid);
                req.setCallNumber(record.getCallNumber());
                req.setOperationType("RENEW");

                String requestJson = new Gson().toJson(req);
                System.out.println("发送续借请求: " + requestJson);

                out.writeUTF("BorrowRequest");
                out.writeUTF(requestJson);
                out.flush();

                // 接收响应
                String response = in.readUTF();
                System.out.println("收到服务器响应: " + response);

                if (response.startsWith("SUCCESS|")) {
                    String dataPart = response.substring(8);
                    System.out.println("响应数据部分: '" + dataPart + "'");

                    if (dataPart.isEmpty()) {
                        // 服务器返回了空数据，提示用户并重新加载数据
                        Platform.runLater(() -> {
                            showAlert("续借结果未知", "续借操作完成，但服务器未返回确认信息。请刷新页面查看最新状态。");
                            // 重新加载借阅数据以确保状态正确
                            loadBorrowDataFromServer();
                        });
                    } else {
                        String[] parts = dataPart.split("\\|");
                        System.out.println("响应解析结果: " + Arrays.toString(parts));

                        if (parts.length >= 2) {
                            String newDueDate = parts[0];
                            int newRenewCount;

                            try {
                                newRenewCount = Integer.parseInt(parts[1]);
                            } catch (NumberFormatException e) {
                                System.err.println("续借次数格式错误: " + parts[1]);
                                newRenewCount = record.getRenewCount() + 1; // 默认增加1
                            }

                            final String finalDueDate = newDueDate;
                            final int finalRenewCount = newRenewCount;

                            Platform.runLater(() -> {
                                record.setDueDate(finalDueDate);
                                record.setRenewCount(finalRenewCount);
                                record.setStatus(calculateStatus(finalDueDate));

                                updateBorrowStats();
                                updateCurrentPageRecords();
                                showAlert("续借成功", String.format("《%s》续借成功！\n新应还时间：%s\n续借次数：%d",
                                        record.getTitle(), finalDueDate, finalRenewCount));
                            });
                        } else {
                            Platform.runLater(() -> {
                                System.err.println("续借响应格式错误，期望2个参数，实际收到: " + parts.length);
                                System.err.println("完整响应: " + response);
                                showAlert("续借失败", "服务器返回数据格式不正确");
                            });
                        }
                    }
                } else if (response.startsWith("ERROR|")) {
                    String errorMsg = response.substring(6);
                    Platform.runLater(() -> {
                        System.err.println("续借失败，服务器返回错误: " + errorMsg);
                        showAlert("续借失败", errorMsg);
                    });
                } else {
                    // 处理未知响应格式
                    Platform.runLater(() -> {
                        System.err.println("未知响应格式: " + response);
                        showAlert("错误", "服务器返回未知响应格式: " + response);
                    });
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    System.err.println("网络通信异常: " + e.getMessage());
                    e.printStackTrace();
                    showAlert("错误", "网络连接失败: " + e.getMessage());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("续借请求发生未知异常: " + e.getMessage());
                    e.printStackTrace();
                    showAlert("错误", "续借请求失败: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 计算借阅状态
     */
    private String calculateStatus(Date dueDate) {
        Date today = new Date();
        long diffDays = (dueDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);
        if (diffDays < 0) {
			return "已逾期";
		} else if (diffDays <= EXPIRING_SOON_DAYS) {
			return "即将到期";
		} else {
			return "正常";
		}
    }

    // 添加calculateStatus方法
    private String calculateStatus(String dueDateStr) {
        try {
            Date dueDate = sdf.parse(dueDateStr);
            Date today = new Date();
            long diffDays = (dueDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);

            if (diffDays < 0) {
				return "已逾期";
			} else if (diffDays <= EXPIRING_SOON_DAYS) {
				return "即将到期";
			} else {
				return "正常";
			}
        } catch (ParseException e) {
            e.printStackTrace();
            return "日期错误";
        }
    }

    /**
     * 更新借阅统计
     */
    private void updateBorrowStats() {
        if (allBorrowRecords == null || allBorrowRecords.isEmpty()) {
            totalBorrowLabel.setText("0");
            expiringSoonLabel.setText("0");
            overdueLabel.setText("0");
            return;
        }
        int total = allBorrowRecords.size();
        int expiringSoon = 0, overdue = 0;
        for (BorrowRecord r : allBorrowRecords) {
            if ("即将到期".equals(r.getStatus())) {
				expiringSoon++;
			}
            if ("已逾期".equals(r.getStatus())) {
				overdue++;
			}
        }
        totalBorrowLabel.setText(String.valueOf(total));
        expiringSoonLabel.setText(String.valueOf(expiringSoon));
        overdueLabel.setText(String.valueOf(overdue));
    }

    /**
     * 初始化分页
     */
    private void initPagination() {
        pageSize = pageSizeCombo.getValue();
        totalPages = (allBorrowRecords.size() + pageSize - 1) / pageSize;
        updatePageInfoLabel();
        updatePaginationButtonStatus();
    }

    /**
     * 绑定分页事件
     */
    private void bindPaginationEvents() {
        firstPageBtn.setOnAction(e -> {
            if (currentPage != 1) {
                currentPage = 1;
                updateCurrentPageRecords();
                updatePageInfoLabel();
                updatePaginationButtonStatus();
            }
        });
        prevPageBtn.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                updateCurrentPageRecords();
                updatePageInfoLabel();
                updatePaginationButtonStatus();
            }
        });
        nextPageBtn.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                updateCurrentPageRecords();
                updatePageInfoLabel();
                updatePaginationButtonStatus();
            }
        });
        lastPageBtn.setOnAction(e -> {
            if (currentPage != totalPages) {
                currentPage = totalPages;
                updateCurrentPageRecords();
                updatePageInfoLabel();
                updatePaginationButtonStatus();
            }
        });
        pageSizeCombo.setOnAction(e -> {
            pageSize = pageSizeCombo.getValue();
            currentPage = 1;
            initPagination();
            updateCurrentPageRecords();
        });
    }

    /**
     * 更新当前页数据
     */
    private void updateCurrentPageRecords() {
        if (allBorrowRecords == null || allBorrowRecords.isEmpty()) {
            currentPageRecords = FXCollections.observableArrayList();
        } else {
            currentPage = Math.max(1, Math.min(currentPage, totalPages));
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, allBorrowRecords.size());
            currentPageRecords = FXCollections.observableArrayList(allBorrowRecords.subList(start, end));
        }
        borrowTable.setItems(currentPageRecords);
    }

    /**
     * 更新页码提示
     */
    private void updatePageInfoLabel() {
        pageInfoLabel.setText(String.format("第 %d 页 / 共 %d 页", currentPage, totalPages));
    }

    /**
     * 更新分页按钮状态
     */
    private void updatePaginationButtonStatus() {
        firstPageBtn.setDisable(currentPage <= 1 || totalPages <= 1);
        prevPageBtn.setDisable(currentPage <= 1 || totalPages <= 1);
        nextPageBtn.setDisable(currentPage >= totalPages || totalPages <= 1);
        lastPageBtn.setDisable(currentPage >= totalPages || totalPages <= 1);
    }

    /**
     * 弹窗提示
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ====================== 6. 借阅记录实体类（保持不变）======================
    /**
     * 借阅记录实体类（public static 确保外部可访问）
     */
    public static class BorrowRecord {
        private final SimpleStringProperty callNumber;   // 索书号
        private final SimpleStringProperty title;       // 书名
        private final SimpleStringProperty author;      // 作者
        private final SimpleStringProperty publisher;   // 出版社
        private final SimpleStringProperty borrowDate;  // 借阅时间
        private final SimpleStringProperty dueDate;     // 应还时间
        private final SimpleStringProperty status;      // 状态
        private final SimpleIntegerProperty renewCount; // 续借次数

        public BorrowRecord(String callNumber, String title, String author, String publisher,
                           String borrowDate, String dueDate, String status, int renewCount) {
            this.callNumber = new SimpleStringProperty(callNumber);
            this.title = new SimpleStringProperty(title);
            this.author = new SimpleStringProperty(author);
            this.publisher = new SimpleStringProperty(publisher);
            this.borrowDate = new SimpleStringProperty(borrowDate);
            this.dueDate = new SimpleStringProperty(dueDate);
            this.status = new SimpleStringProperty(status);
            this.renewCount = new SimpleIntegerProperty(renewCount);
        }

        public String getCallNumber() {
            return callNumber.get();
        }

        // 添加状态计算方法
        public String calculateStatus() {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date dueDate = sdf.parse(getDueDate());
                Date today = new Date();
                long diffDays = (dueDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);

                if (diffDays < 0) {
					return "已逾期";
				} else if (diffDays <= 3) {
					return "即将到期";
				} else {
					return "正常";
				}
            } catch (ParseException e) {
                e.printStackTrace();
                return "日期错误";
            }
        }

        // Getter（非绑定场景用）
        public String getTitle() { return title.get(); }
        public String getAuthor() { return author.get(); }
        public String getPublisher() { return publisher.get(); }
        public String getBorrowDate() { return borrowDate.get(); }
        public String getDueDate() { return dueDate.get(); }
        public String getStatus() { return status.get(); }
        public int getRenewCount() { return renewCount.get(); }

        // Setter（续借时更新）
        public void setDueDate(String dueDate) { this.dueDate.set(dueDate); }
        public void setStatus(String status) { this.status.set(status); }
        public void setRenewCount(int renewCount) { this.renewCount.set(renewCount); }

        // Property方法（显式绑定用，核心）
        public SimpleStringProperty callNumberProperty() { return callNumber; }
        public SimpleStringProperty titleProperty() { return title; }
        public SimpleStringProperty authorProperty() { return author; }
        public SimpleStringProperty publisherProperty() { return publisher; }
        public SimpleStringProperty borrowDateProperty() { return borrowDate; }
        public SimpleStringProperty dueDateProperty() { return dueDate; }
        public SimpleStringProperty statusProperty() { return status; }
        public SimpleIntegerProperty renewCountProperty() { return renewCount; }
    }
}