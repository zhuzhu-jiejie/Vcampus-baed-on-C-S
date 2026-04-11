package seu.vcampus.client.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.LocalDate;
import java.time.Month;

/**
 * 年级转换工具类（全静态方法）
 */
public final class GradeConverter {
    
    // 私有构造器防止实例化
    private GradeConverter() {
        throw new AssertionError("不能实例化工具类");
    }
    
    private static final String[] CHINESE_NUMBERS = {
        "", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"
    };
    
    private static final int ACADEMIC_YEAR_START_MONTH = Month.SEPTEMBER.getValue();
    private static final int ACADEMIC_YEAR_START_DAY = 1;
    
    // ------------------------------------- 2023 -> 大三 ----------------------------------
    
    /**
     * 根据入学年份和当前日期计算年级描述
     * 
     * @param enrollmentYearStr 入学年份字符串（如"2023级"）
     * @param currentDate 当前日期
     * @param isPostgraduate 是否为研究生（本科为false，研究生为true）
     * @return 年级描述（如"大三"、"研二"）
     */
    public static String enrollmentYearToGrade(String enrollmentYearStr, LocalDate currentDate, boolean isPostgraduate) {
        // 去除"级"字并转换为整数
        int enrollmentYear = parseEnrollmentYear(enrollmentYearStr);
        return enrollmentYearToGrade(enrollmentYear, currentDate, isPostgraduate);
    }
    
    /**
     * 根据入学年份和当前日期计算年级描述
     * 
     * @param enrollmentYearStr 入学年份字符串（如"2023级"）
     * @param currentDate 当前日期
     * @param levelOfStudy 培养层级（"本科"/"硕士"/"博士"）
     * @return 年级描述（如"大三"、"研二"）
     */
    public static String enrollmentYearToGrade(String enrollmentYearStr, LocalDate currentDate, String levelOfStudy) {
        // 去除"级"字并转换为整数
        int enrollmentYear = parseEnrollmentYear(enrollmentYearStr);
        return enrollmentYearToGrade(enrollmentYear, currentDate, levelOfStudy);
    }
    
    /**
     * 根据入学年份和当前日期计算年级描述
     * 
     * @param enrollmentYear 入学年份（整数形式）
     * @param currentDate 当前日期
     * @param isPostgraduate 是否为研究生（本科为false，研究生为true）
     * @return 年级描述（如"大三"、"研二"）
     */
    public static String enrollmentYearToGrade(int enrollmentYear, LocalDate currentDate, boolean isPostgraduate) {
        // 计算学年开始日期（假设每年9月1日为新学年开始）
        LocalDate academicYearStart = LocalDate.of(currentDate.getYear(), Month.SEPTEMBER, 1);
        
        // 计算当前学年
        int currentAcademicYear = currentDate.isBefore(academicYearStart) ? 
            currentDate.getYear() - 1 : currentDate.getYear();
        
        // 计算年级数（当前学年 - 入学年份 + 1）
        int gradeLevel = currentAcademicYear - enrollmentYear + 1;
        
        // 处理特殊边界情况
        if (gradeLevel < 1) {
            return isPostgraduate ? "研前" : "大前"; // 未入学状态
        }
        
        // 转换为中文年级描述
        String prefix = isPostgraduate ? "研" : "大";
        return prefix + numberToChinese(gradeLevel);
    }
    
    /**
     * 根据入学年份和当前日期计算年级描述（自动区分本科/研究生）
     * 
     * @param enrollmentYear 入学年份（整数形式）
     * @param currentDate 当前日期
     * @param levelOfStudy 培养层级（"本科"/"硕士"/"博士"）
     * @return 年级描述（如"大三"、"研二"）
     */
    public static String enrollmentYearToGrade(int enrollmentYear, LocalDate currentDate, String levelOfStudy) {
        boolean isPostgraduate = !"本科".equals(levelOfStudy);
        return enrollmentYearToGrade(enrollmentYear, currentDate, isPostgraduate);
    }
    
