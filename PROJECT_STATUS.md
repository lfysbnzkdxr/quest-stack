# 项目情况文档（QuestStack / 题栈）

> 记录时间：2026-08-16 · 分支：`feat/atomic-practice`（基于 `main`）

## 1. 项目概述

Android 面试题练习应用（题栈）。单 `:app` 模块，包名 `com.queststack`。UI 使用 Jetpack Compose + Miuix（MIUI 风格组件库），毛玻璃风格（顶栏/底栏/卡片均带模糊）。无 README、无测试、无 CI。

版本：`versionCode = 1`，`versionName = 0.1.0`；`minSdk = 33`，`targetSdk = compileSdk = 37`。

## 2. 技术栈与版本

| 组件 | 版本 | 说明 |
|---|---|---|
| AGP | 9.3.0 | 内置 Kotlin 支持，**不可添加 `org.jetbrains.kotlin.android` 插件** |
| Kotlin | 2.4.10 | 根 buildscript 显式 pin `kotlin-gradle-plugin` classpath，勿删 |
| Compose BOM | 2026.06.00 | |
| Miuix | 0.9.3 | `top.yukonga.miuix.kmp`，含 `miuix-ui` / `miuix-icons` / `miuix-blur` |
| Room | 2.8.4 | KSP 编译器 |
| KSP | 2.3.10 | |
| kotlinx.serialization | 1.11.0 | 备份/导入导出 JSON |
| OkHttp | 5.4.0 | AI 接口（OpenAI 兼容） |
| DataStore | 1.2.1 | Preferences，设置 + SecureStorage 加密后的密钥 |
| coroutines | 1.11.0 | |

依赖注入：**无 Hilt**，`DataContainer` 全局单例在 `QuestStackApp.onCreate()` 初始化，ViewModel 通过构造器默认参数取依赖。

## 3. 目录结构（app/src/main/java/com/queststack）

```
QuestStackApp.kt        Application，DataContainer 初始化
MainActivity.kt         edge-to-edge 全透明系统栏（KernelSU 同款），内容延伸状态栏/导航栏
ai/AiClient.kt          OpenAI 兼容 Chat Completions，异常不捕获（由 ViewModel 处理）
data/
  DataContainer.kt      全局单例，装配 DB/仓库/Settings/DataStore
  SecureStorage.kt      AES-GCM 加密密钥后才写入 DataStore
  Seed.kt               种子数据（已改用备份导出格式 seed-questions.json）
  db/                  Room：AppDatabase(v5)、Question、Category、PracticeLogEntity(+Dao)
  repository/          Question / Category / PracticeLog / Settings 各 Repository(Impl)
  backup/              BackupData(JSON v1) / BackupRepository / WebDavClient
ui/
  MainScreen.kt        主界面：HorizontalPager 三 Tab + 悬浮毛玻璃底栏 + 全屏 overlay
  theme/AppTheme.kt    主题（跟随系统深色模式）
  component/           GlassTopAppBar / GlassNavigationBar / PageScaffold / CategoryFilterBar
  screen/
    home/              主页仪表盘（随机练题、分类练习、练题记录入口）
    library/           题库（分类/难度筛选、题目列表、添加入口）
    practice/          练题浮层（PracticeSession + PracticeSessionScreen/ViewModel）
    practiceLog/       练题记录（热力图 + 每日列表）
    settings/          设置（AI 配置、WebDAV 备份、主题等）
    add/               添加题目
```

## 4. 数据层

- **Room 数据库** `AppDatabase`：当前 `version = 5`，`fallbackToDestructiveMigration()` 兜底，`exportSchema = true`（快照已入库 `app/schemas/`，含 2~5 各版本）。
- 实体：`Question`（题目）、`Category`（分类）、`PracticeLogEntity`（练题记录，v5 新增，替代 v4 的 Round 系列——`RoundDao`/`QuestionWithRounds` 已删除）。
- **v0.x 阶段约定**：改实体只递增 version，不写 Migration 对象。
- 多表写操作必须包 `database.withTransaction { }`。
- 备份：kotlinx.serialization JSON，导入 `ignoreUnknownKeys = true`，校验版本号（backup v1），高版本提示"请升级应用"。
- 种子数据 `seed-questions.json` 现为备份导出格式（含 version/exportedAt/categories/questions，13 道题、5 个分类：Android/Kotlin/算法/系统/网络）。

## 5. 功能现状

| 功能 | 状态 |
|---|---|
| 主页仪表盘（随机练题、分类练习、练题记录入口） | ✅ 已实现（本分支新增） |
| 题库列表 + 分类/难度筛选（共享 CategoryFilterBar） | ✅ 已实现 |
| 练题浮层刷题（PracticeSessionScreen，全屏 overlay） | ✅ 已实现（本分支重构，替代原 PracticeScreen 聊天式练题） |
| 模拟面试模式（AI 对话） | ✅ 本分支已提交 |
| 练题记录（热力图、每日列表） | ✅ 已实现（未提交，v5 数据层） |
| 添加题目（支持 AI 生成） | ✅ 已实现 |
| 设置（AI 模型/Key/超时、WebDAV 备份、主题切换） | ✅ 已实现 |
| AI 答题分析（OpenAI 兼容接口） | ✅ 已实现 |

## 6. UI 架构特点（近期重点）

