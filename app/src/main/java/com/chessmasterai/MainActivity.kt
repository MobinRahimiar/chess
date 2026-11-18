package com.chessmasterai

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Forest = Color(0xFF10251D)
private val Panel = Color(0xFF182F25)
private val Cream = Color(0xFFF0E6CC)
private val Gold = Color(0xFFD9AE52)
private val Muted = Color(0xFFB9C7B9)
private val LightSquare = Color(0xFFE8DDBF)
private val DarkSquare = Color(0xFF66865B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChessMasterApp() }
    }
}

@Composable
private fun ChessMasterApp() {
    var screen by remember { mutableStateOf("welcome") }
    var color by remember { mutableStateOf("White") }
    MaterialTheme(colorScheme = darkColorScheme(background = Forest, surface = Panel, primary = Gold, onPrimary = Color(0xFF17130A), onBackground = Cream, onSurface = Cream)) {
        Surface(Modifier.fillMaxSize(), color = Forest) {
            when (screen) {
                "welcome" -> WelcomeScreen(color, { color = it }, { screen = "game" }, { screen = "academy" })
                "academy" -> AcademyScreen { screen = "welcome" }
                else -> GameScreen(color, { screen = "welcome" })
            }
        }
    }
}

@Composable
private fun WelcomeScreen(selected: String, onSelect: (String) -> Unit, onStart: () -> Unit, onAcademy: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("♛", color = Gold, fontSize = 64.sp)
            Text("ChessMaster AI", color = Cream, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("PLAY  •  ANALYZE  •  LEARN  •  IMPROVE", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(26.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Welcome to ChessMaster AI", color = Cream, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Your chess companion for deeper analysis, tactical ideas and smarter training.", color = Muted, textAlign = TextAlign.Center)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Choose your side", color = Cream, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SideCard("♙", "White", selected == "White", Modifier.weight(1f)) { onSelect("White") }
                SideCard("♟", "Black", selected == "Black", Modifier.weight(1f)) { onSelect("Black") }
            }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF17130A))) { Text("Start Game  ▶", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            OutlinedButton(onClick = onAcademy, modifier = Modifier.fillMaxWidth()) { Text("♙  آکادمی شطرنج و تمرین", color = Cream) }
        }
        Text("Local chess rules • Engine-powered analysis when Stockfish is installed", color = Muted, fontSize = 11.sp)
    }
}


