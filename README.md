# BuddyLink

> a uniWeb to contact everyone

## Features

- 引入 Redis 设计缓存，限流器，BitMap积分表，分布式锁等。
- 引入 ElaticSearch + ik 中文分词器 ，进行统一聚合搜索。
- 引入 WebSocket 实现双工通信功能。
- 引入 RqbbitMQ + Java SSE 实现消息推送通知。
- Vue and VantComponent for UI

### Prerequisites

Before running the project, ensure you have the following installed:

- [Java 8+](https://adoptopenjdk.net/)
- [Maven](https://maven.apache.org/)
- [Node.js](https://nodejs.org/)

---

## 供参考的工具版本

- Spring Boot: `2.6.13` 
- node.js: `18.16.0`
- rabbitmq_server-3.12.0
- elasticsearch-7.17.9
- kibana-7.17.9-windows-x86_64
- elasticsearch-analysis-ik-7.17.7
- logstash-7.17.9

---

### Step-by-Step Guide

1. Clone the repository:
   
   ```bash
   git clone https://github.com/your-username/project-name.git
   ```

2. Clone the repository:
   
   ```bash
   cd BuddyLink
   ```

3. Clone the repository:
   
   #### For Java (If applicable)
   
   ```bash
   mvn clean install
   ```
   
   #### For Frontend (If applicable)
   
   ```bash
   npm install
   ```

4. Run the project:
   
   #### For Java (If applicable)
   
   ```bash
   mvn spring-boot:run
   ```
   
   #### For Frontend (If applicable)
   
   ```bash
   npm start dev
   ```

## Example for showing

<img src="./assets/example.gif" title="" alt="Logo" width="183">

---

## Contributing

1. Fork this repository.
2. Create a new branch: `git checkout -b feature/your-feature`.
3. Commit your changes: `git commit -am 'Add new feature'`.
4. Push to the branch: `git push origin feature/your-feature`.
5. Open a Pull Request.

We welcome all contributions, whether it's bug fixes, new features, or documentation improvements!