- **edge-to-edge**：`MainActivity.enableEdgeToEdge()` + 透明系统栏，状态栏/导航栏均不占用内容。
- **顶栏**：`GlassTopAppBar` 毛玻璃背景延伸进状态栏（`windowInsetsTopHeight`），无状态栏色差。
- **底栏**：`GlassNavigationBar` 为 **overlay 真悬浮**（`Box(fillMaxSize, BottomCenter)` 叠加在 `Scaffold` 之上，不占内容区域；参考 KernelSU `FloatingBottomBar`）。胶囊毛玻璃采样全局 backdrop。
- **页面切换**：`HorizontalPager` 三 Tab（主页/题库/设置），预加载相邻页（`beyondViewportPageCount`，首帧后开启）+ KernelSU 同款弹簧动画 `springAnimateToPage`（stiffness 322.2 / damping≈0.9）+ `overscrollEffect = null`。
- **分类面板**：Popup 锚定卡片右下 + 缩放动画 + 同窗口全屏变暗遮罩（`categoryMenuExpanded` 提升到 MainScreen，返回键/点击遮罩关闭）。
- **全屏 overlay**：练题、添加、练题记录均为叠加在 pager 之上的全屏层。

## 7. 当前分支工作状态

分支 `feat/atomic-practice` 已提交 `d51284b`（题目答案原子化、练题改浮层刷题、模拟面试模式）。**当前有大量未提交改动**，分三类：

**新增（未跟踪）**
- 练题记录：`PracticeLogDao` / `PracticeLogEntity` / `PracticeLogRepository(Impl)` / `practiceLog/` 界面
- 主页：`ui/screen/home/`（HomeScreen / HomeViewModel）
- 通用页骨架：`ui/component/PageScaffold.kt`
- 练题浮层：`PracticeSessionScreen.kt` / `PracticeSessionViewModel.kt`
- schema 快照 `4.json` / `5.json`
- `META-INF/`（miuix kotlin_module，疑似构建产物）、`top/`（Miuix 库源码导出，疑似临时排错用）

**删除**
- v4 遗留：`Round.kt` / `RoundDao.kt` / `QuestionWithRounds.kt`
- 旧练题/面试 UI：`ChatBubbles.kt`、`interview/`、`practice/PracticeChatScreen.kt`、`practice/PracticeScreen.kt` 及其 ViewModel

**修改**
- `MainActivity`（edge-to-edge）、`AiClient`、`DataContainer`、`Backup*`（seed 改导出格式）、`AppDatabase`（v4→v5）、`QuestionDao` / `QuestionRepository*`、`MainScreen`（overlay 底栏 + 遮罩 + 预加载）、`CategoryFilterBar`、`GlassNavigationBar`（overlay 化）、`GlassTopAppBar`（状态栏延伸）、`Add*`、`Library*`、`SettingsScreen`、`seed-questions.json`

**近期修复记录**
- 玻璃顶栏循环采样真机崩溃（commit 633726d）
- 分类/难度筛选后按钮文字不变（ViewModel 双份状态漏同步，commit aa12d06）
- 底栏从"占位伪悬浮"改为 overlay 真悬浮；切换加预加载消除卡顿（未提交）

## 8. 构建与验证

- Windows：`.\gradlew.bat :app:assembleDebug`（Gradle 9.6.1 wrapper）
- 仓库无测试源码，验证用 `assembleDebug` / `:app:compileDebugKotlin`，不要跑 test 任务
- 真机验证：`adb install -r` + `uiautomator dump` / `screencap` 像素采样 / `input tap`
- `local.properties`（sdk.dir）已 gitignore，机器相关，不提交
- 注意：AGP 9.3.0 内置 Kotlin，勿加 `org.jetbrains.kotlin.android` 插件；根 buildscript 的 `kotlin-gradle-plugin:2.4.10` classpath 是必须的

## 9. 约定与注意事项

- 注释、UI 文案、commit message 均中文；commit 风格 `type: 中文描述`（`feat:` / `fix:` / `chore:`）
- UI 字符串硬编码在 composable 里，`strings.xml` 只有 app_name
- AI 层方法不捕获异常，ViewModel 处理 `IOException` / `IllegalArgumentException` / `TimeoutCancellationException`；`timeoutSeconds` 从设置一路传入
- API Key 与 WebDAV 密码经 `SecureStorage`（AES-GCM）加密后才落盘
- **ViewModel 双份筛选状态陷阱**：`PracticeViewModel` / `LibraryViewModel` 有私有 StateFlow（驱动数据加载）+ uiState 同名字段（驱动 UI 显示），`selectCategory`/`selectDifficulty` 必须同时更新两处
- `OPTIMIZATION_PLAN.md` 为历史审查文档，11 项问题已在 commit 78b35b7 修复，不要再照改；该文档不入库（ac19141）
- 毛玻璃采样/模糊逻辑改动需谨慎（真机崩溃史，commit 633726d）

## 10. 待办 / 风险

- 未提交改动量大，建议尽快分 commit 提交（练题记录 / 主页 / UI overlay 改造各自独立提交）
- `top/`（Miuix 源码导出）与 `META-INF/` 疑似临时文件，确认后应从工作区清理，勿入库
- 无测试、无 CI，回归靠真机手动验证