@Composable
private fun AcademyScreen(onBack: () -> Unit) {
    var puzzleIndex by remember { mutableIntStateOf(0) }
    var position by remember(puzzleIndex) { mutableStateOf(ChessEngine.fromFen(TrainingAcademy.puzzles[puzzleIndex].fen)) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var solved by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("حرکت مات را روی صفحه پیدا کن.") }
    val puzzle = TrainingAcademy.puzzles[puzzleIndex]
    val legal = remember(position) { ChessEngine.legalMoves(position) }
    val targets = remember(selected, legal) { legal.filter { it.from == selected }.map { it.to }.toSet() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Gold, fontSize = 32.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(10.dp)); Text("آکادمی شطرنج", color = Cream, fontWeight = FontWeight.Bold, fontSize = 21.sp)
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("تمرین تاکتیکی", color = Gold, fontWeight = FontWeight.Bold)
                Text(puzzle.title, color = Cream, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("${puzzle.theme} • نوبت سفید", color = Muted, fontSize = 11.sp)
                ChessBoard(position, selected, targets, true, solved, onSquare = { square ->
                    if (solved) return@ChessBoard
                    val piece = position.board[square]
                    if (selected == null) {
                        if (ChessEngine.colorOf(piece) == 1) selected = square
                    } else {
                        val move = legal.firstOrNull { it.from == selected && it.to == square }
                        if (move != null) {
                            if (TrainingAcademy.isSolved(puzzle, move)) {
                                position = ChessEngine.apply(position, move) ?: position
                                solved = true
                                feedback = "درست است! ${puzzle.explanation}"
                            } else {
                                feedback = "این حرکت جواب تمرین نیست. دوباره کیش‌ها، گرفتن‌ها و تهدیدها را بررسی کن."
                                selected = null
                            }
                        } else if (ChessEngine.colorOf(piece) == 1) selected = square else selected = null
                    }
                })
                Text(feedback, color = if (solved) Gold else Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { position = ChessEngine.fromFen(puzzle.fen); selected = null; solved = false; feedback = "حرکت مات را روی صفحه پیدا کن." }, modifier = Modifier.weight(1f)) { Text("شروع دوباره") }
                    Button(onClick = { feedback = "راهنما: شاه سیاه در h8 است؛ خانه‌های فرار و مسیر وزیر را بررسی کن." }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF17130A))) { Text("راهنمایی") }
                }
            }
        }
        Text("درس‌های کوتاه", color = Gold, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        TrainingAcademy.lessons.forEach { lesson ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(lesson.title, color = Cream, fontWeight = FontWeight.Bold)
                    Text(lesson.summary, color = Muted, fontSize = 12.sp)
                    lesson.keyPoints.forEach { Text("• $it", color = Cream, fontSize = 11.sp) }
                }
            }
        }
        Text("تمرین فعلی یک پازل نمونهٔ تأییدشده با موتور قوانین داخلی است؛ بانک بزرگ پازل و سطح‌بندی تطبیقی در نسخه‌های بعدی اضافه می‌شود.", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun SideCard(piece: String, label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(118.dp).border(if (active) 2.dp else 1.dp, if (active) Gold else Color(0xFF3D5145), RoundedCornerShape(16.dp)).background(if (active) Color(0xFF294633) else Panel, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(piece, fontSize = 42.sp, color = Cream); Text(label, color = Cream, fontWeight = FontWeight.Bold); Text(if (label == "White") "Play first" else "Play second", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun GameScreen(playerColor: String, onBack: () -> Unit) {
    var history by remember { mutableStateOf(listOf(ChessEngine.initial())) }
    var movesText by remember { mutableStateOf(emptyList<String>()) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var promotion by remember { mutableStateOf<ChessMove?>(null) }
    var engineStatus by remember { mutableStateOf("Stockfish engine not checked") }
    var engineError by remember { mutableStateOf<String?>(null) }
    var engineReport by remember { mutableStateOf<EngineReport?>(null) }
    var analysisDepth by remember { mutableIntStateOf(14) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitle by remember { mutableStateOf("") }
    var savedGames by remember { mutableStateOf(emptyList<SavedChessGame>()) }
    val context = LocalContext.current
    val library = remember { GameLibrary(context) }
    LaunchedEffect(library) { savedGames = library.all() }
    val stockfish = remember { StockfishUci(context) }
    DisposableEffect(stockfish) { onDispose { stockfish.close() } }
    val state = history.last()
    val legal = remember(state) { ChessEngine.legalMoves(state) }
    val tacticalFindings = remember(state) { TacticalAnalyzer.analyze(state) }
    val targets = remember(selected, legal) { legal.filter { it.from == selected }.map { it.to }.toSet() }
    val isWhiteBottom = playerColor == "White"
    val result = ChessEngine.result(state, history.count { ChessEngine.positionKey(it) == ChessEngine.positionKey(state) })
    val status = when(result) {
        GameResult.CHECK -> if(state.whiteToMove) "White is in check" else "Black is in check"
        GameResult.CHECKMATE_WHITE -> "Checkmate — White wins"
        GameResult.CHECKMATE_BLACK -> "Checkmate — Black wins"
        GameResult.STALEMATE -> "Draw — Stalemate"
        GameResult.DRAW_FIFTY_MOVE -> "Draw — 50-move rule"
        GameResult.DRAW_REPETITION -> "Draw — Threefold repetition"
        GameResult.DRAW_INSUFFICIENT -> "Draw — Insufficient material"
        else -> if(state.whiteToMove) "White to move" else "Black to move"
    }
    fun makeMove(move: ChessMove) {
        val next = ChessEngine.apply(state, move) ?: return
        stockfish.stop()
        engineReport = null
        engineError = null
        engineStatus = "Position changed — analyze again"
        val notation = ChessEngine.notation(state, move)
        history = history + next
        movesText = movesText + "${state.fullmove}${if(state.whiteToMove) "." else "..."} $notation"
        selected = null; promotion = null
    }
    // In the selected-side game mode, Stockfish automatically replies for the opponent.
    LaunchedEffect(state, playerColor) {
        val humanSideToMove = state.whiteToMove == (playerColor == "White")
        val finished = result.name.startsWith("CHECKMATE") || result.name.startsWith("DRAW") || result == GameResult.STALEMATE
        if (!humanSideToMove && !finished && stockfish.isAvailable()) {
            engineStatus = "AI opponent is thinking…"
            stockfish.analyze(
                fen = ChessEngine.fen(state), depth = analysisDepth, multiPv = 1,
                onStatus = { engineStatus = it },
                onReport = { report ->
                    if (report.bestMove.isNotBlank()) {
                        uciToMove(state, report.bestMove)?.let { makeMove(it) }
                    }
                },
                onError = { engineError = it; engineStatus = "AI opponent unavailable" }
            )
        } else if (!humanSideToMove && !finished) {
            engineStatus = "Add the Android Stockfish binary to enable the AI opponent."
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Gold, fontSize = 32.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(10.dp)); Text("ChessMaster AI", color = Cream, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f)); Text(if(playerColor=="White") "♙ White" else "♟ Black", color = Muted)
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) { Text(status, Modifier.fillMaxWidth().padding(12.dp), color = if(result==GameResult.CHECK || result.name.startsWith("CHECKMATE")) Gold else Cream, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
        ChessBoard(state, selected, targets, isWhiteBottom, result.name.startsWith("CHECKMATE") || result.name.startsWith("DRAW") || result==GameResult.STALEMATE, onSquare = { square ->
            if(result.name.startsWith("CHECKMATE") || result.name.startsWith("DRAW") || result==GameResult.STALEMATE) return@ChessBoard
            val p = state.board[square]
            if(selected == null) { if(ChessEngine.colorOf(p) == if(state.whiteToMove) 1 else -1) selected = square }
            else {
                val candidates = legal.filter { it.from == selected && it.to == square }
                when { candidates.size == 1 -> makeMove(candidates.first()); candidates.size > 1 -> promotion = candidates.first(); ChessEngine.colorOf(p) == if(state.whiteToMove) 1 else -1 -> selected = square; else -> selected = null }
            }
        })
        Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Tactical Intelligence", color = Gold, fontWeight = FontWeight.Bold)
                Text("Pattern scan • heuristic guidance, not a forced engine line", color = Muted, fontSize = 10.sp)
                if (tacticalFindings.isEmpty()) {
                    Text("No clear tactical pattern detected in this position. Run Stockfish for deeper calculation.", color = Muted, fontSize = 12.sp)
                } else {
                    tacticalFindings.take(5).forEach { finding ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(finding.title, color = Cream, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Spacer(Modifier.weight(1f))
                                Text("★".repeat(finding.severity.coerceIn(1, 5)), color = Gold, fontSize = 10.sp)
                            }
                            Text(finding.description, color = Muted, fontSize = 11.sp)
                            if (finding.squares.isNotEmpty()) {
                                Text("Squares: ${finding.squares.distinct().joinToString(" · ") { ChessEngine.square(it) }}", color = Gold, fontSize = 10.sp)
                            }
                        }
                        HorizontalDivider(color = Color(0xFF34483B))
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Stockfish Analysis", color = Gold, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("Depth $analysisDepth", color = Muted, fontSize = 11.sp)
                }
                Slider(value = analysisDepth.toFloat(), onValueChange = { analysisDepth = it.toInt() }, valueRange = 8f..24f, steps = 15)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        engineError = null
                        engineReport = null
                        stockfish.analyze(
                            fen = ChessEngine.fen(state), depth = analysisDepth, multiPv = 3,
                            onStatus = { engineStatus = it },
                            onReport = { engineReport = it },
                            onError = { engineError = it; engineStatus = "Engine unavailable" }
                        )
                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF17130A))) { Text("Analyze position") }
                    OutlinedButton(onClick = { stockfish.stop(); engineStatus = "Stopping analysis…" }, modifier = Modifier.weight(1f)) { Text("Stop") }
                }
                Text(engineStatus, color = if (engineError == null) Muted else Gold, fontSize = 11.sp)
                engineError?.let { Text(it, color = Gold, fontSize = 11.sp) }
                engineReport?.let { report ->
                    Text("بهترین حرکت: ${report.bestMove}   •   عمق: ${report.depth}", color = Cream, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(explainEngineLine(report.lines.firstOrNull(), state.whiteToMove), color = Muted, fontSize = 12.sp)
                    val suggested = remember(state, report.bestMove) { uciToMove(state, report.bestMove) }
                    if (suggested != null) {
                        Button(onClick = { makeMove(suggested) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF17130A))) {
                            Text("اجرای حرکت پیشنهادی موتور")
                        }
                    }
                    report.lines.forEach { line ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text("${line.rank}. ${line.score}  |  ${line.pv.take(8).joinToString(" ")}", color = Cream, fontSize = 11.sp)
                            Text(explainEngineLine(line, state.whiteToMove), color = Muted, fontSize = 10.sp)
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF203B2D)), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("مربی هوشمند فارسی", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            AiCoach.insights(state, report).forEachIndexed { index, insight ->
                                Text("${index + 1}. $insight", color = Cream, fontSize = 11.sp)
                            }
                            Text("این توضیحات از خطوط واقعی موتور استخراج شده‌اند؛ الگوهای تاکتیکی جداگانه ماهیت احتمالی دارند.", color = Muted, fontSize = 9.sp)
                        }
                    }
                }
                if (engineError == null && !stockfish.isAvailable()) {
                    Text("Engine binary required: libstockfish.so (device ABI). No placeholder evaluation is shown.", color = Muted, fontSize = 10.sp)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { if(history.size>1){history=history.dropLast(1);movesText=movesText.dropLast(1);selected=null;engineReport=null} }, enabled=history.size>1, modifier=Modifier.weight(1f), colors=ButtonDefaults.buttonColors(containerColor=Panel)) { Text("↶ Undo") }
            Button(onClick = { showSaveDialog = true }, modifier=Modifier.weight(1f), colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color(0xFF17130A))) { Text("Save") }
            OutlinedButton(onClick = { history=listOf(ChessEngine.initial());movesText= emptyList();selected=null;promotion=null;engineReport=null;engineError=null }, modifier=Modifier.weight(1f)) { Text("Restart") }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("My saved games", color = Gold, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f)); Text("${savedGames.size}", color = Muted, fontSize = 11.sp)
                }
                if (savedGames.isEmpty()) Text("Saved games stay on this device. Save a game to see it here.", color = Muted, fontSize = 11.sp)
                savedGames.take(8).forEach { game ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(game.title, color = Cream, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("${game.moves.size} half-moves • ${game.playerColor}", color = Muted, fontSize = 10.sp)
                        }
                        TextButton(onClick = {
                            val restored = game.fens.mapNotNull { runCatching { ChessEngine.fromFen(it) }.getOrNull() }
                            if (restored.isNotEmpty()) { history = restored; movesText = game.moves; selected = null; promotion = null; engineReport = null; engineError = null }
                        }) { Text("Load") }
                        TextButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, library.toPgn(game)); putExtra(Intent.EXTRA_TITLE, "${game.title}.pgn") }
                            context.startActivity(Intent.createChooser(send, "Export PGN"))
                        }) { Text("Share PGN") }
                        TextButton(onClick = { library.delete(game.id); savedGames = library.all() }) { Text("Delete", color = Gold) }
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier=Modifier.fillMaxWidth().weight(1f, fill=false)) {
            Column(Modifier.padding(12.dp)) {
                Text("Move history", color=Gold, fontWeight=FontWeight.Bold)
                Text(if(movesText.isEmpty()) "No moves yet" else movesText.takeLast(12).joinToString("   "), color=Muted, fontSize=12.sp)
                Spacer(Modifier.height(5.dp)); Text("FEN", color=Gold, fontSize=11.sp)
                Text(ChessEngine.fen(state), color=Muted, fontSize=10.sp)
            }
        }
        Text("Legal moves • Check detection • Castling • En passant • Promotion", color=Muted, fontSize=10.sp, textAlign=TextAlign.Center, modifier=Modifier.fillMaxWidth())
    }
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save this game") },
            text = { OutlinedTextField(value = saveTitle, onValueChange = { saveTitle = it }, label = { Text("Game name") }, singleLine = true) },
            confirmButton = { TextButton(onClick = {
                library.save(saveTitle.ifBlank { "Game ${savedGames.size + 1}" }, playerColor, history, movesText)
                savedGames = library.all(); saveTitle = ""; showSaveDialog = false
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } }
        )
    }
    if(promotion != null) {
        AlertDialog(onDismissRequest={promotion=null}, title={Text("Promote pawn")}, text={Text("Choose the piece for your pawn.")}, confirmButton={
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf('q','r','b','n').forEach { piece -> TextButton(onClick={ val base=promotion!!; makeMove(base.copy(promotion=if(state.whiteToMove)piece.uppercaseChar() else piece)) }) { Text(when(piece){'q'->"Queen";'r'->"Rook";'b'->"Bishop";else->"Knight"}) } } }
        }, dismissButton={TextButton(onClick={ promotion=null }) { Text("Cancel") }})
    }
}

