package com.queststack.ui.screen.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.data.db.Category
import com.queststack.ui.component.PageScaffold
import com.queststack.ui.screen.practice.PracticeSession
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 主页（仪表盘）：随机练题、分类练习、练题记录三大块 */
@Composable
fun HomeScreen(
    onStartPractice: (PracticeSession) -> Unit,
    onGoLibrary: () -> Unit,
    onOpenPracticeLog: () -> Unit,
    categoryMenuExpanded: Boolean,
    onCategoryMenuExpandedChange: (Boolean) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PageScaffold(title = "题栈") { scrollBehavior ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                RandomPracticeCard(
                    totalCount = uiState.totalCount,
                    onStart = { onStartPractice(PracticeSession()) },
                    onGoLibrary = onGoLibrary,
                )
            }
            item { SectionTitle("按分类练习") }
            item {
                CategoryPracticeCard(
                    categories = uiState.categories,
                    counts = uiState.counts,
                    expanded = categoryMenuExpanded,
                    onExpandedChange = onCategoryMenuExpandedChange,
                    onSelect = { onStartPractice(PracticeSession(categoryId = it.id)) },
                    onGoLibrary = onGoLibrary,
                )
            }
            item { SectionTitle("练题记录") }
            item {
                PracticeLogBanner(
                    todayCount = uiState.todayPracticeCount,
                    totalCount = uiState.totalPracticeCount,
                    onClick = onOpenPracticeLog,
                )
            }
        }
    }
}

/** 随机练题大卡：主题色渐变 + 大图标，一键开刷 */
@Composable
private fun RandomPracticeCard(
    totalCount: Int,
    onStart: () -> Unit,
    onGoLibrary: () -> Unit,
) {
    val hasQuestions = totalCount > 0
    val primary = MiuixTheme.colorScheme.primary
    val onPrimary = MiuixTheme.colorScheme.onPrimary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = if (hasQuestions) onStart else onGoLibrary),
        cornerRadius = 24.dp,
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(primary.copy(alpha = 0.92f))
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Icon(
                imageVector = MiuixIcons.GridView,
                contentDescription = null,
                tint = onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.size(34.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "随机练题",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = onPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasQuestions) "从全部 $totalCount 道题中随机抽取"
                else "题库暂无题目，去添加第一道题",
                fontSize = 13.sp,
                color = onPrimary.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MiuixTheme.colorScheme.onBackgroundVariant,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp, bottom = 2.dp),
    )
}

/** 练题记录入口卡：今日/累计练题数，点击进入练题记录页 */
@Composable
private fun PracticeLogBanner(
    todayCount: Int,
    totalCount: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "练题记录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "今日 $todayCount 题 · 累计 $totalCount 题",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * 按分类练习大卡：默认收缩占位，点击弹出分类选择面板（面板自卡片右侧偏下弹出，参考 KernelSU），
 * 再点某分类进入对应闪卡练题。无分类时点击引导去题库添加。
 */
@Composable
private fun CategoryPracticeCard(
    categories: List<Category>,
    counts: Map<Long?, Int>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Category) -> Unit,
    onGoLibrary: () -> Unit,
) {
    var cardHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val hasCategories = categories.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { cardHeight = it.height },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (hasCategories) onExpandedChange(true) else onGoLibrary()
                },
            cornerRadius = 24.dp,
            insideMargin = PaddingValues(0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                Icon(
                    imageVector = MiuixIcons.ListView,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "按分类练习",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (hasCategories) "选择分类开始刷题" else "暂无分类，去题库添加题目",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }

        if (expanded) {
            // 面板：在卡片父级 bounds 内右对齐、自右上角向左下 scale 展开
            val scaleAnim = remember { Animatable(0f) }
            val alphaAnim = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                launch {
                    scaleAnim.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 480f))
                }
                launch {
                    alphaAnim.animateTo(1f, tween(durationMillis = 150))
                }
            }
            Popup(
                // TopEnd 使面板右上角与父级（卡片）右上角对齐；
                // y 偏移 cardHeight 后，面板位于卡片正下方，右缘与卡片右缘对齐
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, cardHeight + with(density) { 4.dp.roundToPx() }),
                properties = PopupProperties(focusable = false),
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(min = 200.dp, max = 320.dp)
                        .graphicsLayer {
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                            alpha = alphaAnim.value
                            // 变换原点固定在面板右上角：视觉上从卡片右侧向左下拉伸展开
                            transformOrigin = TransformOrigin(1f, 0f)
                        }
                        .dropShadow(
                            shape = RoundedCornerShape(16.dp),
                            shadow = Shadow(radius = 12.dp, color = Color.Black, alpha = 0.15f),
                        ),
                    cornerRadius = 16.dp,
                    insideMargin = PaddingValues(0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .heightIn(max = 420.dp)
                            .padding(vertical = 4.dp),
                    ) {
                        categories.forEach { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onExpandedChange(false)
                                        onSelect(category)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = category.name,
                                    fontSize = 15.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${counts[category.id] ?: 0} 题",
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Icon(
                                    imageVector = MiuixIcons.ChevronForward,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onBackgroundVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
