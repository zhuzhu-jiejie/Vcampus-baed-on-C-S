1、在电脑上配置好redis（Server.java的第156-160行），mysql（Server.java的第115-118行），nginx，都要启用，
2、想要实现本地局域网下不同pc间的客户端服务器通信，在"vcampus-projects-new\vcampus-projects-new\vcampus-projects-2\vcampus_client\src\seu\vcampus\client\network\SocketManager.java"的22-23行修改服务器设置
3、Server.java的5096行设置了绝对路径，该路径为客户端上传到服务器本地的pdf资源的存储路径，需要修改好对应路径。（不存在数据库是因为pdf资源太大会降低mysql性能）
