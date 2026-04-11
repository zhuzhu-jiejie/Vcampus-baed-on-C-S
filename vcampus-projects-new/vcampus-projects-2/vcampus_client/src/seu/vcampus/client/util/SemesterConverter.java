package seu.vcampus.client.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 学期格式双向转化器：
 * 1. 正向：yyyy-yyyy-x → yyyy-yyyy-第一学期/第二学期/第三学期（x=1/2/3）
 * 2. 反向：yyyy-yyyy-第一学期/第二学期/第三学期 → yyyy-yyyy-x（x=1/2/3）
 */
public class SemesterConverter {
    // -------------------------- 共享配置 --------------------------
    // 1. 学期映射（正向：数字→中文；反向：中文→数字）
    private static final Map<String, String> NUM_TO_CHINESE;
    private static final Map<String, String> CHINESE_TO_NUM;
    // 2. 年份部分正则（匹配 "yyyy-yyyy"）
    private static final String YEAR_PATTERN_STR = "^\\d{4}-\\d{4}";

    // 静态代码块：初始化映射关系
    static {
        // 正向映射：数字x → 中文学期名
        NUM_TO_CHINESE = new HashMap<>();
        NUM_TO_CHINESE.put("1", "第一学期");
        NUM_TO_CHINESE.put("2", "第二学期");
        NUM_TO_CHINESE.put("3", "第三学期");

        // 反向映射：中文学期名 → 数字x（从正向映射反向生成，避免重复维护）
        CHINESE_TO_NUM = new HashMap<>();
        for (Map.Entry<String, String> entry : NUM_TO_CHINESE.entrySet()) {
            CHINESE_TO_NUM.put(entry.getValue(), entry.getKey());
        }
    }


    // -------------------------- 正向转化（原功能） --------------------------
    /**
     * 正向转化：yyyy-yyyy-x → yyyy-yyyy-第一学期/第二学期/第三学期
     * @param input 输入格式：yyyy-yyyy-x（x=1/2/3）
     * @return 转化后中文学期
     * @throws IllegalArgumentException 输入格式错误时抛出
     */
    public static String toChinese(String input) {
        // 1. 非空校验
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("输入学期不能为空！");
        }
        String trimmedInput = input.trim();

        // 2. 格式校验（匹配 "yyyy-yyyy-x"）
        Pattern pattern = Pattern.compile(YEAR_PATTERN_STR + "-[123]$");
        if (!pattern.matcher(trimmedInput).matches()) {
            throw new IllegalArgumentException(
                "正向转化格式错误！需为 'yyyy-yyyy-x'（x=1/2/3），示例：'2023-2024-1'→'2023-2024-第一学期'"
            );
        }

        // 3. 拆分并转化
        int lastHyphenIdx = trimmedInput.lastIndexOf("-");
        String yearPart = trimmedInput.substring(0, lastHyphenIdx); // 提取 "yyyy-yyyy"
        String numPart = trimmedInput.substring(lastHyphenIdx + 1); // 提取 "x"
        String chinesePart = NUM_TO_CHINESE.get(numPart); // 映射为中文

        return yearPart + "-" + chinesePart;
    }

    /**
     * 正向转化（容错版）：非法输入返回默认值
     */
    public static String toChinese(String input, String defaultValue) {
        try {
            return toChinese(input);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }


    // -------------------------- 反向转化（新增功能） --------------------------
    /**
     * 反向转化：yyyy-yyyy-第一学期/第二学期/第三学期 → yyyy-yyyy-x（x=1/2/3）
     * @param input 输入格式：yyyy-yyyy-第一学期/第二学期/第三学期
     * @return 转化后数字学期（yyyy-yyyy-x）
     * @throws IllegalArgumentException 输入格式错误时抛出
     */
    public static String toNumber(String input) {
        // 1. 非空校验
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("输入学期不能为空！");
        }
        String trimmedInput = input.trim();

        // 2. 格式校验（匹配 "yyyy-yyyy-第一/第二/第三学期"）
        String chinesePartRegex = "第一学期|第二学期|第三学期"; // 匹配3种中文学期
        Pattern pattern = Pattern.compile(YEAR_PATTERN_STR + "-(" + chinesePartRegex + ")$");
        if (!pattern.matcher(trimmedInput).matches()) {
            throw new IllegalArgumentException(
                "反向转化格式错误！需为 'yyyy-yyyy-第一学期/第二学期/第三学期'，示例：'2023-2024-第二学期'→'2023-2024-2'"
            );
        }

        // 3. 拆分并转化（从最后一个 "-" 分割，避免年份中 "-" 干扰）
        int lastHyphenIdx = trimmedInput.lastIndexOf("-");
        String yearPart = trimmedInput.substring(0, lastHyphenIdx); // 提取 "yyyy-yyyy"
        String chinesePart = trimmedInput.substring(lastHyphenIdx + 1); // 提取 "第一学期" 等

        // 4. 中文→数字映射（因前面已校验格式，映射必然存在，无需判空）
        String numPart = CHINESE_TO_NUM.get(chinesePart);

        return yearPart + "-" + numPart;
    }

    /**
     * 反向转化（容错版）：非法输入返回默认值
     */
    public static String toNumber(String input, String defaultValue) {
        try {
            return toNumber(input);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}