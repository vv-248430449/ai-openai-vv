# ai-openai-vv

![CI](https://github.com/vv-248430449/ai-openai-vv/actions/workflows/ci.yml/badge.svg)

> 一个基于 **Spring AI 2.0** 的 LLM（大语言模型，*Large Language Model*）应用**学习 / 演示工程**。
> 用同一套 `ChatClient` 抽象，把「文本对话、流式响应、文生图、语音、多模态、Function Calling（函数调用）、多租户数据隔离」等能力一次性跑通。

---

## 一、项目介绍

`ai-openai-vv` 是 `F:\Idea_Project_code` 工作区下的一个**子项目**，定位是 **Java + AI 应用落地的练手工程**，不是生产系统。

它想解决的核心问题只有一个：

> **怎么用 Java / Spring 把大模型「接进来、调得动、能扩展」？**

为此它覆盖了 3 类最常被问到的 AI 应用能力：

1. **基础对话**：同步问答、流式输出（SSE，*Server-Sent Events*，服务器向浏览器单向推送事件）。
2. **多模态（Multimodal，即模型同时理解文字+图片/音频）**：文生图、语音识别（ASR，*Automatic Speech Recognition*）、语音合成（TTS，*Text-To-Speech*）、图片理解。
3. **Agent 能力**：Function Calling（让模型自己决定"该调哪个工具/函数"来获取实时数据）、以及**多租户（Multi-tenant，多个客户共用一套系统但数据彼此隔离）** 场景下的工具调用。

> ⚠️ **命名与配置不符的提醒**：项目名叫 `ai-openai-vv`，但 `application.yaml` 里的 `base-url` 实际指向 **Moonshot（Kimi）** 的 OpenAI 兼容端点（`https://api.moonshot.cn/v1`），模型为 `kimi-k2.6`。
> 之所以能这么做，是因为 `spring-ai-starter-model-openai` 走的是 **OpenAI 兼容协议**——任何实现了该协议的国产模型（Kimi / 智谱 / 通义等）都能直接对接，换厂商只改配置、不动代码。

---

## 二、项目概览

| 维度 | 说明 |
|------|------|
| 目标人群 | 正在学「Java + AI 应用开发」的开发者（练手用） |
| 核心抽象 | `ChatClient`：Spring AI 统一的对话客户端，屏蔽底层厂商差异 |
| 演示维度 1 | **单租户全能对话**：文本 / 流式 / 图 / 音 / 视觉，全在一个 `SimpleAiController` |
| 演示维度 2 | **多租户 + 工具调用**：`multitenant/demo` 包下，展示「每个租户独立数据库 + @Tool 随调用走租户上下文」 |
| 模型来源 | Moonshot(Kimi) OpenAI 兼容端点（默认 `kimi-k2.6`） |
| 是否生产级 | ❌ 否，包含调试代码与演示用内存库，仅作学习参考 |

---

## 三、技术栈

| 技术 | 版本 / 说明 |
|------|------------|
| **Java** | 17（LTS，*Long-Term Support* 长期支持版） |
| **Spring Boot** | 4.1.0 |
| **Spring AI** | 2.0.0（`spring-ai-starter-model-openai`，OpenAI 兼容客户端） |
| **模型服务** | Moonshot(Kimi) `https://api.moonshot.cn/v1`，模型 `kimi-k2.6`（OpenAI 兼容协议） |
| **数据库** | H2 内存库（仅多租户演示用，无需外部 MySQL） |
| **Lombok** | 1.18.30（编译期注解，减少样板代码） |
| **构建工具** | Maven（CI 用 runner 自带 `mvn`；本地 `./mvnw` 当前为空文件，可改用 `mvn` 命令） |

> 依赖关系：`spring-boot-starter-webmvc`（Web 服务）+ `spring-ai-starter-model-openai`（AI 客户端）+ `spring-boot-starter-jdbc` + `h2`（多租户演示）+ `spring-boot-starter-webmvc-test`（测试）。

---

## 四、功能模块

### 4.1 单租户全能对话 — `SimpleAiController`
最全的「AI 能力橱窗」，每个能力一个 HTTP 接口：

| 接口 | 能力 | 备注 |
|------|------|------|
| `GET /ai/simple` | 同步文本问答 | 最简单，eval 演示的 SUT（被测系统）原型就在这里 |
| `GET /ai/stream` | 流式响应（SSE） | 边生成边返回，体验更顺 |
| `GET /ai/img` | 文生图（DALL·E-3） | 依赖模型是否支持文生图 |
| `GET /ai/audio2text` | 语音转文字（Whisper-1） | 读取 `classpath:/hello.mp3` |
| `GET /ai/text2audio` | 文字转语音（TTS-1） | 输出 `xushu.mp3` |
| `GET /ai/mutil` | 多模态图片理解 | 读取 `classpath:/test.png` |

### 4.2 函数调用 — `FunctionCallController` + `LocationNamesService`
- `GET /ai/fc`：演示 **Function Calling（函数调用）** —— 模型遇到"实时数据"类问题（如"长沙有多少人叫徐庶"）时，自己决定调用 `@Tool` 注解的 `LocationNamesService` 拿结果，再组织成自然语言回答。
- Spring AI 2.0 已移除 `withFunction`，改为 `.tools(bean)` + `@Tool` 注解声明工具方法。

### 4.3 多租户 + 工具调用 — `multitenant/demo`
核心演示「**上下文跟着每次调用走**」：

| 类 | 作用 |
|----|------|
| `TenantProperties` | 绑定 `application.yaml` 的 `tenants:` 列表（含 t1 / t2 两个租户的连接信息） |
| `TenantRepo` | 单个租户的 DAO（数据访问对象，*Data Access Object*），各自连自己的库 |
| `TenantRepoRegistry` | 「心脏」：`Map<租户ID, TenantRepo>`，`@PostConstruct` 时建好各租户连接池 |
| `TenantLocationService` | 一个 `@Tool`，**每次调用时从 `ToolContext` 取 `tenantId`**，再查对应租户的库 |
| `DemoController` | `GET /demo/count`（不调 LLM，直接验证多租户隔离）、`GET /ai/fc/tenant/count`（带租户的函数调用） |

启动后访问：
```
http://localhost:8080/demo/count?tenant=t1&name=张伟&location=北京   → "租户[t1] 的 北京 有 10 个叫 张伟 的人"
http://localhost:8080/demo/count?tenant=t2&name=张伟&location=北京   → "租户[t2] 的 北京 有 3 个叫 张伟 的人"
```
t1 / t2 返回不同数字，证明**数据真的隔离到了各自独立的库**。

---

## 五、目录结构

```
ai-openai-vv/
├── pom.xml                          # 依赖与构建（Spring Boot 4.1 / Spring AI 2.0 / Java 17）
├── mvnw / mvnw.cmd                  # Maven 包装器（无需本机预装 Maven）
├── src/main/java/ai/openai/vv/
│   ├── AiOpenaiVvApplication.java   # 启动类（@EnableConfigurationProperties 注册租户配置）
│   ├── callbacks/
│   │   ├── AIConfigs.java           # 全局 ChatClient Bean（默认系统提示 + 默认租户 t1）
│   │   └── LocationNamesService.java# 单租户版 @Tool（函数调用工具）
│   ├── controller/
│   │   ├── SimpleAiController.java  # 全能对话：文本/流式/图/音/视觉
│   │   └── FunctionCallController.java # /ai/fc 函数调用演示
│   └── multitenant/demo/            # 多租户 + 工具调用演示
│       ├── DemoController.java
│       ├── TenantLocationService.java
│       ├── TenantProperties.java
│       ├── TenantRepo.java
│       └── TenantRepoRegistry.java
├── src/main/resources/
│   ├── application.yaml             # ⚠️ 含明文 API Key，见「注意事项」第 1 条
│   ├── hello.MP3                    # 语音识别演示素材
│   └── test.png                     # 图片理解演示素材
└── src/test/java/ai/openai/vv/
    ├── AiOpenaiVvApplicationTests.java  # 原 Spring Boot 上下文加载测试
    └── AiEvalTest.java                  # ✅ 新增：最简 eval（评估）演示，见下方说明
```

---

## 六、快速开始

```bash
# 1. 进入项目（用自带 Maven 包装器，避免本机 Maven 版本问题）
cd F:\Idea_Project_code\ai-openai-vv
./mvnw spring-boot:run          # Windows 用 mvnw.cmd

# 2. 启动后访问（默认 8080 端口）
curl "http://localhost:8080/ai/simple?message=给我讲个笑话"
curl "http://localhost:8080/demo/count?tenant=t1&name=张伟&location=北京"
```

> 前置条件：`application.yaml` 中需配置**可用的 API Key** 且运行环境能访问对应模型端点。当前默认指向 Moonshot(Kimi)，需保证网络可达。

---

## 七、注意事项（⚠️ 重点）

### ⛔ 1. API Key 明文泄露 —— 红线，请立即处理
`application.yaml` 第 16 行把真实 Key 写死在代码里，且 `SimpleAiController.java` 的 **第 59、75 行** 用 `System.out.println(openAiKey)` **把 Key 打印到控制台日志**。
- **风险**：任何能看代码 / 日志的人都能拿走你的 Key，产生费用或被滥用。
- **必须做**：
  1. 立刻去 Moonshot 后台**吊销（revoke）该 Key**；
  2. 改为从环境变量读取（文件里已预留 `vv.openai.key: ${OPENAI_KEY}` 等写法，把注释打开、删掉明文那行）；
  3. **删除** `SimpleAiController` 里的两处 `System.out.println(openAiKey)` 调试代码。
- 永远不要把 Key 提交进 Git。

### 2. 项目名与配置不符
见「项目介绍」。它对接的是 Kimi 而非 OpenAI 官方，换模型/厂商只改 `application.yaml` 的 `base-url` + `chat.model`，代码无需动。

### 3. Spring AI 2.0 的破坏性变更（踩坑结论，已用真实容器验证）
- `withFunction(...)` 已移除 → 改用 `.tools(bean)` + `@Tool` 注解。
- `@ConfigurationProperties` **构造器绑定不支持 `Map<String, 对象>`**（实测 `size=null`），已改用 `List<扁平 record>` 再手动拼 `Map`。

### 4. 部分接口依赖模型能力
文生图（DALL·E-3）、语音（Whisper / TTS-1）、视觉理解（GPT-4o 类）等接口**依赖具体模型是否提供支持**。当前默认 `kimi-k2.6` 可能不支持文生图 / 语音，调用会报错——这是模型能力边界，不是代码 bug。

### 5. 代理设置
`AiOpenaiVvApplication.java` 里有一段被注释掉的代理设置。当前指向国内 Moonshot 端点**不需要代理**；若日后改回 OpenAI 官方（`api.openai.com`），再按需打开代理注释。

### 6. 这是练手工程
`AIConfigs` 写死了角色扮演系统提示（"我叫徐庶，我 5 岁…以我爸爸身份对话"），`TenantRepoRegistry` 用 `System.out.println` 打印注册日志——都是演示痕迹，生产环境应移除或改日志框架。

---

## 八、总结

`ai-openai-vv` 用一个工程把 **Spring AI 2.0 的对话 / 多模态 / 函数调用 / 多租户** 四条主线都跑了一遍，是「Java 怎么接大模型」的优质练手样本。

**建议的学习顺序**：
1. 先读 `SimpleAiController` 的 `/ai/simple`（最干净的 `ChatClient` 调用）；
2. 再看 `FunctionCallController` + `@Tool`（模型如何"调工具"）；
3. 最后啃 `multitenant/demo`（上下文隔离、DAO、注册表）——这块最贴近真实企业需求。

**它和你找工作有什么关系**：新闻里反复在说——"AI 生成得快，但**对不对没人敢直接信**"。本项目里 `/ai/simple` 就是天然的 **eval（评估）SUT（被测系统）**：把"问题+期望答案"写成用例集、自动跑一遍、统计通过率，你就掌握了 2026 年 AI coding 岗最值钱的"**会验证 AI**"能力。相关演示见 `src/test/java/ai/openai/vv/AiEvalTest.java`。

---

## 九、eval 自动跑 + 门禁（GitHub Actions CI）

本项目把「有 eval」升级成了「**eval 自动跑 + 多层门禁**」：每次 push / PR 到 `main`，GitHub Actions 用 JDK 17 执行 `mvn -B verify`，依次完成：

1. **跑测试**：surefire 执行全部测试，其中 `AiEvalTest` 在配置 `OPENAI_KEY` 时**真实调用大模型、统计通过率**（③ eval 通过率门禁）。
2. **覆盖率门禁**：jacoco:check 比对 `src/main` 被测试碰过的比例，低于阈值（当前 `LINE >= 0.40`）即 `BUILD FAILURE`（② 覆盖率门禁）。

- CI 工作流文件：`.github/workflows/ci.yml`
- 状态徽标（标题下方）：绿 = 构建 + eval + 覆盖率全过；红 = 任一失败
- 状态含义：
  - **绿**：你自己的 push（仓库已配 `OPENAI_KEY` secret）真实跑 LLM eval 全过，且行覆盖率达标。
  - **红**：编译失败 / 模型回答不满足用例（③ 触发）/ 覆盖率低于阈值（② 触发）。未配 `OPENAI_KEY` 时真实调用会失败 → 红（**宁可红，不假绿**）。

### 让 CI 真正跑起 eval，你需要做一件事
在 GitHub 仓库 **Settings → Secrets and variables → Actions → New repository secret** 里添加：
```
Name:  OPENAI_KEY
Value: 你的真实 API Key（sk- 开头那个，和本地 application.yaml 用的是同一个）
```
添加后，下一次 push 到 `main` 就会自动带上密钥、真实执行 eval 与覆盖率检查。

### ② 覆盖率门禁（JaCoCo）怎么调阈值
- 阈值写在 `pom.xml` 的 `jacoco-maven-plugin` → `check` → `minimum`（当前 0.40，即行覆盖率 40%）。
- 首次 CI 跑完后，看 Actions 日志里 jacoco 报告的**实际行覆盖率**；若门禁标红，二选一：
  - 把 `minimum` 调到「实际值 − 0.1」附近（先让门禁稳定绿），或
  - 补测试把覆盖率顶上去（更符合门禁本意）。
- 本地看报告：跑 `mvn test` 后打开 `target/site/jacoco/index.html`。

> 注：本机 `./mvnw` 包装器目前在仓库里是空文件（0 字节），本地可用 `mvn test` 代替；CI 用的是 runner 自带的 `mvn`，不受影响。

---

### 附：常见缩写对照
- **LLM**：Large Language Model，大语言模型
- **API**：Application Programming Interface，应用程序接口
- **SSE**：Server-Sent Events，服务器向客户端单向推送事件（流式响应用）
- **ASR**：Automatic Speech Recognition，自动语音识别（语音转文字）
- **TTS**：Text-To-Speech，文本转语音
- **DAO**：Data Access Object，数据访问对象（封装数据库操作）
- **LTS**：Long-Term Support，长期支持（Java 版本类型）
- **SUT**：System Under Test，被测系统（eval 里你要评估的那个对象）
- **eval**：evaluation，评估（用用例集自动检验 AI 输出对不对）
