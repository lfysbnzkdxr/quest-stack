package com.queststack.ui.screen.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.data.db.Question
import com.queststack.ui.component.AnswerText
import com.queststack.ui.component.CategoryFilterBar
import com.queststack.ui.component.animation.NavSlideEasing
import com.queststack.ui.component.liquid.lens
import com.queststack.ui.component.liquid.vibrancy
import com.queststack.ui.screen.library.difficultyColor
import com.queststack.ui.screen.library.difficultyLabel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixIndication

/** 闪卡练题页：全屏覆盖，看题 → 思考 → 显示答案 → 下一题 */
@Composable
fun PracticeSessionScreen(
    session: PracticeSession,
    onBack: () -> Unit,
) {
    val viewModel: PracticeSessionViewModel = viewModel(
        key = "practice_session_${session.categoryId}_${session.difficulty}_${session.startQuestionId}",
        initializer = { PracticeSessionViewModel(session) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 同 key 会复用同一个 ViewModel（挂在 Activity 的 ViewModelStore），进入时必须重置会话状态
    LaunchedEffect(session) {
        viewModel.start(session)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 全屏 overlay 必须铺底，否则下面的 pager 内容透出来
            .background(MiuixTheme.colorScheme.surface),
    ) {
        SmallTopAppBar(
            title = "练题",
            color = MiuixTheme.colorScheme.surface,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.ChevronBackward,
                        contentDescription = "返回",
                        tint = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
        )
        // 闪卡内筛选：分类 + 难度，变更后自动换题
        CategoryFilterBar(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            difficulty = uiState.difficulty,
            onSelectCategory = viewModel::selectCategory,
            onSelectDifficulty = viewModel::selectDifficulty,
        )
        when {
            uiState.empty -> EmptyPlaceholder(onBack = onBack)
            uiState.current != null -> SlideFlashCards(
                question = uiState.current,
                revealed = uiState.revealed,
                canPrevious = uiState.canPrevious,
                categoryNameOf = { id ->
                    uiState.categories.firstOrNull { it.id == id }?.name ?: "未分类"
                },
                onReveal = viewModel::toggleReveal,
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
            )
            else -> LoadingPlaceholder()
        }
    }
}

/**
 * 闪卡切题动画容器：左右滑动切题（对齐主页三 Tab 横滑效果），两帧相邻同屏滑动。
 * - 下一题 / 筛选切换：旧题左移出屏、新题从右滑入；
 * - 上一题：反向——旧题右移出屏、新题从左滑入；
 * - 按钮触发切题时旧帧快照"答案展开"状态，随帧滑出。
 */
@Composable
private fun SlideFlashCards(
    question: Question?,
    revealed: Boolean,
    canPrevious: Boolean,
    categoryNameOf: (Long?) -> String,
    onReveal: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    // 初始化为完成态（1）：首帧新帧直接归位；切题时 snapTo(0) → animateTo(1) 驱动滑动
    val slideProgress = remember { Animatable(1f) }
    // 直接用首帧 question 初始化，避免首帧渲染空窗（when 分支保证首次组合时 question 非空）
    var currentFrame by remember { mutableStateOf(question) }
    // 当前帧正在展示的答案态：点"显示答案"时展开；切题动画触发瞬间同步为最新值
    var currentRevealed by remember { mutableStateOf(false) }
    var outgoing by remember { mutableStateOf<Question?>(null) }
    var outgoingRevealed by remember { mutableStateOf(false) }
    // 本次切题方向标记：上一题（反向）为 true
    var pendingBackward by remember { mutableStateOf(false) }
    // 当前动画方向乘子：正向（新题从右滑入）为 -1，反向（上一题）为 +1
    var slideDirection by remember { mutableStateOf(-1f) }
    // 首次加载的题已由初始化直接展示，无需入场动画（question 首帧非空时恒为 false）
    var firstFrame by remember { mutableStateOf(question == null) }

    // 切题驱动：current 变化时双帧同存，progress 0 → 1 完成滑动
    LaunchedEffect(question?.id) {
        val newQ = question ?: return@LaunchedEffect
        if (newQ.id != currentFrame?.id) {
            // 旧帧快照当前帧最后展示的状态（答案展开时切题则保持展开滑出）
            outgoing = currentFrame
            outgoingRevealed = currentRevealed
            // 当前帧同步为最新答案态，动画期间新帧直接使用，无滞后闪帧
            currentRevealed = revealed
            slideDirection = if (pendingBackward) 1f else -1f
            pendingBackward = false
            currentFrame = newQ
            if (firstFrame) {
                firstFrame = false
                // 首帧无入场动画：直接把进度置为完成态（新帧归位），否则新帧停在屏幕外
                slideProgress.snapTo(1f)
            } else {
                slideProgress.snapTo(0f)
                slideProgress.animateTo(1f, tween(500, easing = NavSlideEasing))
                outgoing = null
            }
        }
    }
    // 无切题时答案展开态跟随（点"显示答案"时展开）
    LaunchedEffect(revealed) {
        currentRevealed = revealed
    }

    // 上一题触发反向动画标记
    val handlePrevious = {
        pendingBackward = true
        onPrevious()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 题目内容采集层：供底部液态玻璃按钮模糊采样（与玻璃层互为兄弟，避免循环采样）
        val surface = MiuixTheme.colorScheme.surface
        val backdrop = rememberLayerBackdrop {
            drawRect(surface)
            drawContent()
        }
        // 题目内容滑动层：延伸到屏幕底部（沉浸），两帧相邻同屏滑动
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            outgoing?.let { old ->
                key(old.id) {
                    FlashCardContent(
                        question = old,
                        categoryName = categoryNameOf(old.categoryId),
                        revealed = outgoingRevealed,
                        modifier = Modifier.graphicsLayer {
                            // progress 0 → 1：旧帧从左（正向下钻）或右（反向）完全滑出屏幕
                            translationX = slideDirection * slideProgress.value * size.width
                        },
                    )
                }
            }
            currentFrame?.let { current ->
                key(current.id) {
                    FlashCardContent(
                        question = current,
                        categoryName = categoryNameOf(current.categoryId),
                        revealed = currentRevealed,
                        modifier = Modifier.graphicsLayer {
                            // progress 0 → 1：新帧从右（正向下钻）或左（反向）滑入归位
                            translationX = -slideDirection * (1f - slideProgress.value) * size.width
                        },
                    )
                }
            }
        }
        // 三个液态玻璃按钮悬浮于内容之上（不占内容区，参考主页悬浮底栏），底部沉浸
        FlashCardActionBar(
            backdrop = backdrop,
            revealed = currentRevealed,
            canPrevious = canPrevious,
            onReveal = onReveal,
            onNext = onNext,
            onPrevious = handlePrevious,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** 闪卡内容区：题目 + 答案（可滚动），随切题动画横向滑动 */
@Composable
private fun FlashCardContent(
    question: Question,
    categoryName: String,
    revealed: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        // 题目 + 答案区（可滚动，避免长题/长答案溢出）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                insideMargin = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = categoryName,
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(MiuixTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                        Text(
                            text = difficultyLabel(question.difficulty),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = difficultyColor(question.difficulty),
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(difficultyColor(question.difficulty).copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = question.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onBackground,
                        lineHeight = 31.sp,
                    )
                }
            }
            AnimatedVisibility(
                visible = revealed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    cornerRadius = 20.dp,
                    insideMargin = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    Column {
                        Text(
                            text = "参考答案",
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AnswerText(
                            text = question.answer.ifBlank { "（答案为空）" },
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                        )
                    }
                }
            }
            // 底部留白：内容沉浸到底，为悬浮玻璃按钮与投影留出空间
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

/** 操作栏玻璃高光：双峰白光描边（上方主光 + 下方次光），模拟玻璃边缘反光（与悬浮底栏一致） */
private val actionBarSpecular: Highlight = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.0.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

/**
 * 底部液态玻璃操作栏（悬浮 overlay，不占内容区）：左/右为上一题、下一题箭头，中间为"显示答案/收起答案"切换。
 * 每个按钮各自采样题目内容层做玻璃折射（vibrancy + blur + lens + 双峰高光），
 * 不支持 AGSL 时降级为半透明胶囊；底部沉浸，切换题目时按钮文字不闪动。
 */
@Composable
private fun FlashCardActionBar(
    backdrop: LayerBackdrop,
    revealed: Boolean,
    canPrevious: Boolean,
    onReveal: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GlassActionButton(
            backdrop = backdrop,
            onClick = onPrevious,
            enabled = canPrevious,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = MiuixIcons.ChevronBackward,
                contentDescription = "上一题",
                modifier = Modifier.size(22.dp),
            )
        }
        GlassActionButton(
            backdrop = backdrop,
            onClick = onReveal,
            emphasized = true,
            modifier = Modifier.weight(1f),
        ) {
            Text(text = if (revealed) "收起答案" else "显示答案", fontSize = 15.sp)
        }
        GlassActionButton(
            backdrop = backdrop,
            onClick = onNext,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = "下一题",
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * 单个液态玻璃操作按钮：胶囊形玻璃背景（采样 backdrop）+ 按压变暗 + 禁用置灰。
 * @param emphasized 主操作强调：内容使用 primary 色。
 */
@Composable
private fun GlassActionButton(
    backdrop: LayerBackdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isInDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val shape = RoundedCornerShape(percent = 50)
    val isBlurEnabled = isRuntimeShaderSupported()
    val containerColor = if (isBlurEnabled) {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val contentColor = when {
        !enabled -> MiuixTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        emphasized -> MiuixTheme.colorScheme.primary
        else -> MiuixTheme.colorScheme.onBackground
    }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(48.dp)
            .then(
                if (isBlurEnabled) {
                    Modifier
                        .dropShadow(
                            shape = shape,
                            shadow = Shadow(
                                radius = 10.dp,
                                color = Color.Black,
                                alpha = if (isInDark) 0.2f else 0.1f,
                            ),
                        )
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            effects = {
                                vibrancy()
                                blur(4.dp.toPx(), 4.dp.toPx())
                                lens(
                                    refractionHeight = 10.dp.toPx(),
                                    refractionAmount = 10.dp.toPx(),
                                )
                            },
                            highlight = { actionBarSpecular.copy(alpha = 0.75f) },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                } else {
                    Modifier.background(containerColor, shape)
                }
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = MiuixIndication(),
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "加载中…",
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
        )
    }
}

@Composable
private fun EmptyPlaceholder(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = MiuixIcons.Help,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "该范围内暂无题目",
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "换个筛选条件，或先去「题库」添加题目",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) {
                Text(text = "返回", fontSize = 14.sp)
            }
        }
    }
}
