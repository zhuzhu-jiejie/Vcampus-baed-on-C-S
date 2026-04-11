/**
 *
 */
/**
 *
 */
module seu.vcampus.client {
	// require的是模块名 不是包名
	requires com.google.gson;
	requires javafx.controls;
	requires javafx.fxml;
	requires seu.vcampus.model;
	requires javafx.web;
	requires java.desktop;
	requires javafx.graphics;
	requires java.prefs;

	// 由于controller需要通过反射机制 由fxml、gson访问 因此发放给它们权限
	opens seu.vcampus.client.controller to javafx.graphics, javafx.fxml, com.google.gson,javafx.base;

	// 不知道为什么 javafx自带这个指令
	exports seu.vcampus.client;
}