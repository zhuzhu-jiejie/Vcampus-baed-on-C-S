/**
 * 
 */
/**
 * 
 */
module seu.vcampus.model {
	requires com.google.gson;
	// 该包（与模块同名）需要开放反射权限给gson 但是有可能使用别的解析工具 所以全部开放
	opens seu.vcampus.model;
	opens seu.vcampus.util;
	// 导出该包 其它地方应用模块名requires 然后再import包名（弄清楚什么时候是包 什么时候是模块）
	exports seu.vcampus.model;
	exports seu.vcampus.util;
	requires java.sql;
}