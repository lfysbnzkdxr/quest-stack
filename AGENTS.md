# AGENTS.md

QuestStack（题栈）—— Android 面试题练习应用。Kotlin + Jetpack Compose + Miuix（MIUI 风格 UI），单 `:app` 模块，包名 `com.queststack`。minSdk 33 / target+compile 37。无 README。CI 见 `.github/workflows/ci.yml`（单测 + 编译，push 到 main 或 PR 时触发）。

## 构建与验证

- Windows 下用 `.\gradlew.bat`（Gradle 9.6.1 wrapper），如 `.\gradlew.bat :app:assembleDebug`。
- 验证改动：跑 `.\gradlew.bat :app:testDebugUnitTest`（`app/src/test` 下的 JVM 单测）+ `:app:assembleDebug` 编译；UI 效果靠真机冒烟（本仓库不做 Compose UI 测试，Miuix 毛玻璃曾有真机崩溃风险）。
- `local.properties`（sdk.dir）被 gitignore，机器相关，不要提交。
- 注意 AGP 9.3.0 内置 Kotlin，**不要添加 `org.jetbrains.kotlin.android` 插件**；根 `build.gradle.kts` 中 `buildscript` 显式 pin 的 `kotlin-gradle-plugin:2.4.10` classpath 是必须的，别删。

## 架构要点（文件名看不出来）

- **无依赖注入框架**：`DataContainer` 全局单例在 `QuestStackApp.onCreate()` 初始化；ViewModel 通过构造器默认参数取依赖（如 `= DataContainer.questionRepository`）。新 ViewModel 照此模式，不要引入 Hilt。
- **Room**：`AppDatabase` 用 `fallbackToDestructiveMigration()` 兜底 + `exportSchema = true`（schema 快照提交在 `app/schemas/`，已入库）。**修改实体时必须递增 `version`**，v0.x 阶段不写 Migration 对象。
- **主题**：`AppSettings.themeMode` 是全局变量，由 `DataContainer.init()` 里 collect DataStore 更新；改主题逻辑注意这层联动。
- **AI 层**（`ai/AiClient.kt`）：OpenAI 兼容 API，方法**不捕获异常**，由 ViewModel 处理 `IOException` / `IllegalArgumentException` / `TimeoutCancellationException`；`timeoutSeconds` 参数从用户设置一路传入。
- **敏感信息**：API Key 和 WebDAV 密码经 `SecureStorage`（AES-GCM）加密后才写入 DataStore Preferences。
- **备份**：kotlinx.serialization JSON，导出/导入需 `ignoreUnknownKeys = true`；导入时校验版本号（当前 backup v1），高版本文件要报"请升级应用"而非"格式不正确"。
- **多表写操作**必须包在 `database.withTransaction { }` 里（见 `QuestionRepositoryImpl`）。

## 约定

- 注释、UI 文案、commit message 均用中文；commit 风格为 `type: 中文描述`（`feat:` / `fix:` / `chore:`）。
- UI 字符串硬编码在 composable 里（`strings.xml` 只有 app_name），新增界面沿用此做法。
- UI 走 Miuix 组件 + 毛玻璃（`ui/component/GlassTopAppBar.kt` 等）；注意真机毛玻璃崩溃风险（曾有修复记录，改动采样/模糊逻辑需谨慎）。

## 陷阱

- `OPTIMIZATION_PLAN.md`（未跟踪文件）是历史审查文档，其 11 项问题**已在 commit 78b35b7 全部修复**（含 INTERNET 权限、事务、外键、SecureStorage 等），不要照它再改一遍；该文档曾被刻意移出仓库（ac19141），不要重新提交。
- **ViewModel 双份筛选状态**：`PracticeViewModel` / `LibraryViewModel` 有 `_selectedCategoryId`/`_difficulty` 私有 StateFlow（驱动数据加载）与 `uiState` 中的同名字段（驱动 UI 显示），`selectCategory`/`selectDifficulty` 必须**同时更新两处**，漏同步会导致"点击分类后按钮文字不变"（曾因此误判为 Popup 触摸 bug，实测弹窗交互一直正常）。
