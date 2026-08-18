package com.queststack.data

import com.queststack.data.db.Category
import com.queststack.data.db.CategoryDao
import com.queststack.data.db.Question
import com.queststack.data.db.QuestionDao
import kotlinx.coroutines.flow.first

object Seed {

    private data class SeedQuestion(val title: String, val answer: String, val difficulty: Int)
    private data class SeedCategory(val name: String, val questions: List<SeedQuestion>)

    private val seedData = listOf(
        SeedCategory(
            name = "RAG",
            questions = listOf(
                SeedQuestion(
                    "什么是 RAG？为什么需要它？",
                    "RAG（Retrieval-Augmented Generation，检索增强生成）是一种将外部知识检索与大语言模型生成结合的范式：**先从事先构建的知识库中检索与问题相关的片段，再连同问题一起交给 LLM 生成答案**。\n\n它缓解了 LLM 的幻觉、知识过时和无法访问私有知识的问题，且**不需要重新训练模型**。",
                    1,
                ),
                SeedQuestion(
                    "RAG 的典型流程是什么？",
                    "**离线阶段：** 文档切分（chunk）→ 向量化（embedding）→ 写入向量库并建索引。\n\n**在线阶段：** 查询向量化 → 相似度检索 Top-K →（可选）重排序 → 拼接 prompt → LLM 生成 → 输出并附引用来源。\n\n核心是**检索**与**生成**两个阶段。",
                    1,
                ),
                SeedQuestion(
                    "什么是 embedding？如何度量文本相似度？",
                    "Embedding 是把文本映射为高维稠密向量（如 768/1536 维）的表示，**语义相近的文本在向量空间中距离更近**。\n\n相似度度量：\n\n1) 常用**余弦相似度**（向量夹角余弦）；\n2) 也可用点积或欧氏距离。\n\n现代 embedding 模型基于 Transformer 训练，能编码上下文语义。",
                    1,
                ),
                SeedQuestion(
                    "常见的向量数据库有哪些？",
                    "**Milvus、Qdrant、Weaviate、Pinecone（托管服务）、Chroma、FAISS（检索库）。**\n\n它们支持 ANN（近似最近邻）检索（如 HNSW、IVF-PQ 索引），并提供数据管理、过滤、持久化等能力。",
                    1,
                ),
                SeedQuestion(
                    "什么是 chunk 切分？常见策略有哪些？",
                    "把长文档切分为小块（chunk）以便嵌入和检索。\n\n**常见策略：**\n\n1) 按固定长度切分（固定 token 数并带重叠）；\n2) 按段落/标题/语义边界切分；\n3) 递归字符切分、按句子边界切。\n\n同时要考虑语义完整性、块大小与检索粒度的匹配，以及元数据保留（文档名、页码等）。",
                    2,
                ),
                SeedQuestion(
                    "RAG 检索效果差（检索不到相关内容）如何优化？",
                    "**检索端：**\n\n1) 更好的 chunk 切分；\n2) 混合检索（关键词 BM25 + 向量）；\n3) 查询改写/扩充、假设性文档嵌入（HyDE）；\n4) 重排序。\n\n**索引端：** 多路召回、父子分块（大块供生成上下文、小块供精确匹配）。\n\n**生成端：** 调整 prompt，提供\"未检索到相关内容就拒答\"的指令。\n\n最后先评估定位问题（命中率/召回率）。",
                    3,
                ),
                SeedQuestion(
                    "RAG 与微调的区别？如何选择？",
                    "RAG **不修改模型权重**，通过实时注入检索到的外部知识更新信息，适合知识频繁变化、需要可溯源（引用来源）的场景。\n\n微调**把知识或风格写入权重**，适合固定领域、指令遵循或风格定型，但知识更新需重新训练且无法溯源。\n\n实践中常组合使用：**用 RAG 解决知识来源，再用微调对齐输出风格**。",
                    2,
                ),
                SeedQuestion(
                    "什么是重排序（Rerank）？在 RAG 中的作用？",
                    "重排序是对初步检索出的 Top-K 候选做更精细的相关度打分并重新排序，通常用**交叉编码器（cross-encoder）**逐对计算查询-文档相关性，比 embedding 双塔检索更准确。\n\n重排序后的 Top-N 再送入 LLM，能**显著提升生成质量**。",
                    2,
                ),
                SeedQuestion(
                    "什么是查询改写（Query Rewriting）？为什么需要？",
                    "用户原始查询常口语化、指代不清或多轮依赖。查询改写由 LLM 将原始查询改写为更利于检索的表述：\n\n1) 补全指代；\n2) 提取关键词；\n3) 分解子问题；\n4) 生成假设性答案（HyDE）。\n\n从而**提升召回质量**，是 RAG 的常见优化手段。",
                    2,
                ),
                SeedQuestion(
                    "RAG 常用的评估指标有哪些？",
                    "**检索质量：**\n\n1) 召回率（检索命中相关文档的比例）；\n2) MRR（首个相关结果排名）；\n3) NDCG（排序质量）。\n\n**生成质量：**\n\n1) 忠实度/幻觉率（答案是否基于检索内容）；\n2) 答案相关性与完整性。\n\n可用 **LLM-as-Judge** 或 **RAGAS** 等框架评测。",
                    3,
                ),
            ),
        ),
        SeedCategory(
            name = "Agent",
            questions = listOf(
                SeedQuestion(
                    "什么是 AI Agent？核心组成部分？",
                    "AI Agent 是基于 LLM 的自主智能体，能**感知环境、基于目标推理决策并执行行动**。\n\n**核心组成：**\n\n1) LLM（大脑）；\n2) 工具（函数调用/API，能力）；\n3) 规划（拆解任务）；\n4) 记忆（短期上下文 + 长期存储）；\n5) 执行与反馈循环。",
                    1,
                ),
                SeedQuestion(
                    "什么是 ReAct 模式？",
                    "ReAct 让 LLM 交替进行**推理（Reasoning）**与**行动（Acting）**：\n\n1) 思考：\"我需要查资料\"；\n2) 行动：调用工具/查询；\n3) 观察：把结果作为下一步思考的输入。\n\n形成\"思考→行动→观察\"循环直到得出答案。它结合了思维链的推理能力与工具交互能力，是 Agent 最常用的工作流之一。",
                    1,
                ),
                SeedQuestion(
                    "什么是 Function Calling（工具调用）？",
                    "Function Calling 指让 LLM 从预定义的函数列表中挑选合适的函数并输出结构化调用参数，应用执行后把结果回传给 LLM 继续生成。\n\n**关键步骤：**\n\n1) 定义函数 schema（名称/描述/参数 JSON Schema）；\n2) LLM 输出 tool call；\n3) 执行函数；\n4) 结果注入上下文。",
                    1,
                ),
                SeedQuestion(
                    "Agent 如何进行任务规划？",
                    "**常见方式：**\n\n1) Plan-and-Execute：先一次性生成整体步骤计划再逐步执行；\n2) 逐步规划：每步根据当前状态决定下一步；\n3) 树/图规划：探索多条路径选择最优；\n4) 由规划模型生成 DAG 依赖图。\n\n规划结果常持久化以便**回溯与修正**。",
                    2,
                ),
                SeedQuestion(
                    "什么是多 Agent 协作？",
                    "多个各有专长/角色的 Agent（如规划者、写码者、审查者）通过消息传递分工协作完成任务，类似团队。\n\n**常见模式：**\n\n1) 主管-下属（supervisor）；\n2) 评审/辩论；\n3) 流水线衔接；\n4) 并行众包。\n\n难点在于角色设计、消息协议与状态共享，**过度协作会引入成本与错误传播**。",
                    2,
                ),
                SeedQuestion(
                    "Agent 的记忆如何实现？",
                    "**分短期与长期：**\n\n1) 短期记忆：对话上下文窗口，可通过摘要压缩保留关键信息；\n2) 长期记忆：外部存储（向量库、键值库、数据库），记录用户偏好、历史决策与事实，需要时检索注入。\n\n记忆系统还涉及**写入策略**（哪些值得存）、**检索策略**与**遗忘/更新机制**。",
                    2,
                ),
                SeedQuestion(
                    "如何保证 Agent 的安全性？",
                    "1) 工具白名单与权限最小化（沙箱执行、默认只读）；\n2) 输出过滤与敏感信息脱敏；\n3) 高风险操作要求人工确认；\n4) 限制循环步数与资源消耗；\n5) 防止提示注入（对检索/外部内容做隔离与标注）；\n6) 监控审计日志，设置终止条件与回滚机制。",
                    2,
                ),
                SeedQuestion(
                    "Agent 与普通 LLM 对话的区别？",
                    "普通对话是**单轮问答**，模型只做生成；Agent 具备**自主性**：\n\n1) 可调用外部工具获取信息；\n2) 执行动作；\n3) 依据反馈循环调整；\n4) 完成多步骤闭环任务。\n\nAgent 还引入记忆、规划与错误恢复，从\"会说话\"升级为\"会做事\"。",
                    1,
                ),
                SeedQuestion(
                    "什么是 Reflexion（自我反思）？",
                    "Reflexion 让 Agent 在失败后对自己的行为做**语言化反思**（总结哪里错了、下次怎么做），把反思写入记忆供后续尝试参考，从而在任务中不断自我修正。\n\n与 ReAct 结合形成\"执行→反馈→反思→再执行\"的循环，能**显著提升复杂任务成功率**。",
                    2,
                ),
                SeedQuestion(
                    "如何用 LangGraph 构建 Agent？",
                    "LangGraph 把 Agent 建模为**图**（节点=LLM/工具/条件边），支持循环与状态管理，比纯链式更适合有条件的多步 Agent。\n\n**构建流程：**\n\n1) 定义状态 schema；\n2) 添加节点（agent 决策、工具执行）；\n3) 连接边与条件边（是否继续/结束）；\n4) 用 checkpoint 持久化以支持断点续跑与人机交互。",
                    3,
                ),
            ),
        ),
        SeedCategory(
            name = "LLM",
            questions = listOf(
                SeedQuestion(
                    "Transformer 的基本结构是什么？",
                    "Transformer 由多层 encoder/decoder 堆叠而成，每层包含**多头自注意力（Multi-Head Self-Attention）**与**前馈网络（FFN）**，并带残差连接和层归一化。\n\n核心创新是注意力机制，摆脱了 RNN 的序列依赖，可并行训练并建模长距离依赖。现代 LLM（GPT、LLaMA 等）基本采用 decoder-only 架构。",
                    2,
                ),
                SeedQuestion(
                    "什么是注意力机制？Self-Attention 如何计算？",
                    "注意力机制让每个 token **依据与序列中其他 token 的相关性加权聚合信息**。\n\n**Self-Attention 计算：**\n\n1) 输入经 Q/K/V 三个线性变换得到 query、key、value；\n2) 打分 = Q·Kᵀ/√d（缩放点积）；\n3) 经 softmax 归一化为权重；\n4) 对 V 加权求和。\n\n多头注意力并行多组 QKV，捕捉不同子空间的关系。",
                    2,
                ),
                SeedQuestion(
                    "什么是位置编码？为什么需要它？",
                    "注意力机制本身对输入顺序不敏感（置换等变），无法感知 token 的先后关系，因此需要位置编码**为每个位置注入顺序信息**。\n\n原始 Transformer 用正弦/余弦绝对位置编码，现代模型常用**旋转位置编码（RoPE）**、**ALiBi** 等相对位置编码，支持更好的长上下文外推。",
                    2,
                ),
                SeedQuestion(
                    "什么是 Prompt Engineering？常用技巧有哪些？",
                    "Prompt Engineering 是通过设计输入提示让 LLM 输出更准确、符合预期的工程方法。\n\n**常用技巧：**\n\n1) 明确指令与角色设定；\n2) Few-shot 示例；\n3) 思维链（Chain-of-Thought）；\n4) 指定输出格式（JSON/Markdown）；\n5) 约束条件与边界；\n6) \"不知道就说不知道\"的防幻觉提示；\n7) 分步骤指令与结构化模板。",
                    1,
                ),
                SeedQuestion(
                    "什么是上下文窗口？超长上下文如何处理？",
                    "上下文窗口是模型一次能处理的 **token 上限**。\n\n**超长内容处理方式：**\n\n1) 截断/滑窗；\n2) 对历史做摘要压缩；\n3) 检索式注入（RAG 只放入相关内容）；\n4) KV-cache 优化；\n5) 分块处理。\n\n超出窗口时模型会裁剪最旧内容，导致关键信息丢失。",
                    1,
                ),
                SeedQuestion(
                    "什么是幻觉？如何缓解？",
                    "幻觉指模型生成**看似合理但不忠实于事实或上下文**的输出。\n\n**缓解方法：**\n\n1) 检索增强（RAG）注入事实依据；\n2) 要求输出引用来源；\n3) 降低温度；\n4) 显式\"不知道就拒答\"指令；\n5) 解码约束（如 JSON schema）；\n6) 输出后事实核查/一致性校验；\n7) 针对性微调对齐。",
                    1,
                ),
                SeedQuestion(
                    "什么是 Temperature 与 Top-p？对生成有何影响？",
                    "二者控制采样随机性。\n\n**Temperature** 缩放 softmax 对数概率：越高越随机多样，越低越确定（趋近贪心解码）。\n\n**Top-p（核采样）** 只从累计概率达到 p 的最小 token 集合中采样，避免低概率 token。\n\n低温度 + 低 Top-p 适合确定性任务（代码、JSON），高值适合创意生成。",
                    2,
                ),
                SeedQuestion(
                    "什么是微调？LoRA 是什么？",
                    "微调（Fine-tuning）是在预训练模型基础上用下游数据继续训练，使模型适配特定任务、领域或风格。\n\n**LoRA（低秩适配）** 冻结原权重，只训练注入的低秩分解矩阵（ΔW = A·B）：\n\n1) 参数量通常不到 1%；\n2) 训练快、显存省；\n3) 推理时可将低秩矩阵合并进权重，零额外延迟。\n\n是主流的 PEFT 方法。",
                    2,
                ),
                SeedQuestion(
                    "什么是 RLHF？",
                    "RLHF（基于人类反馈的强化学习）通过人类偏好数据训练奖励模型，再用强化学习（如 PPO）优化策略，使模型输出**更符合人类价值观与偏好**。\n\n**流程：**\n\n1) SFT 监督微调；\n2) 收集对比数据训练奖励模型；\n3) RL 优化。\n\nDPO 等是无需显式奖励模型的替代方案。",
                    3,
                ),
                SeedQuestion(
                    "什么是多模态 LLM？",
                    "多模态 LLM 能**同时处理文本、图像、音频、视频**等输入。\n\n**实现思路：** 将各模态编码器（如 ViT、Whisper）的输出经投影层对齐到 LLM 的嵌入空间，再统一生成文本或跨模态内容。\n\n代表模型有 GPT-4V、Gemini、Qwen-VL。挑战在于模态对齐、训练数据与评测。",
                    2,
                ),
            ),
        ),
        SeedCategory(
            name = "后端",
            questions = listOf(
                SeedQuestion(
                    "什么是 RESTful API？",
                    "REST 是一种基于**资源与 HTTP 语义**的架构风格。\n\n1) 资源用 URL 标识；\n2) 用标准 HTTP 方法表达操作：GET 查询、POST 创建、PUT/PATCH 更新、DELETE 删除；\n3) 无状态、可缓存、分层。\n\n**设计要点：** 资源命名用名词复数、状态码语义化（200/201/400/404/5xx）、接口版本化。",
                    1,
                ),
                SeedQuestion(
                    "什么是幂等性？如何实现？",
                    "幂等指**同一操作重复执行多次的结果与执行一次相同**，副作用不累积。\n\n**实现方式：**\n\n1) GET/PUT/DELETE 天然幂等；\n2) POST 可引入幂等键（Idempotency-Key）或唯一索引约束去重；\n3) 写操作前先查状态；\n4) 用数据库唯一约束/乐观锁；\n5) 在业务层做去重判断。",
                    2,
                ),
                SeedQuestion(
                    "数据库索引是什么？为什么能加速查询？",
                    "索引是为加速查询建立的额外数据结构（如 B+ 树、Hash），按索引字段组织数据，使查找从**全表扫描 O(n)** 降到**对数或常数级**。\n\n**代价：** 占用存储，增删改需维护索引而变慢。\n\n**使用注意：** 最左前缀原则、覆盖索引、避免索引失效（隐式类型转换、对列用函数等）。",
                    1,
                ),
                SeedQuestion(
                    "什么是事务？ACID 是什么？",
                    "事务是一组**要么全部成功要么全部回滚**的数据库操作序列。\n\n**ACID：**\n\n1) 原子性（Atomicity）：全成或全败；\n2) 一致性（Consistency）：约束不被破坏；\n3) 隔离性（Isolation）：并发事务互不干扰；\n4) 持久性（Durability）：提交后不丢失。\n\n隔离级别从低到高：读未提交、读已提交、可重复读、串行化。",
                    1,
                ),
                SeedQuestion(
                    "什么是缓存穿透、击穿、雪崩？如何解决？",
                    "**穿透：** 查询不存在的 key 每次都打到数据库。解决：布隆过滤器、缓存空值。\n\n**击穿：** 热点 key 过期瞬间大量请求打库。解决：互斥锁、热点数据逻辑过期。\n\n**雪崩：** 大量 key 同时失效或缓存整体宕机。解决：过期时间加随机抖动、多级缓存、限流降级、缓存集群高可用。",
                    2,
                ),
                SeedQuestion(
                    "什么是消息队列？应用场景有哪些？",
                    "消息队列是**异步解耦**的中间件（Kafka、RabbitMQ、RocketMQ）。\n\n**应用场景：**\n\n1) 异步削峰（秒杀、通知）；\n2) 服务解耦；\n3) 最终一致性（订单-库存-支付）；\n4) 流处理；\n5) 日志收集。\n\n**要点：** 消息不丢（ack/持久化）、不重（幂等消费）、不乱序、延迟可控。",
                    2,
                ),
                SeedQuestion(
                    "什么是水平扩展与垂直扩展？",
                    "**垂直扩展：** 升级单机硬件（CPU/内存/磁盘），有物理上限且成本高。\n\n**水平扩展：** 增加机器节点分担负载，理论无上限。要求：\n\n1) 应用无状态或状态外置（会话放 Redis、文件放对象存储）；\n2) 数据分片/读写分离；\n3) 引入负载均衡与服务发现。",
                    1,
                ),
                SeedQuestion(
                    "什么是 API 网关？作用是什么？",
                    "API 网关是客户端与后端之间的**统一入口**，负责：\n\n1) 路由转发；\n2) 认证鉴权（OAuth/JWT）；\n3) 限流熔断；\n4) 灰度发布；\n5) 协议转换；\n6) 日志监控；\n7) 服务聚合。\n\n它收敛了客户端到后端的复杂度，是微服务架构的关键组件（Kong、APISIX、Spring Cloud Gateway）。",
                    2,
                ),
                SeedQuestion(
                    "如何设计高可用系统？",
                    "核心思想是**消除单点、快速故障转移**。\n\n1) 冗余与多副本（主备/多活）；\n2) 负载均衡、健康检查与自动摘除；\n3) 超时重试与熔断降级；\n4) 限流防雪崩；\n5) 异步解耦；\n6) 数据多副本与备份恢复；\n7) 可观测性（监控、日志、链路追踪）与故障演练；\n8) 容量规划。",
                    3,
                ),
                SeedQuestion(
                    "什么是限流？常见算法有哪些？",
                    "限流是**控制系统单位时间内的请求处理速率**，防止过载。\n\n**常见算法：**\n\n1) 固定窗口计数：简单但有临界突刺；\n2) 滑动窗口：平滑；\n3) 令牌桶：允许一定突发，应用最广；\n4) 漏桶：恒定速率流出。\n\n实现可基于 Redis Lua 脚本、本地计数器或网关层。",
                    2,
                ),
            ),
        ),
        SeedCategory(
            name = "机器学习",
            questions = listOf(
                SeedQuestion(
                    "什么是过拟合？如何防止？",
                    "过拟合指模型在训练集上表现很好但在未见数据上很差，即**学到了噪声而非泛化规律**。\n\n**防止方法：**\n\n1) 增加数据量/数据增强；\n2) 正则化（L1/L2/Dropout）；\n3) 早停；\n4) 简化模型；\n5) 交叉验证；\n6) 集成方法。\n\n识别信号：训练损失远低于验证损失。",
                    1,
                ),
                SeedQuestion(
                    "什么是交叉验证？",
                    "交叉验证把数据分成多份轮流做验证集，以**更稳健地评估模型**。\n\nK 折交叉验证：数据分成 K 份，每次取 1 份验证、其余 K-1 份训练，循环 K 次取平均指标。留一法（LOO）是 K=n 的特例。\n\n它降低了对单次随机划分的依赖，用于调参与模型选择。",
                    1,
                ),
                SeedQuestion(
                    "什么是正则化？L1 与 L2 的区别？",
                    "正则化在损失函数中加入**模型复杂度惩罚**以抑制过拟合。\n\n**L1（Lasso）：** 惩罚权重绝对值之和，产生稀疏解（部分权重被推为 0，可做特征选择）。\n\n**L2（Ridge）：** 惩罚权重平方和，权重整体变小但不为 0，数值更稳定。\n\nElastic Net 是两者的组合。",
                    2,
                ),
                SeedQuestion(
                    "什么是梯度下降？批量/随机/小批量的区别？",
                    "梯度下降**沿损失函数梯度反方向迭代更新参数**以最小化损失。\n\n1) **批量梯度下降（BGD）：** 每步用全部样本计算梯度，准确但慢、耗内存；\n2) **随机梯度下降（SGD）：** 每步用一个样本，快、能逃离局部最优但噪声大；\n3) **小批量（Mini-batch）：** 每步用一小批样本，兼顾效率与稳定性，是主流。\n\n可配合动量、Adam 等优化器。",
                    2,
                ),
                SeedQuestion(
                    "什么是精确率、召回率、F1？",
                    "针对二分类的正类而言。\n\n1) **精确率 Precision = TP/(TP+FP)：** 预测为正里实际为正的比例（查得准）；\n2) **召回率 Recall = TP/(TP+FN)：** 实际为正里被预测出的比例（查得全）。\n\n二者常此消彼长，**F1** 是两者的调和平均（2·P·R/(P+R)），用于平衡；多分类可做宏/微平均。",
                    1,
                ),
                SeedQuestion(
                    "什么是混淆矩阵？",
                    "混淆矩阵是分类结果与实际类别的**交叉计数表**。\n\n二分类中：行=实际、列=预测，含 TP、FP、FN、TN 四格。\n\n由它可导出精确率、召回率、F1、准确率、特异度等指标。多分类为 N×N 矩阵，便于发现哪些类别容易混淆。",
                    1,
                ),
                SeedQuestion(
                    "什么是决策树与随机森林？",
                    "**决策树：** 按特征划分递归生成树形规则（CART 用基尼指数/信息增益选划分点），可解释性强、无需特征缩放，但单棵树易过拟合。\n\n**随机森林：** Bagging 集成，并行训练多棵在\"样本+特征双重随机采样\"下的决策树，投票/平均输出，**降低方差、抗过拟合**。",
                    2,
                ),
                SeedQuestion(
                    "什么是 K-means？",
                    "K-means 是**无监督聚类算法**：\n\n1) 随机初始化 K 个质心；\n2) 把样本分到最近质心；\n3) 更新质心为类内均值；\n4) 迭代至收敛。\n\n需要预设 K、对初始质心敏感（常用 K-means++ 初始化），适合球形簇和数值特征，使用前需标准化；可用肘部法则/轮廓系数选择 K。",
                    2,
                ),
                SeedQuestion(
                    "什么是 PCA？",
                    "PCA（主成分分析）是**无监督降维方法**：通过对协方差矩阵做特征分解，找到数据方差最大的正交方向（主成分），把数据投影到前 K 个主成分上。\n\n**作用：** 降维可视化、去相关、压缩特征、缓解过拟合。\n\n**代价：** 损失部分信息、可解释性下降，使用前需标准化。",
                    2,
                ),
                SeedQuestion(
                    "什么是偏差-方差权衡？",
                    "泛化误差由**偏差（Bias）、方差（Variance）**与不可约噪声构成。\n\n1) 偏差高：模型过于简单（欠拟合）；\n2) 方差高：对训练数据过于敏感（过拟合）。\n\n模型复杂度上升时偏差下降而方差上升，存在最优平衡点。调节手段：正则化降方差，更强模型/更多特征降偏差。",
                    3,
                ),
            ),
        ),
    )

    suspend fun seedCategories(categoryDao: CategoryDao, questionDao: QuestionDao) {
        val isEmpty = categoryDao.observeAll().first().isEmpty()
        if (isEmpty) {
            seedData.forEachIndexed { index, category ->
                val categoryId = categoryDao.insert(Category(name = category.name, sortOrder = index))
                val now = System.currentTimeMillis()
                category.questions.forEach { q ->
                    questionDao.insert(
                        Question(
                            title = q.title,
                            answer = q.answer,
                            categoryId = categoryId,
                            difficulty = q.difficulty,
                            createdAt = now,
                            updatedAt = now,
                        )
                    )
                }
            }
        }
    }
}