package seu.vcampus.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import seu.vcampus.model.AbsenceInfo;
import seu.vcampus.model.AdminBillRecord;
import seu.vcampus.model.AdminBookRequest;
import seu.vcampus.model.AdminDeleteRoomRequest;
import seu.vcampus.model.AdminDormitoryInfoResponse;
import seu.vcampus.model.AdminInfo;
import seu.vcampus.model.AdminInfoRequest;
import seu.vcampus.model.AdminRoomBookingRequest;
import seu.vcampus.model.AdminRoomInfoRequest;
import seu.vcampus.model.BookInfo;
import seu.vcampus.model.BookInfoRequest;
import seu.vcampus.model.BorrowInfo;
import seu.vcampus.model.BorrowInfoRequest;
import seu.vcampus.model.BorrowRequest;
import seu.vcampus.model.CartItem;
import seu.vcampus.model.CartRequest;
import seu.vcampus.model.CartResponse;
import seu.vcampus.model.ChangePasswordRequest;
import seu.vcampus.model.CheckoutRequest;
import seu.vcampus.model.CheckoutResponse;
import seu.vcampus.model.CourseTime;
import seu.vcampus.model.CurrentBill;
import seu.vcampus.model.DormitoryExchangeApplication;
import seu.vcampus.model.DormitoryExchangeRequest;
import seu.vcampus.model.DormitoryInfoRequest;
import seu.vcampus.model.DormitoryInfoResponse;
import seu.vcampus.model.ElectricityWaterBillRequest;
import seu.vcampus.model.ElectricityWaterBillResponse;
import seu.vcampus.model.FinalCourse;
import seu.vcampus.model.HistoryBill;
import seu.vcampus.model.HygieneRecord;
import seu.vcampus.model.LoginRequest;
import seu.vcampus.model.LoginResponse;
import seu.vcampus.model.Order;
import seu.vcampus.model.OrderDetailRequest;
import seu.vcampus.model.OrderDetailResponse;
import seu.vcampus.model.OrderHistoryRequest;
import seu.vcampus.model.OrderHistoryResponse;
import seu.vcampus.model.OrderItem;
import seu.vcampus.model.OrderStatusUpdateRequest;
import seu.vcampus.model.PaymentRequest;
import seu.vcampus.model.PaymentResponse;
import seu.vcampus.model.Product;
import seu.vcampus.model.ProductDeleteRequest;
import seu.vcampus.model.ProductInfoRequest;
import seu.vcampus.model.ProductInfoResponse;
import seu.vcampus.model.RepairInfo;
import seu.vcampus.model.RepairRequest;
import seu.vcampus.model.RewardPunishment;
import seu.vcampus.model.RoomBookingInfo;
import seu.vcampus.model.RoomBookingRequest;
import seu.vcampus.model.RoomInfo;
import seu.vcampus.model.StatusChangeApplication;
import seu.vcampus.model.StudentGrades;
import seu.vcampus.model.StudentInfo;
import seu.vcampus.model.StudentInfoRequest;
import seu.vcampus.model.StudentProfile;
import seu.vcampus.model.TeacherInfo;
import seu.vcampus.model.TeacherInfoRequest;
import seu.vcampus.model.TestCourse;
import seu.vcampus.model.Transaction;
import seu.vcampus.model.TransactionRequest;
import seu.vcampus.model.TransactionResponse;
import seu.vcampus.util.*;
import seu.vcampus.model.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;
public class Server {

