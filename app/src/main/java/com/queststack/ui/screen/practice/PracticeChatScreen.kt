package com.queststack.ui.screen.practice

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
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PracticeChatScreen(
    questionId: Long,
    onBack: () -> Unit,
    viewModel: PracticeChatViewModel =
        viewModel(key = "practice_chat_$questionId") { PracticeChatViewModel(questionId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val question = uiState.question
    val revealed = uiState.revealed.coerceIn(0, 1)

    // 气泡流：主问题 +（已揭示时）参考答案
    val messages = remember(question, revealed) {
        buildList {
            if (question != null) {
                add(MessageItem.Interviewer(question.question.title))
                if (revealed >= 1) add(MessageItem.Answer(question.question.answer))
            }
        }
    }

    // 新消息出现时自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SmallTopAppBar(
            title = question?.question?.title ?: "答题",
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
            uiState.loading -> ChatLoadingPlaceholder()
            question == null -> QuestionNotExistPlaceholder()
            else -> {
                ChatMessageList(
                    messages = messages,
                    listState = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                ChatActionBar(
                    canPrev = uiState.history.isNotEmpty(),
                    exhausted = revealed >= 1,
                    onReveal = viewModel::revealAnswer,
                    onPrev = viewModel::prevQuestion,
                    onNext = viewModel::nextQuestion,
                )
            }
        }
    }
}

private sealed interface MessageItem {
    data class Interviewer(val text: String) : MessageItem
    data class Answer(val text: String) : MessageItem
}

@Composable
private fun ChatMessageList(
    messages: List<MessageItem>,
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
                is MessageItem.Interviewer -> InterviewerBubble(text = message.text, round = null)
                is MessageItem.Answer -> AnswerBubble(text = message.text)
            }
        }
    }
}

/** 底部操作区：未看答案时「查看答案」，看完后「上一问 + 下一问」 */
@Composable
private fun ChatActionBar(
    canPrev: Boolean,
    exhausted: Boolean,
    onReveal: () -> Unit,
    onPrev: () -> Unit,
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
                    onClick = onPrev,
                    enabled = canPrev,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "上一问", fontSize = 14.sp)
                }
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "下一问", fontSize = 14.sp)
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

@Composable
private fun ChatLoadingPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "加载中…",
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
        )
    }
}

@Composable
private fun QuestionNotExistPlaceholder() {
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
                text = "题目不存在",
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
    }
}
