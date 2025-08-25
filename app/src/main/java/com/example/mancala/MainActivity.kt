// =============================
// File: app/src/main/java/com/example/mancala/MainActivity.kt
// =============================
package com.example.mancala

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.BorderStroke

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
                        if (pitIndex !in sideRange || state.pits[pitIndex] == 0) return@GameBoard

                        scope.launch {
                            isAnimating = true
                            val (animatedPits, lastIndex) = animateSowingOnce(
                                state = state,
                                startPit = pitIndex,
                                dropDelayMs = 350L
                            ) { snapshot ->
                                state = state.copy(pits = snapshot)
                            }
                            val after = state.afterAnimatedSowing(animatedPits, lastIndex)
                            state = after
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
                Button(onClick = {
                    if (!isAnimating) showResetConfirm = true
                }) {
                    Text("Reset")
                }

                OutlinedButton(onClick = {
                    if (!isAnimating) state = state.withHintMove()
                }) {
                    Text("Hint")
                }
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
                        TextButton(onClick = { showResetConfirm = false }) {
                            Text("Cancel")
                        }
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
    val pitSize = 100.dp
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
            StoreBox(count = state.pits[13], label = "B Store", highlight = state.currentPlayer == Player.B)
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 12 downTo 7) {
                        PitBox(index = i, count = state.pits[i], size = pitSize,
                            enabled = !isAnimating && state.currentPlayer == Player.B && state.pits[i] > 0,
                            onClick = { onPitClick(i) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0..5) {
                        PitBox(index = i, count = state.pits[i], size = pitSize,
                            enabled = !isAnimating && state.currentPlayer == Player.A && state.pits[i] > 0,
                            onClick = { onPitClick(i) })
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            StoreBox(count = state.pits[6], label = "A Store", highlight = state.currentPlayer == Player.A)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Player A (Left-to-Right)",
            fontWeight = if (state.currentPlayer == Player.A) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun PitBox(index: Int, count: Int, size: Dp, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(size)
            .padding(4.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (enabled) 2.dp else 0.dp,
        border = if (enabled) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = count.toString(), fontSize = 28.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StoreBox(count: Int, label: String, highlight: Boolean) {
    Surface(
        modifier = Modifier
            .width(120.dp)
            .height(260.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (highlight) 2.dp else 0.dp,
        border = if (highlight) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 14.sp, textAlign = TextAlign.Center)
            Text(count.toString(), fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
    }
}

suspend fun animateSowingOnce(
    state: GameState,
    startPit: Int,
    dropDelayMs: Long,
    onTick: (IntArray) -> Unit
): Pair<IntArray, Int> {
    val pits = state.pits.copyOf()
    var stones = pits[startPit]
    pits[startPit] = 0
    onTick(pits.copyOf())

    val myStore = if (state.currentPlayer == Player.A) 6 else 13
    val oppStore = if (state.currentPlayer == Player.A) 13 else 6

    var index = startPit
    while (stones > 0) {
        index = (index + 1) % 14
        if (index == oppStore) continue
        pits[index]++
        onTick(pits.copyOf())
        delay(dropDelayMs)
        stones--
    }
    return pits to index
}
