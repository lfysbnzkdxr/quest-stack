package com.queststack.ui.screen.interview

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.queststack.data.db.QuestionWithRounds
import com.queststack.data.db.Round
import com.queststack.ui.component.AnswerBubble
import com.queststack.ui.component.InterviewerBubble
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 面试会话气泡：面试官问题（带轮次）/ 参考答案 */
private sealed interface InterviewMessage {
    data class Interviewer(val text: String, val round: Int) : InterviewMessage
    data class Answer(val text: String, val round: Int) : InterviewMessage
}

@Composable
fun InterviewScreen(
    categoryId: Long?,
    difficulty: Int?,
    onBack: () -> Unit,
    viewModel: InterviewViewModel =
        viewModel(key = "interview_$categoryId/$difficulty") {
            InterviewViewModel(categoryId, difficulty)
        },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        SmallTopAppBar(
            title = "模拟面试",
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
        when {
            uiState.loading -> InterviewLoadingPlaceholder()
            uiState.empty -> InterviewEmptyPlaceholder(onBack = onBack)
            uiState.finished -> InterviewSummary(
                count = uiState.questions.size,
                onBack = onBack,
            )
            else -> uiState.current?.let { question ->
                val revealed = uiState.revealed.coerceIn(0, 1 + question.rounds.size)
                val messages = remember(question, revealed) { buildMessages(question, revealed) }
                // 新消息出现时自动滚动到底部
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.lastIndex)
                    }
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    // 进度指示：第 X 题 / 共 N 题
                    Text(
                        text = "第 ${uiState.currentIndex + 1} 题 / 共 ${uiState.questions.size} 题",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                    InterviewMessageList(
                        messages = messages,
                        listState = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    InterviewActionBar(
                        exhausted = uiState.currentExhausted,
                        isLast = uiState.currentIndex >= uiState.questions.lastIndex,
                        onReveal = viewModel::revealAnswer,
                        onNext = viewModel::nextQuestion,
                    )
                }
            }
        }
    }
}

/** 构建当前题的气泡流：主问题 + 主答案 + 已揭示的追问对 + 当前未揭示问题 */
private fun buildMessages(question: QuestionWithRounds, revealed: Int): List<InterviewMessage> {
    val rounds = question.rounds.sortedBy { it.orderIndex }
    return buildList {
        add(InterviewMessage.Interviewer(question.question.title, 1))
        if (revealed >= 1) {
            add(InterviewMessage.Answer(question.question.answer, 1))
        }
        for (i in 0 until (revealed - 1)) {
            add(InterviewMessage.Interviewer(rounds[i].question, i + 2))
            add(InterviewMessage.Answer(rounds[i].answer, i + 2))
        }
        if (revealed >= 1 && revealed - 1 < rounds.size) {
            add(InterviewMessage.Interviewer(rounds[revealed - 1].question, revealed + 1))
        }
    }
}

@Composable
private fun InterviewMessageList(
    messages: List<InterviewMessage>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(messages.size, key = { it }) { index ->
            when (val message = messages[index]) {
                is InterviewMessage.Interviewer -> InterviewerBubble(text = message.text, round = message.round)
                is InterviewMessage.Answer -> AnswerBubble(text = message.text)
            }
        }
    }
}

/** 底部操作区：当前题未走完时「查看答案」，走完后「下一题 / 完成面试」 */
@Composable
private fun InterviewActionBar(
    exhausted: Boolean,
    isLast: Boolean,
    onReveal: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (exhausted) {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = if (isLast) "完成面试" else "下一题", fontSize = 14.sp)
                }
            } else {
                Button(
                    onClick = onReveal,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "查看答案", fontSize = 14.sp)
                }
            }
        }
        if (exhausted) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "本问完成 ✓",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.6f),
            )
        }
    }
}

/** 整场面试完成小结（不落库，仅展示） */
@Composable
private fun InterviewSummary(count: Int, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = MiuixIcons.Ok,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "面试完成",
                fontSize = 16.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "共完成 $count 题",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .height(44.dp),
            ) {
                Text(text = "返回练题", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun InterviewLoadingPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "正在准备面试题目…",
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
        )
    }
}

@Composable
private fun InterviewEmptyPlaceholder(onBack: () -> Unit) {
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
                text = "当前筛选下暂无题目",
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .height(44.dp),
            ) {
                Text(text = "返回", fontSize = 14.sp)
            }
        }
    }
}
