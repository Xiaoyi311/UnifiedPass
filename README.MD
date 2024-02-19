# BackroomsMC 统一通行证(后端)
一个基于 Yggdrasil 协议规范制作的外置登录系统

## 配置文件
```properties
#Debug Mode
debug=false

#Mysql Data Base Setting
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/backroom
spring.datasource.username=root
spring.datasource.password=REPLACE_YOUR_PASSWORD
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.jpa.open-in-view=false

#Redis Data Base Setting
spring.redis.host=127.0.0.1
spring.redis.port=6379
spring.redis.database=0
```

## SQL 表
由于 Springboot 的特殊性，SQL 数据库的表需要自行创建

### 角色表
存储所有角色数据
```sql
CREATE TABLE `up_profilelist` (
    `uuid` text NOT NULL,
    `name` text NOT NULL,
    `user` text NOT NULL,
    `model` text NOT NULL,
    `skin` text,
    `cape` text,
    PRIMARY KEY (`uuid`(32))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

### 设置表
设置服务器的相关配置
```sql
CREATE TABLE `up_settings` (
    `id` int NOT NULL AUTO_INCREMENT,
    `key` int NOT NULL,
    `value` text NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

#### 数据配置规范

| Key(键) |    Value(值)    | 范例                        |
|:------:|:--------------:|---------------------------|
|   0    |     服务器名称      | BackroomsMC 统一通行证         |
|   1    |     服务器网址      | http://127.0.0.1:8080     |
|   2    | RSA 公钥(PKCS#8) | ----- BEGIN ... KEY ----- |
|   3    | RSA 私钥(PKCS#8) | ----- BEGIN ... KEY ----- |

### 令牌表
存储用户令牌数据
```sql
CREATE TABLE `up_tokenlist` (
  `access_token` text NOT NULL,
  `client_token` text NOT NULL,
  `profile` text,
  `user` text NOT NULL,
  `time` mediumtext NOT NULL,
  `temp_invalid` tinyint(1) NOT NULL,
  PRIMARY KEY (`access_token`(32))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

### 用户表
存储所有用户信息
```sql
CREATE TABLE `up_userlist` (
  `id` text NOT NULL,
  `username` text NOT NULL,
  `password` text NOT NULL,
  `mojang` text NOT NULL,
  `admin` tinyint(1) NOT NULL,
  `register_time` mediumtext NOT NULL,
  `profile` text,
  PRIMARY KEY (`id`(32))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```


## 使用方法
1. 下载 Release 的最新版 Jar，并放到一个文件夹内
2. 在文件夹内新建 application.properties (编写方法见 [配置文件](#配置文件))
3. 在 MySQL 数据库中新建所需的表 (SQL 语句见 [SQL 表](#sql-表))
4. 使用 java -jar 文件名.jar 启动服务
5. 访问 http://127.0.0.1:8080/ 使用