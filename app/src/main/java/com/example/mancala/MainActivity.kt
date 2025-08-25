package com.example.mancala

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MancalaApp() }
    }
}

@Composable
fun MancalaApp() {
    var state by remember { mutableStateOf(GameState.initial()) }
    var message by remember { mutableStateOf("Player A's turn") }
    var isAnimating by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Mancala (Kalah)", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)

                GameBoard(
                    state = state,
                    isAnimating = isAnimating,
                    onPitClick = { pitIndex ->
                        if (state.isGameOver || isAnimating) return@GameBoard
                        val sideRange = if (state.currentPlayer == Player.A) 0..5 else 7..12
                        if (pitIndex !in sideRange || state.pits[pitIndex].isEmpty()) return@GameBoard

                        scope.launch {
                            isAnimating = true
                            val (animatedPits, lastIndex) = animateSowingOnce(
                                state = state,
                                startPit = pitIndex,
                                dropDelayMs = 350L
                            ) { snapshot ->
                                state = state.copyWith(pits = snapshot)
                            }
                            state = state.afterAnimatedSowing(animatedPits, lastIndex)
                            message = when {
                                state.isGameOver -> state.endMessage()
                                state.extraTurn -> "Extra turn! ${state.currentPlayer.name} again"
                                else -> "${state.currentPlayer.name}'s turn"
                            }
                            isAnimating = false
                        }
                    }
                )

                Text(message)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                Button(onClick = { if (!isAnimating) showResetConfirm = true }) { Text("Reset") }
                OutlinedButton(onClick = { if (!isAnimating) state = state.withHintMove() }) { Text("Hint") }
            }

            if (showResetConfirm) {
                AlertDialog(
                    onDismissRequest = { showResetConfirm = false },
                    confirmButton = {
                        TextButton(onClick = {
                            state = GameState.initial()
                            message = "Player A's turn"
                            showResetConfirm = false
                        }) { Text("Yes") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
                    },
                    title = { Text("Reset Game") },
                    text = { Text("Are you sure you want to reset the game?") }
                )
            }
        }
    }
}

@Composable
fun GameBoard(
    state: GameState,
    isAnimating: Boolean,
    onPitClick: (Int) -> Unit
) {
    val pitSize = 140.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Player B (Right-to-Left)",
            fontWeight = if (state.currentPlayer == Player.B) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StoreBox(stones = state.pits[13], label = "B Store", highlight = state.currentPlayer == Player.B)
            Spacer(Modifier.width(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 12 downTo 7) {
                        PitBox(
                            stones = state.pits[i],
                            size = pitSize,
                            enabled = !isAnimating && state.currentPlayer == Player.B && state.pits[i].isNotEmpty(),
                            onClick = { onPitClick(i) },
                            label = i.toString()
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0..5) {
                        PitBox(
                            stones = state.pits[i],
                            size = pitSize,
                            enabled = !isAnimating && state.currentPlayer == Player.A && state.pits[i].isNotEmpty(),
                            onClick = { onPitClick(i) },
                            label = i.toString()
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))
            StoreBox(stones = state.pits[6], label = "A Store", highlight = state.currentPlayer == Player.A)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Player A (Left-to-Right)",
            fontWeight = if (state.currentPlayer == Player.A) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun PitBox(
    stones: List<Stone>,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    label: String
) {
    val shape = MaterialTheme.shapes.medium
    val baseBorder = MaterialTheme.colorScheme.outline
    val highlightColor = MaterialTheme.colorScheme.primary

    val pulseAlpha by remember(enabled) {
        mutableStateOf(enabled)
    }.let { isOn ->
        if (isOn.value) {
            val t = rememberInfiniteTransition(label = "pitPulse")
            t.animateFloat(
                initialValue = 0.08f,
                targetValue = 0.22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pitPulseAnim"
            )
        } else {
            mutableStateOf(0f)
        }
    }

    val backgroundTint = if (enabled)
        highlightColor.copy(alpha = pulseAlpha)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    Surface(
        modifier = Modifier
            .size(size)
            .padding(4.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = shape,
        tonalElevation = 0.dp,
        border = BorderStroke(2.dp, baseBorder),
        color = backgroundTint
    ) {
        Box(Modifier.fillMaxSize()) {
            if (enabled) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.TopCenter)
                        .background(highlightColor)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stones.joinToString("") { it.symbol },
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "(${stones.size})", // show number of stones
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Pit $label", // pit label
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StoreBox(stones: List<Stone>, label: String, highlight: Boolean) {
    val shape = MaterialTheme.shapes.medium
    val storeBorderColor = if (highlight) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline

    Surface(
        modifier = Modifier
            .width(140.dp)
            .height(320.dp),
        shape = shape,
        tonalElevation = 0.dp,
        border = BorderStroke(2.dp, storeBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 16.sp, textAlign = TextAlign.Center)
            Text(text = stones.joinToString("") { it.symbol }, fontSize = 28.sp)
            Text(text = "(${stones.size})", fontSize = 20.sp)
        }
    }
}

suspend fun animateSowingOnce(
    state: GameState,
    startPit: Int,
    dropDelayMs: Long,
    onTick: (List<MutableList<Stone>>) -> Unit
): Pair<List<MutableList<Stone>>, Int> {
    val pits = state.pits.map { it.toMutableList() }.toMutableList()
    val oppStore = if (state.currentPlayer == Player.A) 13 else 6
    val myStore  = if (state.currentPlayer == Player.A) 6 else 13

    var index = startPit
    var hand = pits[startPit].toMutableList()
    pits[startPit].clear()
    onTick(pits.map { it.toMutableList() })

    while (true) {
        while (hand.isNotEmpty()) {
            do { index = (index + 1) % 14 } while (index == oppStore)
            val stone = hand.removeAt(0)
            pits[index].add(stone)
            onTick(pits.map { it.toMutableList() })
            delay(dropDelayMs)
        }
        if (index == myStore) break
        if (pits[index].size > 1) {
            hand = pits[index].toMutableList()
            pits[index].clear()
            onTick(pits.map { it.toMutableList() })
        } else break
    }
    return pits to index
}
