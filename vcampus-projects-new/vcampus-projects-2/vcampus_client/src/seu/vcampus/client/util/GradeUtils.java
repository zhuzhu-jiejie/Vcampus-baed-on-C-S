package seu.vcampus.client.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 成绩计算/格式化工具类
public class GradeUtils {
    // 计算总成绩：平时成绩*平时比例 + 期末成绩*期末比例（保留2位小数）
    public static BigDecimal calculateTotalGrade(BigDecimal regularGrade, BigDecimal finalGrade, int finalRatio) {
        int regularRatio = 100 - finalRatio; // 平时成绩占比
        // 处理null（未录入成绩时按0计算，或根据业务返回null）
        BigDecimal regular = regularGrade == null ? BigDecimal.ZERO : regularGrade;
        BigDecimal finalG = finalGrade == null ? BigDecimal.ZERO : finalGrade;
        
        return regular.multiply(new BigDecimal(regularRatio))
                     .add(finalG.multiply(new BigDecimal(finalRatio)))
                     .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP); // 四舍五入保留2位
    }

    // 格式化BigDecimal为字符串（如 86.0 → "86.00"，避免科学计数法）
    public static String formatBigDecimal(BigDecimal value) {
        if (value == null) return "";
        return value.setScale(2, RoundingMode.HALF_UP).toString();
    }
    
    /**
     * BigDecimal 转 Double（默认保留2位小数，四舍五入，适配成绩场景）
     * @param value 待转换的BigDecimal值（如成绩）
     * @return 转换后的Double，若输入为null则返回null
     * @note Double存在精度限制，建议仅在UI组件（如Chart、Spinner）需要Double类型时使用
     */
    public static Double bigDecimalToDouble(BigDecimal value) {
        // 调用重载方法，默认保留2位小数+四舍五入
        return bigDecimalToDouble(value, 2, RoundingMode.HALF_UP);
    }

    /**
     * 重载：自定义小数位数和舍入模式的BigDecimal转Double
     * @param value 待转换的BigDecimal值
     * @param scale 保留的小数位数（如1、2）
     * @param roundingMode 舍入模式（如四舍五入、向上取整）
     * @return 转换后的Double，若输入为null则返回null；若scale<0则抛出IllegalArgumentException
     */
    public static Double bigDecimalToDouble(BigDecimal value, int scale, RoundingMode roundingMode) {
        // 1. 处理null值（与现有方法的null处理逻辑一致）
        if (value == null) {
            return null;
        }
        // 2. 校验小数位数合法性（避免无效输入）
        if (scale < 0) {
            throw new IllegalArgumentException("小数位数不能为负数：" + scale);
        }
        // 3. 先按指定精度格式化BigDecimal，再转为Double（规避直接转换的精度丢失）
        BigDecimal scaledValue = value.setScale(scale, roundingMode);
        return scaledValue.doubleValue();
    }
}