    /**
     * 解析入学年份字符串（去除"级"字）
     * 
     * @param enrollmentYearStr 入学年份字符串（如"2023级"）
     * @return 整数形式的入学年份
     * @throws IllegalArgumentException 如果格式无效
     */
    private static int parseEnrollmentYear(String enrollmentYearStr) {
        if (enrollmentYearStr == null || enrollmentYearStr.isEmpty()) {
            throw new IllegalArgumentException("入学年份不能为空");
        }
        
        // 去除"级"字和空格
        String cleaned = enrollmentYearStr.replace("级", "").trim();
        
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的入学年份格式: " + enrollmentYearStr);
        }
    }
    
    // ------------------------------------- 大三 -> 2023 ----------------------------------
    
    /**
     * 根据年级名称和当前日期计算入学年份
     * 
     * @param currentDate 当前日期
     * @param gradeName 年级名称（如"大一"、"研三"）
     * @return 入学年份（如2024表示2024级）
     * @throws IllegalArgumentException 如果年级名称格式无效
     */
    public static int calculateEnrollmentYear(LocalDate currentDate, String gradeName) {
        // 验证年级名称格式
        if (gradeName == null || gradeName.length() < 2) {
            throw new IllegalArgumentException("无效的年级名称: " + gradeName);
        }
        
        // 提取年级前缀和数字部分
        String prefix = gradeName.substring(0, 1);
        String numberPart = gradeName.substring(1);
        
        // 将中文数字转换为阿拉伯数字
        int gradeLevel = parseChineseNumber(numberPart);
        
        // 计算基准年份（考虑学年开始时间）
        int baseYear = calculateBaseYear(currentDate);
        
        // 计算入学年份
        return baseYear - (gradeLevel - 1);
    }
    
    /**
     * 计算基准年份（考虑学年开始时间）
     * 
     * 如果当前日期在学年开始日期之前，使用前一年作为基准
     * 否则使用当前年份作为基准
     */
    private static int calculateBaseYear(LocalDate currentDate) {
        int currentYear = currentDate.getYear();
        
        // 检查是否在学年开始日期之前
        if (currentDate.isBefore(LocalDate.of(currentYear, ACADEMIC_YEAR_START_MONTH, ACADEMIC_YEAR_START_DAY))) {
            return currentYear - 1;
        }
        return currentYear;
    }
    
    /**
     * 将中文数字转换为阿拉伯数字
     */
    private static int parseChineseNumber(String chineseNumber) {
        // 尝试解析为阿拉伯数字
        try {
            return Integer.parseInt(chineseNumber);
        } catch (NumberFormatException e) {
            // 继续尝试解析中文数字
        }
        
        // 查找中文数字匹配
        for (int i = 1; i < CHINESE_NUMBERS.length; i++) {
            if (CHINESE_NUMBERS[i].equals(chineseNumber)) {
                return i;
            }
        }
        
        // 尝试匹配"十"开头的数字（如"十二"）
        if (chineseNumber.startsWith("十")) {
            if (chineseNumber.length() == 1) {
                return 10;
            }
            String rest = chineseNumber.substring(1);
            for (int i = 1; i < CHINESE_NUMBERS.length; i++) {
                if (CHINESE_NUMBERS[i].equals(rest)) {
                    return 10 + i;
                }
            }
        }
        
        throw new IllegalArgumentException("无法识别的年级数字: " + chineseNumber);
    }

    // ------------------------------------- 四年 -> 大一 大二 大三 大四 ----------------------------------

    /**
     * 根据学制生成年级列表
     * 
     * @param undergraduateDuration 本科学制（如"4年"）
     * @param postgraduateDuration 研究生学制（如"3年"）
     * @return 不可修改的年级列表（如["大一", "大二", "大三", "大四", "研一", "研二", "研三"]）
     */
    public static List<String> durationToGrades(String undergraduateDuration, String postgraduateDuration) {
        List<String> grades = new ArrayList<>();
        
        addGradesForDuration(grades, undergraduateDuration, "大");
        addGradesForDuration(grades, postgraduateDuration, "研");
        
        return Collections.unmodifiableList(grades);
    }
    
    /**
     * 为特定学制添加年级
     * 
     * @param grades 年级列表
     * @param duration 学制字符串
     * @param prefix 年级前缀
     */
    private static void addGradesForDuration(List<String> grades, String duration, String prefix) {
        if (duration == null || duration.trim().isEmpty()) {
            return;
        }
        
        int years = extractYears(duration);
        if (years <= 0) {
            return;
        }
        
        for (int i = 1; i <= years; i++) {
            grades.add(prefix + numberToChinese(i));
        }
    }
    
    /**
     * 从学制字符串中提取年份数
     */
    private static int extractYears(String duration) {
        try {
            // 移除非数字字符并转换为整数
            String years = duration.replaceAll("[^0-9]", "");
            return years.isEmpty() ? 0 : Integer.parseInt(years);
        } catch (NumberFormatException e) {
            return 0; // 无效格式返回0
        }
    }
    
    /**
     * 将数字转换为中文数字（1-10）
     */
    private static String numberToChinese(int number) {
        if (number >= 1 && number <= 10) {
            return CHINESE_NUMBERS[number];
        }
        return String.valueOf(number); // 超过10直接返回数字
    }
    
    /**
     * 仅生成本科学制年级
     */
    public static List<String> undergraduateGrades(String duration) {
        return generateGrades(duration, "大");
    }
    
    /**
     * 仅生成研究生学制年级
     */
    public static List<String> postgraduateGrades(String duration) {
        return generateGrades(duration, "研");
    }
    
    /**
     * 通用年级生成方法
     */
    private static List<String> generateGrades(String duration, String prefix) {
        if (duration == null || duration.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        int years = extractYears(duration);
        if (years <= 0) {
            return Collections.emptyList();
        }
        
        List<String> grades = new ArrayList<>(years);
        for (int i = 1; i <= years; i++) {
            grades.add(prefix + numberToChinese(i));
        }
        
        return Collections.unmodifiableList(grades);
    }
}