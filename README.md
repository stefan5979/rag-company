# RAG-DEMO 轻量化检索增强问答系统
## 项目简介
基于 `SpringBoot3 + Spring AI Alibaba + LangChain4j` 搭建完整RAG检索增强生成系统，解决通用大模型知识滞后、回答幻觉（凭空编造内容）的问题。
代码分层遵循标准后端工程化规范，开箱即用，可直接在此基础上迭代扩展为线上业务服务。

## 技术栈
- JDK 17
- 核心框架：Spring Boot 3.3.4
- AI 集成组件：Spring AI Alibaba（阿里通义DashScope）、LangChain4j 0.36.2
- 大模型能力：通义千问 qwen-turbo（对话生成）、text-embedding-v1（文本向量Embedding）
- 向量存储：InMemoryEmbeddingStore 内存向量库（轻量化验证方案，无需额外部署中间件）
- 项目构建工具：Maven
- 简化开发工具：Lombok

## 完整RAG执行流水线
1. 文档加载：项目启动自动读取 `resources/rag-test.txt` 本地私有知识库
2. 文本切片：采用递归文本分割策略，单片段最大200字符，相邻片段重叠50字符，避免完整语义被切割断裂
3. Embedding向量化：调用阿里向量模型，将文本转换为多维数字向量存入内存向量库
4. 语义相似度检索：将用户提问转为向量，匹配库内语义最相近的文档片段，筛选Top2参考内容
5. 检索增强Prompt构造：将检索得到的参考原文拼接至用户提问，作为大模型输入上下文
6. 合规回答生成：通过系统提示词约束模型输出逻辑，仅依据检索到的知识库内容作答；无匹配信息时统一返回「暂无相关信息」

## 项目分层架构

rag-demo
├─ src/main/java/com/ai/ragdemo
│  ├─ RagDemoApplication.java        # 项目启动入口
│  ├─ config/AiBeanConfig.java      # AI模型Bean统一配置，完成框架适配
│  ├─ entity/Result.java             # 全局统一HTTP接口返回封装类
│  ├─ service/RagService.java        # 文档加载、文本切片、向量存储核心业务逻辑
│  └─ agent/RagChatAgent.java        # RAG逻辑代理层，封装检索增强、模型提示词规则
│  └─ controller/RagController.java  # 对外HTTP问答接口
├─ src/main/resources
│  ├─ application-demo.yml           # 配置模板（密钥占位，无敏感信息）
│  └─ rag-test.txt                   # 本地知识库文本文件
├─ pom.xml                           # Maven依赖统一管理
├─ .gitignore                        # Git提交过滤规则
└─ README.md                         # 项目说明文档


## 快速启动步骤
1. 克隆本仓库至本地开发环境
2. 在 `resources` 目录新建 `application.yml`，复制 `application-demo.yml` 模板内容，将 `api-key` 替换为个人DashScope密钥
3. IDEA刷新Maven依赖列表，等待全部依赖包下载完成
4. 运行 `RagDemoApplication` 启动项目，控制台输出 `✅ 文档加载完成` 代表初始化成功
5. 通过浏览器或接口工具访问问答接口：
   ```
GET http://127.0.0.1:8080/rag/chat?question=你的问题


## 项目核心特性
1. **轻量化部署**：内置内存向量库， 无需部署Milvus、Elasticsearch等第三方向量中间件，单服务即可完整运行
2. **分层解耦设计**：配置层、业务服务层、代理逻辑层、接口控制层职责完全隔离，符合标准后端工程规范
3. **规避模型幻觉**：通过固定提示词限制模型输出范围，强制仅基于私有知识库内容生成回答
4. **高可扩展预留**：大模型、向量存储均通过Bean统一注入，无需重构核心流程即可完成替换升级，支持扩展方向：
   - Milvus持久化向量数据库
   - Rerank重排模型优化检索精准度
   - BM25关键词+向量混合检索方案
5. **无编译异常**：适配DashScope官方SDK，规避原生集成场景下类型转换、方法重写、异常捕获等常见编译问题

## 当前版本局限
1. 仅使用向量相似度完成粗检索，未接入Rerank重排模型，检索结果精准度存在优化空间
2. 采用内存向量存储，服务重启后所有向量数据丢失，不支持持久化存储
3. 未实现接口限流、问答日志持久化、多轮对话上下文记忆功能

## 后续迭代规划
1. 接入轻量级Rerank重排模型，过滤检索结果中无关片段，提升问答匹配精度
2. 集成Milvus向量数据库，实现向量数据持久化存储
3. 新增BM25关键词检索，构建关键词+向量混合检索架构
4. 补充全局异常捕获、接口限流、问答记录数据库存储能力
5. 支持多格式文件解析，包含TXT、PDF等文档自动加载切片