    private static final String URL = "jdbc:mysql://localhost:3306/testdb?useSSL=false&serverTimezone=GMT%2B8";
    private static final String USER = "root";
    private static final String PASSWORD = "你自己的数据库密码";
    private static final int PORT = 8083;
    
    
    // 在 Server 类中添加这个静态内部类
    private static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public void write(JsonWriter out, LocalDate value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(formatter.format(value));
            }
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String dateStr = in.nextString();
            return LocalDate.parse(dateStr, formatter);
        }
    }

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd")
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();

    private static JedisPool jedisPool;
    public static JedisPool getJedisPool() {
        return jedisPool;
    }
    public static void main(String[] args) {
    	
    	// 初始化 Redis 连接池（Redis 在本机 6379 端口，无密码）
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(30);      // 最大连接数
        config.setMaxIdle(10);       // 最大空闲连接
        jedisPool = new JedisPool(config, "localhost", 6379);
        
        System.out.println("V-Campus服务器启动中...");
        try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		}catch(Exception e) {
			e.printStackTrace();
		}

        // 创建与数据库的连接
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            System.out.println("[Success]数据库连接成功");

            // 检查表结构
            checkTableStructure(conn);

            // 创建ServerSocket并循环接受客户端连接
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("[Success]服务器套接字创建成功，监听端口: " + PORT);

                // 循环接受客户端连接，实现多客户端支持
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        String clientInfo = clientSocket.getRemoteSocketAddress().toString();
                        System.out.println("[Event]收到客户端" + clientInfo + "的连接请求");

                        // 为每个客户端连接创建新线程处理，避免阻塞主线程
                        new Thread(new ClientHandler(clientSocket, conn)).start();
                    } catch (IOException e) {
                        System.out.println("[Error]接受客户端连接失败: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.out.println("[Error]服务器套接字创建失败: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.out.println("[Error]数据库连接失败: " + e.getMessage());
            return; // 数据库连接失败，无法继续运行
        }
    }

    // 检查表结构
    private static void checkTableStructure(Connection conn) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "user_auth", new String[]{"TABLE"});
            if (tables.next()) {
                System.out.println("[Info]找到user_auth表");

                // 检查userid列是否存在
                ResultSet columns = meta.getColumns(null, null, "user_auth", "userid");
                if (columns.next()) {
                    System.out.println("[Info]找到userid列");
                } else {
                    System.out.println("[Error]未找到userid列，实际列名可能不同");
                    // 列出所有列名
                    ResultSet allColumns = meta.getColumns(null, null, "user_auth", null);
                    System.out.println("[Info]user_auth表的列:");
                    while (allColumns.next()) {
                        System.out.println("  - " + allColumns.getString("COLUMN_NAME"));
                    }
                }
            } else {
                System.out.println("[Error]未找到user_auth表");
            }
        } catch (SQLException e) {
            System.out.println("[Error]检查表结构时出错: " + e.getMessage());
        }
    }
    
    // 客户端处理器，每个客户端连接对应一个实例
    private static class ClientHandler implements Runnable {

        private Socket clientSocket;
        private Connection conn;
        private DataInputStream in;
        private DataOutputStream out;
        private String userId;

        public ClientHandler(Socket socket, Connection connection) {
            this.clientSocket = socket;
            this.conn = connection;
        }

        @Override
        public void run() {
            try {
                // 初始化输入输出流
                in = new DataInputStream(clientSocket.getInputStream());
                out = new DataOutputStream(clientSocket.getOutputStream());

                System.out.println("[Success]与客户端" + clientSocket.getRemoteSocketAddress() + "的连接已建立");

                // 处理客户端请求
                while (!clientSocket.isClosed()) {
                    // 第一步：读取请求标识（区分是LoginRequest还是StudentInfoRequest）

                    String requestType = in.readUTF();  // 接收客户端发送的标识字符串

                    // 第二步：根据标识处理不同请求
                    switch (requestType) {
                        case "LoginRequest":
                            processLoginRequest();  // 处理登录请求
                            break;
                        case "StudentInfoRequest":
                            processStudentInfoRequest();  // 处理学生信息请求
                            break;
                        case "TeacherInfoRequest":
                        	processTeacherInfoRequest();
                        	break;
                        case "AdminInfoRequest":
                        	processAdminInfoRequest();
                        	break;
                        case "ChangePasswordRequest":
                            processChangePasswordRequest();  // 处理密码修改请求
                            break;
                        case "LogoutRequest":
                        	processLogoutRequest();
                        	break;
                        case "ProductInfoRequest":
                            processProductInfoRequest();
                            break;
                        case "CartRequest":
                            processCartRequest();
                            break;
                        case "CheckoutRequest":
                            processCheckoutRequest();
                            break;
                        case "OrderHistoryRequest":
                            processOrderHistoryRequest();
                            break;
                        case "OrderDetailRequest":
                            processOrderDetailRequest();
                            break;
                        case "TransactionHistoryRequest":
                            processTransactionHistoryRequest();
                            break;
                        case "PaymentRequest": // 新增支付请求类型
                            processPaymentRequest();
                            break;
                        case "ProductAddRequest":
                            processProductAddRequest();
                            break;
                        case "ProductUpdateRequest":
                            processProductUpdateRequest();
                            break;
                        case "ProductDeleteRequest":
                            processProductDeleteRequest();
                            break;
                        case "OrderStatusUpdateRequest":
                            processOrderStatusUpdateRequest();
                            break;
                        case "PaymentMethodStatsRequest":
                            processPaymentMethodStatsRequest();
                            break;
                        case "DormitoryInfoRequest":
                            System.out.println("[Debug]处理宿舍信息请求");
                            processDormitoryInfoRequest();
                            break;
                        case "ElectricityWaterBillRequest":
                            System.out.println("[Debug]处理水电费请求");
                            processElectricityWaterBillRequest();
                            break;
                        case "DormitoryExchangeRequest":
                            System.out.println("[Debug]处理退换宿舍请求");
                            processDormitoryExchangeRequest();
                            break;
                        case "RepairRequest":
                            System.out.println("[Debug]处理报修请求");
                            processRepairRequest();
                            break;
                        case "RepairListRequest":
                            System.out.println("[Debug]处理维修列表请求");
                            processRepairListRequest();
                            break;
                        case "ProcessRepairRequest":
                            System.out.println("[Debug]处理维修状态更新请求");
                            processProcessRepairRequest();
                            break;
                        case "ExchangeListRequest":
                            System.out.println("[Debug]处理退换宿舍申请列表请求");
                            processExchangeListRequest();
                            break;
                        case "ExchangeStatusUpdate":
                            System.out.println("[Debug]处理退换宿舍申请状态更新");
                            processExchangeStatusUpdate();
                            break;
                        case "AdminBillListRequest":
                            System.out.println("[Debug]处理管理员水电费列表请求");
                            processAdminBillListRequest();
                            break;
                        case "AdminRemindRequest":
                            System.out.println("[Debug]处理管理员催缴请求");
                            processAdminRemindRequest();
                            break;
                        case "AdminDormitoryInfoRequest":
                            System.out.println("[Debug]处理管理员宿舍信息请求");
                            processAdminDormitoryInfoRequest();
                            break;
                        // 学籍系统开始
                        case "GetCampusData":
                        	processGetCampusData();
                        	break;
                        case "GetStudentsByConditions":
                        	processGetStudentsByConditions();
                        	break;
                        case "GetStudentProfile":
                        	processGetStudentProfile();
                        	break;
                        case "GetStudentById":
                        	processGetStudentById();
                        	break;
                        case "GetStudentRewardPunishmentById":
                        	processGetStudentRewardPunishmentById();
                        	break;
                        case "AddRewardPunishment":
                        	processAddRewardPunishment();
                        	break;
                        case "UpdateRewardPunishment":
                        	processUpdateRewardPunishment();
                        	break;
                        case "RevokeRewardPunishment":
                        	processRevokeRewardPunishment();
                        	break;
                        case "UpdateStudentProfile":
                        	processUpdateStudentProfile();
                        	break;
                        case "AddStudentProfile":
                        	processAddStudentProfile();
                        	break;
                        case "GetCourseSectionsMap":
                        	processGetCourseSectionsMap();
                        	break;
                        case "GetStudentGradeListByTeacher":
                        	processGetStudentGradeListByTeacher();
                        	break;
                        case "UpdateStudentGrade":
                        	processUpdateStudentGrade();
                        	break;
                        case "SubmitCourseSectionStatus":
                        	processSubmitCourseSectionStatus();
                        	break;	
                        case "GetStudentGradeAllSemester":
                        	processGetStudentGradeAllSemester();
                        	break;
                        case "GetStudentSemesterGrade":
                        	processGetStudentSemesterGrade();
                        	break;
                        case "AdminRetrieveCourses":
                        	processAdminRetrieveCourses();
                        	break;
                        case "AdminGetStudentsBySectionId":
                        	processAdminGetStudentsBySectionId();
                        	break;
                        case "AdminUpdateStudentGrade":
                        	processAdminUpdateStudentGrade();
                        	break;
                        case "AdminPassCourseSection":
                        	processAdminPassCourseSection();
                        	break;
                        // 学籍系统结束
                        case "BookInfoRequest":  // 新增图书信息请求处理
                            processBookInfoRequest();
                            break;
                        case "BorrowRequest":
                            processBorrowRequest();
                            break;
                        case "BorrowInfoRequest":
                            processBorrowInfoRequest();
                            break;
                        case "RoomInfoRequest":
                            processRoomInfoRequest();
                            break;
                        case "RoomBookingRequest":
                            processRoomBookingRequest();
                            break;
                        case "AdminBookRequest":
                            processAdminBookRequest();
                            break;
                        case "AdminRoomInfoRequest":
                            processAdminRoomInfoRequest();
                            break;
                        case "AdminAddRoomRequest":
                            processAdminAddRoomRequest();
                            break;
                        case "AdminEditRoomRequest":
                            processAdminEditRoomRequest();
                            break;
                        case "AdminDeleteRoomRequest":
                            processAdminDeleteRoomRequest();
                            break;
                        case "AdminRoomBookingInfoRequest":
                            processAdminRoomBookingInfoRequest();
                            break;
                        case "FileUploadRequest":
                            processFileUploadRequest();
                            break;
                        case "CourseApplication":
                            processCourseApplication();  //处理课程申请
                            break;
                        case "CoursePublicAgree":
                            processCoursePublish();  //同意课程申请的处理
                            break;
                        case "CourseAppliedRequire":
                            processCourseAppliedRequire();  //传输已申请课程的数据
                            break;
                        case "CoursePublicDisagree":
                            processCourseCancel(); //拒绝课程申请的处理
                            break;
                        case "CoursePublicedRequire":
                            processCoursePublicedRequire(); //拒绝课程申请的处理
                            break;
                        case "TimeSubmit":
                            processTimeSubmit(); //拒绝课程申请的处理
                            break;
                        case "TimeOk":
                            processTimeOk(); //拒绝课程申请的处理
                            break;
                        case "SetIsSchedule":
                            processSetIsSchedule(); //拒绝课程申请的处理
                            break;
                        case "CourseScheduledRequire":
                            processScheduleRequire(); //拒绝课程申请的处理
                            break;
                        case "DateToSelectRequire":
                            processForSelectData(); //拒绝课程申请的处理
                            break;
                        case "CourseSelectedRequire":
                            processCourseSelected(); //拒绝课程申请的处理
                            break;
                        case "CourseDropRequire":
                            processCourseDrop(); //拒绝课程申请的处理
                            break;
                        case "SelectedCourseRequire":
                            processDeskRequire(); //拒绝课程申请的处理
                            break;
                        case "SelectCourseInfo":
                            SelectCourseInfo(); //拒绝课程申请的处理
                            break;
                        case "ApplyCourseInfo":
                            ApplyCourseInfo(); //拒绝课程申请的处理
                            break;
                        case "CourseDeskInfo":
                            CourseDeskInfo(); //拒绝课程申请的处理
                            break;
                        case "TeacherDeskRequire":
                            TeacherInfo(); //拒绝课程申请的处理
                            break;
                        case "getTForC":
                        	getTforC();
                        	break;
                        default:
                            // 未知请求，返回错误
                            out.writeUTF("ERROR|未知请求类型：" + requestType);
                            out.flush();
                            break;
                    }
                }
            } catch(EOFException e) {
            	System.out.println("[Info]与客户端"+ userId +"的通信结束"); // TODO
            } catch (IOException e) {
                System.out.println("[Error]与客户端通信时发生错误: " + e.getMessage());
            } finally {
                // 确保资源关闭
                try {

                	// 如果用户已登录但未正常登出，更新online状态为0
                    if (userId != null) {
                        try (Jedis jedis = Server.getJedisPool().getResource()){
                        	 jedis.del("login:user:" + userId);
                             System.out.println("[Event]用户 " + userId + " 连接断开，已清理 Redis 登录态，已更新online状态为0");
                            updateOnlineStatus(userId, 0);
                            //System.out.println("[Event]用户" + userId + "连接异常断开，已更新online状态为0");
                        } catch (SQLException e) {
                            System.out.println("[Error]更新online状态时发生错误: " + e.getMessage());
                        }
                    }

                    if (in != null) {
						in.close();
					}
                    if (out != null) {
						out.close();
					}
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                        System.out.println("[Event]与客户端" + clientSocket.getRemoteSocketAddress() + "的连接已关闭");
                    }
                } catch (IOException e) {
                    System.out.println("[Error]关闭客户端连接时发生错误: " + e.getMessage());
                }
            }
        }

        //处理登出请求
        private void processLogoutRequest() {
            try {
                if (userId != null) {
                    // 更新online状态为0
                	try (Jedis jedis = Server.getJedisPool().getResource()) {
                        jedis.del("login:user:" + userId);
                        System.out.println("[Event]用户 " + userId + " 主动登出，已清理 Redis 登录态");}
                    updateOnlineStatus(userId, 0);
                    userId = null;
                    out.writeUTF("SUCCESS|登出成功");
                    System.out.println("[Event]用户" + userId + "已登出");
                } else {
                    out.writeUTF("ERROR|用户未登录");
                }
                out.flush();
            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理登出请求失败：" + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送登出错误响应失败：" + ex.getMessage());
                }
            }
        }
        
        
		//处理登录请求
        private void processLoginRequest() {
            String id = null;
            String password = null;
            int type = 0;
            String username = null;

            try {
                String jsonReq = in.readUTF();
                LoginRequest loginReq = gson.fromJson(jsonReq, LoginRequest.class);
                id = loginReq.getUserID();
                password = loginReq.getPassword();

                System.out.println("[Event]收到登录请求 - 账号: " + id);

                // 1. 校验账号密码
                boolean isValid = validateUser(id, password);
                if (!isValid) {
                    out.writeUTF(gson.toJson(new LoginResponse(0, "", 9)));
                    System.out.println("[Event]账号" + id + "密码错误");
                    out.flush();
                    return;
                }

                // 2. 获取用户类型和姓名
                type = getUserType(id);
                username = getUserName(id);

                // 3. 使用 Redis 原子操作判断是否已登录
                String redisKey = "login:user:" + id;
                try (Jedis jedis = Server.getJedisPool().getResource()) {
                    // SET key value NX EX 1800  (30分钟过期)
                    SetParams params = SetParams.setParams().nx().ex(1800);
                    String result = jedis.set(redisKey, String.valueOf(System.currentTimeMillis()), params);
                    
                    if (!"OK".equals(result)) {
                        // key 已存在 → 账号已在线
                        out.writeUTF(gson.toJson(new LoginResponse(-2, username, type)));
                        System.out.println("[Event]账号" + id + "已在别处登录，拒绝本次登录");
                        out.flush();
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("[Error]Redis 操作失败: " + e.getMessage());
                    // Redis 异常时降级处理：可以暂时回退到数据库 state 检查，或直接返回 -1
                    out.writeUTF(gson.toJson(new LoginResponse(-1, "", 9)));
                    out.flush();
                    return;
                }

                // 4. 登录成功
                this.userId = id;  // 记录当前线程对应的用户ID，用于后续清理
                //可选：异步更新数据库 state=1 作为备份（不必须）
                updateOnlineStatus(id, 1);

                out.writeUTF(gson.toJson(new LoginResponse(1, username, type)));
                System.out.println("[Event]账号" + id + "登录成功");
                out.flush();

            } catch (IOException e) {
                System.out.println("[Error]处理登录请求时发生IO错误: " + e.getMessage());
                try {
                    out.writeInt(-1); // -1表示发生错误
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送错误状态时失败: " + ex.getMessage());
                }
            } catch (SQLException e) {
                System.out.println("[Error]数据库操作失败: " + e.getMessage());
                try {
                    out.writeInt(-1); // -1表示发生错误
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送错误状态时失败: " + ex.getMessage());
                }
            }
        }

        //处理学生基本信息请求
        private void processStudentInfoRequest() {
            try {
                // 读取StudentInfoRequest的JSON数据
                String jsonReq = in.readUTF();
                StudentInfoRequest req = gson.fromJson(jsonReq, StudentInfoRequest.class);
                String userid = req.getUserid();

                System.out.println("[Event]收到学生信息请求 - 学号: " + userid);

                // 查询数据库
                StudentInfo info = getStudentInfoFromDB(userid);

                // 发送响应（成功/失败都必须返回）
                if (info != null) {
                    String json = gson.toJson(info);
                    out.writeUTF("SUCCESS|" + json);  // 成功响应
                    System.out.println("[Event]学号" + userid + "查询成功");
                } else {
                    out.writeUTF("ERROR|未找到学号为" + userid + "的信息");  // 失败响应
                    System.out.println("[Event]学号" + userid + "查询失败");
                }
                out.flush();

            } catch (SQLException e) {
                // 数据库异常：返回错误
                try {
                    out.writeUTF("ERROR|数据库查询失败：" + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送数据库错误响应失败：" + ex.getMessage());
                }
            } catch (Exception e) {
                // 其他异常：返回错误
                try {
                    out.writeUTF("ERROR|处理请求失败：" + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送异常响应失败：" + ex.getMessage());
                }
            }
        }

        //处理教师基本信息请求
        private void processTeacherInfoRequest() {
            try {
                // 读取教师信息请求
                String jsonReq = in.readUTF();
                TeacherInfoRequest req = gson.fromJson(jsonReq, TeacherInfoRequest.class);
                String userid = req.getUserid();

                System.out.println("[Event]收到教师信息请求 - 工号: " + userid);

                // 从数据库查询教师信息
                TeacherInfo info = getTeacherInfoFromDB(userid);

                // 发送响应
                if (info != null) {
                    String json = gson.toJson(info);
                    out.writeUTF("SUCCESS|" + json);
                    System.out.println("[Event]工号" + userid + "查询成功");
                } else {
                    out.writeUTF("ERROR|未找到工号为" + userid + "的信息");
                    System.out.println("[Event]工号" + userid + "查询失败");
                }
                out.flush();
            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理教师信息请求失败：" + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送教师信息错误响应失败：" + ex.getMessage());
                }
            }
        }

        //处理管理员基本信息情求
        private void processAdminInfoRequest() {
            try {
                // 读取教师信息请求
                String jsonReq = in.readUTF();
                AdminInfoRequest req = gson.fromJson(jsonReq, AdminInfoRequest.class);
                String userid = req.getUserid();

                System.out.println("[Event]收到管理员信息请求 - 工号: " + userid);

                // 从数据库查询教师信息
                AdminInfo info = getAdminInfoFromDB(userid);

                // 发送响应
                if (info != null) {
                    String json = gson.toJson(info);
                    out.writeUTF("SUCCESS|" + json);
                    System.out.println("[Event]工号" + userid + "查询成功");
                } else {
                    out.writeUTF("ERROR|未找到工号为" + userid + "的信息");
                    System.out.println("[Event]工号" + userid + "查询失败");
                }
                out.flush();
            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理管理员信息请求失败：" + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送管理员信息错误响应失败：" + ex.getMessage());
                }
            }
        }

     // 添加处理商品信息请求的方法
        private void processProductInfoRequest() {
            try {
                // 读取请求参数
                String jsonReq = in.readUTF();
                ProductInfoRequest req = gson.fromJson(jsonReq, ProductInfoRequest.class);

                System.out.println("[Event]收到商品信息请求 - 分类: " + req.getCategory() +
                                  ", 关键词: " + req.getKeyword());

                // 从数据库查询商品信息
                List<Product> products = getProductsFromDB(req.getCategory(), req.getKeyword());

                // 发送响应
                if (products != null && !products.isEmpty()) {
                    ProductInfoResponse response = new ProductInfoResponse(1, products);
                    String jsonRsp = gson.toJson(response);
                    out.writeUTF("SUCCESS|" + jsonRsp);
                    System.out.println("[Event]返回" + products.size() + "条商品信息");
                } else {
                    out.writeUTF("ERROR|未找到符合条件的商品");
                    System.out.println("[Event]未找到符合条件的商品");
                }
                out.flush();

            } catch (SQLException e) {
                try {
                    out.writeUTF("ERROR|数据库查询失败：" + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送数据库错误响应失败：" + ex.getMessage());
                }
            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理商品信息请求失败：" + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送异常响应失败：" + ex.getMessage());
                }
            }
        }

        //修改密码
        private void processChangePasswordRequest() {
            try {
                // 读取ChangePasswordRequest的JSON数据
                String jsonReq = in.readUTF();
                ChangePasswordRequest req = gson.fromJson(jsonReq, ChangePasswordRequest.class);
                String userid = req.getUserId();
                String oldPassword = req.getOldPassword();
                String newPassword = req.getNewPassword();

                System.out.println("[Event]收到密码修改请求 - 用户: " + userid);

                // 验证旧密码并更新密码
                boolean success = changePasswordInDB(userid, oldPassword, newPassword);

                // 发送响应
                if (success) {
                    out.writeUTF("SUCCESS|密码修改成功");
                    System.out.println("[Event]用户" + userid + "密码修改成功");
                } else {
                    out.writeUTF("ERROR|密码修改失败，请检查原密码是否正确");
                    System.out.println("[Event]用户" + userid + "密码修改失败");
                }
                out.flush();

            } catch (SQLException e) {
                try {
                    out.writeUTF("ERROR|数据库操作失败：" + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送数据库错误响应失败：" + ex.getMessage());
                }
            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理请求失败：" + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送异常响应失败：" + ex.getMessage());
                }
            }
        }

     // 添加购物车请求处理方法
        private void processCartRequest() {
            try {
                String jsonReq = in.readUTF();
                CartRequest req = gson.fromJson(jsonReq, CartRequest.class);

                switch (req.getAction()) {
                    case "get":
                        getCartItems(userId);
                        break;
                    case "add":
                        addToCart(userId, req.getProductId(), req.getQuantity());
                        break;
                    case "update":
                        updateCartItem(userId, req.getProductId(), req.getQuantity());
                        break;
                    case "remove":
                        removeFromCart(userId, req.getProductId());
                        break;
                    case "clear":
                        clearCart(userId);
                        break;
                    default:
                        out.writeUTF("ERROR|未知的购物车操作: " + req.getAction());
                        out.flush();
                }
            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理购物车请求失败: " + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送错误响应失败: " + ex.getMessage());
                }
            }
        }


     // 获取购物车商品
        private void getCartItems(String userId) throws SQLException, IOException {
            String sql = "SELECT sc.*, pi.name, pi.price, pi.image_url " +
                         "FROM shopping_cart sc " +
                         "JOIN product_info pi ON sc.product_id = pi.id " +
                         "WHERE sc.user_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                ResultSet rs = pstmt.executeQuery();

                List<CartItem> cartItems = new ArrayList<>();
                while (rs.next()) {
                    // 创建Product对象
                    Product product = new Product(
                        rs.getString("product_id"),
                        rs.getString("name"),
                        "", // brand
                        rs.getDouble("price"),
                        0,  // originalPrice
                        0,  // rating
                        "", // category
                        "", // description
                        0,  // stock
                        null, // expirationDate
                        rs.getString("image_url")
                    );

                    // 创建CartItem对象
                    CartItem item = new CartItem(product, rs.getInt("quantity"));
                    cartItems.add(item);
                }

                // 发送响应
                CartResponse response = new CartResponse(1, "获取成功", cartItems);
                String jsonRsp = gson.toJson(response);
                out.writeUTF("SUCCESS|" + jsonRsp);
                out.flush();
            }
        }



     // 添加到购物车
        private void addToCart(String userId, String productId, int quantity) throws SQLException, IOException {
            String sql = "INSERT INTO shopping_cart (user_id, product_id, quantity) " +
                         "VALUES (?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE quantity = quantity + ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, productId);
                pstmt.setInt(3, quantity);
                pstmt.setInt(4, quantity);
                pstmt.executeUpdate();

                out.writeUTF("SUCCESS|添加成功");
                out.flush();
            }
        }

        // 更新购物车商品数量
        private void updateCartItem(String userId, String productId, int quantity) throws SQLException, IOException {
            if (quantity <= 0) {
                removeFromCart(userId, productId);
                return;
            }

            String sql = "UPDATE shopping_cart SET quantity = ? WHERE user_id = ? AND product_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, quantity);
                pstmt.setString(2, userId);
                pstmt.setString(3, productId);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    out.writeUTF("SUCCESS|更新成功");
                } else {
                    out.writeUTF("ERROR|商品不存在");
                }
                out.flush();
            }
        }

        // 从购物车移除商品
        private void removeFromCart(String userId, String productId) throws SQLException, IOException {
            String sql = "DELETE FROM shopping_cart WHERE user_id = ? AND product_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, productId);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    out.writeUTF("SUCCESS|移除成功");
                } else {
                    out.writeUTF("ERROR|商品不存在");
                }
                out.flush();
            }
        }

        // 清空购物车
        private void clearCart(String userId) throws SQLException {
            String sql = "DELETE FROM shopping_cart WHERE user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.executeUpdate();
            }
        }


        // 处理结算请求
     // 处理结算请求
        private void processCheckoutRequest() {
            try {
                // 读取客户端结算请求
                String jsonReq = in.readUTF();
                Type checkoutType = new TypeToken<CheckoutRequest>() {}.getType();
                CheckoutRequest req = gson.fromJson(jsonReq, checkoutType);
                System.out.println("【服务器】收到结算请求: " + jsonReq);

                // 生成订单号
                String orderId = "ORD" + System.currentTimeMillis();
                conn.setAutoCommit(false);

                try {
                    // 插入主订单记录
                    String orderSql = "INSERT INTO orders (order_id, user_id, total_amount, status) VALUES (?, ?, ?, 'pending')";
                    try (PreparedStatement pstmt = conn.prepareStatement(orderSql)) {
                        pstmt.setString(1, orderId);
                        pstmt.setString(2, userId);
                        pstmt.setDouble(3, req.getTotalAmount());
                        pstmt.executeUpdate();
                    }

                    // 插入订单项记录
                    String itemSql = "INSERT INTO order_items (order_id, product_id, quantity, price, subtotal) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(itemSql)) {
                        for (CartItem item : req.getItems()) {
                            pstmt.setString(1, orderId);
                            pstmt.setString(2, item.getProduct().getId());
                            pstmt.setInt(3, item.getQuantity());
                            pstmt.setDouble(4, item.getProduct().getPrice());
                            pstmt.setDouble(5, item.getSubtotal());
                            pstmt.addBatch();
                        }
                        pstmt.executeBatch();
                    }

                    // 清空购物车（仅数据库操作，不发送响应）
                    clearCart(userId);
                    conn.commit();

                    // 发送唯一成功响应
                    CheckoutResponse successResp = new CheckoutResponse(1, "订单创建成功", orderId);
                    String successJson = gson.toJson(successResp);
                    out.writeUTF("SUCCESS|" + successJson);
                    out.flush();
                    System.out.println("【服务器】结算成功，响应: " + "SUCCESS|" + successJson);

                } catch (SQLException e) {
                    conn.rollback();
                    String errorMsg = "数据库错误: " + e.getMessage();
                    out.writeUTF("ERROR|" + errorMsg);
                    out.flush();
                    System.err.println("【服务器】结算失败: " + errorMsg);
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (IOException e) {
                System.err.println("【服务器】IO错误: " + e.getMessage());
                try {
                    out.writeUTF("ERROR|网络错误，无法处理结算请求");
                    out.flush();
                } catch (IOException ex) {
                    System.err.println("【服务器】发送错误响应失败: " + ex.getMessage());
                }
            } catch (Exception e) {
                String errorMsg = "未知错误: " + e.getMessage();
                try {
                    out.writeUTF("ERROR|" + errorMsg);
                    out.flush();
                } catch (IOException ex) {
                    System.err.println("【服务器】发送错误响应失败: " + ex.getMessage());
                }
                System.err.println("【服务器】结算异常: " + errorMsg);
            }
        }
     // 添加订单历史请求处理方法
        private void processOrderHistoryRequest() {
            try {
                String jsonReq = in.readUTF();
                OrderHistoryRequest req = gson.fromJson(jsonReq, OrderHistoryRequest.class);

                System.out.println("[Debug] 收到订单历史请求: " + jsonReq + ", 用户ID: " + userId);

                // 获取用户类型
                int userType = getUserType(userId);
                List<Order> orders;

                // 如果是管理员（假设管理员类型为2），获取所有订单
                if (userType == 5) {
                    orders = getAllOrderHistory(req.getStatusFilter());
                } else {
                    // 普通用户获取自己的订单
                    orders = getOrderHistory(userId, req.getStatusFilter());
                }

                // 发送响应
                OrderHistoryResponse response = new OrderHistoryResponse(1, "获取成功", orders);
                String jsonRsp = gson.toJson(response);
                out.writeUTF("SUCCESS|" + jsonRsp);
                out.flush();

            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理订单历史请求失败: " + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送错误响应失败: " + ex.getMessage());
                }
            }
        }

        // 添加订单详情请求处理方法
        private void processOrderDetailRequest() {
            try {
                String jsonReq = in.readUTF();
                OrderDetailRequest req = gson.fromJson(jsonReq, OrderDetailRequest.class);

                System.out.println("[Debug] 收到订单详情请求: " + jsonReq + ", 用户ID: " + userId);

                // 获取订单详情
                Order order = getOrderDetail(userId, req.getOrderId());

                // 发送响应
                if (order != null) {
                    OrderDetailResponse response = new OrderDetailResponse(1, "获取成功", order);
                    String jsonRsp = gson.toJson(response);
                    out.writeUTF("SUCCESS|" + jsonRsp);
                } else {
                    out.writeUTF("ERROR|订单不存在");
                }
                out.flush();

            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理订单详情请求失败: " + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送错误响应失败: " + ex.getMessage());
                }
            }
        }

     // 处理交易记录请求
        private void processTransactionHistoryRequest() {
            try {
                String jsonReq = in.readUTF();
                TransactionRequest req = gson.fromJson(jsonReq, TransactionRequest.class);

                // 获取当前用户类型
                int userType = getUserType(userId);
                List<Transaction> transactions;

                if (userType == 5) { // 管理员，获取所有交易记录
                    transactions = getAllTransactionsFromDB();
                } else { // 普通用户，获取自己的交易记录
                    // 注意：这里忽略请求中的userId，使用当前登录用户的userId，防止越权
                    transactions = getTransactionsFromDB(userId);
                }

                // 构建响应
                TransactionResponse response = new TransactionResponse();
                response.setTransactions(transactions);
                String jsonRsp = gson.toJson(response);
                out.writeUTF("SUCCESS|" + jsonRsp);
                out.flush();

            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|获取交易记录失败: " + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送交易记录响应失败: " + ex.getMessage());
                }
            }
        }

     // 处理支付请求
        private void processPaymentRequest() {
            try {
                String jsonReq = in.readUTF();
                PaymentRequest req = gson.fromJson(jsonReq, PaymentRequest.class);

                System.out.println("[Debug] 收到支付请求: " + jsonReq + ", 用户ID: " + userId);

                // 开始事务
                conn.setAutoCommit(false);

                try {
                    // 1. 验证订单状态和金额
                    String checkOrderSql = "SELECT total_amount, status FROM orders WHERE order_id = ? AND user_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(checkOrderSql)) {
                        pstmt.setString(1, req.getOrderId());
                        pstmt.setString(2, userId);
                        ResultSet rs = pstmt.executeQuery();

                        if (!rs.next()) {
                            throw new Exception("订单不存在");
                        }

                        double orderAmount = rs.getDouble("total_amount");
                        String orderStatus = rs.getString("status");

                        if (!"pending".equals(orderStatus)) {
                            throw new Exception("订单状态不正确，无法支付");
                        }

                        if (Math.abs(orderAmount - req.getAmount()) > 0.01) {
                            throw new Exception("支付金额与订单金额不符");
                        }
                    }

                    // 2. 更新订单状态为已支付
                    String updateOrderSql = "UPDATE orders SET status = 'paid' WHERE order_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateOrderSql)) {
                        pstmt.setString(1, req.getOrderId());
                        pstmt.executeUpdate();
                    }

                    // 3. 创建交易记录
                    String transactionId = "T" + System.currentTimeMillis();
                    String insertTransactionSql = "INSERT INTO transaction (id, user_id, date, category, amount, type, description, status) " +
                                                 "VALUES (?, ?, NOW(), ?, ?, 'expense', ?, 'success')";

                    try (PreparedStatement pstmt = conn.prepareStatement(insertTransactionSql)) {
                        pstmt.setString(1, transactionId);
                        pstmt.setString(2, userId);
                        pstmt.setString(3, req.getPaymentMethod());
                        pstmt.setDouble(4, req.getAmount());
                        pstmt.setString(5, "订单号: " + req.getOrderId() + " (" + req.getPaymentMethod() + ")");
                        pstmt.executeUpdate();
                    }

                    // 提交事务
                    conn.commit();

                    // 发送成功响应
                    PaymentResponse response = new PaymentResponse(1, "支付成功", transactionId);
                    String jsonRsp = gson.toJson(response);
                    out.writeUTF("SUCCESS|" + jsonRsp);
                    out.flush();

                    System.out.println("[Event] 订单 " + req.getOrderId() + " 支付成功，交易记录ID: " + transactionId);

                } catch (Exception e) {
                    // 回滚事务
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (Exception e) {
                try {
                    String errorMsg = "支付失败: " + e.getMessage();
                    System.out.println("[Error] " + errorMsg);
                    PaymentResponse response = new PaymentResponse(0, errorMsg, null);
                    String jsonRsp = gson.toJson(response);
                    out.writeUTF("ERROR|" + jsonRsp);
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送错误响应失败: " + ex.getMessage());
                }
            }
        }


     // 处理添加商品请求
        private void processProductAddRequest() {
            try {
                String jsonReq = in.readUTF();
                Product product = gson.fromJson(jsonReq, Product.class);

                System.out.println("[Event]收到添加商品请求 - 商品ID: " + product.getId());

                // 插入商品到数据库
                boolean success = addProductToDB(product);

                if (success) {
                    out.writeUTF("SUCCESS|商品添加成功");
                    System.out.println("[Event]商品" + product.getId() + "添加成功");
                } else {
                    out.writeUTF("ERROR|商品添加失败");
                    System.out.println("[Event]商品" + product.getId() + "添加失败");
                }
                out.flush();

            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理添加商品请求失败: " + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送错误响应失败: " + ex.getMessage());
                }
            }
        }

        // 处理更新商品请求
        private void processProductUpdateRequest() {
            try {
                String jsonReq = in.readUTF();
                Product product = gson.fromJson(jsonReq, Product.class);

                System.out.println("[Event]收到更新商品请求 - 商品ID: " + product.getId());

                // 更新商品到数据库
                boolean success = updateProductInDB(product);

                if (success) {
                    out.writeUTF("SUCCESS|商品更新成功");
                    System.out.println("[Event]商品" + product.getId() + "更新成功");
                } else {
                    out.writeUTF("ERROR|商品更新失败");
                    System.out.println("[Event]商品" + product.getId() + "更新失败");
                }
                out.flush();

            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理更新商品请求失败: " + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送错误响应失败: " + ex.getMessage());
                }
            }
        }

     // 添加处理支付方式统计请求的方法
        private void processPaymentMethodStatsRequest() {
            try {
                String jsonReq = in.readUTF();
                Map<String, String> request = gson.fromJson(jsonReq, 
                    new TypeToken<Map<String, String>>(){}.getType());
                
                String startDate = request.get("startDate");
                String endDate = request.get("endDate");
                
                // 从数据库获取支付方式统计
                Map<String, Double> paymentStats = getPaymentMethodStatsFromDB(startDate, endDate);
                
                // 发送响应
                String jsonRsp = gson.toJson(paymentStats);
                out.writeUTF("SUCCESS|" + jsonRsp);
                out.flush();
                
            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理支付方式统计请求失败: " + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送错误响应失败: " + ex.getMessage());
                }
            }
        }

        // 从数据库获取支付方式统计的方法
        private Map<String, Double> getPaymentMethodStatsFromDB(String startDate, String endDate) throws SQLException {
            Map<String, Double> paymentStats = new HashMap<>();
            
            String sql = "SELECT category, SUM(amount) as total_amount " +
                         "FROM transaction " +
                         "WHERE date >= ? AND date <= ?" +
                         "GROUP BY category";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, startDate + " 00:00:00");
                pstmt.setString(2, endDate + " 23:59:59");
                
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String paymentMethod = rs.getString("category");
                    double totalAmount = rs.getDouble("total_amount");
                    paymentStats.put(paymentMethod, totalAmount);
                }
            }
            
            return paymentStats;
        }
        
        
        // 处理删除商品请求
        private void processProductDeleteRequest() {
            try {
                String jsonReq = in.readUTF();
                ProductDeleteRequest request = gson.fromJson(jsonReq, ProductDeleteRequest.class);

                System.out.println("[Event]收到删除商品请求 - 商品ID: " + request.getProductId());

                // 从数据库删除商品
                boolean success = deleteProductFromDB(request.getProductId());

                if (success) {
                    out.writeUTF("SUCCESS|商品删除成功");
                    System.out.println("[Event]商品" + request.getProductId() + "删除成功");
                } else {
                    out.writeUTF("ERROR|商品删除失败");
                    System.out.println("[Event]商品" + request.getProductId() + "删除失败");
                }
                out.flush();

            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理删除商品请求失败: " + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送错误响应失败: " + ex.getMessage());
                }
            }
        }

        // 处理订单状态更新请求
        private void processOrderStatusUpdateRequest() {
            try {
                String jsonReq = in.readUTF();
                OrderStatusUpdateRequest request = gson.fromJson(jsonReq, OrderStatusUpdateRequest.class);

                System.out.println("[Event]收到订单状态更新请求 - 订单ID: " + request.getOrderId() + ", 状态: " + request.getStatus());

                // 更新订单状态
                boolean success = updateOrderStatusInDB(request.getOrderId(), request.getStatus());

                if (success) {
                    out.writeUTF("SUCCESS|订单状态更新成功");
                    System.out.println("[Event]订单" + request.getOrderId() + "状态更新成功");
                } else {
                    out.writeUTF("ERROR|订单状态更新失败");
                    System.out.println("[Event]订单" + request.getOrderId() + "状态更新失败");
                }
                out.flush();

            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理订单状态更新请求失败: " + e.getMessage());
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送错误响应失败: " + ex.getMessage());
                }
            }
        }

     // 处理退换宿舍申请列表请求
        private void processExchangeListRequest() {
            try {
                // 读取请求参数
                String jsonReq = in.readUTF();
                JsonObject request = gson.fromJson(jsonReq, JsonObject.class);
                int exchangeType = request.get("exchangeType").getAsInt(); // 0:退宿, 1:换宿

                // 从数据库获取申请列表
                List<DormitoryExchangeApplication> applications = getExchangeApplicationsFromDB(exchangeType);

                // 发送响应
                String json = gson.toJson(applications);
                out.writeUTF("SUCCESS|" + json);
                System.out.println("[Event]退换宿舍申请列表查询成功，类型: " + exchangeType);
                out.flush();

            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|获取申请列表失败: " + e.getMessage());
                    out.flush();
                    System.out.println("[Error]获取退换宿舍申请列表错误: " + e.getMessage());
                } catch (IOException ex) {
                    System.out.println("[Error]发送申请列表错误响应失败: " + ex.getMessage());
                }
            }
        }

        // 处理退换宿舍申请状态更新
        private void processExchangeStatusUpdate() {
            try {
                // 读取请求参数
                String jsonReq = in.readUTF();
                JsonObject request = gson.fromJson(jsonReq, JsonObject.class);
                int applicationId = request.get("applicationId").getAsInt();
                int newStatus = request.get("newStatus").getAsInt(); // 1:通过, 2:拒绝
                String feedback = request.get("feedback").getAsString();

                // 更新数据库中的申请状态
                boolean success = updateExchangeApplicationStatus(applicationId, newStatus, feedback);

                // 发送响应
                if (success) {
                    out.writeUTF("SUCCESS|申请状态更新成功");
                    System.out.println("[Event]申请ID " + applicationId + " 状态更新为: " + newStatus);
                } else {
                    out.writeUTF("ERROR|申请状态更新失败");
                    System.out.println("[Event]申请ID " + applicationId + " 状态更新失败");
                }
                out.flush();

            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理申请状态更新失败: " + e.getMessage());
                    out.flush();
                    System.out.println("[Error]处理申请状态更新错误: " + e.getMessage());
                } catch (IOException ex) {
                    System.out.println("[Error]发送状态更新错误响应失败: " + ex.getMessage());
                }
            }
        }

        private List<DormitoryExchangeApplication> getExchangeApplicationsFromDB(int exchangeType) throws SQLException {
            String sql = "SELECT de.*, si.name as student_name " +
                         "FROM dormitory_exchange de " +
                         "LEFT JOIN student_info si ON de.student_id = si.studentid " +
                         "WHERE de.exchange_type = ? ORDER BY de.apply_time DESC";
            List<DormitoryExchangeApplication> applications = new ArrayList<>();

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, exchangeType);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    DormitoryExchangeApplication app = new DormitoryExchangeApplication();
                    app.setId(rs.getInt("id"));
                    app.setStudentId(rs.getString("student_id"));
                    app.setStudentName(rs.getString("student_name"));
                    app.setBuilding(rs.getString("building"));
                    app.setRoom(rs.getString("room"));
                    app.setBed(rs.getString("bed"));
                    app.setExchangeType(rs.getInt("exchange_type"));
                    app.setReason(rs.getString("reason"));
                    app.setApplyTime(rs.getTimestamp("apply_time"));
                    app.setStatus(rs.getInt("status"));
                    app.setFeedback(rs.getString("feedback")); // 添加这一行
                    applications.add(app);
                }
            }

            return applications;
        }

        // 更新退换宿舍申请状态
        private boolean updateExchangeApplicationStatus(int applicationId, int newStatus, String feedback) throws SQLException {
            String sql = "UPDATE dormitory_exchange SET status = ?, feedback = ? WHERE id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, newStatus);
                pstmt.setString(2, feedback);
                pstmt.setInt(3, applicationId);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        }


     // 添加处理维修列表请求的方法
        private void processRepairListRequest() {
            try {
                // 从数据库获取维修列表
                List<RepairInfo> repairList = getRepairListFromDB();

                // 转换为JSON
                String json = gson.toJson(repairList);
                out.writeUTF("SUCCESS|" + json);
                out.flush();
                System.out.println("[Event]维修列表查询成功，返回" + repairList.size() + "条记录");
            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|获取维修列表失败: " + e.getMessage());
                    out.flush();
                    System.out.println("[Error]获取维修列表错误: " + e.getMessage());
                } catch (IOException ex) {
                    System.out.println("[Error]发送维修列表错误响应失败: " + ex.getMessage());
                }
            }
        }

        // 添加处理维修状态更新请求的方法
        private void processProcessRepairRequest() {
            try {
                // 读取维修ID
                String repairIdStr = in.readUTF();
                int repairId = Integer.parseInt(repairIdStr);

                // 更新数据库中的维修状态
                boolean success = updateRepairStatus(repairId);

                if (success) {
                    out.writeUTF("SUCCESS|维修状态更新成功");
                    System.out.println("[Event]维修ID " + repairId + " 状态更新成功");
                } else {
                    out.writeUTF("ERROR|维修状态更新失败");
                    System.out.println("[Event]维修ID " + repairId + " 状态更新失败");
                }
                out.flush();
            } catch (Exception e) {
                try {
                    out.writeUTF("ERROR|处理维修状态更新失败: " + e.getMessage());
                    out.flush();
                    System.out.println("[Error]处理维修状态更新错误: " + e.getMessage());
                } catch (IOException ex) {
                    System.out.println("[Error]发送维修状态更新错误响应失败: " + ex.getMessage());
                }
            }
        }

     // 从数据库获取维修列表
        private List<RepairInfo> getRepairListFromDB() throws SQLException {
            String sql = "SELECT id, student_id, building, room, description, repair_time, status FROM dormitory_repair ORDER BY repair_time DESC";
            List<RepairInfo> list = new ArrayList<>();

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    RepairInfo info = new RepairInfo();
                    info.setId(rs.getInt("id"));
                    info.setStudentId(rs.getString("student_id"));  // 设置studentId
                    info.setDormBuilding(rs.getString("building"));
                    info.setDormRoom(rs.getString("room"));
                    info.setDescription(rs.getString("description"));

                    // 转换状态：0->待处理，1->已处理
                    int status = rs.getInt("status");
                    info.setStatus(status == 0 ? "待处理" : "已处理");

                    // 格式化日期
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    info.setDate(dateFormat.format(rs.getTimestamp("repair_time")));

                    list.add(info);
                }
            }

            return list;
        }
        // 更新维修状态
        private boolean updateRepairStatus(int repairId) throws SQLException {
            String sql = "UPDATE dormitory_repair SET status = 1 WHERE id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, repairId);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        }
     // 在Server.java的ClientHandler类中添加以下方法

     // 处理管理员水电费列表请求
     private void processAdminBillListRequest() {
         try {
             // 读取筛选条件
             String jsonReq = in.readUTF();
             JsonObject filters = gson.fromJson(jsonReq, JsonObject.class);

             String building = filters.has("building") ? filters.get("building").getAsString() : null;
             String room = filters.has("room") ? filters.get("room").getAsString() : null;
             String month = filters.has("month") ? filters.get("month").getAsString() : null;
             String status = filters.has("status") ? filters.get("status").getAsString() : null;
             String reminderStatus = filters.has("reminderStatus") ? filters.get("reminderStatus").getAsString() : null;

             // 从数据库获取账单数据
             List<AdminBillRecord> billRecords = getAdminBillRecordsFromDB(building, room, month, status, reminderStatus);

             // 发送响应
             String json = gson.toJson(billRecords);
             out.writeUTF("SUCCESS|" + json);
             System.out.println("[Event]管理员水电费列表查询成功，返回" + billRecords.size() + "条记录");
             out.flush();

         } catch (Exception e) {
             try {
                 out.writeUTF("ERROR|获取水电费列表失败: " + e.getMessage());
                 out.flush();
                 System.out.println("[Error]获取水电费列表错误: " + e.getMessage());
             } catch (IOException ex) {
                 System.out.println("[Error]发送水电费列表错误响应失败: " + ex.getMessage());
             }
         }
     }

     // 处理管理员催缴请求
     private void processAdminRemindRequest() {
         try {
             // 读取催缴请求
             String jsonReq = in.readUTF();
             JsonObject request = gson.fromJson(jsonReq, JsonObject.class);

             String building = request.get("building").getAsString();
             String room = request.get("room").getAsString();
             String month = request.get("month").getAsString();

             // 更新数据库中的催缴状态
             boolean success = updateBillReminderStatus(building, room, month);

             if (success) {
                 out.writeUTF("SUCCESS|催缴通知发送成功");
                 System.out.println("[Event]宿舍" + building + "-" + room + "的" + month + "账单催缴成功");
             } else {
                 out.writeUTF("ERROR|催缴通知发送失败");
                 System.out.println("[Event]宿舍" + building + "-" + room + "的" + month + "账单催缴失败");
             }
             out.flush();

         } catch (Exception e) {
             try {
                 out.writeUTF("ERROR|处理催缴请求失败: " + e.getMessage());
                 out.flush();
                 System.out.println("[Error]处理催缴请求错误: " + e.getMessage());
             } catch (IOException ex) {
                 System.out.println("[Error]发送催缴错误响应失败: " + ex.getMessage());
             }
         }
     }

     // 从数据库获取管理员账单记录
     private List<AdminBillRecord> getAdminBillRecordsFromDB(String buildingFilter, String roomFilter,
                                                            String monthFilter, String statusFilter,
                                                            String reminderStatusFilter) throws SQLException {
         StringBuilder sql = new StringBuilder("SELECT * FROM current_dormitory_bill WHERE 1=1");
         List<Object> params = new ArrayList<>();

         if (buildingFilter != null && !buildingFilter.equals("全部")) {
             sql.append(" AND building = ?");
             params.add(buildingFilter);
         }

         if (roomFilter != null && !roomFilter.isEmpty()) {
             sql.append(" AND room = ?");
             params.add(roomFilter);
         }

         if (monthFilter != null && !monthFilter.equals("全部")) {
             sql.append(" AND month = ?");
             params.add(monthFilter);
         }

         if (statusFilter != null && !statusFilter.equals("全部")) {
             sql.append(" AND status = ?");
             params.add(statusFilter.equals("未缴费") ? "未缴费" : "已缴费");
         }

         if (reminderStatusFilter != null && !reminderStatusFilter.equals("全部")) {
             if (reminderStatusFilter.equals("未催缴")) {
                 sql.append(" AND (reminder_status IS NULL OR reminder_status = '未催缴')");
             } else {
                 sql.append(" AND reminder_status = '已催缴'");
             }
         }

         List<AdminBillRecord> records = new ArrayList<>();

         try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
             for (int i = 0; i < params.size(); i++) {
                 pstmt.setObject(i + 1, params.get(i));
             }

             ResultSet rs = pstmt.executeQuery();

             while (rs.next()) {
                 AdminBillRecord record = new AdminBillRecord();
                 record.setBuilding(rs.getString("building"));
                 record.setRoom(rs.getString("room"));
                 record.setMonth(rs.getString("month"));
                 record.setElectricityFee(rs.getDouble("electricity_fee"));
                 record.setWaterFee(rs.getDouble("water_fee"));
                 record.setTotalFee(rs.getDouble("electricity_fee") + rs.getDouble("water_fee"));
                 record.setStatus(rs.getString("status"));
                 record.setDeadline(rs.getString("deadline"));
                 record.setReminderStatus(rs.getString("reminder_status"));
                 record.setLastReminderTime(rs.getString("last_reminder_time"));

                 records.add(record);
             }
         }

         return records;
     }

     // 更新账单催缴状态
     private boolean updateBillReminderStatus(String building, String room, String month) throws SQLException {
         String sql = "UPDATE current_dormitory_bill SET reminder_status = '已催缴', last_reminder_time = NOW() " +
                      "WHERE building = ? AND room = ? AND month = ?";

         try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
             pstmt.setString(1, building);
             pstmt.setString(2, room);
             pstmt.setString(3, month);

             int rowsAffected = pstmt.executeUpdate();
             return rowsAffected > 0;
         }
     }

  // 处理退换宿舍请求
     private void processDormitoryExchangeRequest() {
         try {
             // 读取DormitoryExchangeRequest的JSON数据
             String jsonReq = in.readUTF();
             DormitoryExchangeRequest req = gson.fromJson(jsonReq, DormitoryExchangeRequest.class);
             String userid = req.getUserid();
             int exchangeType = req.getExchangeType();
             String reason = req.getReason();

             System.out.println("[Event]收到退换宿舍请求 - 学号: " + userid + ", 类型: " +
                               (exchangeType == 0 ? "退宿" : "换宿"));

             // 获取学生宿舍信息
             StudentInfo studentInfo = getStudentInfoFromDB(userid);
             if (studentInfo == null) {
                 out.writeUTF("ERROR|未找到学号为" + userid + "的信息");
                 out.flush();
                 return;
             }

             String building = studentInfo.getDormitoryBuilding();
             String room = studentInfo.getDormitoryRoom();
             String bed = studentInfo.getDormitoryBed();

             // 检查宿舍信息是否完整
             if (building == null || room == null || bed == null ||
                 building.isEmpty() || room.isEmpty() || bed.isEmpty()) {
                 out.writeUTF("ERROR|学号" + userid + "没有宿舍信息，无法申请");
                 out.flush();
                 return;
             }

             // 插入申请记录到数据库
             boolean success = insertDormitoryExchangeRequest(userid, building, room, bed, exchangeType, reason);

             // 发送响应
             if (success) {
                 out.writeUTF("SUCCESS|申请提交成功");
                 System.out.println("[Event]学号" + userid + "的退换宿舍申请提交成功");
             } else {
                 out.writeUTF("ERROR|申请提交失败，请稍后重试");
                 System.out.println("[Event]学号" + userid + "的退换宿舍申请提交失败");
             }
             out.flush();

         } catch (Exception e) {
             try {
                 out.writeUTF("ERROR|处理退换宿舍请求失败：" + e.getMessage());
                 out.flush();
                 System.out.println("[Error]处理退换宿舍请求错误: " + e.getMessage());
             } catch (IOException ex) {
                 System.out.println("[Error]发送退换宿舍错误响应失败：" + ex.getMessage());
             }
         }
     }
  //处理维修请求
     private void processRepairRequest() {
         try {
             // 读取RepairRequest的JSON数据
             String jsonReq = in.readUTF();
             RepairRequest req = gson.fromJson(jsonReq, RepairRequest.class);
             String userid = req.getUserid();
             String description = req.getDescription();

             System.out.println("[Event]收到报修请求 - 学号: " + userid);

             // 获取学生宿舍信息
             StudentInfo studentInfo = getStudentInfoFromDB(userid);
             if (studentInfo == null) {
                 out.writeUTF("ERROR|未找到学号为" + userid + "的信息");
                 out.flush();
                 return;
             }

             String building = studentInfo.getDormitoryBuilding();
             String room = studentInfo.getDormitoryRoom();

             // 检查宿舍信息是否完整
             if (building == null || room == null || building.isEmpty() || room.isEmpty()) {
                 out.writeUTF("ERROR|学号" + userid + "没有宿舍信息，无法报修");
                 out.flush();
                 return;
             }

             // 插入报修记录到数据库
             boolean success = insertRepairRequest(userid, building, room, description);

             // 发送响应
             if (success) {
                 out.writeUTF("SUCCESS|报修申请提交成功");
                 System.out.println("[Event]学号" + userid + "的报修申请提交成功");
             } else {
                 out.writeUTF("ERROR|报修申请提交失败，请稍后重试");
                 System.out.println("[Event]学号" + userid + "的报修申请提交失败");
             }
             out.flush();

         } catch (Exception e) {
             try {
                 out.writeUTF("ERROR|处理报修请求失败：" + e.getMessage());
                 out.flush();
                 System.out.println("[Error]处理报修请求错误: " + e.getMessage());
             } catch (IOException ex) {
                 System.out.println("[Error]发送报修错误响应失败：" + ex.getMessage());
             }
         }
     }
  // 插入报修请求到数据库（处理student_id类型转换）
     private boolean insertRepairRequest(String studentId, String building, String room, String description) throws SQLException {
         try {
             int studentIdInt = Integer.parseInt(studentId);
             String sql = "INSERT INTO dormitory_repair (student_id, building, room, description) VALUES (?, ?, ?, ?)";
             try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                 pstmt.setInt(1, studentIdInt);
                 pstmt.setString(2, building);
                 pstmt.setString(3, room);
                 pstmt.setString(4, description);
                 int rowsAffected = pstmt.executeUpdate();
                 return rowsAffected > 0;
             }
         } catch (NumberFormatException e) {
             System.out.println("[Error]学生ID不是有效的数字: " + studentId);
             return false;
         }
     }
  // 插入退换宿舍申请到数据库（处理student_id类型转换）
     private boolean insertDormitoryExchangeRequest(String studentId, String building, String room, String bed,
                                                   int exchangeType, String reason) throws SQLException {
         try {
             int studentIdInt = Integer.parseInt(studentId);
             String sql = "INSERT INTO dormitory_exchange (student_id, building, room, bed, exchange_type, reason) VALUES (?, ?, ?, ?, ?, ?)";
             try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                 pstmt.setInt(1, studentIdInt);
                 pstmt.setString(2, building);
                 pstmt.setString(3, room);
                 pstmt.setString(4, bed);
                 pstmt.setInt(5, exchangeType);
                 pstmt.setString(6, reason);
                 int rowsAffected = pstmt.executeUpdate();
                 return rowsAffected > 0;
             }
         } catch (NumberFormatException e) {
             System.out.println("[Error]学生ID不是有效的数字: " + studentId);
             return false;
         }
     }
     private void processAdminDormitoryInfoRequest() {
         try {
             // 读取请求参数
             String jsonReq = in.readUTF();
             JsonObject request = gson.fromJson(jsonReq, JsonObject.class);
             String building = request.get("building").getAsString();
             String room = request.get("room").getAsString();

             System.out.println("[Event]收到管理员宿舍信息请求 - 楼栋: " + building + ", 房间: " + room);

             // 查询宿舍学生信息
             List<StudentInfo> students = getDormitoryStudentsFromDB(building, room);

             // 查询宿舍卫生评分记录
             List<HygieneRecord> hygieneRecords = getDormitoryHygieneRecords(building, room);

             // 计算平均分
             double avgScore = calculateAverageScore(hygieneRecords);

             // 创建响应对象
             AdminDormitoryInfoResponse response = new AdminDormitoryInfoResponse();
             response.setStatus(1);
             response.setMessage("成功");
             response.setStudents(students);
             response.setHygieneRecords(hygieneRecords);
             response.setAverageScore(avgScore);
             response.setCapacity(4); // 假设默认4人间
             response.setCurrentCount(students.size());

             // 发送响应
             String json = gson.toJson(response);
             out.writeUTF("SUCCESS|" + json);
             System.out.println("[Event]宿舍" + building + "-" + room + "信息查询成功");
             out.flush();

         } catch (Exception e) {
             try {
                 out.writeUTF("ERROR|处理宿舍信息请求失败: " + e.getMessage());
                 out.flush();
                 System.out.println("[Error]处理宿舍信息请求错误: " + e.getMessage());
             } catch (IOException ex) {
                 System.out.println("[Error]发送宿舍信息错误响应失败: " + ex.getMessage());
             }
         }
     }
     private List<StudentInfo> getDormitoryStudentsFromDB(String building, String room) throws SQLException {
         String sql = "SELECT studentid, name, gender, college, major, dormitory_bed, phone, " +
                      "dormitory_building, dormitory_room " +
                      "FROM student_info WHERE dormitory_building = ? AND dormitory_room = ?";
         List<StudentInfo> students = new ArrayList<>();

         try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
             pstmt.setString(1, building);
             pstmt.setString(2, room);
             ResultSet rs = pstmt.executeQuery();

             while (rs.next()) {
                 StudentInfo student = new StudentInfo();
                 student.setUserid(rs.getString("studentid"));
                 student.setName(rs.getString("name"));
                 student.setGender(rs.getString("gender"));
                 student.setCollege(rs.getString("college"));
                 student.setMajor(rs.getString("major"));
                 student.setDormitoryBed(rs.getString("dormitory_bed"));
                 student.setPhone(rs.getString("phone"));
                 student.setDormitoryBuilding(rs.getString("dormitory_building"));
                 student.setDormitoryRoom(rs.getString("dormitory_room"));
                 students.add(student);
             }
         }

         return students;
     }

     // 计算平均分
     private double calculateAverageScore(List<HygieneRecord> records) {
         if (records == null || records.isEmpty()) {
             return 0;
         }

         int sum = 0;
         for (HygieneRecord record : records) {
             sum += record.getScore();
         }

         return (double) sum / records.size();
     }

     private List<HygieneRecord> getDormitoryHygieneRecords(String building, String room) throws SQLException {
         String sql = "SELECT score, check_date, inspector, remark FROM dormitory_hygiene " +
                      "WHERE building = ? AND room = ? ORDER BY check_date DESC";
         List<HygieneRecord> records = new ArrayList<>();

         try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
             pstmt.setString(1, building);
             pstmt.setString(2, room);
             ResultSet rs = pstmt.executeQuery();

             while (rs.next()) {
                 HygieneRecord record = new HygieneRecord();
                 record.setScore(rs.getInt("score"));

                 // 格式化日期为字符串
                 SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                 record.setDate(dateFormat.format(rs.getDate("check_date")));

                 record.setInspector(rs.getString("inspector"));
                 record.setRemark(rs.getString("remark"));
                 records.add(record);
             }
         }

         return records;
     }
     private void processDormitoryInfoRequest() {
         try {
             // 读取DormitoryInfoRequest的JSON数据
             String jsonReq = in.readUTF();
             System.out.println("[Debug]收到宿舍信息请求JSON: " + jsonReq);

             DormitoryInfoRequest req = gson.fromJson(jsonReq, DormitoryInfoRequest.class);

             if (req == null || req.getUserid() == null) {
                 out.writeUTF("ERROR|无效的请求数据");
                 out.flush();
                 System.out.println("[Error]宿舍信息请求数据无效");
                 return;
             }

             String userid = req.getUserid();
             System.out.println("[Event]收到宿舍信息请求 - 学号: " + userid);

             // 查询数据库获取学生信息和舍友信息
             StudentInfo studentInfo = getStudentInfoFromDB(userid);
             List<StudentInfo> roommates = getRoommatesFromDB(userid);

             // 查询宿舍卫生评分记录
             List<HygieneRecord> hygieneRecords = new ArrayList<>();
             if (studentInfo != null && studentInfo.getDormitoryBuilding() != null &&
                 studentInfo.getDormitoryRoom() != null) {
                 System.out.println("[Debug]查询卫生记录 - 楼栋: " + studentInfo.getDormitoryBuilding() +
                                   ", 房间: " + studentInfo.getDormitoryRoom());
                 hygieneRecords = getDormitoryHygieneRecords(
                     studentInfo.getDormitoryBuilding(),
                     studentInfo.getDormitoryRoom()
                 );
                 System.out.println("[Debug]找到 " + hygieneRecords.size() + " 条卫生记录");
             } else {
                 System.out.println("[Debug]学生宿舍信息不完整，无法查询卫生记录");
             }

             // 发送响应
             if (studentInfo != null) {
                 DormitoryInfoResponse response = new DormitoryInfoResponse(
                     1, "成功", studentInfo, roommates, hygieneRecords);
                 String json = gson.toJson(response);
                 out.writeUTF("SUCCESS|" + json);
                 System.out.println("[Event]学号" + userid + "宿舍信息查询成功");
             } else {
                 out.writeUTF("ERROR|未找到学号为" + userid + "的信息");
                 System.out.println("[Event]学号" + userid + "宿舍信息查询失败");
             }
             out.flush();

         } catch (JsonSyntaxException e) {
             try {
                 out.writeUTF("ERROR|JSON格式错误：" + e.getMessage());
                 out.flush();
                 System.out.println("[Error]JSON解析错误: " + e.getMessage());
             } catch (IOException ex) {
                 System.out.println("[Error]发送JSON错误响应失败：" + ex.getMessage());
             }
         } catch (SQLException e) {
             try {
                 out.writeUTF("ERROR|数据库查询失败：" + e.getMessage());
                 out.flush();
                 System.out.println("[Error]数据库查询错误: " + e.getMessage());
             } catch (IOException ex) {
                 System.out.println("[Error]发送数据库错误响应失败：" + ex.getMessage());
             }
         } catch (Exception e) {
             try {
                 out.writeUTF("ERROR|处理请求失败：" + e.getMessage());
                 out.flush();
                 System.out.println("[Error]处理请求错误: " + e.getMessage());
             } catch (IOException ex) {
                 System.out.println("[Error]发送异常响应失败：" + ex.getMessage());
             }
         }
     }
     //水电费
     private void processElectricityWaterBillRequest() {
         try {
             String jsonReq = in.readUTF();
             ElectricityWaterBillRequest req = gson.fromJson(jsonReq, ElectricityWaterBillRequest.class);
             String userid = req.getUserid();

             System.out.println("[Event]收到水电费请求 - 学号: " + userid);

             // 获取学生宿舍信息
             StudentInfo studentInfo = getStudentInfoFromDB(userid);
             if (studentInfo == null) {
                 out.writeUTF("ERROR|未找到学号为" + userid + "的信息");
                 out.flush();
                 return;
             }

             String building = studentInfo.getDormitoryBuilding();
             String room = studentInfo.getDormitoryRoom();

             if (building == null || room == null || building.isEmpty() || room.isEmpty()) {
                 out.writeUTF("ERROR|学号" + userid + "没有宿舍信息");
                 out.flush();
                 return;
             }

             // 获取当前费用信息 - 这是在实例方法中调用实例方法，是正确的
             CurrentBill currentBill = getCurrentBillFromDB(building, room);
             // 获取历史缴费记录 - 这是在实例方法中调用实例方法，是正确的
             List<HistoryBill> historyBills = getHistoryBillsFromDB(building, room);

             // 创建响应
             ElectricityWaterBillResponse response = new ElectricityWaterBillResponse(
                 1, "成功", building, room, currentBill, historyBills
             );

             String json = gson.toJson(response);
             out.writeUTF("SUCCESS|" + json);
             System.out.println("[Event]宿舍" + building + "-" + room + "水电费查询成功");
             out.flush();

         } catch (Exception e) {
             try {
                 out.writeUTF("ERROR|处理水电费请求失败：" + e.getMessage());
                 out.flush();
                 System.out.println("[Error]处理水电费请求错误: " + e.getMessage());
             } catch (IOException ex) {
                 System.out.println("[Error]发送水电费错误响应失败：" + ex.getMessage());
             }
         }
     }

  // 从数据库获取当前费用信息
     private CurrentBill getCurrentBillFromDB(String building, String room) throws SQLException {
         String sql = "SELECT * FROM current_dormitory_bill WHERE building = ? AND room = ?";
         try (PreparedStatement pstmt = this.conn.prepareStatement(sql)) {
             pstmt.setString(1, building);
             pstmt.setString(2, room);
             ResultSet rs = pstmt.executeQuery();

             if (rs.next()) {
                 // 使用SimpleDateFormat将日期格式化为ISO 8601格式
                 SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");
                 Date deadline = rs.getDate("deadline");
                 String formattedDeadline = deadline != null ? isoFormat.format(deadline) : null;

                 return new CurrentBill(
                     rs.getDouble("electricity_fee"),
                     rs.getDouble("water_fee"),
                     rs.getString("status"),
                     rs.getString("month"),
                     formattedDeadline // 使用格式化后的日期字符串
                 );
             }
             return null;
         }
     }

     // 从数据库获取历史缴费记录
     private List<HistoryBill> getHistoryBillsFromDB(String building, String room) throws SQLException {
         String sql = "SELECT * FROM dormitory_bill_history WHERE building = ? AND room = ? ORDER BY month DESC";
         List<HistoryBill> bills = new ArrayList<>();

         try (PreparedStatement pstmt = this.conn.prepareStatement(sql)) {
             pstmt.setString(1, building);
             pstmt.setString(2, room);
             ResultSet rs = pstmt.executeQuery();

             // 使用SimpleDateFormat将日期格式化为ISO 8601格式
             SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");

             while (rs.next()) {
                 Date payDate = rs.getDate("pay_date");
                 String formattedPayDate = payDate != null ? isoFormat.format(payDate) : null;

                 HistoryBill bill = new HistoryBill(
                     rs.getString("month"),
                     rs.getDouble("electricity_fee"),
                     rs.getDouble("water_fee"),
                     rs.getDouble("total_fee"),
                     rs.getString("status"),
                     formattedPayDate // 使用格式化后的日期字符串
                 );
                 bills.add(bill);
             }
         }
         return bills;
     }

     private List<StudentInfo> getRoommatesFromDB(String userid) throws SQLException {
         // 先获取当前学生的宿舍信息
         String getDormSql = "SELECT dormitory_building, dormitory_room FROM student_info WHERE studentid = ?";
         String building = null;
         String room = null;

         try (PreparedStatement pstmt = conn.prepareStatement(getDormSql)) {
             pstmt.setString(1, userid);
             ResultSet rs = pstmt.executeQuery();
             if (rs.next()) {
                 building = rs.getString("dormitory_building");
                 room = rs.getString("dormitory_room");

                 // 添加空值检查
                 if (rs.wasNull()) {
                     building = null;
                     room = null;
                 }
             }
         }

         if (building == null || room == null || building.isEmpty() || room.isEmpty()) {
             System.out.println("[Debug]学号" + userid + "没有宿舍信息");
             return new ArrayList<>();
         }

         // 查询同一宿舍的其他学生
         String sql = "SELECT * FROM student_info WHERE dormitory_building = ? AND dormitory_room = ? AND studentid != ?";
         List<StudentInfo> roommates = new ArrayList<>();

         try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
             pstmt.setString(1, building);
             pstmt.setString(2, room);
             pstmt.setString(3, userid);
             ResultSet rs = pstmt.executeQuery();

             while (rs.next()) {
                 StudentInfo info = new StudentInfo();
                 info.setUserid(rs.getString("studentid"));
                 info.setName(rs.getString("name"));
                 info.setCollege(rs.getString("college"));
                 info.setMajor(rs.getString("major"));
                 info.setDormitoryBed(rs.getString("dormitory_bed"));
                 info.setPhone(rs.getString("phone"));
                 roommates.add(info);
             }
         }

         System.out.println("[Debug]找到" + roommates.size() + "个舍友");
         return roommates;
     }

        // 验证用户账号密码
        private boolean validateUser(String userid, String password) throws SQLException {
            // SQL查询语句，使用参数化查询防止SQL注入
            String sql = "SELECT password FROM user_auth WHERE userid = ? AND status = 1";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userid);
                System.out.println("[Debug]执行SQL: " + pstmt.toString());
                ResultSet rs = pstmt.executeQuery();

                // 检查账号是否存在
                if (!rs.next()) {
                    return false; // 账号不存在
                }

                // 获取数据库中的密码
                String storedPassword = rs.getString("password");

                // 可以修改成使用哈希验证密码
                return password.equals(storedPassword);
            }
        }

     // 检查用户是否已在线
        private boolean isUserOnline(String userid) throws SQLException {
            String sql = "SELECT online FROM user_auth WHERE userid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userid);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    return rs.getInt("online") == 1;
                }
                return false; // 用户不存在
            }
        }

     // 更新用户在线状态
        private void updateOnlineStatus(String userid, int onlinestatus) throws SQLException {
            String sql = "UPDATE user_auth SET online = ? WHERE userid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, onlinestatus);
                pstmt.setString(2, userid);
                pstmt.executeUpdate();
            }
        }

        //获取用户类型
        private int getUserType(String userid) throws SQLException{
        	String sql = "SELECT usertype FROM user_auth WHERE userid = ? AND status = 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userid);
                ResultSet rs = pstmt.executeQuery();


                // 返回数据库中用户类型
                if (rs.next()) {
                    return rs.getInt("usertype");
                } else {
                    return -1; // 用户不存在或状态不为1
                }
            }
        }

        //获取用户姓名
        private String getUserName(String userid) throws SQLException{
        	String sql = "SELECT username FROM user_auth WHERE userid = ? AND status = 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userid);
                ResultSet rs = pstmt.executeQuery();


                // 返回数据库中用户姓名
                if (rs.next()) {
                    return rs.getString("username");
                } else {
                    return null; // 用户不存在或状态不为1
                }
            }
        }

        //从数据库中获取学生信息方法
        private StudentInfo getStudentInfoFromDB(String userid) throws SQLException {
            String sql = "SELECT * FROM student_info WHERE studentid = ?";
            String studentid=userid;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, studentid);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    StudentInfo info = new StudentInfo();
                    info.setUserid(rs.getString("studentid"));
                    info.setName(rs.getString("name"));
                    info.setGender(rs.getString("gender"));
                    info.setBirth(rs.getString("birth"));
                    info.setIdCard(rs.getString("id_card"));
                    info.setPhone(rs.getString("phone"));
                    info.setEmail(rs.getString("email"));
                    info.setPolitics(rs.getString("politics"));
                    info.setCollege(rs.getString("college"));
                    info.setMajor(rs.getString("major"));
                    info.setClassName(rs.getString("class_name"));
                    info.setAdmissionYear(rs.getString("admission_year"));
                    info.setEducationSystem(rs.getString("education_system"));
                    info.setStudentStatus(rs.getString("student_status"));
                    info.setEducationLevel(rs.getString("education_level"));
                    info.setGraduationTime(rs.getString("graduation_time"));
                    info.setTotalGPA(rs.getString("totalGPA"));
                    info.setTotalCredits(rs.getString("totalCredits"));
                    info.setCompletedCredits(rs.getString("completedCredits"));
                    info.setAvgScore(rs.getString("avgScore"));
                    info.setDormitoryBuilding(rs.getString("dormitory_building"));
                    info.setDormitoryRoom(rs.getString("dormitory_room"));
                    info.setDormitoryBed(rs.getString("dormitory_bed"));
                    return info;
                }else {return null;}
            }
        }


     //在数据库中获取教师信息的方法
        private TeacherInfo getTeacherInfoFromDB(String userid) throws SQLException {
            String sql = "SELECT * FROM teacher_info WHERE teacherid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userid);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    TeacherInfo info = new TeacherInfo();
                    info.setUserid(rs.getString("teacherid"));
                    info.setName(rs.getString("name"));
                    info.setGender(rs.getString("gender"));
                    info.setBirth(rs.getString("birth"));
                    info.setIdCard(rs.getString("id_card"));
                    info.setPhone(rs.getString("phone"));
                    info.setEmail(rs.getString("email"));
                    info.setPolitics(rs.getString("politics"));
                    info.setCollege(rs.getString("college"));
                    info.setDepartment(rs.getString("department"));
                    info.setTitle(rs.getString("title"));
                    info.setEntryYear(rs.getString("entry_year"));
                    info.setMajor(rs.getString("major"));
                    info.setEducation(rs.getString("education"));
                    info.setDegree(rs.getString("degree"));
                    return info;
                }else {return null;}
            }
        }

     //从数据库获取管理员信息的方法
        private AdminInfo getAdminInfoFromDB(String userid) throws SQLException {
            String sql = "SELECT * FROM admin_info WHERE adminid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userid);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    AdminInfo info = new AdminInfo();
                    info.setUserid(rs.getString("adminid"));
                    info.setName(rs.getString("name"));
                    info.setGender(rs.getString("gender"));
                    info.setBirth(rs.getString("birth"));
                    info.setIdCard(rs.getString("id_card"));
                    info.setPhone(rs.getString("phone"));
                    info.setEmail(rs.getString("email"));
                    info.setPolitics(rs.getString("politics"));
                    info.setPosition(rs.getString("position"));
                    return info;
                }else {return null;}
            }
        }

     // 在数据库中修改密码的方法
        private boolean changePasswordInDB(String userid, String oldPassword, String newPassword) throws SQLException {
            // 首先验证旧密码是否正确
            String verifySql = "SELECT password FROM user_auth WHERE userid = ? AND status = 1";
            try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
                verifyStmt.setString(1, userid);
                ResultSet rs = verifyStmt.executeQuery();

                if (!rs.next()) {
                    return false; // 用户不存在
                }

                String storedPassword = rs.getString("password");
                if (!oldPassword.equals(storedPassword)) {
                    return false; // 旧密码不正确
                }
            }

            // 更新密码
            String updateSql = "UPDATE user_auth SET password = ? WHERE userid = ? AND status = 1";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, newPassword);
                updateStmt.setString(2, userid);
                int rowsAffected = updateStmt.executeUpdate();
                return rowsAffected > 0; // 更新成功返回true
            }
        }

     // 从数据库获取商品信息的方法
        private List<Product> getProductsFromDB(String category, String keyword) throws SQLException {
            List<Product> products = new ArrayList<>();
            StringBuilder sql = new StringBuilder("SELECT * FROM product_info WHERE status = 1");
            List<Object> params = new ArrayList<>();

            if (category != null && !category.isEmpty() && !"all".equals(category)) {
                sql.append(" AND category = ?");
                params.add(category);
            }

            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND (name LIKE ? OR brand LIKE ? OR description LIKE ?)");
                String likeKeyword = "%" + keyword + "%";
                params.add(likeKeyword);
                params.add(likeKeyword);
                params.add(likeKeyword);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }

                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Product product = new Product(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getDouble("original_price"),
                        rs.getDouble("rating"),
                        rs.getString("category"),
                        rs.getString("description")
                    );

                    // 设置新增字段
                    product.setBrand(rs.getString("brand"));
                    product.setStock(rs.getInt("stock"));
                    product.setExpirationDate(rs.getString("expiration_date"));
                    product.setImageUrl(rs.getString("image_url"));

                    products.add(product);
                }
            }
            return products;
        }

     // 获取订单历史
        private List<Order> getOrderHistory(String userId, String statusFilter) throws SQLException {
            List<Order> orders = new ArrayList<>();

            String sql = "SELECT o.order_id, o.total_amount, o.status, o.created_at, " +
                         "oi.product_id, oi.quantity, oi.price, oi.subtotal, " +
                         "pi.name as product_name, pi.image_url " +
                         "FROM orders o " +
                         "JOIN order_items oi ON o.order_id = oi.order_id " +
                         "JOIN product_info pi ON oi.product_id = pi.id " +
                         "WHERE o.user_id = ?";

            if (statusFilter != null && !statusFilter.equals("all")) {
                sql += " AND o.status = ?";
            }

            sql += " ORDER BY o.created_at DESC";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);

                if (statusFilter != null && !statusFilter.equals("all")) {
                    pstmt.setString(2, statusFilter);
                }

                ResultSet rs = pstmt.executeQuery();

                Map<String, Order> orderMap = new HashMap<>();

                while (rs.next()) {
                    String orderId = rs.getString("order_id");

                    Order order;
                    if (orderMap.containsKey(orderId)) {
                        order = orderMap.get(orderId);
                    } else {
                        order = new Order(
                            orderId,
                            rs.getString("created_at").substring(0, 10), // 只取日期部分
                            rs.getString("status"),
                            rs.getDouble("total_amount")
                        );
                        orderMap.put(orderId, order);
                    }

                    // 创建商品对象
                    Product product = new Product(
                        rs.getString("product_id"),
                        rs.getString("product_name"),
                        "", // brand
                        rs.getDouble("price"),
                        0,  // originalPrice
                        0,  // rating
                        "", // category
                        "", // description
                        0,  // stock
                        null, // expirationDate
                        rs.getString("image_url")
                    );

                    // 创建订单项
                    OrderItem item = new OrderItem(
                        rs.getString("product_id"),
                        rs.getString("product_name"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                    );

                    order.addItem(item);
                }

                orders.addAll(orderMap.values());
            }

            return orders;
        }

        // 获取订单详情
        private Order getOrderDetail(String userId, String orderId) throws SQLException {
            String sql = "SELECT o.order_id, o.total_amount, o.status, o.created_at, " +
                         "oi.product_id, oi.quantity, oi.price, oi.subtotal, " +
                         "pi.name as product_name, pi.image_url " +
                         "FROM orders o " +
                         "JOIN order_items oi ON o.order_id = oi.order_id " +
                         "JOIN product_info pi ON oi.product_id = pi.id " +
                         "WHERE o.user_id = ? AND o.order_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, orderId);

                ResultSet rs = pstmt.executeQuery();

                Order order = null;

                while (rs.next()) {
                    if (order == null) {
                        order = new Order(
                            rs.getString("order_id"),
                            rs.getString("created_at").substring(0, 10),
                            rs.getString("status"),
                            rs.getDouble("total_amount")
                        );
                    }

                    // 创建商品对象
                    Product product = new Product(
                        rs.getString("product_id"),
                        rs.getString("product_name"),
                        "", // brand
                        rs.getDouble("price"),
                        0,  // originalPrice
                        0,  // rating
                        "", // category
                        "", // description
                        0,  // stock
                        null, // expirationDate
                        rs.getString("image_url")
                    );

                    // 创建订单项
                    OrderItem item = new OrderItem(
                        rs.getString("product_id"),
                        rs.getString("product_name"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                    );

                    order.addItem(item);
                }

                return order;
            }
        }

     // 从数据库查询交易记录
        private List<Transaction> getTransactionsFromDB(String userId) throws SQLException {
            List<Transaction> transactions = new ArrayList<>();
            String sql = "SELECT * FROM transaction WHERE user_id = ? ORDER BY date DESC";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Transaction t = new Transaction(
                        rs.getString("id"),
                        rs.getString("date"),
                        rs.getString("category"),
                        rs.getDouble("amount"),
                        rs.getString("type"), // "income"或"expense"
                        rs.getString("description"),
                        rs.getString("status") // "success"、"pending"、"failed"
                    );
                    transactions.add(t);
                }
            }
            return transactions;
        }


     // 添加商品到数据库
        private boolean addProductToDB(Product product) throws SQLException {
            String sql = "INSERT INTO product_info (id, name, brand, price, original_price, rating, category, description, stock, expiration_date, image_url, status) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, product.getId());
                pstmt.setString(2, product.getName());
                pstmt.setString(3, product.getBrand());
                pstmt.setDouble(4, product.getPrice());
                pstmt.setDouble(5, product.getOriginalPrice());
                pstmt.setDouble(6, product.getRating());
                pstmt.setString(7, product.getCategory());
                pstmt.setString(8, product.getDescription());
                pstmt.setInt(9, product.getStock());
                pstmt.setString(10, product.getExpirationDate());
                pstmt.setString(11, product.getImageUrl());

                return pstmt.executeUpdate() > 0;
            }
        }

        // 更新商品到数据库
        private boolean updateProductInDB(Product product) throws SQLException {
            String sql = "UPDATE product_info SET name = ?, brand = ?, price = ?, original_price = ?, rating = ?, " +
                         "category = ?, description = ?, stock = ?, expiration_date = ?, image_url = ? WHERE id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, product.getName());
                pstmt.setString(2, product.getBrand());
                pstmt.setDouble(3, product.getPrice());
                pstmt.setDouble(4, product.getOriginalPrice());
                pstmt.setDouble(5, product.getRating());
                pstmt.setString(6, product.getCategory());
                pstmt.setString(7, product.getDescription());
                pstmt.setInt(8, product.getStock());
                pstmt.setString(9, product.getExpirationDate());
                pstmt.setString(10, product.getImageUrl());
                pstmt.setString(11, product.getId());

                return pstmt.executeUpdate() > 0;
            }
        }

        
        
        
        // 从数据库删除商品
        private boolean deleteProductFromDB(String productId) throws SQLException {
            String sql = "UPDATE product_info SET status = 0 WHERE id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, productId);
                return pstmt.executeUpdate() > 0;
            }
        }

        // 更新订单状态
        private boolean updateOrderStatusInDB(String orderId, String status) throws SQLException {
            String sql = "UPDATE orders SET status = ? WHERE order_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, status);
                pstmt.setString(2, orderId);
                return pstmt.executeUpdate() > 0;
            }
        }

        private List<Order> getAllOrderHistory(String statusFilter) throws SQLException {
            List<Order> orders = new ArrayList<>();

            String sql = "SELECT o.order_id, o.user_id, o.total_amount, o.status, o.created_at, " +
                         "oi.product_id, oi.quantity, oi.price, oi.subtotal, " +
                         "pi.name as product_name, pi.image_url " +
                         "FROM orders o " +
                         "JOIN order_items oi ON o.order_id = oi.order_id " +
                         "JOIN product_info pi ON oi.product_id = pi.id " +
                         "WHERE 1=1";

            if (statusFilter != null && !statusFilter.equals("all")) {
                sql += " AND o.status = ?";
            }

            sql += " ORDER BY o.created_at DESC";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int paramIndex = 1;
                if (statusFilter != null && !statusFilter.equals("all")) {
                    pstmt.setString(paramIndex++, statusFilter);
                }

                ResultSet rs = pstmt.executeQuery();

                Map<String, Order> orderMap = new HashMap<>();

                while (rs.next()) {
                    String orderId = rs.getString("order_id");

                    Order order;
                    if (orderMap.containsKey(orderId)) {
                        order = orderMap.get(orderId);
                    } else {
                        order = new Order(
                            orderId,
                            rs.getString("created_at").substring(0, 10),
                            rs.getString("status"),
                            rs.getDouble("total_amount")
                        );
                        // 设置用户ID，便于管理员查看订单所属用户
                        order.setUserId(rs.getString("user_id"));
                        orderMap.put(orderId, order);
                    }

                    // 创建商品对象
                    Product product = new Product(
                        rs.getString("product_id"),
                        rs.getString("product_name"),
                        "", // brand
                        rs.getDouble("price"),
                        0,  // originalPrice
                        0,  // rating
                        "", // category
                        "", // description
                        0,  // stock
                        null, // expirationDate
                        rs.getString("image_url")
                    );

                    // 创建订单项
                    OrderItem item = new OrderItem(
                        rs.getString("product_id"),
                        rs.getString("product_name"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                    );

                    order.addItem(item);
                }

                orders.addAll(orderMap.values());
            }

            return orders;
        }


        private List<Transaction> getAllTransactionsFromDB() throws SQLException {
            List<Transaction> transactions = new ArrayList<>();
            String sql = "SELECT * FROM transaction ORDER BY date DESC";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Transaction t = new Transaction(
                        rs.getString("id"),
                        rs.getString("date"),
                        rs.getString("category"),
                        rs.getDouble("amount"),
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getString("status")
                    );
                    transactions.add(t);
                }
            }
            return transactions;
        }


        private void handleDBError(String action, SQLException e) {
            try {
                out.writeUTF("ERROR|" + action + "失败：" + e.getMessage());
                out.flush();
                System.out.println("[Error]数据库" + action + "错误: " + e.getMessage());
            } catch (IOException ex) {
                System.out.println("[Error]发送数据库错误响应失败: " + ex.getMessage());
            }
        }
        
