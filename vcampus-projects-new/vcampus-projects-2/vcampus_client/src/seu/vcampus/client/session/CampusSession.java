package seu.vcampus.client.session;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import java.time.LocalDate;

import seu.vcampus.model.CollegeInfo;
import seu.vcampus.model.MajorInfo;
import seu.vcampus.model.ClassInfo;

public class CampusSession {
    // 单例实例
    private static volatile CampusSession instance;
    
    // 数据字段
    private List<CollegeInfo> colleges;
    private Map<String, List<MajorInfo>> collegeMajorsMap; // 键：学院ID/名称；值：专业列表
    private Map<String, List<ClassInfo>> majorClassesMap;  // 键：专业ID/名称；值：班级列表
    private List<String> classRooms;                       // 教室列表
    private LocalDate serverDate;                          // 服务器日期
    private LocalDate foundingDate;                      // 建校日期

    // 私有构造器（单例）
    private CampusSession() {}

    // 获取单例实例
    public static CampusSession getInstance() {
        if (instance == null) {
            synchronized (CampusSession.class) {
                if (instance == null) {
                    instance = new CampusSession();
                }
            }
        }
        return instance;
    }


    // -------------------------- 1. 数据初始化/更新函数 --------------------------
    /**
     * 初始化校园数据（首次加载或全量刷新时调用）
     * 建议从服务端请求数据后，通过此方法批量设置
     */
    public void initData(List<CollegeInfo> colleges,
                         Map<String, List<MajorInfo>> collegeMajorsMap,
                         Map<String, List<ClassInfo>> majorClassesMap,
                         List<String> classRooms,
                         LocalDate serverDate,
                         LocalDate foundingDate) {
        this.colleges = Collections.unmodifiableList(new ArrayList<>(colleges)); // 不可修改的列表，防止意外篡改
        this.collegeMajorsMap = Collections.unmodifiableMap(new HashMap<>(collegeMajorsMap));
        this.majorClassesMap = Collections.unmodifiableMap(new HashMap<>(majorClassesMap));
        this.classRooms = Collections.unmodifiableList(new ArrayList<>(classRooms));
        this.serverDate = serverDate;
        this.foundingDate = foundingDate;
    }

    /**
     * 单独更新服务器日期（可定时调用，无需全量刷新）
     */
    public void updateServerDate(LocalDate newServerDate) {
    	// TODO 这边做一个刷新？
        this.serverDate = newServerDate;
    }

    /**
     * 清空数据（用户退出登录时调用，保护数据安全）
     */
    public void clear() {
        this.colleges = null;
        this.collegeMajorsMap = null;
        this.majorClassesMap = null;
        this.classRooms = null;
        this.serverDate = null;
        this.foundingDate = null;
    }


    // -------------------------- 2. 数据查询函数（核心） --------------------------
    /**
     * 获取所有学院列表
     * @return 学院列表（无数据时返回空列表，避免NullPointerException）
     */
    public List<CollegeInfo> getAllColleges() {
        return colleges == null ? new ArrayList<>() : colleges;
    }

    /**
     * 根据学院ID获取对应的专业列表
     * @param collegeId
     * @return 专业列表（无匹配时返回空列表）
     */
    public List<MajorInfo> getMajorsByCollege(String collegeId) {
        if (collegeMajorsMap == null || collegeId == null) {
            return new ArrayList<>();
        }
        List<MajorInfo> majors = collegeMajorsMap.get(collegeId);
        return majors == null ? new ArrayList<>() : majors;
    }

    /**
     * 根据专业ID获取对应的班级列表
     * @param majorId
     * @return 班级列表（无匹配时返回空列表）
     */
    public List<ClassInfo> getClassesByMajor(String majorId) {
        if (majorClassesMap == null || majorId == null) {
            return new ArrayList<>();
        }
        List<ClassInfo> classes = majorClassesMap.get(majorId);
        return classes == null ? new ArrayList<>() : classes;
    }

    /**
     * @return 获取所有教室列表
     */
    public List<String> getAllClassRooms() {
        return classRooms == null ? new ArrayList<>() : classRooms;
    }

    /**
     * @return 服务器当前日期
     */
    public LocalDate getServerDate() {
        return serverDate;
    }

    /**
     * @return 学校成立日期
     */
    public LocalDate getFoundingDate() {
        return foundingDate;
    }


    // -------------------------- 3. 辅助工具函数 --------------------------
    /**
     * 检查数据是否已初始化（避免使用未加载的数据）
     * @return true：数据已加载；false：未加载或已清空
     */
    public boolean isDataLoaded() {
        return colleges != null && !colleges.isEmpty() 
            && collegeMajorsMap != null && !collegeMajorsMap.isEmpty()
            && majorClassesMap != null && !majorClassesMap.isEmpty();
    }

    /**
     * 根据学院名称查询学院ID（辅助函数，用于快速获取collegeKey）
     * @param collegeName 学院名称（如“计算机学院”）
     * @return 学院ID（无匹配时返回null）
     */
    public String getCollegeIdByName(String collegeName) {
        if (colleges == null || collegeName == null) {
            return null;
        }
        for (CollegeInfo college : colleges) {
            if (college.getCollegeName().equals(collegeName)) {
                return college.getCollegeId();
            }
        }
        return null;
    }

    /**
     * 根据专业名称查询专业ID（辅助函数，用于快速获取majorKey）
     * @param majorName 专业名称（如“软件工程”）
     * @return 专业ID（无匹配时返回null）
     */
    public String getMajorIdByName(String majorName) {
        if (collegeMajorsMap == null || majorName == null) {
            return null;
        }
        // 遍历所有专业列表，查找名称匹配的专业
        for (List<MajorInfo> majors : collegeMajorsMap.values()) {
            for (MajorInfo major : majors) {
                if (major.getMajorName().equals(majorName)) {
                    return major.getMajorId();
                }
            }
        }
        return null;
    }
    
    public String getMajorIdByNames(String collegeName, String majorName) {
    	if (collegeMajorsMap == null || majorName == null) {
            return null;
        }
        String collegeId = getCollegeIdByName(collegeName);
        for (MajorInfo major: collegeMajorsMap.get(collegeId)) {
        	if (major.getMajorName().equals(majorName)) {
        		return major.getMajorId();
        	}
        }
        return null;
    }
    
    public MajorInfo getMajorInfoByNames(String collegeName, String majorName) {
        if (collegeMajorsMap == null || majorName == null) {
            return null;
        }
        String collegeId = getCollegeIdByName(collegeName);
        for (MajorInfo major: collegeMajorsMap.get(collegeId)) {
        	if (major.getMajorName().equals(majorName)) {
        		return major;
        	}
        }
        return null;
    }
    
    public String getClassIdByNames(String collegeName, String majorName, String className) {
    	if (collegeMajorsMap == null || majorName == null || className == null) {
            return null;
        }
    	String majorId = getMajorIdByNames(collegeName, majorName);
    	for (ClassInfo classInfo : majorClassesMap.get(majorId)) {
    		if (classInfo.getClassName().equals(className)) {
    			return classInfo.getClassId();
    		}
    	}
    	
    	return null;
    }
}