@Composable
private fun ChessBoard(state: ChessState, selected: Int?, targets: Set<Int>, whiteBottom: Boolean, disabled: Boolean, onSquare: (Int)->Unit) {
    val order = if(whiteBottom) (0..63).toList() else (63 downTo 0).toList()
    Column(Modifier.fillMaxWidth().aspectRatio(1f).border(2.dp, Gold, RoundedCornerShape(4.dp))) {
        for(r in 0..7) Row(Modifier.weight(1f)) {
            for(c in 0..7) {
                val index=order[r*8+c]; val piece=state.board[index]
                val light=((index/8)+(index%8))%2==0
                val isTarget=index in targets
                Box(Modifier.weight(1f).fillMaxHeight().background(if(light)LightSquare else DarkSquare).then(if(index==selected)Modifier.border(3.dp,Gold) else Modifier).clickable { onSquare(index) }, contentAlignment=Alignment.Center) {
                    if(isTarget) Box(Modifier.size(if(piece=='.')10.dp else 30.dp).border(3.dp,Color(0xFF244B2D),RoundedCornerShape(if(piece=='.')50 else 5)))
                    if(piece!='.') Text(glyph(piece),fontSize=if(c==0)27.sp else 31.sp,color=if(piece.isUpperCase())Color(0xFFFFFEF5) else Color(0xFF171717),textAlign=TextAlign.Center)
                    if(c==0) Text("${8-index/8}",Modifier.align(Alignment.TopStart).padding(start=2.dp,top=1.dp),fontSize=8.sp,color=Color(0xFF242424))
                    if(r==7) Text("${'a'+index%8}",Modifier.align(Alignment.BottomEnd).padding(end=2.dp,bottom=0.dp),fontSize=8.sp,color=Color(0xFF242424))
                }
            }
        }
    }
}

private fun glyph(piece: Char): String = when(piece) {
    'K'->"♔";'Q'->"♕";'R'->"♖";'B'->"♗";'N'->"♘";'P'->"♙"
    'k'->"♚";'q'->"♛";'r'->"♜";'b'->"♝";'n'->"♞";'p'->"♟"
    else->""
}