// ------------------------------------------------------------------------------------------------------------------------------------------
        
        // 获取校园基本信息
        private void processGetCampusData() {
        	try {
                Map<String, Object> campusData = getCampusDataFromDB();
                
                List<CollegeInfo> colleges = (List<CollegeInfo>) campusData.get("colleges");
                Map<String, List<MajorInfo>> collegeMajorsMap = (Map<String, List<MajorInfo>>) campusData.get("collegeMajorsMap");
                Map<String, List<ClassInfo>> majorClassesMap = (Map<String, List<ClassInfo>>) campusData.get("majorClassesMap");
                List<String> classRooms = (List<String>) campusData.get("classRooms");
                LocalDate serverDate = LocalDate.now(); // 服务器当前日期（实时获取）
                LocalDate foundingDate = (LocalDate) campusData.get("foundingDate");
        		
                Gson gson = new GsonBuilder()
                	    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                	    .create();
                
        		String jsonColleges = gson.toJson(colleges);
        		String jsonCollegeMajorsMap = gson.toJson(collegeMajorsMap);
        		String jsonMajorClassesMap = gson.toJson(majorClassesMap);
        		String jsonClassRooms = gson.toJson(classRooms);
        		String jsonSeverDate = gson.toJson(serverDate);
        		String jsonFoundingDate = gson.toJson(foundingDate);
        		
        		out.writeUTF(jsonColleges);
        		out.writeUTF(jsonCollegeMajorsMap);
        		out.writeUTF(jsonMajorClassesMap);
        		out.writeUTF(jsonClassRooms);
        		out.writeUTF(jsonSeverDate);
        		out.writeUTF(jsonFoundingDate);
        		
        		out.flush();
        	} catch (SQLException e) {
                handleDBError("查询学校信息", e);
            } catch (IOException e) {
                System.out.println("[Error]处理学校信息查询IO错误：" + e.getMessage());
            }
        }
        
        private Map<String, Object> getCampusDataFromDB() throws SQLException {
        	Map<String, Object> dataMap = new HashMap<>();

        	List<CollegeInfo> colleges = queryColleges();
        	dataMap.put("colleges", colleges);
        		
        	Map<String, List<MajorInfo>> collegeMajorsMap = queryCollegeMajors(colleges);
        	dataMap.put("collegeMajorsMap", collegeMajorsMap);
        		
        	Map<String, List<ClassInfo>> majorClassesMap = queryMajorClasses(collegeMajorsMap);
        	dataMap.put("majorClassesMap", majorClassesMap);
        		
        	List<String> classRooms = queryClassRooms();
        	dataMap.put("classRooms", classRooms);
        	
        	 LocalDate foundingDate = queryFoundingDate();
             dataMap.put("foundingDate", foundingDate);
        	
        	return dataMap;
        }
        
        private List<CollegeInfo> queryColleges() throws SQLException {
        	List<CollegeInfo> colleges = new ArrayList<>();
        	String sql = "SELECT college_id, college_name FROM college";
        	
        	try (PreparedStatement ps = conn.prepareStatement(sql);
        		 ResultSet rs = ps.executeQuery()) {
        			
        		while (rs.next()) {
        			CollegeInfo college = new CollegeInfo();
        			college.setCollegeId(rs.getString("college_id"));
        			college.setCollegeName(rs.getString("college_name"));
        			colleges.add(college);
        		}
        	}
        	
        	return colleges;
        }
        
        private Map<String, List<MajorInfo>> queryCollegeMajors(List<CollegeInfo> colleges) throws SQLException {
        	Map<String, List<MajorInfo>> map = new HashMap<>();
        	for (CollegeInfo college : colleges) {
        		map.put(college.getCollegeId(), new ArrayList<>());
        	}
        	
        	String sql = "SELECT major_id, major_name, college_id, " +
        				 "undergraduate_duration, postgraduate_duration, " +
        				 "undergraduate_credits, postgraduate_credits " +
        				 "FROM major";
        	try (PreparedStatement ps = conn.prepareStatement(sql);
        		 ResultSet rs = ps.executeQuery()) {
        		
        		while (rs.next()) {
        			MajorInfo major = new MajorInfo();
        			major.setMajorId(rs.getString("major_id"));
        			major.setMajorName(rs.getString("major_name"));
        			major.setCollegeId(rs.getString("college_id"));
        			major.setUndergraduateDuration(rs.getString("undergraduate_duration"));
        			major.setPostgraduateDuration(rs.getString("postgraduate_duration"));
        			major.setUndergraduateCredits(rs.getBigDecimal("undergraduate_credits"));
        			major.setPostgraduateCredits(rs.getBigDecimal("postgraduate_credits"));
        			
        			String collegeId = rs.getString("college_id");
        			if (map.containsKey(collegeId)) { // 防止出现专业没有对应学院
        				map.get(collegeId).add(major);
        			}
        		}
        	}
        	return map;
        }
        
        private Map<String, List<ClassInfo>> queryMajorClasses(Map<String, List<MajorInfo>> collegeMajorsMap) throws SQLException {
        	Map<String, List<ClassInfo>> map = new HashMap<>();
        	for (List<MajorInfo> majors : collegeMajorsMap.values()) {
        		for (MajorInfo major : majors) {
        			map.put(major.getMajorId(), new ArrayList<>());
        		}
        	}
        	
        	String sql = "SELECT class_id, class_name, grade, education_level, major_id " +
        				 "FROM `class`"; // class是SQL关键字 需要使用反引号
        	
        	try (PreparedStatement ps = conn.prepareStatement(sql);
        		 ResultSet rs = ps.executeQuery()) {
        		
        		while (rs.next()) {
        			ClassInfo clazz = new ClassInfo();
        			clazz.setClassId(rs.getString("class_id"));
        			clazz.setClassName(rs.getString("class_name"));
        			clazz.setGrade(rs.getString("grade"));
        			clazz.setEducationLevel(rs.getString("education_level"));
        			clazz.setMajorId(rs.getString("major_id"));
        			
        			String majorId = rs.getString("major_id");
        			if (map.containsKey(majorId)) {
        				map.get(majorId).add(clazz);
        			}
        		}
        	}
        	return map;
        }
        
        private List<String> queryClassRooms() throws SQLException {
        	List<String> classRooms = new ArrayList<>();
        	String sql = "SELECT class_room_id FROM class_room";
        	
        	try (PreparedStatement ps = conn.prepareStatement(sql);
        		 ResultSet rs = ps.executeQuery()) {
        		
        		while (rs.next()) {
        			classRooms.add(rs.getString("class_room_id"));
        		}
        	}
        	return classRooms;
        }
        
        private LocalDate queryFoundingDate() throws SQLException {
        	String sql = "SELECT founding_date FROM school_info LIMIT 1";
        	
        	try (PreparedStatement ps = conn.prepareStatement(sql);
        		 ResultSet rs = ps.executeQuery()) {
        		
        		if (rs.next()) {
        			return rs.getObject("founding_date", LocalDate.class);
        		}
        		throw new SQLException("数据库中未找到学校建校日期信息");
        	}
        }
        
        // 获取一个学生学籍信息
        private void processGetStudentProfile() {
        	try {
        		StudentProfile studentProfile = getStudentProfileFromDB();
        		Gson gson = new GsonBuilder()
        				.registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                        .create();
        		
        		String json = gson.toJson(studentProfile);

        		out.writeUTF(json);
        		out.flush();
        	} catch (SQLException e) {
        		sendErrorResponse("数据库操作失败：" + e.getMessage());
        	} catch (IOException e) {
        		System.out.println("[Error]学生档案传输失败：" + e.getMessage());
        	}
        }
        
        private StudentProfile getStudentProfileFromDB() throws SQLException {
        	StudentProfile profile = null;
        	String sql = "SELECT " +
                    // 学生基本信息
                    "sa.student_id AS studentId, " +
                    "si.name AS name, " +
                    // 学院/专业/班级（通过class_id推导）
                    "c.college_name AS college, " +
                    "m.major_name AS major, " +
                    "cl.class_name AS className, " +
                    // 导师/辅导员（关联teacher_info）
                    "t2.name AS counsellor, " +
                    "t1.name AS mentor, " +
                    // 培养层级/学籍状态
                    "cl.education_level AS levelOfStudy, " +
                    "sa.enrollment_status AS studentStatus, " +
                    // 日期相关
                    "sa.admission_date AS admissionDate, " +
                    "CASE " +
                    "   WHEN cl.education_level = '本科' THEN m.undergraduate_duration " +
                    "   WHEN cl.education_level = '研究生' THEN m.postgraduate_duration " +
                    "END AS duration, " +
                    "cl.grade AS grade, " +
                    // 计算预期毕业日期
                    "DATE_FORMAT(" +
                    "   STR_TO_DATE(" +
                    "       CONCAT(" +
                    "           YEAR(sa.admission_date) + " +
                    "           CASE " +
                    "               WHEN cl.education_level = '本科' THEN m.undergraduate_duration " +  // 用专业表的本科年限
                    "               ELSE m.postgraduate_duration " +  // 用专业表的研究生/博士年限
                    "           END, " +
                    "           '-07-01'" +
                    "       ), " +
                    "       '%Y-%m-%d'" +
                    "   ), " +
                    "   '%Y-%m-%d'" +
                    ") AS expectedGraduationDate "+
                    // 关联表（注意外键字段名匹配你的表结构）
                    "FROM student_archive sa " +
                    "LEFT JOIN student_info si ON sa.student_id = si.studentId " +
                    "LEFT JOIN `class` cl ON sa.class_id = cl.class_id " +
                    "LEFT JOIN major m ON cl.major_id = m.major_id " +
                    "LEFT JOIN college c ON m.college_id = c.college_id " +
                    "LEFT JOIN teacher_info t1 ON sa.tutor_id = t1.teacherId " +  // 导师（INT类型关联）
                    "LEFT JOIN teacher_info t2 ON sa.counselor_id = t2.teacherId " +  // 辅导员（INT类型关联）
                    "WHERE sa.student_id = ?";
        	
        	try (PreparedStatement ps = conn.prepareStatement(sql)) {
        		ps.setString(1, userId);
        		ResultSet rs = ps.executeQuery();
        		if (rs.next()) {
        			profile = new StudentProfile();
        			profile.setStudentId(rs.getString("studentId"));
                    profile.setName(rs.getString("name"));
                    profile.setCollege(rs.getString("college"));
                    profile.setMajor(rs.getString("major"));
                    profile.setClassName(rs.getString("className"));
                    // 导师/辅导员（处理导师为NULL的情况）
                    profile.setCounsellor(rs.getString("counsellor"));
                    profile.setMentor(rs.getString("mentor") == null ? "无" : rs.getString("mentor"));
                    // 培养层级/学籍状态
                    profile.setLevelOfStudy(rs.getString("levelOfStudy"));
                    profile.setStudentStatus(rs.getString("studentStatus"));
                    // 日期相关（直接用数据库格式化后的字符串）
                    profile.setAdmissionDate(rs.getString("admissionDate"));
                    profile.setDuration(rs.getString("duration"));
                    profile.setGrade(rs.getString("grade"));
                    profile.setExpectedGraduationDate(rs.getString("expectedGraduationDate"));
        		}
        	}
        	
        	return profile;
        }
        
        // 使用条件检索学生
        private void processGetStudentsByConditions() {
        	try {
        		String conditionJson = in.readUTF();
        		StudentRetrieveCondition condition = gson.fromJson(conditionJson, StudentRetrieveCondition.class);
        		StringBuilder sql = new StringBuilder();
        		sql.append("SELECT ")
                .append("sa.student_id AS studentId, ")
                .append("si.name AS name, ")
                .append("c.college_name AS college, ")
                .append("m.major_name AS major, ")
                .append("cl.grade AS grade, ")
                .append("cl.class_name AS className, ")
                .append("cl.education_level AS levelOfStudy, ")
                .append("sa.enrollment_status AS studentStatus, ")
                .append("sa.admission_date AS admissionDate, ")
                .append("CASE WHEN cl.education_level = '本科' THEN m.undergraduate_duration ELSE m.postgraduate_duration END AS duration, ")
                .append("DATE_FORMAT(STR_TO_DATE(CONCAT(YEAR(sa.admission_date) + CASE WHEN cl.education_level = '本科' THEN m.undergraduate_duration ELSE m.postgraduate_duration END, '-07-01'), '%Y-%m-%d'), '%Y-%m-%d') AS expectedGraduationDate, ")
                .append("t1.name AS mentor, ")
                .append("t2.name AS counsellor ")
                .append("FROM student_archive sa ")
                .append("LEFT JOIN student_info si ON sa.student_id = si.studentId ")
                .append("LEFT JOIN `class` cl ON sa.class_id = cl.class_id ")
                .append("LEFT JOIN major m ON cl.major_id = m.major_id ")
                .append("LEFT JOIN college c ON m.college_id = c.college_id ")
                .append("LEFT JOIN teacher_info t1 ON sa.tutor_id = t1.teacherId ")
                .append("LEFT JOIN teacher_info t2 ON sa.counselor_id = t2.teacherId ")
                .append("WHERE 1=1 "); // 基础条件，方便拼接AND
        		
        		List<String> params = new ArrayList<>();
                if (condition.getStudentId() != null && !condition.getStudentId().isEmpty()) {
                    sql.append("AND sa.student_id = ? ");
                    params.add(condition.getStudentId());
                }
                if (condition.getName() != null && !condition.getName().isEmpty()) {
                    sql.append("AND si.name LIKE ? "); // 支持模糊查询
                    params.add("%" + condition.getName() + "%");
                }
                if (condition.getCollege() != null) {
                    sql.append("AND c.college_name = ? ");
                    params.add(condition.getCollege());
                }
                if (condition.getMajor() != null) {
                    sql.append("AND m.major_name = ? ");
                    params.add(condition.getMajor());
                }
                if (condition.getGrade() != null) {
                    sql.append("AND cl.grade = ? ");
                    params.add(condition.getGrade());
                }
                if (condition.getClassName() != null) {
                    sql.append("AND cl.class_name = ? ");
                    params.add(condition.getClassName());
                }

                // 4. 执行查询
                PreparedStatement ps = conn.prepareStatement(sql.toString());
                for (int i = 0; i < params.size(); i++) {
                    ps.setString(i + 1, params.get(i)); // 设置参数（索引从1开始）
                }
                ResultSet rs = ps.executeQuery();

                // 5. 解析结果到StudentProfile列表
                List<StudentProfile> profiles = new ArrayList<>();
                while (rs.next()) {
                    StudentProfile sp = new StudentProfile();
                    sp.setStudentId(rs.getString("studentId"));
                    sp.setName(rs.getString("name"));
                    sp.setCollege(rs.getString("college"));
                    sp.setMajor(rs.getString("major"));
                    sp.setClassName(rs.getString("className"));
                    sp.setGrade(rs.getString("grade"));
                    sp.setLevelOfStudy(rs.getString("levelOfStudy"));
                    sp.setStudentStatus(rs.getString("studentStatus"));
                    sp.setAdmissionDate(rs.getString("admissionDate"));
                    sp.setDuration(rs.getString("duration"));
                    sp.setExpectedGraduationDate(rs.getString("expectedGraduationDate"));
                    sp.setMentor(rs.getString("mentor") == null ? "无" : rs.getString("mentor"));
                    sp.setCounsellor(rs.getString("counsellor"));
                    profiles.add(sp);
                }

                // 6. 序列化并发送结果
                String resultJson = gson.toJson(profiles);
                out.writeUTF(resultJson);
                out.flush();

                // 7. 关闭资源（保留连接）
                rs.close();
                ps.close();
        		
        	} catch (SQLException e) {
                handleDBError("按条件查询学生", e);
                try {
                    out.writeUTF("{\"error\":\"数据库查询失败：" + e.getMessage() + "\"}");
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送查询错误信息失败：" + ex.getMessage());
                }
            } catch (IOException e) {
                System.out.println("[Error]处理学生条件查询IO错误：" + e.getMessage());
            }
        }
        
        // 通过学生Id查询学生
        private void processGetStudentById() {
            try {
                // 1. 读取客户端发送的学生ID
                String studentId = in.readUTF();
                StudentProfile profile = null;
                PreparedStatement ps = null;
                ResultSet rs = null;

                // 2. 编写查询SQL（关联所有必要表）
                String sql = "SELECT " +
                        // 学生基础信息
                        "sa.student_id AS studentId, " +
                        "si.name AS name, " +
                        // 学院/专业/班级信息
                        "c.college_name AS college, " +
                        "m.major_name AS major, " +
                        "cl.class_name AS className, " +
                        "cl.grade AS grade, " +
                        "cl.education_level AS levelOfStudy, " +
                        // 导师/辅导员
                        "t1.name AS mentor, " +
                        "sa.tutor_id AS mentorId, " +
                        "t2.name AS counsellor, " +
                        "sa.counselor_id AS counsellorId, " +
                        // 学籍信息
                        "sa.enrollment_status AS studentStatus, " +
                        "sa.admission_date AS admissionDate, " +
                        // 学制与毕业日期
                        "CASE WHEN cl.education_level = '本科' THEN m.undergraduate_duration ELSE m.postgraduate_duration END AS duration, " +
                        "DATE_FORMAT(STR_TO_DATE(CONCAT(YEAR(sa.admission_date) + CASE WHEN cl.education_level = '本科' THEN m.undergraduate_duration ELSE m.postgraduate_duration END, '-07-01'), '%Y-%m-%d'), '%Y-%m-%d') AS expectedGraduationDate " +
                        // 关联表
                        "FROM student_archive sa " +
                        "LEFT JOIN student_info si ON sa.student_id = si.studentId " +
                        "LEFT JOIN `class` cl ON sa.class_id = cl.class_id " +
                        "LEFT JOIN major m ON cl.major_id = m.major_id " +
                        "LEFT JOIN college c ON m.college_id = c.college_id " +
                        "LEFT JOIN teacher_info t1 ON sa.tutor_id = t1.teacherId " +
                        "LEFT JOIN teacher_info t2 ON sa.counselor_id = t2.teacherId " +
                        "WHERE sa.student_id = ?";

                // 3. 执行查询
                ps = conn.prepareStatement(sql);
                ps.setString(1, studentId); // 绑定学生ID参数
                rs = ps.executeQuery();

                // 4. 解析结果到StudentProfile
                if (rs.next()) {
                    profile = new StudentProfile();
                    profile.setStudentId(rs.getString("studentId"));
                    profile.setName(rs.getString("name"));
                    profile.setCollege(rs.getString("college"));
                    profile.setMajor(rs.getString("major"));
                    profile.setClassName(rs.getString("className"));
                    profile.setGrade(rs.getString("grade"));
                    profile.setLevelOfStudy(rs.getString("levelOfStudy"));
                    profile.setMentor(rs.getString("mentor") == null ? "无" : rs.getString("mentor"));
                    profile.setMentorId(rs.getString("mentorId"));
                    profile.setCounsellor(rs.getString("counsellor"));
                    profile.setCounsellorId(rs.getString("counsellorId"));
                    profile.setStudentStatus(rs.getString("studentStatus"));
                    profile.setAdmissionDate(rs.getString("admissionDate"));
                    profile.setDuration(rs.getString("duration"));
                    profile.setExpectedGraduationDate(rs.getString("expectedGraduationDate"));
                }

                // 5. 序列化并发送结果（未找到时返回null，客户端可处理）
                String resultJson = gson.toJson(profile);
                out.writeUTF(resultJson);
                out.flush();

                // 6. 关闭资源
                rs.close();
                ps.close();

            } catch (SQLException e) {
                handleDBError("查询单个学生档案", e);
                try {
                    out.writeUTF(gson.toJson(null)); // 异常时返回null
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送学生查询错误信息失败：" + ex.getMessage());
                }
            } catch (IOException e) {
                System.out.println("[Error]处理单个学生查询IO错误：" + e.getMessage());
            }
        }
        
        // 通过学生id获取当前奖惩列表
        private void processGetStudentRewardPunishmentById() {
        	try {
                // 1. 读取客户端发送的学生ID
                String studentId = in.readUTF();
                Gson gson = new Gson();

                // 2. 从数据库查询奖惩记录
                List<RewardPunishment> records = getStudentRewardPunishmentByIdFromDB(studentId);

                // 3. 序列化结果并发送给客户端
                String resultJson = gson.toJson(records);
                out.writeUTF(resultJson);
                out.flush();

            } catch (SQLException e) {
                handleDBError("查询学生奖惩记录", e);
                try {
                    // 发送错误信息给客户端
                    out.writeUTF("{\"error\":\"查询奖惩记录失败：" + e.getMessage() + "\"}");
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("[Error]发送奖惩记录错误信息失败：" + ex.getMessage());
                }
            } catch (IOException e) {
                System.out.println("[Error]处理奖惩记录查询IO错误：" + e.getMessage());
            }
        }
        
        private List<RewardPunishment> getStudentRewardPunishmentByIdFromDB(String studentId) throws SQLException {
        	List<RewardPunishment> records = new ArrayList<>();
            PreparedStatement ps = null;
            ResultSet rs = null;

            // 1. 编写SQL（关联college表获取颁发组织名称）
            String sql = "SELECT " +
                    "srp.id AS record_id, " +
                    "srp.reward_punish_type AS type, " +
                    "srp.reward_punish_name AS title, " +
                    "srp.reason AS reason, " +
                     // 优先显示学院名称，校级组织（NULL）显示"校级组织"
                    "COALESCE(c.college_name, '校级组织') AS awarding_org, " +
                    "srp.effective_date AS effective_date, " +
                    "srp.status AS status " +
                    "FROM student_reward_punishment srp " +
                    // 左连接college表，获取颁发组织（学院）名称
                    "LEFT JOIN college c ON srp.issuer_college_id = c.college_id " +
                    "WHERE srp.studentid = ? " +
                    // 按生效日期倒序，最新记录在前
                    "ORDER BY srp.effective_date DESC";

            try {
                // 2. 执行查询
                ps = conn.prepareStatement(sql);
                ps.setString(1, studentId); // 绑定学生ID参数
                rs = ps.executeQuery();

                // 3. 解析结果集到实体类
                while (rs.next()) {
                    RewardPunishment record = new RewardPunishment();
                    record.setId(rs.getString("record_id"));
                    record.setType(rs.getString("type"));
                    record.setTitle(rs.getString("title"));
                    record.setReason(rs.getString("reason"));
                    record.setAwardingOrganization(rs.getString("awarding_org")); // 学院名称或"校级组织"
                    record.setEffectiveDate(rs.getString("effective_date")); // 数据库DATE类型直接转为字符串
                    record.setStatus(rs.getString("status"));
                    records.add(record);
                }
            } finally {
                // 4. 关闭资源（保留数据库连接）
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }

            return records;
        }
        
        // 更新奖惩条目
        private void processAddRewardPunishment() {
            try {
                // 1. 读取客户端发送的学生ID和奖惩记录JSON
                String studentId = in.readUTF();
                String rpJson = in.readUTF();
                Gson gson = new Gson();
                RewardPunishment rp = gson.fromJson(rpJson, RewardPunishment.class);

                // 2. 将颁发组织名称转换为college_id（从college表查询）
                String issuerCollegeId = getCollegeIdByName(rp.getAwardingOrganization());

                // 3. 编写插入SQL（自增ID由数据库生成）
                String sql = "INSERT INTO student_reward_punishment (" +
                        "studentid, reward_punish_type, reward_punish_name, reason, " +
                        "issuer_college_id, effective_date, status" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?)";

                // 4. 执行插入操作
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, studentId);
                ps.setString(2, rp.getType());
                ps.setString(3, rp.getTitle());
                ps.setString(4, rp.getReason());
                ps.setString(5, issuerCollegeId); // 关联学院ID（可能为NULL）
                ps.setString(6, rp.getEffectiveDate()); // 客户端已格式化为yyyy-MM-dd
                ps.setString(7, rp.getStatus()); // 添加时固定为"通过"

                int rowsAffected = ps.executeUpdate();
                ps.close();

                // 5. 向客户端返回结果
                if (rowsAffected > 0) {
                    out.writeUTF("{\"success\":true}");
                } else {
                    out.writeUTF("{\"error\":\"添加奖惩记录失败，未影响任何行\"}");
                }
                out.flush();

            } catch (SQLException e) {
                sendErrorResponse("数据库操作失败：" + e.getMessage());
            } catch (IOException e) {
                System.out.println("[Error]添加奖惩记录IO错误：" + e.getMessage());
            }
        }

        // 处理修改奖惩记录请求（对应客户端"UpdateRewardPunishment"指令）
        private void processUpdateRewardPunishment() {
            try {
                // 1. 读取客户端发送的学生ID和奖惩记录JSON
                String studentId = in.readUTF(); // 冗余字段，可用于二次校验
                String rpJson = in.readUTF();
                Gson gson = new Gson();
                RewardPunishment rp = gson.fromJson(rpJson, RewardPunishment.class);

                // 2. 校验记录ID是否存在（防止修改不存在的记录）
                if (!isRewardPunishmentExists(rp.getId())) {
                    out.writeUTF("{\"error\":\"奖惩记录ID不存在：" + rp.getId() + "\"}");
                    out.flush();
                    return;
                }

                // 3. 将颁发组织名称转换为college_id
                String issuerCollegeId = getCollegeIdByName(rp.getAwardingOrganization());

                // 4. 编写更新SQL（不修改status字段，保持原状态）
                String sql = "UPDATE student_reward_punishment SET " +
                        "reward_punish_type = ?, " +
                        "reward_punish_name = ?, " +
                        "reason = ?, " +
                        "issuer_college_id = ?, " +
                        "effective_date = ? " +
                        "WHERE id = ?";

                // 5. 执行更新操作
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, rp.getType());
                ps.setString(2, rp.getTitle());
                ps.setString(3, rp.getReason());
                ps.setString(4, issuerCollegeId);
                ps.setString(5, rp.getEffectiveDate());
                ps.setString(6, rp.getId()); // 按ID定位记录

                int rowsAffected = ps.executeUpdate();
                ps.close();

                // 6. 向客户端返回结果
                if (rowsAffected > 0) {
                    out.writeUTF("{\"success\":true}");
                } else {
                    out.writeUTF("{\"error\":\"修改奖惩记录失败，未找到对应记录\"}");
                }
                out.flush();

            } catch (SQLException e) {
                sendErrorResponse("数据库操作失败：" + e.getMessage());
            } catch (IOException e) {
                System.out.println("[Error]修改奖惩记录IO错误：" + e.getMessage());
            }
        }

        // 辅助方法：通过学院名称查询college_id（支持NULL场景）
        private String getCollegeIdByName(String collegeName) throws SQLException {
            if (collegeName == null || collegeName.trim().isEmpty()) {
                return null;
            }

            String sql = "SELECT college_id FROM college WHERE college_name = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, collegeName);
            ResultSet rs = ps.executeQuery();

            String collegeId = null;
            if (rs.next()) {
                collegeId = rs.getString("college_id");
            }

            rs.close();
            ps.close();
            return collegeId;
        }

        // 辅助方法：校验奖惩记录ID是否存在
        private boolean isRewardPunishmentExists(String recordId) throws SQLException {
            String sql = "SELECT id FROM student_reward_punishment WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, recordId);
            ResultSet rs = ps.executeQuery();

            boolean exists = rs.next();
            rs.close();
            ps.close();
            return exists;
        }

        // 辅助方法：发送错误响应
        private void sendErrorResponse(String message) {
            try {
                out.writeUTF("{\"error\":\"" + message + "\"}");
                out.flush();
            } catch (IOException e) {
                System.out.println("[Error]发送错误响应失败：" + e.getMessage());
            }
        }
        
        private void processRevokeRewardPunishment() {
            try {
                // 1. 读取客户端发送的记录ID
                String recordId = in.readUTF();
                if (recordId == null || recordId.trim().isEmpty()) {
                    out.writeUTF("{\"error\":\"记录ID不能为空\"}");
                    out.flush();
                    return;
                }

                // 2. 校验记录是否存在
                if (!isRewardPunishmentExists(recordId)) {
                    out.writeUTF("{\"error\":\"奖惩记录不存在：" + recordId + "\"}");
                    out.flush();
                    return;
                }

                // 3. 编写更新SQL（仅修改状态为"撤销"）
                String sql = "UPDATE student_reward_punishment " +
                             "SET status = '撤销' " +
                             "WHERE id = ?";

                // 4. 执行更新
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, recordId);
                int rowsAffected = ps.executeUpdate();
                ps.close();

                // 5. 响应客户端
                if (rowsAffected > 0) {
                    out.writeUTF("{\"success\":true}");
                } else {
                    out.writeUTF("{\"error\":\"撤销失败，未找到对应记录\"}");
                }
                out.flush();

            } catch (SQLException e) {
                sendErrorResponse("数据库操作失败：" + e.getMessage());
            } catch (IOException e) {
                System.out.println("[Error]撤销奖惩记录IO错误：" + e.getMessage());
            }
        }
        
        // 更新学生学籍信息
        private void processUpdateStudentProfile() {
            try {
                // 1. 读取客户端发送的修改请求
                String jsonRequest = in.readUTF();
                Gson gson = new Gson();
                StudentProfile updateData = gson.fromJson(jsonRequest, StudentProfile.class);

                // 2. 校验必填字段
                String studentId = updateData.getStudentId();
                if (studentId == null || studentId.trim().isEmpty()) {
                    sendErrorResponse("学生ID不能为空");
                    return;
                }

                // 3. 验证学生是否存在
                if (!isStudentExists(studentId)) {
                    sendErrorResponse("学生ID不存在：" + studentId);
                    return;
                }

                // 4. 验证关联字段有效性
                String classId = updateData.getClassId();
                if (!isClassExists(classId)) {
                    sendErrorResponse("班级ID不存在：" + classId);
                    return;
                }

                String mentorId = updateData.getMentorId();
                if (!isTeacherExists(mentorId)) {
                    sendErrorResponse("导师ID不存在：" + mentorId);
                    return;
                }

                String counsellorId = updateData.getCounsellorId();
                if (!isTeacherExists(counsellorId)) {
                    sendErrorResponse("辅导员ID不存在：" + counsellorId);
                    return;
                }

                // 5. 执行更新操作（更新student_archive表，假设该表存储学籍核心信息）
                String sql = "UPDATE student_archive SET " +
                             "class_id = ?, " +
                             "tutor_id = ?, " +
                             "counselor_id = ?, " +
                             "admission_date = ?, " +
                             "enrollment_status = ? " + 
                             "WHERE student_id = ?";

                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, classId);
                ps.setString(2, mentorId);
                ps.setString(3, counsellorId);
                ps.setString(4, updateData.getAdmissionDate()); // 客户端已格式化为yyyy-MM-dd
                ps.setString(5, updateData.getStudentStatus());
                ps.setString(6, studentId);

                int rowsAffected = ps.executeUpdate();
                ps.close();

                if (rowsAffected <= 0) {
                    sendErrorResponse("修改失败，未找到对应学生记录");
                    return;
                }

                // 6. 查询更新后的完整信息，返回给客户端
                StudentProfile updatedProfile = getStudentProfileById(studentId);
                String jsonResponse = gson.toJson(updatedProfile);
                out.writeUTF(jsonResponse);
                out.flush();

            } catch (SQLException e) {
                sendErrorResponse("数据库操作失败：" + e.getMessage());
            } catch (IOException e) {
                System.out.println("[Error]处理学生信息修改IO错误：" + e.getMessage());
            }
        }
        
        // 辅助方法：验证学生是否存在
        private boolean isStudentExists(String studentId) throws SQLException {
            String sql = "SELECT student_id FROM student_archive WHERE student_id = ?";
            return existsInTable(sql, studentId);
        }

        // 辅助方法：验证班级是否存在
        private boolean isClassExists(String classId) throws SQLException {
            String sql = "SELECT class_id FROM class WHERE class_id = ?";
            return existsInTable(sql, classId);
        }

        // 辅助方法：验证教师（导师/辅导员）是否存在
        private boolean isTeacherExists(String teacherId) throws SQLException {
            String sql = "SELECT teacherId FROM teacher_info WHERE teacherId = ?";
            return existsInTable(sql, teacherId);
        }

        // 通用验证方法：检查记录是否存在
        private boolean existsInTable(String sql, String param) throws SQLException {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, param);
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            rs.close();
            ps.close();
            return exists;
        }
        
        // 复用之前的查询方法：获取学生完整信息（与processGetStudentById逻辑一致）
        private StudentProfile getStudentProfileById(String studentId) throws SQLException {
            // 1. 读取客户端发送的学生ID
            StudentProfile profile = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
        	try {
                // 2. 编写查询SQL（关联所有必要表）
                String sql = "SELECT " +
                        // 学生基础信息
                        "sa.student_id AS studentId, " +
                        "si.name AS name, " +
                        // 学院/专业/班级信息
                        "c.college_name AS college, " +
                        "m.major_name AS major, " +
                        "cl.class_name AS className, " +
                        "cl.grade AS grade, " +
                        "cl.education_level AS levelOfStudy, " +
                        // 导师/辅导员
                        "t1.name AS mentor, " +
                        "sa.tutor_id AS mentorId, " +
                        "t2.name AS counsellor, " +
                        "sa.counselor_id AS counsellorId, " +
                        // 学籍信息
                        "sa.enrollment_status AS studentStatus, " +
                        "sa.admission_date AS admissionDate, " +
                        // 学制与毕业日期
                        "CASE WHEN cl.education_level = '本科' THEN m.undergraduate_duration ELSE m.postgraduate_duration END AS duration, " +
                        "DATE_FORMAT(STR_TO_DATE(CONCAT(YEAR(sa.admission_date) + CASE WHEN cl.education_level = '本科' THEN m.undergraduate_duration ELSE m.postgraduate_duration END, '-07-01'), '%Y-%m-%d'), '%Y-%m-%d') AS expectedGraduationDate " +
                        // 关联表
                        "FROM student_archive sa " +
                        "LEFT JOIN student_info si ON sa.student_id = si.studentId " +
                        "LEFT JOIN `class` cl ON sa.class_id = cl.class_id " +
                        "LEFT JOIN major m ON cl.major_id = m.major_id " +
                        "LEFT JOIN college c ON m.college_id = c.college_id " +
                        "LEFT JOIN teacher_info t1 ON sa.tutor_id = t1.teacherId " +
                        "LEFT JOIN teacher_info t2 ON sa.counselor_id = t2.teacherId " +
                        "WHERE sa.student_id = ?";

                // 3. 执行查询
                ps = conn.prepareStatement(sql);
                ps.setString(1, studentId); // 绑定学生ID参数
                rs = ps.executeQuery();

                // 4. 解析结果到StudentProfile
                if (rs.next()) {
                    profile = new StudentProfile();
                    profile.setStudentId(rs.getString("studentId"));
                    profile.setName(rs.getString("name"));
                    profile.setCollege(rs.getString("college"));
                    profile.setMajor(rs.getString("major"));
                    profile.setClassName(rs.getString("className"));
                    profile.setGrade(rs.getString("grade"));
                    profile.setLevelOfStudy(rs.getString("levelOfStudy"));
                    profile.setMentor(rs.getString("mentor") == null ? "无" : rs.getString("mentor"));
                    profile.setMentorId(rs.getString("mentorId"));
                    profile.setCounsellor(rs.getString("counsellor"));
                    profile.setCounsellorId(rs.getString("counsellorId"));
                    profile.setStudentStatus(rs.getString("studentStatus"));
                    profile.setAdmissionDate(rs.getString("admissionDate"));
                    profile.setDuration(rs.getString("duration"));
                    profile.setExpectedGraduationDate(rs.getString("expectedGraduationDate"));

                }
            } finally {
            	// 确保资源关闭
                if (rs != null) {
                    try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
                }
                if (ps != null) {
                    try { ps.close(); } catch (SQLException e) { e.printStackTrace(); }
                }
            }
        	
        	return profile;
        }
        
        // 处理添加学生学籍信息请求（对应客户端"AddStudentProfile"指令）
        private void processAddStudentProfile() {
            try {
                // 1. 读取客户端发送的添加请求
                String jsonRequest = in.readUTF();
                Gson gson = new Gson();
                StudentProfile addData = gson.fromJson(jsonRequest, StudentProfile.class);

                // 2. 提取核心字段并校验非空
                String studentId = addData.getStudentId();
                String studentName = addData.getName();
                String classId = addData.getClassId();
                String mentorId = addData.getMentorId();
                String counsellorId = addData.getCounsellorId();
                String admissionDate = addData.getAdmissionDate();
                String studentStatus = addData.getStudentStatus();

                // 基础字段非空校验
                if (isEmpty(studentId) || isEmpty(studentName) || isEmpty(classId) || 
                    isEmpty(mentorId) || isEmpty(counsellorId) || isEmpty(admissionDate) 
                    || isEmpty(studentStatus)) {
                    sendErrorResponse("所有字段均为必填项，不可为空");
                    return;
                }

                // 3. 校验学生在student_info表中是否存在且姓名匹配
                if (!isStudentInfoMatch(studentId, studentName)) {
                    sendErrorResponse("学生ID不存在或姓名不匹配（与student_info表比对）");
                    return;
                }

                // 4. 校验学生是否已存在于student_archive（避免重复添加）
                if (isStudentExists(studentId)) {
                    sendErrorResponse("该学生已存在学籍信息，不可重复添加");
                    return;
                }
                // 5. 校验关联字段有效性（复用之前的验证方法）
                if (!isClassExists(classId)) {
                    sendErrorResponse("班级ID不存在：" + classId);
                    return;
                }
                if (!isTeacherExists(mentorId)) {
                    sendErrorResponse("导师ID不存在：" + mentorId);
                    return;
                }
                if (!isTeacherExists(counsellorId)) {
                    sendErrorResponse("辅导员ID不存在：" + counsellorId);
                    return;
                }

                // 6. 执行添加操作（插入student_archive表）
                String sql = "INSERT INTO student_archive (" +
                             "student_id, class_id, tutor_id, counselor_id, " +
                             "admission_date, enrollment_status" +
                             ") VALUES (?, ?, ?, ?, ?, ?)";

                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, studentId);
                ps.setString(2, classId);
                ps.setString(3, mentorId);
                ps.setString(4, counsellorId);
                ps.setString(5, admissionDate);
                ps.setString(6, studentStatus);

                int rowsAffected = ps.executeUpdate();
                ps.close();

                if (rowsAffected <= 0) {
                    sendErrorResponse("添加学籍信息失败，数据库未响应");
                    return;
                }

                // 7. 查询添加后的完整信息并返回给客户端
                StudentProfile addedProfile = getStudentProfileById(studentId);
                String jsonResponse = gson.toJson(addedProfile);
                out.writeUTF(jsonResponse);
                out.flush();

            } catch (SQLException e) {
                sendErrorResponse("数据库操作失败：" + e.getMessage());
            } catch (IOException e) {
                System.out.println("[Error]处理添加学生信息IO错误：" + e.getMessage());
            }
        }

        // 辅助方法：校验student_info表中存在该学生且姓名匹配
        private boolean isStudentInfoMatch(String studentId, String studentName) throws SQLException {
            String sql = "SELECT name FROM student_info WHERE studentid = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();

            boolean match = false;
            if (rs.next()) {
                // 比较数据库中的姓名与客户端传入的姓名（忽略大小写差异）
                String dbName = rs.getString("name");
                match = dbName != null && dbName.equals(studentName);
            }

            rs.close();
            ps.close();
            return match;
        }

        // 辅助方法：判断字符串是否为空（含空白字符）
        private boolean isEmpty(String str) {
            return str == null || str.trim().isEmpty();
        }
        
        // 处理客户端"获取教师课程-教学班映射"请求
        private void processGetCourseSectionsMap() {
            try {
                // 1. 接收客户端发送的教师ID
                String teacherIdStr = in.readUTF();
                
                // 校验教师ID非空
                if (teacherIdStr == null || teacherIdStr.trim().isEmpty()) {
                    sendErrorResponse("教师ID不能为空");
                    return;
                }
                // 转换为Integer（匹配数据库teacher_info.teacherid类型）
                Integer teacherId;
                try {
                    teacherId = Integer.parseInt(teacherIdStr);
                } catch (NumberFormatException e) {
                    sendErrorResponse("教师ID格式错误（需为数字）");
                    return;
                }

                // 2. 校验教师是否存在（避免查询不存在的教师）
                if (!isTeacherExists(teacherIdStr)) {
                    sendErrorResponse("教师ID不存在：" + teacherId);
                    return;
                }

                // 3. 联表查询：该教师的已通过教学班 + 关联课程信息
                String sql = "SELECT " +
                            "c.course_id, " +          // 课程ID
                            "c.course_name, " +         // 课程名
                            "cs.section_id, " +          // 教学班ID
                            "cs.class_time " +
                            "FROM course_section cs " +
                            "LEFT JOIN course c ON cs.course_id = c.course_id " +
                            "WHERE cs.teacher_id = ? AND cs.semester = ? " +
                            "ORDER BY c.course_name DESC";

                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, teacherId);
                ps.setString(2, getCurrentSemester());
                ResultSet rs = ps.executeQuery();

                // 4. 构建Map：key=课程ID-课程名，value=教学班ID列表
                Map<String, List<String>> courseToSectionsMap = new HashMap<>();
                while (rs.next()) {
                    String courseName = rs.getString("course_name");
                    String classTime = rs.getString("class_time");
                    courseToSectionsMap.computeIfAbsent(courseName, k -> new ArrayList<>())
                                       .add(classTime);
                }

                // 5. 关闭资源
                rs.close();
                ps.close();

                // 6. 序列化Map并返回给客户端（空结果返回空Map，而非null）
                Gson gson = new Gson();
                String jsonResponse = gson.toJson(courseToSectionsMap);
                out.writeUTF(jsonResponse);
                out.flush();

            } catch (SQLException e) {
                sendErrorResponse("数据库操作失败：" + e.getMessage());
            } catch (IOException e) {
                System.out.println("[Error]处理课程-教学班映射IO错误：" + e.getMessage());
            }
        }
        
        // 获取当前学期（格式：YYYY-YYYY-X，如2025-2026-1
        private String getCurrentSemester() {
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1; // 1-12
            int day = cal.get(Calendar.DAY_OF_MONTH);

            // 第一学期（暑期短学期）：8月20日 - 9月30日（含4周教学+10天过渡用于老师填成绩）
            if (month == 8 && day >= 20) return year + "-" + (year + 1) + "-1";
            if (month == 9) return year + "-" + (year + 1) + "-1";

            // 第二学期（秋季学期）：10月1日 - 次年2月28日（含教学+寒假过渡期用于老师填成绩）
            if (month == 10 || month == 11 || month == 12) 
                return year + "-" + (year + 1) + "-2";
            if (month == 1 || month == 2) 
                return (year - 1) + "-" + year + "-2";

            // 第三学期（春季学期）：3月1日 - 8月19日（含教学+暑假过渡期用于老师填成绩）
            if (month >= 3 && month <= 7) 
                return (year - 1) + "-" + year + "-3";
            if (month == 8 && day < 20) 
                return (year - 1) + "-" + year + "-3";

            // 默认返回（理论上不会执行）
            return (year - 1) + "-" + year + "-3"; 
        }
        
        // 处理"教师查询学生成绩列表"请求（核心方法）
        public void processGetStudentGradeListByTeacher() {
            try {
                // 1. 接收客户端请求（JSON格式）
                String jsonRequest = in.readUTF();
                StudentGrades request = gson.fromJson(jsonRequest, StudentGrades.class);

                // 2. 提取并校验核心参数（教师ID + 课程名 + 上课时段）
                String teacherId = request.getTeacherId();
                String courseName = request.getCourseName();
                String classTime = request.getClassTime();

                // 校验非空（保持原有校验风格）
                if (teacherId == null) {
                    sendErrorResponse("教师ID不能为空");
                    return;
                }
                if (courseName == null || courseName.trim().isEmpty()) {
                    sendErrorResponse("课程名不能为空");
                    return;
                }
                if (classTime == null || classTime.trim().isEmpty()) {
                    sendErrorResponse("上课时段不能为空");
                    return;
                }

                // 3. 先查询对应的教学班ID（通过教师ID+课程名+时段定位，利用联合唯一约束）
                Integer sectionId = getSectionIdByTeacherCourseTime(teacherId, courseName, classTime);
                if (sectionId == null) {
                    sendErrorResponse("未找到该课程的教学班（教师ID：" + teacherId + "，课程名：" + courseName + "，时段：" + classTime + "）");
                    return;
                }

                // 4. 联表查询该教学班的学生成绩（复用之前的查询逻辑风格）
                String sql = "SELECT " +
                        // 学生信息（student_info表）
                        "si.studentid AS studentId, " +
                        "si.name AS studentName, " +
                        "si.major AS major, " +
                        // 成绩信息（student_grade表）
                		"sg.grade_id AS gradeId, " +
                        "sg.regular_grade AS regularGrade, " +
                        "sg.final_grade AS finalGrade, " +
                        "sg.total_grade AS totalGrade, " +
                        // 课程信息（course表）
                        "c.course_id AS courseId, " +
                        "c.course_name AS courseName, " +
                        "c.credit AS credit, " +
                        "c.final_ratio AS finalRatio, " +
                        // 教学班信息（course_section表）
                        "cs.semester AS semester, " +
                        "cs.section_status AS sectionStatus " +
                        "FROM student_grade sg " +
                        "INNER JOIN student_info si ON sg.student_id = si.studentid " +
                        "INNER JOIN course_section cs ON sg.section_id = cs.section_id " +
                        "INNER JOIN course c ON cs.course_id = c.course_id " +
                        // 核心条件：通过前面查询到的sectionId过滤
                        "WHERE sg.section_id = ? " +
                        "ORDER BY si.studentid ASC"; // 按学号排序

                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, sectionId);
                ResultSet rs = ps.executeQuery();

                // 5. 组装StudentGrades列表（保持数据完整性）
                List<StudentGrades> gradeList = new ArrayList<>();
                while (rs.next()) {
                    StudentGrades sg = new StudentGrades();
                    // 学生信息
                    sg.setStudentId(rs.getString("studentId"));
                    sg.setStudentName(rs.getString("studentName"));
                    sg.setMajor(rs.getString("major"));
                    // 成绩信息（BigDecimal类型匹配）
                    sg.setGradeId(rs.getInt("gradeId"));
                    sg.setRegularGrade(rs.getBigDecimal("regularGrade"));
                    sg.setFinalGrade(rs.getBigDecimal("finalGrade"));
                    sg.setTotalGrade(rs.getBigDecimal("totalGrade"));
                    // 课程信息
                    sg.setCourseId(rs.getString("courseId"));
                    sg.setCourseName(rs.getString("courseName"));
                    sg.setCredit(rs.getBigDecimal("credit"));
                    sg.setFinalRatio(rs.getInt("finalRatio"));
                    // 教学班信息
                    sg.setSectionId(sectionId);
                    sg.setClassTime(classTime); // 回传客户端传入的时段
                    sg.setSemester(rs.getString("semester"));
                    sg.setSectionStatus(rs.getString("sectionStatus"));
                    sg.setTeacherId(teacherId); // 回传教师ID

                    gradeList.add(sg);
                }

                // 6. 关闭资源并返回结果（保持原有风格）
                rs.close();
                ps.close();
                out.writeUTF(gson.toJson(gradeList));
                out.flush();

            } catch (SQLException e) {
                sendErrorResponse("数据库操作失败：" + e.getMessage());
            } catch (Exception e) {
                sendErrorResponse("处理请求失败：" + e.getMessage());
            }
        }

        // 辅助方法：通过「教师ID+课程名+时段」查询唯一的section_id（核心逻辑）
        private Integer getSectionIdByTeacherCourseTime(String teacherId, String courseName, String classTime) throws SQLException {
            // 利用course和course_section的关联，通过课程名+教师ID+时段定位section_id
            String sql = "SELECT cs.section_id " +
                        "FROM course_section cs " +
                        "INNER JOIN course c ON cs.course_id = c.course_id " +
                        "WHERE cs.teacher_id = ? " +
                        "AND c.course_name = ? " +
                        "AND cs.class_time = ? " +
                        "AND cs.semester = ? "; // 只查询已通过的教学班

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, teacherId);
            ps.setString(2, courseName);
            ps.setString(3, classTime);
            ps.setString(4, getCurrentSemester());

            ResultSet rs = ps.executeQuery();
            Integer sectionId = null;
            if (rs.next()) {
                sectionId = rs.getInt("section_id");
            }
            rs.close();
            ps.close();
            return sectionId;
        }
        
        public void processUpdateStudentGrade() {
            try {
                // 1. 接收客户端发送的JSON请求数据
                String jsonRequest = in.readUTF();
                StudentGrades request = gson.fromJson(jsonRequest, StudentGrades.class);

                // 2. 提取并校验核心参数（确保非空且格式正确）
                Integer gradeId = request.getGradeId();          // 成绩记录ID（更新依据）
                String teacherId = request.getTeacherId();      // 教师ID（权限校验）
                String studentId = request.getStudentId();       // 学生ID（二次校验）
                String courseName = request.getCourseName();     // 课程名（关联校验）
                String classTime = request.getClassTime();       // 上课时段（关联校验）
                BigDecimal regularGrade = request.getRegularGrade(); // 平时成绩
                BigDecimal finalGrade = request.getFinalGrade();     // 期末成绩
                BigDecimal totalGrade = request.getTotalGrade();     // 总成绩

                // 2.1 非空校验
                if (gradeId == null) {
                    sendErrorResponse("成绩记录ID不能为空");
                    return;
                }
                if (teacherId == null) {
                    sendErrorResponse("教师ID不能为空");
                    return;
                }
                if (studentId == null || studentId.trim().isEmpty()) {
                    sendErrorResponse("学生ID不能为空");
                    return;
                }
                if (courseName == null || courseName.trim().isEmpty()) {
                    sendErrorResponse("课程名不能为空");
                    return;
                }
                if (classTime == null || classTime.trim().isEmpty()) {
                    sendErrorResponse("上课时段不能为空");
                    return;
                }
                if (regularGrade == null || finalGrade == null || totalGrade == null) {
                    sendErrorResponse("成绩数据不完整（平时/期末/总成绩缺一不可）");
                    return;
                }

                // 2.2 成绩范围校验（0-100分）
                if (regularGrade.compareTo(BigDecimal.ZERO) < 0 || regularGrade.compareTo(new BigDecimal(100)) > 0) {
                    sendErrorResponse("平时成绩必须在0-100之间");
                    return;
                }
                if (finalGrade.compareTo(BigDecimal.ZERO) < 0 || finalGrade.compareTo(new BigDecimal(100)) > 0) {
                    sendErrorResponse("期末成绩必须在0-100之间");
                    return;
                }
                if (totalGrade.compareTo(BigDecimal.ZERO) < 0 || totalGrade.compareTo(new BigDecimal(100)) > 0) {
                    sendErrorResponse("总成绩必须在0-100之间");
                    return;
                }

                // 3. 权限校验：该成绩记录是否属于当前教师的课程
                if (!isGradeBelongToTeacher(gradeId, teacherId, courseName, classTime)) {
                    sendErrorResponse("无权限更新该成绩（非本人授课课程或记录不存在）");
                    return;
                }

                // 4. 执行数据库更新操作（更新学生成绩）
                String updateSql = "UPDATE student_grade " +
                                 "SET regular_grade = ?, final_grade = ?, total_grade = ? " +
                                 "WHERE grade_id = ? AND student_id = ?";

                PreparedStatement ps = conn.prepareStatement(updateSql);
                ps.setBigDecimal(1, regularGrade);   // 平时成绩
                ps.setBigDecimal(2, finalGrade);     // 期末成绩
                ps.setBigDecimal(3, totalGrade);     // 总成绩
                ps.setInt(4, gradeId);               // 成绩记录ID（更新条件）
                ps.setString(5, studentId);          // 学生ID（二次确认）

                int affectedRows = ps.executeUpdate();
                ps.close();

                // 5. 根据更新结果返回响应
                if (affectedRows > 0) {
                    // 成功：返回包含success字段的JSON
                    out.writeUTF("{\"success\":true, \"message\":\"成绩更新成功\"}");
                    out.flush();
                    System.out.println("教师ID：" + teacherId + " 更新学生ID：" + studentId + " 的成绩成功");
                } else {
                    // 未更新任何行（可能记录已被删除或学生ID不匹配）
                    sendErrorResponse("成绩更新失败，未找到匹配的成绩记录");
                }

            } catch (SQLException e) {
                sendErrorResponse("数据库操作失败：" + e.getMessage());
            } catch (Exception e) {
                sendErrorResponse("处理成绩更新请求失败：" + e.getMessage());
            }
        }

        // 辅助方法：校验成绩记录是否属于当前教师的课程（权限控制核心）
        private boolean isGradeBelongToTeacher(Integer gradeId, String teacherId, 
                                             String courseName, String classTime) throws SQLException {
            // 逻辑：通过grade_id找到对应的section_id，再验证该section属于当前教师的课程+时段
            String sql = "SELECT 1 " +
                        "FROM student_grade sg " +
                        "INNER JOIN course_section cs ON sg.section_id = cs.section_id " +
                        "INNER JOIN course c ON cs.course_id = c.course_id " +
                        "WHERE sg.grade_id = ? " +          // 匹配成绩记录ID
                          "AND cs.teacher_id = ? " +       // 匹配教师ID
                          "AND c.course_name = ? " +       // 匹配课程名
                          "AND cs.class_time = ? " +       // 匹配上课时段
                          "AND cs.section_status = '未提交'"; // 仅允许更新"未提交"状态的教学班

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, gradeId);
            ps.setString(2, teacherId);
            ps.setString(3, courseName);
            ps.setString(4, classTime);

            ResultSet rs = ps.executeQuery();
            boolean isBelong = rs.next(); // 存在则返回true
            rs.close();
            ps.close();
            return isBelong;
        }
        
        // 处理客户端 "提交教学班成绩" 请求（对应指令 SubmitCourseSectionStatus）
        public void processSubmitCourseSectionStatus() {
        	try {
        		// 1. 接收客户端发送的 JSON 请求数据
        		String jsonRequest = in.readUTF ();
        		StudentGrades request = gson.fromJson (jsonRequest, StudentGrades.class);

        		// 2. 提取并校验核心参数（确保非空且格式合法）
        		Integer sectionId = request.getSectionId (); // 目标教学班 ID（更新依据）
        		String teacherId = request.getTeacherId (); // 提交教师 ID（权限校验）
        		String courseName = request.getCourseName (); // 课程名（冗余校验）
        		String classTime = request.getClassTime (); // 上课时段（冗余校验）

        		// 2.1 非空校验
        		if (sectionId == null) {
        			sendErrorResponse ("教学班 ID 不能为空");
        			return;
        		}
        		if (teacherId == null) {
        			sendErrorResponse ("教师 ID 不能为空");
        			return;
        		}
        		if (courseName == null || courseName.trim ().isEmpty ()) {
        			sendErrorResponse ("课程名不能为空");
        			return;
        		}
        		if (classTime == null || classTime.trim ().isEmpty ()) {
        			sendErrorResponse ("上课时段不能为空");
        			return;
        		}

        		// 3. 核心权限校验：
        		// ① 该教学班是否存在 ② 是否属于当前教师 ③ 当前状态是否为 "未提交"（防止重复提交）
        		if (!isValidSectionForSubmit (sectionId, teacherId, courseName, classTime)) {
        			sendErrorResponse ("提交失败：无权限提交该教学班，或教学班已提交 / 不存在");
        			return;
        		}

        		// 4. 执行数据库更新：将教学班状态改为 "已提交"
        		String updateSql = "UPDATE course_section " +
        				"SET section_status = '待审核' " +
        				"WHERE section_id = ?"; // 仅用教学班 ID 作为更新条件（已通过权限校验）

        		PreparedStatement ps = conn.prepareStatement(updateSql);
        		ps.setInt(1, sectionId);
        		int affectedRows = ps.executeUpdate();
        		ps.close();

        		// 5. 根据更新结果返回响应
        		if (affectedRows> 0) {
        			// 成功：返回含 success 字段的 JSON（客户端可解析）
        			String successJson = "{\"success\":true,\"message\":\" 教学班成绩提交成功，状态已锁定 \"}";
        			out.writeUTF (successJson);
        			out.flush ();
        			System.out.println ("教师 ID：" + teacherId + "成功提交教学班 ID：" + sectionId + "的成绩");
        		} else {
        			// 未更新任何行（理论上不会出现，因前面已校验存在）
        			sendErrorResponse ("提交失败：未找到对应的教学班记录");
        		}

        	} catch (SQLException e) {
        		sendErrorResponse ("数据库操作失败：" + e.getMessage ());
        	} catch (Exception e) {
        		sendErrorResponse ("处理提交请求失败：" + e.getMessage ());
        	}
        }

        // 辅助方法：校验教学班是否符合提交条件（存在 + 属当前教师 + 未提交）
        private boolean isValidSectionForSubmit(Integer sectionId, String teacherId,
        		String courseName, String classTime) throws SQLException {
        	String sql = "SELECT 1 " +
        			"FROM course_section cs " +
        			"INNER JOIN course c ON cs.course_id = c.course_id " + // 关联课程表校验课程名
        			"WHERE cs.section_id = ? " + // 匹配教学班 ID
        			"AND cs.teacher_id = ? " + // 匹配教师 ID（权限核心）
        			"AND c.course_name = ? " + // 匹配课程名（冗余防篡改）
        			"AND cs.class_time = ? " + // 匹配上课时段（冗余防篡改）
        			"AND cs.section_status = '未提交'"; // 仅允许 "未提交" 状态提交

        	PreparedStatement ps = conn.prepareStatement(sql);
        	ps.setInt(1, sectionId);
        	ps.setString(2, teacherId);
        	ps.setString(3, courseName);
        	ps.setString(4, classTime);

        	ResultSet rs = ps.executeQuery ();
        	boolean isValid = rs.next (); // 存在则返回 true（符合所有条件）
        	rs.close ();
        	ps.close ();
        	return isValid;
        }
        
        public void processGetStudentGradeAllSemester() {
            try {
                // 1. 接收客户端发送的JSON请求（包含学生ID）
                String jsonRequest = in.readUTF();
                StudentGrades request = gson.fromJson(jsonRequest, StudentGrades.class);

                // 2. 提取并校验核心参数（学生ID是唯一查询依据）
                String studentId = request.getStudentId();
                if (studentId == null || studentId.trim().isEmpty()) {
                    sendErrorResponse("学生ID不能为空");
                    return;
                }
                studentId = studentId.trim(); // 去除前后空格，避免无效空格导致查询失败

                // 3. 校验学生是否存在（避免查询不存在的学生，减少无效数据库操作）
                if (!isStudentExists(studentId)) {
                    sendErrorResponse("学生ID不存在：" + studentId);
                    return;
                }

                // 4. 联表查询：指定学生所有“已通过审核”的成绩（核心SQL）
                // 关联表：student_grade（成绩）→ course_section（教学班状态）→ course（课程学分）
                String sql = "SELECT " +
                        // 学生成绩信息（student_grade表）
                        "sg.student_id AS studentId, " +
                        "sg.total_grade AS totalGrade, " +
                        "sg.grade_id AS gradeId, " +
                        // 课程信息（course表：学分是计算总学分的关键）
                        "c.credit AS credit, " +
                        "c.course_id AS courseId, " +
                        "c.course_name AS courseName, " +
                        "c.final_ratio AS finalRatio, " +
                        // 教学班信息（course_section表：筛选“已通过”状态，获取教师名）
                        "cs.semester AS semester, " +
                        "cs.teacher_id AS teacherId, " +
                        "ti.name AS teacherName " + // 关联teacher_info表获取教师姓名
                        "FROM student_grade sg " +
                        // 关联教学班表：筛选“已通过”的教学班
                        "INNER JOIN course_section cs ON sg.section_id = cs.section_id " +
                        // 关联课程表：获取课程学分
                        "INNER JOIN course c ON cs.course_id = c.course_id " +
                        // 关联教师表：获取教师姓名（客户端可能需要显示）
                        "INNER JOIN teacher_info ti ON cs.teacher_id = ti.teacherid " +
                        // 查询条件：指定学生ID + 教学班已通过审核
                        "WHERE sg.student_id = ? " +
                          "AND cs.section_status = '已通过' " +
                        // 排序：按学期降序（最新学期在前），便于客户端展示
                        "ORDER BY cs.semester DESC, c.course_name ASC";

                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, studentId); // 绑定学生ID参数（防止SQL注入）
                ResultSet rs = ps.executeQuery();

                // 5. 组装查询结果为 List<StudentGrades>（与客户端模型类字段匹配）
                List<StudentGrades> allGrades = new ArrayList<>();
                while (rs.next()) {
                    StudentGrades grade = new StudentGrades();
                    // 学生ID（确保与客户端请求的学生ID一致）
                    grade.setStudentId(rs.getString("studentId"));
                    // 成绩信息（totalGrade是计算GPA的关键，可能为null需保留）
                    grade.setTotalGrade(rs.getBigDecimal("totalGrade"));
                    grade.setGradeId(rs.getInt("gradeId"));
                    // 课程信息（credit是计算总学分的关键）
                    grade.setCredit(rs.getBigDecimal("credit"));
                    grade.setCourseId(rs.getString("courseId"));
                    grade.setCourseName(rs.getString("courseName"));
                    grade.setFinalRatio(rs.getInt("finalRatio"));
                    // 教学班/教师信息（客户端可能需要显示学期、教师名）
                    grade.setSemester(rs.getString("semester"));
                    grade.setTeacherId(rs.getString("teacherId"));
                    grade.setTeacherName(rs.getString("teacherName"));

                    allGrades.add(grade);
                }

                // 6. 关闭数据库资源（避免连接泄漏）
                rs.close();
                ps.close();

                // 7. 序列化结果并返回给客户端（空结果返回空List，而非null，避免客户端反序列化异常）
                String jsonResponse = gson.toJson(allGrades);
                out.writeUTF(jsonResponse);
                out.flush();
                System.out.println("成功返回学生[" + studentId + "]所有学期已通过成绩，共" + allGrades.size() + "条记录");

            } catch (SQLException e) {
                // 数据库异常：记录日志+返回友好错误
                sendErrorResponse("数据库操作失败：" + e.getMessage());
            } catch (Exception e) {
                // 其他异常（如JSON解析、IO错误）：统一捕获处理
                sendErrorResponse("处理请求失败：" + e.getMessage());
            }
        }
        
        public void processGetStudentSemesterGrade() {
            try {
                // 1. 读取客户端请求数据
                String jsonRequest = in.readUTF();
                StudentGrades request = gson.fromJson(jsonRequest, StudentGrades.class);

                // 2. 校验必要参数
                String studentId = request.getStudentId();
                String semester = request.getSemester();

                if (studentId == null || studentId.trim().isEmpty()) {
                    sendErrorResponse("学生ID不能为空");
                    return;
                }
                if (semester == null || semester.trim().isEmpty()) {
                    sendErrorResponse("学期信息不能为空");
                    return;
                }

                // 3. 验证学生合法性
                if (!isValidStudent(studentId)) {
                    sendErrorResponse("学生ID不存在：" + studentId);
                    return;
                }

                // 4. 查询该学生指定学期的已审核成绩
                String sql = "SELECT " +
                        // 课程信息
                        "c.course_id AS courseId, " +
                        "c.course_name AS courseName, " +
                        "c.credit AS credit, " +
                        // 成绩信息
                        "sg.total_grade AS totalGrade, " +
                        // 教师信息
                        "ti.name AS teacherName " +
                        "FROM student_grade sg " +
                        "INNER JOIN course_section cs ON sg.section_id = cs.section_id " +
                        "INNER JOIN course c ON cs.course_id = c.course_id " +
                        "INNER JOIN teacher_info ti ON cs.teacher_id = ti.teacherid " +
                        "WHERE sg.student_id = ? " +
                          "AND cs.semester = ? " +
                          "AND cs.section_status = '已通过' " +  // 只返回已审核成绩
                        "ORDER BY c.course_name ASC";

                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, studentId.trim());
                ps.setString(2, semester.trim());
                ResultSet rs = ps.executeQuery();

                // 5. 组装查询结果
                List<StudentGrades> resultList = new ArrayList<>();
                while (rs.next()) {
                    StudentGrades grade = new StudentGrades();
                    // 课程基本信息
                    grade.setCourseId(rs.getString("courseId"));
                    grade.setCourseName(rs.getString("courseName"));
                    grade.setCredit(rs.getBigDecimal("credit"));
                    
                    // 成绩信息
                    grade.setTotalGrade(rs.getBigDecimal("totalGrade"));
                    
                    // 教师信息
                    grade.setTeacherName(rs.getString("teacherName"));
                    
                    // 冗余信息（用于客户端校验）
                    grade.setStudentId(studentId);
                    grade.setSemester(semester);

                    resultList.add(grade);
                }

                // 6. 关闭资源并返回结果
                rs.close();
                ps.close();
                
                String jsonResponse = gson.toJson(resultList);
                out.writeUTF(jsonResponse);
                out.flush();
                System.out.println("学生[" + studentId + "]查询到" + resultList.size() + "条学期成绩记录");

            } catch (SQLException e) {
                sendErrorResponse("数据库操作失败：" + e.getMessage());
            } catch (Exception e) {
                sendErrorResponse("处理请求失败：" + e.getMessage());
            }
        }
        
        // 验证学生是否存在
        private boolean isValidStudent(String studentId) throws SQLException {
            String sql = "SELECT studentid FROM student_info WHERE studentid = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            rs.close();
            ps.close();
            return exists;
        }
        
        private void processAdminRetrieveCourses() {
            try {
                // 1. 读取客户端发送的查询条件（StudentGrades对象）
                String jsonCondition = in.readUTF();
                StudentGrades condition = gson.fromJson(jsonCondition, StudentGrades.class);

                // 2. 动态构建SQL查询语句
                StringBuilder sql = new StringBuilder();
                sql.append("SELECT " +
                        "cs.semester, " +
                        "c.course_id AS courseId, " +
                        "c.course_name AS courseName, " +
                        "ti.name AS teacherName, " +
                        "cs.class_time AS classTime, " +
                        "cs.section_status AS sectionStatus, " +
                        "cs.section_id AS sectionId " +
                        "FROM course_section cs " +
                        "INNER JOIN course c ON cs.course_id = c.course_id " +
                        "INNER JOIN teacher_info ti ON cs.teacher_id = ti.teacherid " +
                        "WHERE 1=1 "); // 基础条件，方便拼接AND

                // 3. 动态添加查询条件（非空才添加）
                List<String> params = new ArrayList<>();
                if (condition.getSemester() != null && !condition.getSemester().isEmpty()) {
                    sql.append("AND cs.semester = ? ");
                    params.add(condition.getSemester());
                }
                if (condition.getCourseId() != null && !condition.getCourseId().isEmpty()) {
                    sql.append("AND c.course_id LIKE ? "); // 模糊匹配
                    params.add("%" + condition.getCourseId() + "%");
                }
                if (condition.getCourseName() != null && !condition.getCourseName().isEmpty()) {
                    sql.append("AND c.course_name LIKE ? "); // 模糊匹配
                    params.add("%" + condition.getCourseName() + "%");
                }
                if (condition.getTeacherName() != null && !condition.getTeacherName().isEmpty()) {
                    sql.append("AND ti.name LIKE ? "); // 模糊匹配教师姓名
                    params.add("%" + condition.getTeacherName() + "%");
                }
                if (condition.getSectionStatus() != null && !condition.getSectionStatus().isEmpty()) {
                    sql.append("AND cs.section_status = ? ");
                    params.add(condition.getSectionStatus());
                }

                // 4. 执行查询
                PreparedStatement ps = conn.prepareStatement(sql.toString());
                // 设置参数
                for (int i = 0; i < params.size(); i++) {
                    ps.setString(i + 1, params.get(i));
                }
                ResultSet rs = ps.executeQuery();

                // 5. 封装结果为List<StudentGrades>
                List<StudentGrades> resultList = new ArrayList<>();
                while (rs.next()) {
                    StudentGrades grade = new StudentGrades();
                    grade.setSemester(rs.getString("semester"));
                    grade.setCourseId(rs.getString("courseId"));
                    grade.setCourseName(rs.getString("courseName"));
                    grade.setTeacherName(rs.getString("teacherName"));
                    grade.setClassTime(rs.getString("classTime")); // 对应class_time字段
                    grade.setSectionStatus(rs.getString("sectionStatus"));
                    grade.setSectionId(rs.getInt("sectionId")); // 保存教学班ID，便于后续操作
                    resultList.add(grade);
                }

                // 6. 关闭资源并返回结果
                rs.close();
                ps.close();

                String jsonResponse = gson.toJson(resultList);
                out.writeUTF(jsonResponse);
                out.flush();

            } catch (SQLException e) {
                sendErrorResponse("数据库查询失败：" + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                sendErrorResponse("处理请求失败：" + e.getMessage());
                e.printStackTrace();
            }
        }
        
        private void processAdminGetStudentsBySectionId() {
            try {
                // 1. 读取客户端请求
                String jsonRequest = in.readUTF();
                StudentGrades request = gson.fromJson(jsonRequest, StudentGrades.class);
                Integer sectionId = request.getSectionId();
                if (sectionId == null) {
                    sendErrorResponse("教学班ID不能为空");
                    return;
                }

                // 2. 联表查询：通过course_section关联到course表，获取final_ratio
                String sql = "SELECT " +
                        // 学生信息 + 成绩信息
                        "sg.grade_id AS gradeId, " +
                        "si.studentid AS studentId, " +
                        "si.name AS studentName, " +
                        "si.major AS major, " +
                        "sg.regular_grade AS regularGrade, " +
                        "sg.final_grade AS finalGrade, " +
                        "sg.total_grade AS totalGrade, " +
                        // 教学班状态（来自course_section）
                        "cs.section_status AS sectionStatus, " +
                        "cs.section_id AS sectionId," +
                        // 成绩占比（来自course表）
                        "c.final_ratio AS finalRatio " +  // 关键修复：从course表读取
                        "FROM student_grade sg " +
                        // 关联学生信息表
                        "INNER JOIN student_info si ON sg.student_id = si.studentid " +
                        // 关联教学班表（获取course_id）
                        "INNER JOIN course_section cs ON sg.section_id = cs.section_id " +
                        // 关联课程表（获取final_ratio）
                        "INNER JOIN course c ON cs.course_id = c.course_id " +  // 新增关联course表
                        // 查询条件：指定教学班ID
                        "WHERE sg.section_id = ? " +
                        "ORDER BY si.studentid ASC";

                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, sectionId);
                ResultSet rs = ps.executeQuery();

                // 3. 封装结果（包含从course表读取的finalRatio）
                List<StudentGrades> resultList = new ArrayList<>();
                while (rs.next()) {
                    StudentGrades grade = new StudentGrades();
                    // 学生基本信息
                    grade.setSectionId(rs.getInt("sectionId"));
                    grade.setGradeId(rs.getInt("gradeId"));
                    grade.setStudentId(rs.getString("studentId"));
                    grade.setStudentName(rs.getString("studentName"));
                    grade.setMajor(rs.getString("major"));
                    // 成绩信息
                    grade.setRegularGrade(rs.getBigDecimal("regularGrade"));
                    grade.setFinalGrade(rs.getBigDecimal("finalGrade"));
                    grade.setTotalGrade(rs.getBigDecimal("totalGrade"));
                    // 教学班信息
                    grade.setSectionId(sectionId);
                    grade.setSectionStatus(rs.getString("sectionStatus"));
                    // 关键修复：从course表读取final_ratio
                    grade.setFinalRatio(rs.getInt("finalRatio"));  // 正确赋值

                    resultList.add(grade);
                }

                // 4. 返回结果
                rs.close();
                ps.close();
                String jsonResponse = gson.toJson(resultList);
                out.writeUTF(jsonResponse);
                out.flush();
                System.out.println("管理员加载教学班[" + sectionId + "]学生成绩，共" + resultList.size() + "条记录");

            } catch (SQLException e) {
                sendErrorResponse("数据库操作失败：" + e.getMessage());
            } catch (Exception e) {
                sendErrorResponse("处理请求失败：" + e.getMessage());
            }
        }
        
        private void processAdminUpdateStudentGrade() {
        	try {
                // 1. 读取客户端请求（StudentGrades封装的修改参数）
                String jsonRequest = in.readUTF();
                StudentGrades request = gson.fromJson(jsonRequest, StudentGrades.class);

                // 2. 核心参数校验
                Integer gradeId = request.getGradeId();
                Integer sectionId = request.getSectionId();
                if (gradeId == null) {
                    sendErrorResponse("成绩ID不能为空");
                    return;
                }
                if (sectionId == null) {
                    sendErrorResponse("教学班ID不能为空");
                    return;
                }
                if (request.getRegularGrade() == null || request.getFinalGrade() == null || request.getTotalGrade() == null) {
                    sendErrorResponse("成绩字段（平时/期末/总成绩）不能为空");
                    return;
                }

                // 3. 权限校验：确保该成绩属于当前教学班（防止跨班修改）
                if (!isGradeBelongToSection(gradeId, sectionId)) {
                    sendErrorResponse("权限异常：该成绩不属于当前教学班");
                    return;
                }

                // 4. 执行更新SQL（修改平时/期末/总成绩）
                String updateSql = "UPDATE student_grade " +
                        "SET regular_grade = ?, final_grade = ?, total_grade = ? " +
                        "WHERE grade_id = ?";

                PreparedStatement ps = conn.prepareStatement(updateSql);
                ps.setBigDecimal(1, request.getRegularGrade());
                ps.setBigDecimal(2, request.getFinalGrade());
                ps.setBigDecimal(3, request.getTotalGrade());
                ps.setInt(4, gradeId);

                int affectedRows = ps.executeUpdate();
                ps.close();

                // 5. 反馈结果
                if (affectedRows > 0) {
                    out.writeUTF("{\"success\":true}"); // 成功响应
                    out.flush();
                    System.out.println("管理员修改成绩成功：gradeId=" + gradeId + ", sectionId=" + sectionId);
                } else {
                    sendErrorResponse("修改失败：未找到对应的成绩记录");
                }

            } catch (SQLException e) {
                // handleDBError("管理员修改成绩", e);
                sendErrorResponse("数据库操作失败：" + e.getMessage());
            } catch (Exception e) {
                sendErrorResponse("处理请求失败：" + e.getMessage());
            }

        }
        
        private boolean isGradeBelongToSection(Integer gradeId, Integer sectionId) throws SQLException {
            String sql = "SELECT 1 FROM student_grade WHERE grade_id = ? AND section_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, gradeId);
            ps.setInt(2, sectionId);
            return ps.executeQuery().next(); // 存在则返回true
        }
        
        private void processAdminPassCourseSection() {
            try {
                // 1. 读取客户端请求
                String jsonRequest = in.readUTF();
                StudentGrades request = gson.fromJson(jsonRequest, StudentGrades.class);

                // 2. 核心参数校验
                Integer sectionId = request.getSectionId();
                String courseName = request.getCourseName();
                if (sectionId == null) {
                    sendErrorResponse("教学班ID不能为空");
                    return;
                }
                if (courseName == null || courseName.trim().isEmpty()) {
                    sendErrorResponse("课程名称不能为空");
                    return;
                }

                // 3. 校验：教学班存在且状态不是“已通过”（避免重复审核）
                String currentStatus = getSectionCurrentStatus(sectionId, courseName);
                if (currentStatus == null) {
                    sendErrorResponse("教学班不存在：sectionId=" + sectionId + ", 课程=" + courseName);
                    return;
                }
                if ("已通过".equals(currentStatus)) {
                    sendErrorResponse("该教学班已审核通过，不可重复操作");
                    return;
                }

                // 4. 执行更新：将教学班状态改为“已通过”
                String updateSql = "UPDATE course_section " +
                        "SET section_status = '已通过' " +
                        "WHERE section_id = ? AND course_id = (SELECT course_id FROM course WHERE course_name = ?)";

                PreparedStatement ps = conn.prepareStatement(updateSql);
                ps.setInt(1, sectionId);
                ps.setString(2, courseName.trim());

                int affectedRows = ps.executeUpdate();
                ps.close();

                // 5. 反馈结果
                if (affectedRows > 0) {
                    out.writeUTF("{\"success\":true}");
                    out.flush();
                    System.out.println("管理员审核教学班成功：sectionId=" + sectionId + ", 状态改为已通过");
                } else {
                    sendErrorResponse("审核失败：未找到对应的教学班记录");
                }

            } catch (SQLException e) {
                handleDBError("管理员审核教学班", e);
                sendErrorResponse("数据库操作失败：" + e.getMessage());
            } catch (Exception e) {
                sendErrorResponse("处理请求失败：" + e.getMessage());
            }
        }
        
        // 辅助方法：获取教学班当前状态
        private String getSectionCurrentStatus(Integer sectionId, String courseName) throws SQLException {
            String sql = "SELECT cs.section_status " +
                    "FROM course_section cs " +
                    "INNER JOIN course c ON cs.course_id = c.course_id " +
                    "WHERE cs.section_id = ? AND c.course_name = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, sectionId);
            ps.setString(2, courseName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("section_status");
            }
            return null; // 不存在返回null
        }
        
        // ------------------------------------------------------------------------------------------------------------------------------------------
     // 处理图书信息请求
        // 在 Server.java 的 processBookInfoRequest 方法中修改
           private void processBookInfoRequest() {
               try {
                   // 读取BookInfoRequest的JSON数据
                   String jsonReq = in.readUTF();
                   BookInfoRequest req = gson.fromJson(jsonReq, BookInfoRequest.class);

                   System.out.println("[Event]收到图书信息请求 - 搜索类型: " + req.getSearchType());

                   // 查询数据库
                   List<BookInfo> bookList = getBookInfoFromDB(req);

                   // 发送响应 - 确保总是返回有效的JSON数组
                   String json;
                   if (bookList != null && !bookList.isEmpty()) {
                       json = gson.toJson(bookList);
                       System.out.println("[Event]图书信息查询成功，返回" + bookList.size() + "条记录");
                   } else {
                       // 如果没有找到图书，返回空数组而不是null或空字符串
                       json = "[]";
                       System.out.println("[Event]图书信息查询成功，但未找到记录，返回空数组");
                   }

                   out.writeUTF("SUCCESS|" + json);
                   out.flush();

               } catch (SQLException e) {
                   try {
                       out.writeUTF("ERROR|数据库查询失败：" + e.getMessage());
                       out.flush();
                   } catch (IOException ex) {
                       System.out.println("[Error]发送数据库错误响应失败：" + ex.getMessage());
                   }
               } catch (Exception e) {
                   try {
                       out.writeUTF("ERROR|处理请求失败：" + e.getMessage());
                       out.flush();
                   } catch (IOException ex) {
                       System.out.println("[Error]发送异常响应失败：" + ex.getMessage());
                   }
               }
           }

        // 添加处理借阅请求的方法
           private void processBorrowRequest() {
               try {
                   String jsonReq = in.readUTF();
                   BorrowRequest req = gson.fromJson(jsonReq, BorrowRequest.class);

                   System.out.println("[Event]收到借阅请求 - 用户: " + req.getUserId() +
                                     ", 操作: " + req.getOperationType() +
                                     ", 图书: " + req.getCallNumber());

                   boolean success = false;
                   String message = "";

                   if ("BORROW".equals(req.getOperationType())) {
                       success = borrowBook(req.getUserId(), req.getCallNumber());
                       message = success ? "借阅成功" : "借阅失败，图书可能已被借出或不存在";
                   } else if ("RETURN".equals(req.getOperationType())) {
                       success = returnBook(req.getUserId(), req.getCallNumber());
                       message = success ? "归还成功" : "归还失败";
                   }// 在 Server.java 的 processBorrowRequest 方法中，修改 RENEW 分支
                   else if ("RENEW".equals(req.getOperationType())) {
                       try {
                           success = renewBook(req.getUserId(), req.getCallNumber());
                           if (success) {
                               // 续借成功后，获取更新后的借阅信息
                               BorrowInfo updatedInfo = getUpdatedBorrowInfo(req.getUserId(), req.getCallNumber());
                               if (updatedInfo != null) {
                                   // 格式化日期
                                   SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                                   String newDueDate = sdf.format(updatedInfo.getDueDate());
                                   int newRenewCount = updatedInfo.getRenewCount();

                                   // 返回新应还日期和续借次数
                                   String response = "SUCCESS|" + newDueDate + "|" + newRenewCount;
                                   out.writeUTF(response);
                                   System.out.println("[Event]续借成功，返回数据: " + response);
                               } else {
                                   // 如果获取更新信息失败，返回错误
                                   out.writeUTF("ERROR|获取更新后的借阅信息失败");
                                   System.out.println("[Error]获取更新后的借阅信息失败");
                               }
                           } else {
                               out.writeUTF("ERROR|续借失败，可能已达到最大续借次数或图书不存在");
                               System.out.println("[Error]续借失败");
                           }
                       } catch (Exception e) {
                           out.writeUTF("ERROR|续借过程中发生异常: " + e.getMessage());
                           System.out.println("[Error]续借过程中发生异常: " + e.getMessage());
                           e.printStackTrace();
                       }
                       out.flush();
                   }

                   // 发送响应
                   if (success) {
                       out.writeUTF("SUCCESS|" + message);
                   } else {
                       out.writeUTF("ERROR|" + message);
                   }
                   out.flush();

               } catch (Exception e) {
                   try {
                       out.writeUTF("ERROR|处理借阅请求失败：" + e.getMessage());
                       out.flush();
                   } catch (IOException ex) {
                       System.out.println("[Error]发送借阅错误响应失败：" + ex.getMessage());
                   }
               }
           }

        // 处理借阅信息查询请求
           private void processBorrowInfoRequest() {
               try {
                   String jsonReq = in.readUTF();
                   BorrowInfoRequest req = gson.fromJson(jsonReq, BorrowInfoRequest.class);

                   System.out.println("[Event]收到借阅信息查询请求 - 用户: " + req.getUserId());

                   // 查询借阅信息
                   List<BorrowInfo> borrowList = getBorrowInfoFromDB(req.getUserId());

                   // 发送响应
                   if (borrowList != null) {
                       String json = gson.toJson(borrowList);
                       out.writeUTF("SUCCESS|" + json);
                       System.out.println("[Event]用户" + req.getUserId() + "借阅信息查询成功，返回" + borrowList.size() + "条记录");
                   } else {
                       out.writeUTF("ERROR|未找到借阅记录");
                       System.out.println("[Event]用户" + req.getUserId() + "借阅信息查询失败，未找到记录");
                   }
                   out.flush();

               } catch (Exception e) {
                   try {
                       out.writeUTF("ERROR|处理借阅信息查询请求失败：" + e.getMessage());
                       out.flush();
                   } catch (IOException ex) {
                       System.out.println("[Error]发送借阅信息查询错误响应失败：" + ex.getMessage());
                   }
               }
           }

           private void processRoomInfoRequest() {
               try {
                   // 读取请求参数
                   String jsonReq = in.readUTF();
                   System.out.println("[Debug] 接收到的RoomInfoRequest JSON: " + jsonReq);

                   // 尝试解析JSON
                   JsonElement jsonElement = JsonParser.parseString(jsonReq);
                   if (!jsonElement.isJsonObject()) {
                       out.writeUTF("ERROR|请求格式不正确，应为JSON对象");
                       out.flush();
                       return;
                   }

                   JsonObject requestJson = jsonElement.getAsJsonObject();

                   // 检查请求中是否包含必要的字段
                   if (!requestJson.has("date")) {
                       out.writeUTF("ERROR|请求参数缺失date字段");
                       out.flush();
                       return;
                   }
                   if (!requestJson.has("timeSlot")) {
                       out.writeUTF("ERROR|请求参数缺失timeSlot字段");
                       out.flush();
                       return;
                   }

                   // 解析日期和时段
                   JsonElement dateElement = requestJson.get("date");
                   JsonElement timeSlotElement = requestJson.get("timeSlot");

                   if (dateElement.isJsonNull() || timeSlotElement.isJsonNull()) {
                       out.writeUTF("ERROR|date或timeSlot字段值为null");
                       out.flush();
                       return;
                   }

                   LocalDate date = gson.fromJson(dateElement, LocalDate.class);
                   String timeSlot = timeSlotElement.getAsString();

                   System.out.println("[Debug] 解析后的参数 - 日期: " + date + ", 时段: " + timeSlot);

                   // 查询研讨间信息（根据日期和时段）
                   List<RoomInfo> roomList = getRoomInfoFromDBWithDate(date, timeSlot);

                   // 发送响应
                   if (roomList != null && !roomList.isEmpty()) {
                       String json = gson.toJson(roomList);
                       out.writeUTF("SUCCESS|" + json);
                       System.out.println("[Event]研讨间信息查询成功，返回" + roomList.size() + "条记录");
                   } else {
                       out.writeUTF("ERROR|未找到研讨间信息");
                       System.out.println("[Event]研讨间信息查询失败，未找到记录");
                   }
                   out.flush();
               } catch (Exception e) {
                   try {
                       String errorMsg = "处理研讨间信息请求失败：" + e.getMessage();
                       out.writeUTF("ERROR|" + errorMsg);
                       out.flush();
                       System.out.println("[Error]" + errorMsg);
                       e.printStackTrace();
                   } catch (IOException ex) {
                       System.out.println("[Error]发送研讨间信息错误响应失败：" + ex.getMessage());
                   }
               }
           }

           private void processRoomBookingRequest() {
               try {
                   // 读取预约请求
                   String jsonReq = in.readUTF();
                   RoomBookingRequest req = gson.fromJson(jsonReq, RoomBookingRequest.class);
                   System.out.println("[Event]收到研讨间预约请求 - 用户: " + req.getUserId() +
                                     ", 房间: " + req.getRoomId() +
                                     ", 日期: " + req.getBookingDate());

                   // 处理预约
                   boolean success = createRoomBooking(req);

                   // 发送响应
                   if (success) {
                       out.writeUTF("SUCCESS|预约成功");
                       System.out.println("[Event]用户" + req.getUserId() + "研讨间预约成功");
                   } else {
                       out.writeUTF("ERROR|预约失败，该时段可能已被预约");
                       System.out.println("[Event]用户" + req.getUserId() + "研讨间预约失败");
                   }
                   out.flush();
               } catch (Exception e) {
                   try {
                       out.writeUTF("ERROR|处理预约请求失败：" + e.getMessage());
                       out.flush();
                   } catch (IOException ex) {
                       System.out.println("[Error]发送预约错误响应失败：" + ex.getMessage());
                   }
               }
           }
           
           /**
            * 处理文件上传请求
            */
           private void processFileUploadRequest() {
               try {
                   // 读取文件名和文件大小
                   String fileName = in.readUTF();
                   long fileSize = in.readLong();
                   
                // 验证文件类型
                   if (!fileName.toLowerCase().endsWith(".pdf")) {
                       out.writeUTF("ERROR|只支持PDF文件上传");
                       out.flush();
                       return;
                   }
                   
                   // 指定文件保存目录（确保此目录存在且有写权限）
                   File uploadDir = new File("D:/qq/接受文件/vcampus-projects-new/vcampus-projects-new/vcampus-projects-2/vcampus_server/resources/seu/vcampus/server/books");
                   if (!uploadDir.exists()) {
                       uploadDir.mkdirs();
                   }
                   
                   File outputFile = new File(uploadDir, fileName);
                   
                   if (outputFile.exists()) {
                       String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                       String extension = fileName.substring(fileName.lastIndexOf('.'));
                       String timestamp = String.valueOf(System.currentTimeMillis());
                       fileName = baseName + "_" + timestamp + extension;
                       outputFile = new File(uploadDir, fileName);
                   }
                   // 接收文件内容
                   FileOutputStream fos = new FileOutputStream(outputFile);
                   byte[] buffer = new byte[4096];
                   long remaining = fileSize;
                   
                   while (remaining > 0) {
                       int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                       if (read < 0) {
                           throw new EOFException("Unexpected end of stream");
                       }
                       fos.write(buffer, 0, read);
                       remaining -= read;
                   }
                   fos.close();
                   
                   // 返回成功响应和服务器保存的文件名
                   out.writeUTF("SUCCESS|" + fileName);
                   out.flush();
                   
                   System.out.println("[Event]文件上传成功: " + fileName + " -> " + fileName);
                   
               } catch (IOException e) {
                   try {
                       out.writeUTF("ERROR|文件上传失败: " + e.getMessage());
                       out.flush();
                   } catch (IOException ex) {
                       System.out.println("[Error]发送错误响应失败: " + ex.getMessage());
                   }
               }
           }

           /**
            * 处理管理员图书操作请求（添加、编辑、删除）
            */
           private void processAdminBookRequest() {
               try {
                   String jsonReq = in.readUTF();
                   AdminBookRequest req = gson.fromJson(jsonReq, AdminBookRequest.class);

                   System.out.println("[Event]收到管理员图书操作请求 - 操作类型: " + req.getOperationType());

                   boolean success = false;
                   String message = "";

                   if ("ADD".equals(req.getOperationType())) {
                       success = addBook(req.getBookInfo());
                       message = success ? "添加图书成功" : "添加图书失败";
                   } else if ("EDIT".equals(req.getOperationType())) {
                       success = editBook(req.getBookInfo());
                       message = success ? "编辑图书成功" : "编辑图书失败";
                   } else if ("DELETE".equals(req.getOperationType())) {
                       success = deleteBook(req.getBookInfo().getCallNumber());
                       message = success ? "删除图书成功" : "删除图书失败";
                   }

                   // 发送响应
                   if (success) {
                       out.writeUTF("SUCCESS|" + message);
                   } else {
                       out.writeUTF("ERROR|" + message);
                   }
                   out.flush();

               } catch (Exception e) {
                   try {
                       out.writeUTF("ERROR|处理管理员图书请求失败：" + e.getMessage());
                       out.flush();
                   } catch (IOException ex) {
                       System.out.println("[Error]发送管理员图书错误响应失败：" + ex.getMessage());
                   }
               }
           }

           private void processAdminRoomInfoRequest() {
               try {
                   // 读取请求
                   String jsonReq = in.readUTF();
                   AdminRoomInfoRequest req = gson.fromJson(jsonReq, AdminRoomInfoRequest.class);

                   System.out.println("[Event]收到管理员研讨间信息请求");

                   // 从数据库获取所有研讨间信息
                   List<RoomInfo> roomList = getAllRoomInfoFromDB();

                   // 发送响应
                   if (roomList != null && !roomList.isEmpty()) {
                       String json = gson.toJson(roomList);
                       out.writeUTF("SUCCESS|" + json);
                       System.out.println("[Event]管理员研讨间信息查询成功，返回" + roomList.size() + "条记录");
                   } else {
                       out.writeUTF("ERROR|未找到研讨间信息");
                       System.out.println("[Event]管理员研讨间信息查询失败，未找到记录");
                   }
                   out.flush();

               } catch (Exception e) {
                   try {
                       out.writeUTF("ERROR|处理管理员研讨间信息请求失败：" + e.getMessage());
                       out.flush();
                   } catch (IOException ex) {
                       System.out.println("[Error]发送管理员研讨间信息错误响应失败：" + ex.getMessage());
                   }
               }
           }

           private void processAdminAddRoomRequest() {
               try {
                   String jsonReq = in.readUTF();
                   RoomInfo roomInfo = gson.fromJson(jsonReq, RoomInfo.class);

                   System.out.println("[Event]收到添加研讨间请求 - 房间ID: " + roomInfo.getRoomId());

                   // 添加研讨间到数据库
                   boolean success = addRoomToDB(roomInfo);

                   // 发送响应
                   if (success) {
                       out.writeUTF("SUCCESS|添加研讨间成功");
                       System.out.println("[Event]添加研讨间成功: " + roomInfo.getRoomId());
                   } else {
                       out.writeUTF("ERROR|添加研讨间失败，房间ID可能已存在");
                       System.out.println("[Event]添加研讨间失败: " + roomInfo.getRoomId());
                   }
                   out.flush();

               } catch (Exception e) {
                   try {
                       out.writeUTF("ERROR|处理添加研讨间请求失败：" + e.getMessage());
                       out.flush();
                   } catch (IOException ex) {
                       System.out.println("[Error]发送添加研讨间错误响应失败：" + ex.getMessage());
                   }
               }
           }

           private void processAdminEditRoomRequest() {
               try {
                   String jsonReq = in.readUTF();
                   RoomInfo roomInfo = gson.fromJson(jsonReq, RoomInfo.class);

                   System.out.println("[Event]收到编辑研讨间请求 - 房间ID: " + roomInfo.getRoomId());

                   // 更新研讨间信息
                   boolean success = updateRoomInDB(roomInfo);

                   // 发送响应
                   if (success) {
                       out.writeUTF("SUCCESS|编辑研讨间成功");
                       System.out.println("[Event]编辑研讨间成功: " + roomInfo.getRoomId());
                   } else {
                       out.writeUTF("ERROR|编辑研讨间失败，房间可能不存在");
                       System.out.println("[Event]编辑研讨间失败: " + roomInfo.getRoomId());
                   }
                   out.flush();

               } catch (Exception e) {
                   try {
                       out.writeUTF("ERROR|处理编辑研讨间请求失败：" + e.getMessage());
                       out.flush();
                   } catch (IOException ex) {
                       System.out.println("[Error]发送编辑研讨间错误响应失败：" + ex.getMessage());
                   }
               }
           }

           private void processAdminDeleteRoomRequest() {
               try {
                   String jsonReq = in.readUTF();
                   AdminDeleteRoomRequest req = gson.fromJson(jsonReq, AdminDeleteRoomRequest.class);

                   System.out.println("[Event]收到删除研讨间请求 - 房间ID: " + req.getRoomId());

                   // 删除研讨间
                   boolean success = deleteRoomFromDB(req.getRoomId());

                   // 发送响应
                   if (success) {
                       out.writeUTF("SUCCESS|删除研讨间成功");
                       System.out.println("[Event]删除研讨间成功: " + req.getRoomId());
                   } else {
                       out.writeUTF("ERROR|删除研讨间失败，房间可能不存在或有未完成的预约");
                       System.out.println("[Event]删除研讨间失败: " + req.getRoomId());
                   }
                   out.flush();

               } catch (Exception e) {
                   try {
                       out.writeUTF("ERROR|处理删除研讨间请求失败：" + e.getMessage());
                       out.flush();
                   } catch (IOException ex) {
                       System.out.println("[Error]发送删除研讨间错误响应失败：" + ex.getMessage());
                   }
               }
           }

        // 添加处理管理员研讨间预约记录请求的方法
           private void processAdminRoomBookingInfoRequest() {
               try {
                   // 读取请求
                   String jsonReq = in.readUTF();
                   AdminRoomBookingRequest req = gson.fromJson(jsonReq, AdminRoomBookingRequest.class);

                   System.out.println("[Event]收到管理员研讨间预约记录请求 - 房间ID: " + req.getRoomId());

                   // 从数据库获取指定房间的预约记录
                   List<RoomBookingInfo> bookingList = getRoomBookingInfoFromDB(req.getRoomId());

                   // 发送响应
                   if (bookingList != null) {
                       String json = gson.toJson(bookingList);
                       out.writeUTF("SUCCESS|" + json);
                       System.out.println("[Event]管理员研讨间预约记录查询成功，返回" + bookingList.size() + "条记录");
                   } else {
                       out.writeUTF("ERROR|未找到预约记录");
                       System.out.println("[Event]管理员研讨间预约记录查询失败，未找到记录");
                   }
                   out.flush();

               } catch (Exception e) {
                   try {
                       out.writeUTF("ERROR|处理管理员研讨间预约记录请求失败：" + e.getMessage());
                       out.flush();
                   } catch (IOException ex) {
                       System.out.println("[Error]发送管理员研讨间预约记录错误响应失败：" + ex.getMessage());
                   }
               }
           }

        // 从数据库中获取图书信息方法
           private List<BookInfo> getBookInfoFromDB(BookInfoRequest req) throws SQLException {
               List<BookInfo> bookList = new ArrayList<>();
               StringBuilder sql = new StringBuilder("SELECT *, COALESCE(pdf_url, '') as pdf_url FROM book_info WHERE 1=1");
               List<Object> params = new ArrayList<>();

               // 根据请求条件动态构建SQL
               if (req.getKeyword() != null && !req.getKeyword().isEmpty()) {
                   sql.append(" AND (title LIKE ? OR author LIKE ? OR publisher LIKE ?)");
                   String keywordParam = "%" + req.getKeyword() + "%";
                   params.add(keywordParam);
                   params.add(keywordParam);
                   params.add(keywordParam);
               }

               if (req.getCategory() != null && !req.getCategory().isEmpty() && !"全部".equals(req.getCategory())) {
                   sql.append(" AND category = ?");
                   params.add(req.getCategory());
               }

               if (req.getLocation() != null && !req.getLocation().isEmpty() && !"全部".equals(req.getLocation())) {
                   sql.append(" AND location = ?");
                   params.add(req.getLocation());
               }

               try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                   // 设置参数
                   for (int i = 0; i < params.size(); i++) {
                       pstmt.setObject(i + 1, params.get(i));
                   }

                   ResultSet rs = pstmt.executeQuery();
                   while (rs.next()) {
                       BookInfo info = new BookInfo();
                       info.setCallNumber(rs.getString("call_number"));
                       info.setTitle(rs.getString("title"));
                       info.setAuthor(rs.getString("author"));
                       info.setPublisher(rs.getString("publisher"));
                       info.setPublishYear(rs.getString("publish_year"));
                       info.setCategory(rs.getString("category"));
                       info.setLocation(rs.getString("location"));
                       info.setStatus(rs.getString("status"));
                       info.setPdfUrl(rs.getString("pdf_url")); // 新增：设置PDF URL
                       bookList.add(info);
                   }
               }
               return bookList;
           }

        // 从数据库获取借阅信息
           private List<BorrowInfo> getBorrowInfoFromDB(String userId) throws SQLException {
               List<BorrowInfo> borrowList = new ArrayList<>();
               // 使用 DATE_FORMAT 函数确保日期格式为 yyyy-MM-dd
               String sql = "SELECT br.*, bi.author, bi.publisher, " +
                            "DATE_FORMAT(br.borrow_date, '%Y-%m-%d') as borrow_date_fmt, " +
                            "DATE_FORMAT(br.due_date, '%Y-%m-%d') as due_date_fmt " +
                            "FROM borrow_records br " +
                            "LEFT JOIN book_info bi ON br.call_number = bi.call_number " +
                            "WHERE br.user_id = ? AND br.return_date IS NULL ORDER BY br.borrow_date DESC";

               try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                   pstmt.setString(1, userId);
                   ResultSet rs = pstmt.executeQuery();

                   while (rs.next()) {
                       BorrowInfo info = new BorrowInfo();
                       info.setBorrowId(rs.getString("borrow_id"));
                       info.setUserId(rs.getString("user_id"));
                       info.setCallNumber(rs.getString("call_number"));
                       info.setBookTitle(rs.getString("title"));
                       info.setAuthor(rs.getString("author"));
                       info.setPublisher(rs.getString("publisher"));

                       // 使用格式化后的日期
                       SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                       try {
                           info.setBorrowDate(sdf.parse(rs.getString("borrow_date_fmt")));
                           info.setDueDate(sdf.parse(rs.getString("due_date_fmt")));
                       } catch (ParseException e) {
                           // 如果解析失败，使用原始日期
                           info.setBorrowDate(rs.getDate("borrow_date"));
                           info.setDueDate(rs.getDate("due_date"));
                           System.out.println("[Warning] 日期解析失败，使用原始日期: " + e.getMessage());
                       }

                       info.setReturnDate(rs.getDate("return_date"));
                       info.setRenewCount(rs.getInt("renew_count"));
                       info.setStatus(rs.getString("status"));
                       borrowList.add(info);
                   }
               }
               return borrowList;
           }

        // 从数据库获取研讨间信息
           private List<RoomInfo> getRoomInfoFromDB() throws SQLException {
               List<RoomInfo> roomList = new ArrayList<>();
               String sql = "SELECT * FROM room_info ORDER BY floor, room_number";

               try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {

                   while (rs.next()) {
                       RoomInfo info = new RoomInfo();
                       info.setRoomId(rs.getString("room_id"));
                       info.setFloor(rs.getInt("floor"));
                       info.setRoomNumber(rs.getInt("room_number"));
                       info.setCapacity(rs.getInt("capacity"));
                       info.setStatus(rs.getString("status"));
                       info.setFacilities(rs.getString("facilities"));
                       roomList.add(info);
                   }
               }
               return roomList;
           }

        // 添加新的方法，根据日期和时段查询房间信息
           private List<RoomInfo> getRoomInfoFromDBWithDate(LocalDate date, String timeSlot) throws SQLException {
               List<RoomInfo> roomList = new ArrayList<>();

               // 查询所有房间基本信息
               String baseSql = "SELECT * FROM room_info ORDER BY floor, room_number";

               try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(baseSql)) {

                   while (rs.next()) {
                       RoomInfo info = new RoomInfo();
                       info.setRoomId(rs.getString("room_id"));
                       info.setFloor(rs.getInt("floor"));
                       info.setRoomNumber(rs.getInt("room_number"));
                       info.setCapacity(rs.getInt("capacity"));
                       info.setFacilities(rs.getString("facilities"));

                       // 默认状态为可用
                       String status = "available";

                       // 检查房间是否在维护中
                       if ("maintenance".equals(rs.getString("status"))) {
                           status = "maintenance";
                       } else {
                           // 检查指定日期和时段是否已被预约
                           String checkSql = "SELECT COUNT(*) FROM room_booking WHERE room_id = ? AND booking_date = ? AND time_slot = ? AND status = 'confirmed'";
                           try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                               pstmt.setString(1, info.getRoomId());
                               pstmt.setDate(2, java.sql.Date.valueOf(date));
                               pstmt.setString(3, timeSlot);
                               ResultSet checkRs = pstmt.executeQuery();

                               if (checkRs.next() && checkRs.getInt(1) > 0) {
                                   status = "reserved"; // 已被预约
                               }
                           }
                       }

                       info.setStatus(status);
                       roomList.add(info);
                   }
               }
               return roomList;
           }

        // 修改 getUpdatedBorrowInfo 方法
           private BorrowInfo getUpdatedBorrowInfo(String userId, String callNumber) throws SQLException {
               String sql = "SELECT br.due_date, br.renew_count " +
                            "FROM borrow_records br " +
                            "WHERE br.user_id = ? AND br.call_number = ? AND br.return_date IS NULL";

               System.out.println("[Debug] 查询更新后的借阅信息: userId=" + userId + ", callNumber=" + callNumber);

               try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                   pstmt.setString(1, userId);
                   pstmt.setString(2, callNumber);
                   ResultSet rs = pstmt.executeQuery();

                   if (rs.next()) {
                       BorrowInfo info = new BorrowInfo();
                       Date dueDate = rs.getDate("due_date");
                       int renewCount = rs.getInt("renew_count");

                       info.setDueDate(dueDate);
                       info.setRenewCount(renewCount);

                       System.out.println("[Debug] 查询到续借后的信息: due_date=" +
                                         dueDate + ", renew_count=" + renewCount);
                       return info;
                   } else {
                       System.out.println("[Error] 未找到续借后的借阅记录");
                       return null;
                   }
               }
           }

        // 创建研讨间预约
           // 在 createRoomBooking 方法中，修改日期处理
              private boolean createRoomBooking(RoomBookingRequest req) throws SQLException {
                  // 检查时间段是否已被预约
                  for (String timeSlot : req.getTimeSlots()) {
                      String checkSql = "SELECT COUNT(*) FROM room_booking WHERE room_id = ? AND booking_date = ? AND time_slot = ? AND status = 'confirmed'";
                      try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                          pstmt.setString(1, req.getRoomId());
                          pstmt.setDate(2, java.sql.Date.valueOf(req.getBookingDate())); // 使用 valueOf 转换
                          pstmt.setString(3, timeSlot);
                          ResultSet rs = pstmt.executeQuery();

                          if (rs.next() && rs.getInt(1) > 0) {
                              return false; // 该时段已被预约
                          }
                      }
                  }

                  // 插入预约记录
                  String insertSql = "INSERT INTO room_booking (room_id, user_id, booking_date, time_slot, purpose) VALUES (?, ?, ?, ?, ?)";
                  try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                      for (String timeSlot : req.getTimeSlots()) {
                          pstmt.setString(1, req.getRoomId());
                          pstmt.setString(2, req.getUserId());
                          pstmt.setDate(3, java.sql.Date.valueOf(req.getBookingDate())); // 使用 valueOf 转换
                          pstmt.setString(4, timeSlot);
                          pstmt.setString(5, req.getPurpose());
                          pstmt.addBatch();
                      }
                      pstmt.executeBatch();
                  }

                  return true;
              }

           // 新增方法：根据索书号获取图书信息
              private BookInfo getBookInfoByCallNumber(String callNumber) throws SQLException {
                  String sql = "SELECT * FROM book_info WHERE call_number = ?";
                  try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                      pstmt.setString(1, callNumber);
                      ResultSet rs = pstmt.executeQuery();

                      if (rs.next()) {
                          BookInfo info = new BookInfo();
                          info.setCallNumber(rs.getString("call_number"));
                          info.setTitle(rs.getString("title"));
                          info.setAuthor(rs.getString("author"));
                          info.setPublisher(rs.getString("publisher"));
                          info.setPublishYear(rs.getString("publish_year"));
                          info.setCategory(rs.getString("category"));
                          info.setLocation(rs.getString("location"));
                          info.setStatus(rs.getString("status"));
                          return info;
                      }
                      return null;
                  }
              }

              private List<RoomInfo> getAllRoomInfoFromDB() throws SQLException {
                  List<RoomInfo> roomList = new ArrayList<>();
                  String sql = "SELECT * FROM room_info ORDER BY floor, room_number";

                  try (Statement stmt = conn.createStatement();
                       ResultSet rs = stmt.executeQuery(sql)) {

                      while (rs.next()) {
                          RoomInfo info = new RoomInfo();
                          info.setRoomId(rs.getString("room_id"));
                          info.setFloor(rs.getInt("floor"));
                          info.setRoomNumber(rs.getInt("room_number"));
                          info.setCapacity(rs.getInt("capacity"));
                          info.setStatus(rs.getString("status"));
                          info.setFacilities(rs.getString("facilities"));
                          roomList.add(info);
                      }
                  }
                  return roomList;
              }

           // 添加从数据库获取研讨间预约记录的方法
              private List<RoomBookingInfo> getRoomBookingInfoFromDB(String roomId) throws SQLException {
                  List<RoomBookingInfo> bookingList = new ArrayList<>();
                  String sql = "SELECT * FROM room_booking WHERE room_id = ? ORDER BY booking_date DESC, time_slot";

                  try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                      pstmt.setString(1, roomId);
                      ResultSet rs = pstmt.executeQuery();

                      while (rs.next()) {
                          RoomBookingInfo info = new RoomBookingInfo();
                          info.setBookingId(rs.getInt("booking_id"));
                          info.setRoomId(rs.getString("room_id"));
                          info.setUserId(rs.getString("user_id"));
                          info.setBookingDate(rs.getDate("booking_date").toLocalDate());
                          info.setTimeSlot(rs.getString("time_slot"));
                          info.setPurpose(rs.getString("purpose"));
                          info.setCreateTime(rs.getDate("create_time").toLocalDate());
                          info.setStatus(rs.getString("status"));
                          bookingList.add(info);
                      }
                  }
                  return bookingList;
              }

           // 借阅图书方法
              private boolean borrowBook(String userId, String callNumber) throws SQLException {
                  // 首先获取图书信息
                  BookInfo bookInfo = getBookInfoByCallNumber(callNumber);
                  // 检查图书是否可借
                  if ((bookInfo == null) || !"可借".equals(bookInfo.getStatus())) {
                      return false; // 图书不可借
                  }

                  // 更新图书状态为"借出"
                  String updateSql = "UPDATE book_info SET status = '借出' WHERE call_number = ?";
                  try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                      pstmt.setString(1, callNumber);
                      pstmt.executeUpdate();
                  }

               // 添加借阅记录
                  String insertSql = "INSERT INTO borrow_records (user_id, call_number, title, author, publisher, borrow_date, due_date, status) " +
                          "VALUES (?, ?, ?, ?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), '借阅中')";
                  try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                  pstmt.setString(1, userId);
                  pstmt.setString(2, callNumber);
                  pstmt.setString(3, bookInfo.getTitle());
                  pstmt.setString(4, bookInfo.getAuthor());
                  pstmt.setString(5, bookInfo.getPublisher());
                  pstmt.executeUpdate();
                  }

                  return true;
              }

              // 归还图书方法
              private boolean returnBook(String userId, String callNumber) throws SQLException {
                  // 更新图书状态为"可借"
                  String updateBookSql = "UPDATE book_info SET status = '可借' WHERE call_number = ?";
                  try (PreparedStatement pstmt = conn.prepareStatement(updateBookSql)) {
                      pstmt.setString(1, callNumber);
                      pstmt.executeUpdate();
                  }

                  // 更新借阅记录
                  String updateRecordSql = "UPDATE borrow_records SET return_date = CURDATE(), status = '已归还' " +
                                         "WHERE user_id = ? AND call_number = ? AND return_date IS NULL";
                  try (PreparedStatement pstmt = conn.prepareStatement(updateRecordSql)) {
                      pstmt.setString(1, userId);
                      pstmt.setString(2, callNumber);
                      int rowsAffected = pstmt.executeUpdate();
                      return rowsAffected > 0;
                  }
              }

              // 续借图书方法
           // 续借图书方法
              private boolean renewBook(String userId, String callNumber) throws SQLException {
                  // 检查是否已续借过（最多续借三次）
                  String checkSql = "SELECT renew_count FROM borrow_records WHERE user_id = ? AND call_number = ? AND return_date IS NULL";
                  try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                      pstmt.setString(1, userId);
                      pstmt.setString(2, callNumber);
                      ResultSet rs = pstmt.executeQuery();

                      if (rs.next()) {
                          int renewCount = rs.getInt("renew_count");
                          if (renewCount >= 1) { // 最多续借一次
                              System.out.println("[Info] 用户 " + userId + " 尝试续借 " + callNumber + " 但已达到最大续借次数");
                              return false;
                          }
                      } else {
                          System.out.println("[Error] 未找到借阅记录: userId=" + userId + ", callNumber=" + callNumber);
                          return false; // 没有找到借阅记录
                      }
                  }

                  // 更新借阅记录
                  String updateSql = "UPDATE borrow_records SET due_date = DATE_ADD(due_date, INTERVAL 30 DAY), " +
                          "renew_count = renew_count + 1 WHERE user_id = ? AND call_number = ? AND return_date IS NULL";
                  try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                      pstmt.setString(1, userId);
                      pstmt.setString(2, callNumber);
                      int rowsAffected = pstmt.executeUpdate();

                      if (rowsAffected > 0) {
                          System.out.println("[Info] 用户 " + userId + " 成功续借 " + callNumber);
                          return true;
                      } else {
                          System.out.println("[Error] 更新借阅记录失败: userId=" + userId + ", callNumber=" + callNumber);
                          return false;
                      }
                  }
              }
              /**
               * 添加图书到数据库
               */
              private boolean addBook(BookInfo bookInfo) throws SQLException {
                  String sql = "INSERT INTO book_info (call_number, title, author, publisher, publish_year, category, location, status,pdf_url) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?,?)";

                  try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                      pstmt.setString(1, bookInfo.getCallNumber());
                      pstmt.setString(2, bookInfo.getTitle());
                      pstmt.setString(3, bookInfo.getAuthor());
                      pstmt.setString(4, bookInfo.getPublisher());
                      pstmt.setString(5, bookInfo.getPublishYear());
                      pstmt.setString(6, bookInfo.getCategory());
                      pstmt.setString(7, bookInfo.getLocation());
                      pstmt.setString(8, bookInfo.getStatus());
                      pstmt.setString(9, bookInfo.getPdfUrl());
                      int rowsAffected = pstmt.executeUpdate();
                      System.out.println("[Debug] 添加图书: " + bookInfo.getCallNumber() + ", 影响行数: " + rowsAffected);
                      return rowsAffected > 0;
                  } catch (SQLException e) {
                      System.out.println("[Error] 添加图书失败: " + e.getMessage());
                      throw e;
                  }
              }

              /**
               * 编辑图书信息
               */
              private boolean editBook(BookInfo bookInfo) throws SQLException {
                  String sql = "UPDATE book_info SET title = ?, author = ?, publisher = ?, publish_year = ?, " +
                               "category = ?, location = ?, status = ? WHERE call_number = ?";

                  try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                      pstmt.setString(1, bookInfo.getTitle());
                      pstmt.setString(2, bookInfo.getAuthor());
                      pstmt.setString(3, bookInfo.getPublisher());
                      pstmt.setString(4, bookInfo.getPublishYear());
                      pstmt.setString(5, bookInfo.getCategory());
                      pstmt.setString(6, bookInfo.getLocation());
                      pstmt.setString(7, bookInfo.getStatus());
                      pstmt.setString(8, bookInfo.getCallNumber());

                      int rowsAffected = pstmt.executeUpdate();
                      return rowsAffected > 0;
                  }
              }

              /**
               * 删除图书
               */
              private boolean deleteBook(String callNumber) throws SQLException {
                  String sql = "DELETE FROM book_info WHERE call_number = ?";

                  try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                      pstmt.setString(1, callNumber);

                      int rowsAffected = pstmt.executeUpdate();
                      return rowsAffected > 0;
                  }
              }

              private boolean addRoomToDB(RoomInfo roomInfo) throws SQLException {
                  String sql = "INSERT INTO room_info (room_id, floor, room_number, capacity, status, facilities) " +
                               "VALUES (?, ?, ?, ?, ?, ?)";

                  try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                      pstmt.setString(1, roomInfo.getRoomId());
                      pstmt.setInt(2, roomInfo.getFloor());
                      pstmt.setInt(3, roomInfo.getRoomNumber());
                      pstmt.setInt(4, roomInfo.getCapacity());
                      pstmt.setString(5, roomInfo.getStatus());
                      pstmt.setString(6, roomInfo.getFacilities());

                      int rowsAffected = pstmt.executeUpdate();
                      return rowsAffected > 0;
                  }
              }

              private boolean updateRoomInDB(RoomInfo roomInfo) throws SQLException {
                  String sql = "UPDATE room_info SET floor = ?, room_number = ?, capacity = ?, status = ?, facilities = ? " +
                               "WHERE room_id = ?";

                  try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                      pstmt.setInt(1, roomInfo.getFloor());
                      pstmt.setInt(2, roomInfo.getRoomNumber());
                      pstmt.setInt(3, roomInfo.getCapacity());
                      pstmt.setString(4, roomInfo.getStatus());
                      pstmt.setString(5, roomInfo.getFacilities());
                      pstmt.setString(6, roomInfo.getRoomId());

                      int rowsAffected = pstmt.executeUpdate();
                      return rowsAffected > 0;
                  }
              }

              private boolean deleteRoomFromDB(String roomId) throws SQLException {
                  // 首先检查是否有未完成的预约
                  String checkSql = "SELECT COUNT(*) FROM room_booking WHERE room_id = ? AND status = 'confirmed'";
                  try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                      pstmt.setString(1, roomId);
                      ResultSet rs = pstmt.executeQuery();

                      if (rs.next() && rs.getInt(1) > 0) {
                          return false; // 有未完成的预约，不能删除
                      }
                  }

                  // 删除房间
                  String deleteSql = "DELETE FROM room_info WHERE room_id = ?";
                  try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                      pstmt.setString(1, roomId);

                      int rowsAffected = pstmt.executeUpdate();
                      return rowsAffected > 0;
                  }
              }
        //其他方法
              
              private void processCourseApplication() {
              	try {
      			    // 读取 JSON 字符串
      			    String jsonCourse = in.readUTF();
      			    // 处理读取到的数据（如解析 JSON）
      			    System.out.println("收到数据：" + jsonCourse);
      			    TestCourse newCourse = gson.fromJson(jsonCourse, TestCourse.class);
      			    boolean isSuccess = insertCourseApplied(conn,newCourse);
      			    if(isSuccess) {
      			    	System.out.println("数据插入成功！");
      			    	try {
      					    // 可能抛出 IOException 的代码
      					    out.writeUTF("已提交申请");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			    else {
      			    	System.out.println("数据插入失败！");
      			    	try {
      					    // 可能抛出 IOException 的代码
      					    out.writeUTF("提交失败！");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			} catch (IOException e) {
      			    // 异常处理逻辑
      			    e.printStackTrace();
      			    System.err.println("读取数据失败：" + e.getMessage());
      			}
              }
              
              private void SelectCourseInfo() {
              	try {
              		String SID = in.readUTF();
                  	String sql = "SELECT * FROM  student_info WHERE studentid = ?";
                  	String MAJOR = "计算机科学与技术";
                  	int YEAR = 2024;

                  	try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                  		// 设置查询参数
                  		pstmt.setString(1, SID);   
                  		ResultSet rs = pstmt.executeQuery();
                  		if (rs.next()) {
                  		    // 额外检查游标位置
                  		    if (rs.isBeforeFirst()) {
                  		        rs.first(); // 确保游标移动到第一条记录
                  		    }
                  		    MAJOR = rs.getString("major");
                  		    System.out.println(MAJOR);
                  		    YEAR = rs.getInt("admission_year");
                  		} else {
                  		    // 处理没有查询到结果的情况
                  		    System.out.println("未找到学生信息: " + SID);
                  		}
                  	}catch (SQLException e) {
                  		e.printStackTrace();
                  		System.err.println("查询课程排课记录失败：" + e.getMessage());
                  	}
                  	
                  	out.writeUTF(MAJOR);
                  	out.writeInt(YEAR);
                  	out.flush();
                  	
              	}catch(IOException ex) {
              		ex.printStackTrace();
      			    System.err.println("发送数据失败：" + ex.getMessage());
              	}
              }
              
              private void CourseDeskInfo() {
            		try {
                  		String SID = in.readUTF();
                      	String sql = "SELECT * FROM  user_auth WHERE userid = ?";
                      	int TYPE = 1;

                      	try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                      		// 设置查询参数
                      		pstmt.setString(1, SID);   
                      		ResultSet rs = pstmt.executeQuery();
                      		if (rs.next()) {
                      		    // 额外检查游标位置
                      		    if (rs.isBeforeFirst()) {
                      		        rs.first(); // 确保游标移动到第一条记录
                      		    }
                      		    TYPE = rs.getInt("usertype");
                      		} else {
                      		    // 处理没有查询到结果的情况
                      		    System.out.println("未找到学生信息: " + SID);
                      		}
                      	}catch (SQLException e) {
                      		e.printStackTrace();
                      		System.err.println("查询课程排课记录失败：" + e.getMessage());
                      	}
                      	
                      	out.writeInt(TYPE);
                      	out.flush();
                      	
                  	}catch(IOException ex) {
                  		ex.printStackTrace();
          			    System.err.println("发送数据失败：" + ex.getMessage());
                  	}
              }
              
              private void ApplyCourseInfo() {
              	try {
              		String TID = in.readUTF();
                  	String sql = "SELECT * FROM teacher_info WHERE teacherid = ?";
                  	String NAME = "载物";
                  	String DEPARTMENT = "计入智学院";

                  	try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                  		// 设置查询参数
                  		pstmt.setString(1, userId);   
                  		ResultSet rs = pstmt.executeQuery();
                  		if(rs.next()){
                  		NAME = rs.getString("name");
                  		DEPARTMENT = rs.getString("college");
                  		}
                  	}catch (SQLException e) {
                  		e.printStackTrace();
                  		System.err.println("查询课程排课记录失败：" + e.getMessage());
                  	}
                  	
                  	out.writeUTF(NAME);
                  	out.writeUTF(DEPARTMENT);
                  	out.flush();
                  	
              	}catch(IOException ex) {
              		ex.printStackTrace();
      			    System.err.println("发送数据失败：" + ex.getMessage());
              	}
              }
              
              private void processCoursePublish() {
              	try {
      			    // 读取 JSON 字符串
      			    String jsonCourse = in.readUTF();
      			    // 处理读取到的数据（如解析 JSON）
      			    System.out.println("收到数据：" + jsonCourse);
      			    TestCourse newCourse = gson.fromJson(jsonCourse, TestCourse.class);
      			    boolean isSuccess = insertCoursePubliced(conn,newCourse);
      			    if(isSuccess) {
      			    	System.out.println("数据插入成功！");
      			    	boolean ifSuccess = deleteFromCourseApplied(newCourse);
      			    	if(ifSuccess)
      			    		System.out.println("数据删除成功！");
      			    	try {
      					    // 可能抛出 IOException 的代码
      					    out.writeUTF("已发布课程");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			    else {
      			    	System.out.println("数据插入失败！");
      			    	try {
      					    // 可能抛出 IOException 的代码
      					    out.writeUTF("课程发布失败！");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			} catch (IOException e) {
      			    // 异常处理逻辑
      			    e.printStackTrace();
      			    System.err.println("读取数据失败：" + e.getMessage());
      			}
              }
              
              private void processCourseCancel() {
              	try {
      			    // 读取 JSON 字符串
      			    String jsonCourse = in.readUTF();
      			    // 处理读取到的数据（如解析 JSON）
      			    System.out.println("收到数据：" + jsonCourse);
      			    TestCourse newCourse = gson.fromJson(jsonCourse, TestCourse.class);
      			    boolean ifSuccess = deleteFromCourseApplied(newCourse);
      			    if(ifSuccess) {
      			    	System.out.println("数据删除成功！");
      			    	try {
      					    // 可能抛出 IOException 的代码
      					    out.writeUTF("已拒绝发布课程");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			    else {
      			    	System.out.println("数据删除失败！");
      			    	try {
      					    // 可能抛出 IOException 的代码
      					    out.writeUTF("拒绝操作失败");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			} catch (IOException e) {
      			    // 异常处理逻辑
      			    e.printStackTrace();
      			    System.err.println("读取数据失败：" + e.getMessage());
      			}
              }
              
              private void processTimeSubmit() {
              	try {
      			    // 读取 JSON 字符串
      			    String jsonTime = in.readUTF();
      			    // 处理读取到的数据（如解析 JSON）
      			    System.out.println("收到数据：" + jsonTime);
      			    CourseTime newTime = gson.fromJson(jsonTime, CourseTime.class);
      			    boolean isSuccess = insertTime(newTime);
      			    if(isSuccess) {
      			    	System.out.println("数据插入成功！");
      			    	try {
      					    // 可能抛出 IOException 的代码
      					    out.writeUTF("已提交！");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			    else {
      			    	System.out.println("数据插入失败！");
      			    	try {
      					    // 可能抛出 IOException 的代码
      					    out.writeUTF("提交失败！");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			} catch (IOException e) {
      			    // 异常处理逻辑
      			    e.printStackTrace();
      			    System.err.println("读取数据失败：" + e.getMessage());
      			}
              }
              
              private void processTimeOk() {
              	try {
      			    // 读取 JSON 字符串
      			    String courseName = in.readUTF();
      			    String teacherName = in.readUTF();
      			    // 处理读取到的数据（如解析 JSON）
      			    System.out.println("收到数据：{courseName: " + courseName + "teacherName: " + teacherName + "}");
      			    boolean isSuccess = existsByCourseAndTeacher(courseName,teacherName);
      			    if(isSuccess) {
      			    	System.out.println("已查询到相关数据！");
      			    	try {
      					    // 可能抛出 IOException 的代码
      					    out.writeUTF("已完成排课！");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			    else {
      			    	try {
      					    // 可能抛出 IOException 的代码
      					    out.writeUTF("排课未完成！");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			} catch (IOException e) {
      			    // 异常处理逻辑
      			    e.printStackTrace();
      			    System.err.println("读取数据失败：" + e.getMessage());
      			}
              }
              
              private void processSetIsSchedule() {
              	try {
      			    // 读取 JSON 字符串
      			    String courseName = in.readUTF();
      			    String teacherName = in.readUTF();
      			    // 处理读取到的数据（如解析 JSON）
      			    System.out.println("收到数据：{courseName: " + courseName + "teacherName: " + teacherName + "}");
      			    int isSuccess = updateIsScheduled(courseName,teacherName);
      			    if(isSuccess > 0) {
      			    	System.err.println("成功修改数据！");
      			    }
      			    else {
      			    	System.err.println("修改数据失败！");
      			    }
      			} catch (IOException e) {
      			    // 异常处理逻辑
      			    e.printStackTrace();
      			    System.err.println("读取数据失败：" + e.getMessage());
      			}
              }
              
              private void processCourseSelected() {
              	try {
      			    // 读取 JSON 字符串
      			    String jsonCourse = in.readUTF();
      			    // 处理读取到的数据（如解析 JSON）
      			    System.out.println("收到数据：" + jsonCourse);
      			    StudentSC newSelected = gson.fromJson(jsonCourse, StudentSC.class);
      			    boolean isSuccess = insertCourseSelected(newSelected);
      			    if(isSuccess) {
      			    	System.out.println("数据插入成功！");
      			    	try {
      					    // 可能抛出 IOException 的代码
      					    out.writeUTF("选课成功！");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			    else {
      			    	System.out.println("数据插入失败！");
      			    	try {
      					    // 可能抛出 IOException 的代码
      					    out.writeUTF("选课失败");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			} catch (IOException e) {
      			    // 异常处理逻辑
      			    e.printStackTrace();
      			    System.err.println("读取数据失败：" + e.getMessage());
      			}
              }
              
              private void processCourseDrop() {
              	try {
      			    // 读取 JSON 字符串
      			    String jsonSC = in.readUTF();
      			    // 处理读取到的数据（如解析 JSON）
      			    System.out.println("收到数据：" + jsonSC);
      			    StudentSC dropSC = gson.fromJson(jsonSC, StudentSC.class);
      			    boolean isSuccess = deleteCourseSelected(dropSC);
      			    if(isSuccess) {
      			    	System.out.println("数据删除成功！");
      			    	try {
      					    out.writeUTF("已退选");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			    else {
      			    	System.out.println("数据删除失败！");
      			    	try {
      					    // 可能抛出 IOException 的代码
      					    out.writeUTF("退选失败！");
      					    out.flush();
      					} catch (IOException e) {
      					    // 异常处理逻辑（如打印错误信息、提示用户）
      					    e.printStackTrace();
      					    System.err.println("发送数据失败：" + e.getMessage());
      					}
      			    }
      			} catch (IOException e) {
      			    // 异常处理逻辑
      			    e.printStackTrace();
      			    System.err.println("读取数据失败：" + e.getMessage());
      			}
              }
              
              private void processDeskRequire() {
              	try {
      			    String SID = in.readUTF();
      			    System.out.println("学生ID：" + SID);
      			    getSelectedCourse(SID);
      			} catch (IOException e) {
      			    // 异常处理逻辑
      			    e.printStackTrace();
      			    System.err.println("读取数据失败：" + e.getMessage());
      			}
              }
              
              private void getSelectedCourse(String SID) {
              	
              	List<FinalCourse> courseList = new ArrayList<>();
              	List<DeskCourse> sTimeList = new ArrayList<>();
              	
              	String sql = "SELECT " +
                        "cp.ID, cp.name, cp.teacher_name, cp.credit, cp.academy, cp.major, " +
                        "cp.first_week, cp.last_week, cp.student_type, cp.nature, cp.student_num, " +
                        "cp.department, cp.is_scheduled, " +
                        "cs.class_ID, " +
                        "scd.class_num, " +
                        "GROUP_CONCAT( " +
                        "CONCAT( " +
                        "CASE scd.week_day " +
                        "WHEN 1 THEN '星期一' " +
                        "WHEN 2 THEN '星期二' " +
                        "WHEN 3 THEN '星期三' " +
                        "WHEN 4 THEN '星期四' " +
                        "WHEN 5 THEN '星期五' " +
                        "WHEN 6 THEN '星期六' " +
                        "WHEN 7 THEN '星期日' " +
                        "ELSE '未知' END, " +
                        " ' ', scd.start_period, '-', scd.end_period, '节 教室', scd.classroom " +
                        ") ORDER BY scd.week_day, scd.start_period SEPARATOR '、' " +
                        ") AS schedule_info " +
                        "FROM course_selected cs " +
                        "INNER JOIN course_publiced cp " +
                        "ON cs.course_name = cp.name AND cs.teacher_name = cp.teacher_name " +
                        "LEFT JOIN course_schedule scd " +
                        "ON cs.course_name = scd.course_id " +
                        "AND cs.teacher_name = scd.teacher_name " +
                        "AND cs.class_ID = scd.class_num " +
                        "WHERE cs.student_id = ? " +
                        "GROUP BY " +
                        "cp.ID, cp.name, cp.teacher_name, cp.credit, cp.academy, cp.major, " +
                        "cp.first_week, cp.last_week, cp.student_type, cp.nature, cp.student_num, " +
                        "cp.department, cp.is_scheduled, " +
                        "cs.class_ID, scd.class_num " +
                        "ORDER BY cp.name, cp.teacher_name";
              	
              	try (PreparedStatement pstmt = conn.prepareStatement(sql);) {
              		
              		pstmt.setString(1, SID);  

              		ResultSet rs = pstmt.executeQuery();
              		
              		while (rs.next()) {
              			FinalCourse course = new FinalCourse();
                      
              			// 设置course_publiced的字段
              			course.setCourseID(rs.getString("ID"));
              			course.setName(rs.getString("name"));
              			course.setTeacherName(rs.getString("teacher_name"));
              			course.setCredit(rs.getDouble("credit"));
              			course.setAcademy(rs.getString("academy"));
              			course.setMajor(rs.getString("major"));
              			course.setFirstWeek(rs.getInt("first_week"));
              			course.setLastWeek(rs.getInt("last_week"));
              			course.setStudentType(rs.getInt("student_type"));
              			course.setNature(rs.getString("nature"));
              			course.setStudentNum(rs.getInt("student_num"));
              			course.setDepartment(rs.getString("department"));
              			course.setIsS(1);
              			course.setClassID(rs.getInt("class_num"));
                      
              			// 设置整合后的课程安排信息
              			course.setSchedule(rs.getString("schedule_info"));
                      
              			courseList.add(course);
              		}
              	}catch (SQLException e) { 
                 	    e.printStackTrace(); 
                  }
              	
              	sql = "SELECT " +
                          "cs.course_id, cs.teacher_name, cs.class_num, " +
                          "cs.week_day, cs.start_period, cs.end_period, cs.classroom, " +
                          "cp.first_week, cp.last_week " +
                          "FROM course_schedule cs " +
                          "INNER JOIN course_publiced cp " +
                          "ON cs.course_id = cp.name AND cs.teacher_name = cp.teacher_name " +
                          "INNER JOIN course_selected cs2 " +
                          "ON cs.course_id = cs2.course_name " +
                          "AND cs.teacher_name = cs2.teacher_name " +
                          "AND cs.class_num = cs2.class_ID " +
                          "WHERE cs2.student_id = ?";
                  
                  try(PreparedStatement pstmt = conn.prepareStatement(sql);){
                  	
                  	pstmt.setString(1, SID);
                  	
                  	ResultSet rs = pstmt.executeQuery();
                  	
                  	while(rs.next()) {
                  		
                  		DeskCourse time = new DeskCourse();
                  		
                  		time.setCourseName(rs.getString("course_id"));
                  		time.setTeacherName(rs.getString("teacher_name"));
                  		time.setClassId(rs.getInt("class_num"));
                  		time.setDay(rs.getInt("week_day"));
                  		time.setFTime(rs.getInt("start_period"));
                  		time.setETime(rs.getInt("end_period"));
                  		time.setFWeek(rs.getInt("first_week"));
                  		time.setEWeek(rs.getInt("last_week"));
                  		time.setRoom(rs.getString("classroom"));
                  		
                  		sTimeList.add(time);
                  		
                  	}
                  }catch(SQLException e) {
                  	e.printStackTrace();
                  }
                  
                  String jsonData = gson.toJson(courseList);
              	String sTimeData = gson.toJson(sTimeList);
                  
                  try {
                      out.writeUTF(jsonData); 
                      out.writeUTF(sTimeData);
                      out.flush();
                  } catch (Exception e) {
                      e.printStackTrace();
                      throw new RuntimeException("发送课程数据失败", e);
                  }
              }
              
              private void getTforC() {
            	  try {
                		String SID = in.readUTF();
                    	String sql = "SELECT * FROM  teacher_info WHERE teacherid = ?";
                    	String TNAME = "泥扣";

                    	try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    		// 设置查询参数
                    		pstmt.setString(1, SID);   
                    		ResultSet rs = pstmt.executeQuery();
                    		if (rs.next()) {
                    		    // 额外检查游标位置
                    		    if (rs.isBeforeFirst()) {
                    		        rs.first(); // 确保游标移动到第一条记录
                    		    }
                    		    TNAME = rs.getString("name");
                    		} else {
                    		    // 处理没有查询到结果的情况
                    		    System.out.println("未找到老师信息: " + SID);
                    		}
                    	}catch (SQLException e) {
                    		e.printStackTrace();
                    		System.err.println("查询课程排课记录失败：" + e.getMessage());
                    	}
                    	
                    	out.writeUTF(TNAME);
                    	out.flush();
                    	
                	}catch(IOException ex) {
                		ex.printStackTrace();
        			    System.err.println("发送数据失败：" + ex.getMessage());
                	}
              }
              
              private void TeacherInfo() {
                	try {
        			    String SID = in.readUTF();
        			    System.out.println("学生ID：" + SID);
        			    getTeacherInfo(SID);
        			} catch (IOException e) {
        			    // 异常处理逻辑
        			    e.printStackTrace();
        			    System.err.println("读取数据失败：" + e.getMessage());
        			}
               }
              
              private void getTeacherInfo(String SID) {
            	  List<FinalCourse> courseList = new ArrayList<>();
                	List<DeskCourse> sTimeList = new ArrayList<>();
                	
                	String sql = "SELECT " +
                            "    cp.*, " +
                            "    cs.class_num, " +  // 新增class_num字段
                            "    GROUP_CONCAT( " +
                            "        CONCAT( " +
                            "            '星期', " +
                            "            cs.week_day, " +
                            "            ' ', " +
                            "            cs.start_period, " +
                            "            '-', " +
                            "            cs.end_period, " +
                            "            '节 教室', " +
                            "            cs.classroom " +
                            "        ) " +
                            "        ORDER BY cs.week_day, cs.start_period " +
                            "        SEPARATOR '、' " +
                            "    ) AS schedule_info " +
                            "FROM " +
                            "    course_publiced cp " +
                            "LEFT JOIN " +
                            "    course_schedule cs ON cp.teacher_name = cs.teacher_name " +
                            "WHERE " +
                            "    cp.teacher_name = ? " +
                            "GROUP BY " +
                            "    cp.ID, cp.name, cp.teacher_name, cp.credit, " +
                            "    cp.academy, cp.major, cp.first_week, cp.last_week, " +
                            "    cp.student_type, cp.nature, cp.student_num, " +
                            "    cp.department, cp.is_scheduled, " +
                            "    cs.class_num";
                	
                	try (PreparedStatement pstmt = conn.prepareStatement(sql);) {
                		
                		pstmt.setString(1, SID);  

                		ResultSet rs = pstmt.executeQuery();
                		
                		while (rs.next()) {
                			FinalCourse course = new FinalCourse();
                        
                			// 设置course_publiced的字段
                			course.setCourseID(rs.getString("ID"));
                			course.setName(rs.getString("name"));
                			course.setTeacherName(rs.getString("teacher_name"));
                			course.setCredit(rs.getDouble("credit"));
                			course.setAcademy(rs.getString("academy"));
                			course.setMajor(rs.getString("major"));
                			course.setFirstWeek(rs.getInt("first_week"));
                			course.setLastWeek(rs.getInt("last_week"));
                			course.setStudentType(rs.getInt("student_type"));
                			course.setNature(rs.getString("nature"));
                			course.setStudentNum(rs.getInt("student_num"));
                			course.setDepartment(rs.getString("department"));
                			course.setIsS(1);
                			course.setClassID(rs.getInt("class_num"));
                        
                			// 设置整合后的课程安排信息
                			course.setSchedule(rs.getString("schedule_info"));
                        
                			courseList.add(course);
                		}
                	}catch (SQLException e) { 
                   	    e.printStackTrace(); 
                    }
                	
                	sql = "SELECT " +
                            "cs.course_id, " +
                            "cs.teacher_name, " +
                            "cs.class_num, " +
                            "cs.week_day, " +
                            "cs.start_period, " +
                            "cs.end_period, " +
                            "cs.classroom, " +
                            "cp.first_week, " +
                            "cp.last_week " +
                            "FROM course_schedule cs " +
                            "JOIN course_publiced cp " +
                            "ON cs.course_id = cp.name " +
                            "AND cs.teacher_name = cp.teacher_name " +
                            "WHERE cs.teacher_name = ?";
                    
                    try(PreparedStatement pstmt = conn.prepareStatement(sql);){
                    	
                    	pstmt.setString(1, SID);
                    	
                    	ResultSet rs = pstmt.executeQuery();
                    	
                    	while(rs.next()) {
                    		
                    		DeskCourse time = new DeskCourse();
                    		
                    		time.setCourseName(rs.getString("course_id"));
                    		time.setTeacherName(rs.getString("teacher_name"));
                    		time.setClassId(rs.getInt("class_num"));
                    		time.setDay(rs.getInt("week_day"));
                    		time.setFTime(rs.getInt("start_period"));
                    		time.setETime(rs.getInt("end_period"));
                    		time.setFWeek(rs.getInt("first_week"));
                    		time.setEWeek(rs.getInt("last_week"));
                    		time.setRoom(rs.getString("classroom"));
                    		
                    		sTimeList.add(time);
                    		
                    	}
                    }catch(SQLException e) {
                    	e.printStackTrace();
                    }
                    
                    String jsonData = gson.toJson(courseList);
                	String sTimeData = gson.toJson(sTimeList);
                    
                    try {
                        out.writeUTF(jsonData); 
                        out.writeUTF(sTimeData);
                        out.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("发送课程数据失败", e);
                    }
              }
              
              private boolean deleteCourseSelected(StudentSC dropSC) {
              	String sql = "DELETE FROM course_selected " +
                          "WHERE student_id = ? " +
                          "AND course_name = ?";

              	try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
              		//设置SQL参数（顺序与SQL语句中的?对应）
              		pstmt.setString(1, dropSC.getStudentId());
              		pstmt.setString(2, dropSC.getCourseName());

              		//执行删除并返回是否有记录被删除
              		int affectedRows = pstmt.executeUpdate();
              		return affectedRows > 0; // 大于0表示删除成功

              	} catch (SQLException e) {
              		System.err.println("删除数据失败：" + e.getMessage());
              		e.printStackTrace();
              		return false;
              	}
              }
              
              private boolean insertCourseSelected(StudentSC newS){
              	String sql = "INSERT INTO course_selected (student_id, course_name, teacher_name, class_ID) "
                  		+ "VALUES (?, ?, ?, ?)";
              	
              	PreparedStatement pstmt = null;
                  try {
                      pstmt = conn.prepareStatement(sql);

                      pstmt.setString(1, newS.getStudentId());     
                      pstmt.setString(2, newS.getCourseName());       
                      pstmt.setString(3, newS.getTeacherName());   
                      pstmt.setInt(4, newS.getClassId());         

                      int rowsAffected = pstmt.executeUpdate();
                      return rowsAffected > 0;

                  } catch (SQLException e) {
                      e.printStackTrace();
                      System.err.println("插入数据失败：" + e.getMessage());
                      return false;
                  }
              }
              
              private int updateIsScheduled(String courseName, String teacherName) {
                  // SQL更新语句：根据课程名和教师名定位记录，将is_scheduled设为1
                  String sql = "UPDATE course_publiced " +
                               "SET is_scheduled = 1 " +
                               "WHERE name = ? AND teacher_name = ?";

                  try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                      // 设置参数：第一个?对应课程名，第二个?对应教师名
                      pstmt.setString(1, courseName);
                      pstmt.setString(2, teacherName);

                      // 执行更新并返回受影响的行数
                      return pstmt.executeUpdate();

                  } catch (SQLException e) {
                      e.printStackTrace();
                      System.err.println("更新课程排课状态失败：" + e.getMessage());
                      return -1; // 发生异常时返回-1表示失败
                  }
              }
              
              private boolean existsByCourseAndTeacher(String courseName, String teacherName) {
                  // SQL查询：根据course_id和teacher_name查询
                  String sql = "SELECT 1 FROM course_schedule " +
                               "WHERE course_id = ? AND teacher_name = ? LIMIT 1";

                  try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                      // 设置查询参数
                      pstmt.setString(1, courseName);   // 匹配course_id
                      pstmt.setString(2, teacherName);  // 匹配teacher_name

                      // 执行查询并获取结果集
                      try (ResultSet rs = pstmt.executeQuery()) {
                          // 如果结果集有记录，说明存在匹配项
                          return rs.next();
                      }
                  } catch (SQLException e) {
                      e.printStackTrace();
                      System.err.println("查询课程排课记录失败：" + e.getMessage());
                      return false; // 发生异常时返回false
                  }
              }
              
              private boolean insertTime(CourseTime time) {
              	 String insertSql = "INSERT INTO testdb.course_schedule (" +
                           "course_id, teacher_name, class_num, week_day, " +
                           "start_period, end_period, classroom) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?)";

              	 // 2. 使用 try-with-resources 自动关闭 PreparedStatement（避免资源泄漏）
              	 try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
              		 // 3. 给 SQL 占位符设置参数（顺序与 SQL 中 ? 一一对应）
              		 // 注意：表字段与 CourseTime 属性的映射关系
              		 pstmt.setString(1, time.getCourseName());       // course_id ← courseName
              		 pstmt.setString(2, time.getTeacherName());      // teacher_name ← teacherName
              		 pstmt.setInt(3, time.getClassNum());            // class_num ← classNum
              		 pstmt.setInt(4, time.getWeekDay());             // week_day ← weekDay
              		 pstmt.setInt(5, time.getStartTime());           // start_period ← startTime
              		 pstmt.setInt(6, time.getEndTime());             // end_period ← endTime
              		 pstmt.setString(7, time.getClassroom());        // classroom ← classroom

              		 // 4. 执行 SQL：executeUpdate() 返回受影响的行数（插入成功返回 1）
              		 int affectedRows = pstmt.executeUpdate();
              		 return affectedRows > 0; // 受影响行数>0 表示插入成功

              	 } catch (SQLException e) {
              		 // 5. 处理数据库异常（根据异常类型提示具体原因）
              		 e.printStackTrace();
              		 System.err.println("插入排课数据失败，原因：");
              		 if (e.getSQLState().equals("23000")) { // SQLState 23000 表示约束冲突
              			 if (e.getMessage().contains("uk_teacher_time")) {
              				 // 唯一键冲突：同一课程+教师+周几+开始节次已存在
              				 System.err.println("唯一键冲突：该课程在【周" + time.getWeekDay() + "第" + time.getStartTime() + "节】已排课");
              			 } else {
              				 System.err.println("约束冲突：" + e.getMessage());
              			 }
              		 } else {
          	  			// 其他 SQL 异常（如字段类型不匹配、连接断开等）
          	  			System.err.println("SQL 异常：" + e.getMessage());
            			}
            
            			// 任何异常都返回 false，表示插入失败
            			return false;
        			}
              }
              
              private boolean deleteFromCourseApplied(TestCourse course) {
                  // SQL删除语句：根据TestCourse的所有字段进行匹配
                  String sql = "DELETE FROM course_applied " +
                               "WHERE name = ? " +
                               "AND teacher_name = ? " +
                               "AND credit = ? " +
                               "AND academy = ? " +
                               "AND major = ? " +
                               "AND first_week = ? " +
                               "AND last_week = ? " +
                               "AND student_type = ? " +
                               "AND student_num = ? " +
                               "AND department = ?"; 

                  try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                      //设置SQL参数（顺序与SQL语句中的?对应）
                      pstmt.setString(1, course.getName());
                      pstmt.setString(2, course.getTeacherName());
                      pstmt.setDouble(3, course.getCredit());
                      pstmt.setString(4, course.getAcademy());
                      pstmt.setString(5, course.getMajor());
                      pstmt.setInt(6, course.getFirstWeek()); 
                      pstmt.setInt(7, course.getLastWeek()); 
                      pstmt.setInt(8, course.getStudentType()); 
                      pstmt.setInt(9, course.getStudentNum()); 
                      pstmt.setString(10, course.getDepartment());

                      //执行删除并返回是否有记录被删除
                      int affectedRows = pstmt.executeUpdate();
                      return affectedRows > 0; // 大于0表示删除成功

                  } catch (SQLException e) {
                      System.err.println("删除数据失败：" + e.getMessage());
                      e.printStackTrace();
                      return false;
                  }
              }
              
              private void processCourseAppliedRequire() {
              	 List<TestCourse> courses = new ArrayList<>();
                   String sql = "SELECT * FROM course_applied"; // 查询所有记录
                   
                   try (PreparedStatement pstmt = conn.prepareStatement(sql);
                        ResultSet rs = pstmt.executeQuery()) {
                       
                       // 遍历结果集，封装为TestCourse对象
                       while (rs.next()) {
                      	 TestCourse course = new TestCourse();
                           
                           course.setName(rs.getString("name"));
                           course.setTeacherName(rs.getString("teacher_name"));
                           course.setCredit(rs.getDouble("credit"));
                           course.setAcademy(rs.getString("academy"));
                           course.setMajor(rs.getString("major"));
                           course.setFirstWeek(rs.getInt("first_week"));
                           course.setLastWeek(rs.getInt("last_week"));
                           course.setStudentType(rs.getInt("student_type"));
                           course.setStudentNum(rs.getInt("student_num"));
                           course.setDepartment(rs.getString("department"));
                           course.setCourseID("-1");
                           course.setNature("-1");
                           
                           courses.add(course);
                       }
                   }catch (SQLException e) { 
                  	    e.printStackTrace(); 
                   }
                   
                   String jsonData = gson.toJson(courses);
                   
                   try {
                       out.writeUTF(jsonData);      // 发送JSON数据
                       out.flush();
                   } catch (Exception e) {
                       e.printStackTrace();
                       throw new RuntimeException("发送课程数据失败", e);
                   }
              }
              
              private void processCoursePublicedRequire() {
              	List<TestCourse> courses = new ArrayList<>();
                  String sql = "SELECT * FROM course_publiced WHERE is_scheduled = 0"; // 查询所有未排课课程
                  
                  try (PreparedStatement pstmt = conn.prepareStatement(sql);
                       ResultSet rs = pstmt.executeQuery()) {
                      
                      // 遍历结果集，封装为TestCourse对象
                      while (rs.next()) {
                     	 TestCourse course = new TestCourse();
                          
                     	 	course.setCourseID(rs.getString("ID"));
                          course.setName(rs.getString("name"));
                          course.setTeacherName(rs.getString("teacher_name"));
                          course.setCredit(rs.getDouble("credit"));
                          course.setAcademy(rs.getString("academy"));
                          course.setMajor(rs.getString("major"));
                          course.setFirstWeek(rs.getInt("first_week"));
                          course.setLastWeek(rs.getInt("last_week"));
                          course.setStudentType(rs.getInt("student_type"));
                          course.setNature(rs.getString("nature"));
                          course.setStudentNum(rs.getInt("student_num"));
                          course.setDepartment(rs.getString("department"));
                          
                          courses.add(course);
                      }
                  }catch (SQLException e) { 
                 	    e.printStackTrace(); 
                  }
                  
                  String jsonData = gson.toJson(courses);
                  
                  try {
                      out.writeUTF(jsonData);      // 发送JSON数据
                      out.flush();
                  } catch (Exception e) {
                      e.printStackTrace();
                      throw new RuntimeException("发送课程数据失败", e);
                  }
              }
              
              private void processScheduleRequire() {
              	List<CourseTime> courses = new ArrayList<>();
                  String sql = "SELECT * FROM course_schedule"; // 查询所有未排课课程
                  
                  try (PreparedStatement pstmt = conn.prepareStatement(sql);
                       ResultSet rs = pstmt.executeQuery()) {
                      
                      // 遍历结果集，封装为TestCourse对象
                      while (rs.next()) {
                     	 CourseTime course = new CourseTime();
                          
                     	 	course.setCourseName(rs.getString("course_id"));
                          course.setTeacherName(rs.getString("teacher_name"));
                          course.setClassNum(rs.getInt("class_num"));
                          course.setWeekDay(rs.getInt("week_day"));
                          course.setStartTime(rs.getInt("start_period"));
                          course.setEndTime(rs.getInt("end_period"));
                          course.setClassroom(rs.getString("classroom"));
                          
                          courses.add(course);
                      }
                  }catch (SQLException e) { 
                 	    e.printStackTrace(); 
                  }
                  
                  String jsonData = gson.toJson(courses);
                  
                  try {
                      out.writeUTF(jsonData);      // 发送JSON数据
                      out.flush();
                  } catch (Exception e) {
                      e.printStackTrace();
                      throw new RuntimeException("发送课程数据失败", e);
                  }
              }
              
              private void processForSelectData() {
              	
              	List<FinalCourse> courseList = new ArrayList<>();
              	List<StudentSC> studentList = new ArrayList<>();
              	List<CourseToTime> sctList = new ArrayList<>();
              	List<CourseToTime> cttList = new ArrayList<>();
              	List<StudentSC> allStudent = new ArrayList<>();
              	
              	try {
      			    String MAJOR = in.readUTF();
      			    int STYPE = in.readInt();
      			    String SID = in.readUTF();
      			    System.out.println("收到数据：{Major: " + MAJOR + " ,StudentType: " + String.valueOf(STYPE) + ",StudentId: " + SID + "}");
              	
              	String sql = "SELECT " +
                          "cp.ID, cp.name, cp.teacher_name, cp.credit, cp.academy, " +
                          "cp.major, cp.first_week, cp.last_week, cp.student_type, " +
                          "cp.nature, cp.student_num, cp.department, cp.is_scheduled, cs.class_num, " +
                          "GROUP_CONCAT(" +
                              "CONCAT(" +
                                  "CASE cs.week_day " +
                                      "WHEN 1 THEN '星期一' " +
                                      "WHEN 2 THEN '星期二' " +
                                      "WHEN 3 THEN '星期三' " +
                                      "WHEN 4 THEN '星期四' " +
                                      "WHEN 5 THEN '星期五' " +
                                      "WHEN 6 THEN '星期六' " +
                                      "WHEN 7 THEN '星期日' " +
                                  "END, " +
                                  " ' ', cs.start_period, '-', cs.end_period, '节 ', cs.classroom" +
                              ") " +
                              "ORDER BY cs.week_day, cs.start_period " +
                              "SEPARATOR '、'" +
                          ") AS schedule_info " +
                          "FROM course_publiced cp " +
                          "INNER JOIN course_schedule cs " +
                              "ON cp.name = cs.course_id AND cp.teacher_name = cs.teacher_name " +
                          "WHERE cp.major = ? " + 
                          "AND cp.student_type = ? " + 
                          "AND cp.is_scheduled = 1 " +
                          "GROUP BY cp.ID, cp.name, cp.teacher_name, cp.credit, cp.academy, cp.major, cp.first_week, cp.last_week, cp.student_type, cp.nature, cp.student_num, cp.department, cp.is_scheduled, cs.class_num " +
                          "ORDER BY cp.name";
              
              	try (PreparedStatement pstmt = conn.prepareStatement(sql);) {
              		
              		pstmt.setString(1, MAJOR);  
              		pstmt.setInt(2, STYPE);  

              		ResultSet rs = pstmt.executeQuery();
              		
              		while (rs.next()) {
              			FinalCourse course = new FinalCourse();
                      
              			// 设置course_publiced的字段
              			course.setCourseID(rs.getString("ID"));
              			course.setName(rs.getString("name"));
              			course.setTeacherName(rs.getString("teacher_name"));
              			course.setCredit(rs.getDouble("credit"));
              			course.setAcademy(rs.getString("academy"));
              			course.setMajor(rs.getString("major"));
              			course.setFirstWeek(rs.getInt("first_week"));
              			course.setLastWeek(rs.getInt("last_week"));
              			course.setStudentType(rs.getInt("student_type"));
              			course.setNature(rs.getString("nature"));
              			course.setStudentNum(rs.getInt("student_num"));
              			course.setDepartment(rs.getString("department"));
              			course.setIsS(rs.getInt("is_scheduled"));
              			course.setClassID(rs.getInt("class_num"));
                      
              			// 设置整合后的课程安排信息
              			course.setSchedule(rs.getString("schedule_info"));
                      
              			courseList.add(course);
              		}
              	}catch (SQLException e) { 
                 	    e.printStackTrace(); 
                  }
              	
              	sql = "SELECT * FROM course_selected WHERE student_id = ?"; // 查询所有未排课课程
                  
                  try (PreparedStatement pstmt = conn.prepareStatement(sql);){
                  	
                  	pstmt.setString(1, SID);
                  	
                      ResultSet rs = pstmt.executeQuery(); 
                      
                      // 遍历结果集，封装为TestCourse对象
                      while (rs.next()) {
                     	 StudentSC student = new StudentSC();
                          
                     	 	student.setClassId(rs.getInt("class_ID"));
                     	 	student.setCourseName(rs.getString("course_name"));
                     	 	student.setStudentId(SID);
                     	 	student.setTeacherName(rs.getString("teacher_name"));
                          
                          studentList.add(student);
                      }
                  }catch (SQLException e) { 
                 	    e.printStackTrace(); 
                  }
                  
                  sql = "SELECT " +
                          "cs.course_id, cs.teacher_name, cs.class_num, " +
                          "cs.week_day, cs.start_period, cs.end_period, cs.classroom, " +
                          "cp.first_week, cp.last_week " +
                          "FROM course_schedule cs " +
                          "INNER JOIN course_publiced cp " +
                          "ON cs.course_id = cp.name AND cs.teacher_name = cp.teacher_name " +
                          "INNER JOIN course_selected cs2 " +
                          "ON cs.course_id = cs2.course_name " +
                          "AND cs.teacher_name = cs2.teacher_name " +
                          "AND cs.class_num = cs2.class_ID " +
                          "WHERE cs2.student_id = ?";
                  
                  try(PreparedStatement pstmt = conn.prepareStatement(sql);){
                  	
                  	pstmt.setString(1, SID);
                  	
                  	ResultSet rs = pstmt.executeQuery();
                  	
                  	while(rs.next()) {
                  		
                  		CourseToTime time = new CourseToTime();
                  		
                  		time.setCourseName(rs.getString("course_id"));
                  		time.setTeacherName(rs.getString("teacher_name"));
                  		time.setClassId(rs.getInt("class_num"));
                  		time.setDay(rs.getInt("week_day"));
                  		time.setFTime(rs.getInt("start_period"));
                  		time.setETime(rs.getInt("end_period"));
                  		time.setFWeek(rs.getInt("first_week"));
                  		time.setEWeek(rs.getInt("last_week"));
                  		
                  		sctList.add(time);
                  		
                  	}
                  }catch(SQLException e) {
                  	e.printStackTrace();
                  }
                  
                  sql = "SELECT cs.course_id, cs.teacher_name, cs.class_num, cs.week_day, " +
                          "cs.start_period, cs.end_period, cs.classroom, " +
                          "cp.first_week, cp.last_week, cp.major, cp.student_type " +
                          "FROM course_schedule cs " +
                          "INNER JOIN course_publiced cp " +
                          "ON cs.course_id = cp.name " +
                          "AND cs.teacher_name = cp.teacher_name " +
                          "WHERE cp.major = ? AND cp.student_type = ?";
                  
                  try(PreparedStatement pstmt = conn.prepareStatement(sql);){
                  	
                  	pstmt.setString(1, MAJOR);
                  	pstmt.setInt(2, STYPE);
                  	
                  	ResultSet rs = pstmt.executeQuery();
                  	
                  	while(rs.next()) {
                  		
                  		CourseToTime item = new CourseToTime();
                  		
                  		item.setCourseName(rs.getString("course_id"));
                  		item.setTeacherName(rs.getString("teacher_name"));
                  		item.setClassId(rs.getInt("class_num"));
                  		item.setFWeek(rs.getInt("first_week"));
                  		item.setEWeek(rs.getInt("last_week"));
                  		item.setDay(rs.getInt("week_day"));
                  		item.setFTime(rs.getInt("start_period"));
                  		item.setETime(rs.getInt("end_period"));
                  		
                  		cttList.add(item);
                  		
                  	}
                  }catch(SQLException e) {
                  	e.printStackTrace();
                  }
                  
                  sql = "SELECT * FROM course_selected"; // 查询所有未排课课程
                  
                  try (PreparedStatement pstmt = conn.prepareStatement(sql);){
                  	
                      ResultSet rs = pstmt.executeQuery(); 
                      
                      // 遍历结果集，封装为TestCourse对象
                      while (rs.next()) {
                     	 StudentSC student = new StudentSC();
                          
                     	 	student.setClassId(rs.getInt("class_ID"));
                     	 	student.setCourseName(rs.getString("course_name"));
                     	 	student.setStudentId(SID);
                     	 	student.setTeacherName(rs.getString("teacher_name"));
                          
                          allStudent.add(student);
                      }
                  }catch (SQLException e) { 
                 	    e.printStackTrace(); 
                  }
              	
              	String jsonData = gson.toJson(courseList);
              	String sData = gson.toJson(studentList);
              	String scData = gson.toJson(sctList);
              	String ctData = gson.toJson(cttList);
              	String allData = gson.toJson(allStudent);
                  
                  try {
                      out.writeUTF(jsonData); 
                      out.writeUTF(sData);
                      out.writeUTF(scData);
                      out.writeUTF(ctData);
                      out.writeUTF(allData);
                      out.flush();
                  } catch (Exception e) {
                      e.printStackTrace();
                      throw new RuntimeException("发送课程数据失败", e);
                  }
                  
              	} catch (IOException e) {
      			    e.printStackTrace();
      			    System.err.println("读取数据失败：" + e.getMessage());
      			}
              }
              
              public boolean insertCoursePubliced(Connection conn, TestCourse course) {
              	String sql = "INSERT INTO course_publiced (ID, name, teacher_name, credit, academy, major, first_week, last_week, student_type, nature, student_num, department) "
                  		+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
              	
              	PreparedStatement pstmt = null;
                  try {
                      // 1. 创建PreparedStatement对象（传入SQL语句）
                      pstmt = conn.prepareStatement(sql);

                      // 2. 绑定参数：按SQL中占位符的顺序设置Course对象的属性值
                      pstmt.setString(1, course.getCourseID());       // 课程ID
                      pstmt.setString(2, course.getName());           // 课程名
                      pstmt.setString(3, course.getTeacherName());    // 授课老师
                      pstmt.setDouble(4, course.getCredit());         // 学分
                      pstmt.setString(5, course.getAcademy());        // 面向学院
                      pstmt.setString(6, course.getMajor());          // 面向专业
                      pstmt.setInt(7, course.getFirstWeek());         // 第一周
                      pstmt.setInt(8, course.getLastWeek());          // 最后一周
                      pstmt.setInt(9, course.getStudentType());       // 授课对象
                      pstmt.setString(10, course.getNature());        // 课程性质
                      pstmt.setInt(11, course.getStudentNum());       // 授课人数
                      pstmt.setString(12, course.getDepartment());    // 开课单位

                      // 3. 执行插入：返回受影响的行数（1表示成功，0表示失败）
                      int rowsAffected = pstmt.executeUpdate();
                      return rowsAffected > 0;

                  } catch (SQLException e) {
                      e.printStackTrace();
                      System.err.println("插入数据失败：" + e.getMessage());
                      return false;
                  }
              }
              
              public boolean insertCourseApplied(Connection conn, TestCourse course) {
                  // SQL插入语句：根据实际表字段调整占位符对应的字段名
                  String sql = "INSERT INTO course_applied (name, teacher_name, credit, academy, major, first_week, last_week, student_type, student_num, department) "
                  		+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                  PreparedStatement pstmt = null;
                  try {
                      // 1. 创建PreparedStatement对象（传入SQL语句）
                      pstmt = conn.prepareStatement(sql);

                      // 2. 绑定参数：按SQL中占位符的顺序设置Course对象的属性值
                      pstmt.setString(1, course.getName());           // 课程名
                      pstmt.setString(2, course.getTeacherName());    // 授课老师
                      pstmt.setDouble(3, course.getCredit());         // 学分
                      pstmt.setString(4, course.getAcademy());        // 面向学院
                      pstmt.setString(5, course.getMajor());          // 面向专业
                      pstmt.setInt(6, course.getFirstWeek());         // 第一周
                      pstmt.setInt(7, course.getLastWeek());          // 最后一周
                      pstmt.setInt(8, course.getStudentType());       // 授课对象
                      pstmt.setInt(9, course.getStudentNum());        // 授课人数
                      pstmt.setString(10, course.getDepartment());    // 开课单位

                      // 3. 执行插入：返回受影响的行数（1表示成功，0表示失败）
                      int rowsAffected = pstmt.executeUpdate();
                      return rowsAffected > 0;

                  } catch (SQLException e) {
                      e.printStackTrace();
                      System.err.println("插入数据失败：" + e.getMessage());
                      return false;
                  }
              }
    }
    
}