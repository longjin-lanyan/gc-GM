# 原神私服公开注册功能 - 集成说明

## 新增文件清单

| 文件 | 说明 |
|------|------|
| `src/.../entity/IpAccountRecord.java` | IP注册记录实体（对应数据库表 `ip_account_records`） |
| `src/.../repository/IpAccountRecordRepository.java` | IP记录的JPA数据访问接口 |
| `src/.../service/RegisterService.java` | 注册业务逻辑（IP限制 + 调用GC命令） |
| `src/.../controller/RegisterController.java` | 注册API控制器 |
| `html/register.html` | 玩家注册前端页面 |

## API 接口

### POST `/api/register/account`
玩家注册游戏账号。

**请求体：**
```json
{ "username": "MyAccount" }
```

**成功响应：**
```json
{
  "success": true,
  "message": "账号 MyAccount 注册成功！",
  "username": "MyAccount",
  "remainingQuota": 2
}
```

**失败响应（IP限制）：**
```json
{
  "success": false,
  "message": "该IP已达到最大注册数量（3个），无法继续注册",
  "currentCount": 3,
  "maxCount": 3
}
```

### GET `/api/register/quota`
查询当前IP的注册配额。

**响应：**
```json
{
  "used": 1,
  "max": 3,
  "remaining": 2,
  "allowed": true
}
```

## 工作原理

1. 玩家访问 `/html/register.html`，输入账号名并提交
2. 后端检查该IP已注册账号数是否 < 3
3. 若通过，通过 `gc-opencommand-plugin` 的控制台接口执行：
   ```
   account create <username>
   ```
4. Grasscutter 返回成功后，将 `(IP, username)` 记录到 `ip_account_records` 表
5. 前端显示注册成功

## 配置要求

`config.json` 中必须正确设置：
```json
{
  "grasscutter": {
    "serverUrl": "http://你的GC服务器:1145",
    "apiPath": "/opencommand/api",
    "consoleToken": "你的控制台Token"
  }
}
```

> `consoleToken` 是 `gc-opencommand-plugin` 的 `console_token`，用于执行无玩家上下文的管理员命令。

## IP限制修改

如需修改每IP最大账号数，编辑 `RegisterService.java`：
```java
private static final int MAX_ACCOUNTS_PER_IP = 3;  // 改这里
```

## 数据库

应用启动时 Hibernate 会自动创建 `ip_account_records` 表（`spring.jpa.hibernate.ddl-auto=update`）。

表结构：
```sql
CREATE TABLE ip_account_records (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  ip_address VARCHAR(45)  NOT NULL,
  username   VARCHAR(50)  NOT NULL,
  created_at DATETIME     NOT NULL,
  INDEX idx_ip_address (ip_address)
);
```
