package com.chessmasterai

/**
 * Rule-based tactical motif scanner.
 *
 * This is a heuristic explainer, not a replacement for Stockfish search.
 * It identifies candidate motifs in the current position and deliberately
 * labels them as patterns to review rather than guaranteed winning tactics.
 */
data class TacticalFinding(
    val title: String,
    val motif: TacticalMotif,
    val description: String,
    val squares: List<Int>,
    val severity: Int
)

enum class TacticalMotif {
    FORK, PIN, SKEWER, DISCOVERED_ATTACK, HANGING_PIECE, CHECK, KING_SAFETY
}

object TacticalAnalyzer {
    private val values = mapOf('p' to 1, 'n' to 3, 'b' to 3, 'r' to 5, 'q' to 9, 'k' to 100)

    fun analyze(state: ChessState): List<TacticalFinding> {
        val side = if (state.whiteToMove) 1 else -1
        val enemy = -side
        val findings = mutableListOf<TacticalFinding>()
        val board = state.board

        if (ChessEngine.inCheck(state)) {
            findings += TacticalFinding(
                "Check", TacticalMotif.CHECK,
                "Your king is under attack. Resolve the check before considering other plans.",
                listOf(board.indexOf(if (side == 1) 'K' else 'k')), 5
            )
        }

        // Forks: a knight, pawn, king or queen attacks two valuable enemy pieces.
        for (from in 0..63) {
            val piece = board[from]
            if (ChessEngine.colorOf(piece) != side) continue
            val attacked = attackedTargets(state, from, enemy)
            val valuable = attacked.filter { (values[board[it].lowercaseChar()] ?: 0) >= 3 }
            if (valuable.size >= 2) {
                val names = valuable.take(3).joinToString(" and ") { pieceName(board[it]) + " on " + ChessEngine.square(it) }
                findings += TacticalFinding(
                    "Fork opportunity", TacticalMotif.FORK,
                    "${pieceName(piece)} on ${ChessEngine.square(from)} attacks $names. Check whether the targets are adequately defended.",
                    listOf(from) + valuable.take(3), 4
                )
            }
        }

        // Absolute pins: exactly one friendly piece lies between king and enemy slider.
        val king = board.indexOf(if (side == 1) 'K' else 'k')
        if (king >= 0) {
            for (slider in 0..63) {
                if (ChessEngine.colorOf(board[slider]) != enemy || board[slider].lowercaseChar() !in "brq") continue
                val ray = lineBetween(king, slider) ?: continue
                if (ray.isEmpty()) continue
                val occupied = ray.filter { board[it] != '.' }
                if (occupied.size == 1 && ChessEngine.colorOf(board[occupied[0]]) == side) {
                    val pinned = occupied[0]
                    findings += TacticalFinding(
                        "Pinned piece", TacticalMotif.PIN,
                        "${pieceName(board[pinned])} on ${ChessEngine.square(pinned)} is pinned to your king by the enemy ${pieceName(board[slider])}. Moving it may expose check.",
                        listOf(king, pinned, slider), 5
                    )
                }
            }
        }

        // Skewers: enemy slider attacks the king/queen with a second piece behind it.
        for (attacker in 0..63) {
            val p = board[attacker]
            if (ChessEngine.colorOf(p) != enemy || p.lowercaseChar() !in "brq") continue
            for (target in 0..63) {
                if (ChessEngine.colorOf(board[target]) != side) continue
                val targetValue = values[board[target].lowercaseChar()] ?: 0
                if (targetValue < 8) continue
                val ray = lineBetween(attacker, target) ?: continue
                if (ray.any { board[it] != '.' }) continue
                val behind = nextOnRay(target, attacker) ?: continue
                if (ChessEngine.colorOf(board[behind]) == side &&
                    (values[board[behind].lowercaseChar()] ?: 0) in 1..(targetValue - 1)) {
                    findings += TacticalFinding(
                        "Possible skewer", TacticalMotif.SKEWER,
                        "Your ${pieceName(board[target])} on ${ChessEngine.square(target)} is aligned with a lower-value piece on ${ChessEngine.square(behind)} behind it. The enemy slider may win material if the front piece moves.",
                        listOf(attacker, target, behind), 4
                    )
                }
            }
        }

        // Hanging pieces: attacked by the side to move and not defended by their own side.
        for (target in 0..63) {
            val piece = board[target]
            if (ChessEngine.colorOf(piece) != enemy || piece.lowercaseChar() == 'k') continue
            val attackers = attackersTo(state, target, side)
            if (attackers.isEmpty()) continue
            val defenders = attackersTo(state, target, enemy)
            if (defenders.isEmpty()) {
                findings += TacticalFinding(
                    "Undefended enemy piece", TacticalMotif.HANGING_PIECE,
                    "The enemy ${pieceName(piece)} on ${ChessEngine.square(target)} is currently undefended and attacked by ${pieceName(board[attackers.first()])} on ${ChessEngine.square(attackers.first())}. Verify that it can be captured safely.",
                    listOf(attackers.first(), target), 3
                )
            }
        }

        // Discovered attack candidate: a friendly blocker sits between a slider and a valuable enemy target.
        for (slider in 0..63) {
            val p = board[slider]
            if (ChessEngine.colorOf(p) != side || p.lowercaseChar() !in "brq") continue
            for (target in 0..63) {
                if (ChessEngine.colorOf(board[target]) != enemy || (values[board[target].lowercaseChar()] ?: 0) < 5) continue
                val ray = lineBetween(slider, target) ?: continue
                val blockers = ray.filter { board[it] != '.' }
                if (blockers.size == 1 && ChessEngine.colorOf(board[blockers[0]]) == side) {
                    findings += TacticalFinding(
                        "Discovered-attack setup", TacticalMotif.DISCOVERED_ATTACK,
                        "Moving the friendly ${pieceName(board[blockers[0]])} on ${ChessEngine.square(blockers[0])} may uncover your ${pieceName(p)} against the enemy ${pieceName(board[target])} on ${ChessEngine.square(target)}.",
                        listOf(slider, blockers[0], target), 3
                    )
                }
            }
        }

        return findings.distinctBy { "${it.motif}:${it.squares.sorted()}" }
            .sortedWith(compareByDescending<TacticalFinding> { it.severity }.thenBy { it.title })
            .take(12)
    }

    private fun attackedTargets(state: ChessState, from: Int, byColor: Int): List<Int> =
        (0..63).filter { target ->
            ChessEngine.colorOf(state.board[target]) == byColor &&
                attacksSquare(state, from, target)
        }

    private fun attackersTo(state: ChessState, target: Int, byColor: Int): List<Int> =
        (0..63).filter { ChessEngine.colorOf(state.board[it]) == byColor && attacksSquare(state, it, target) }

    private fun attacksSquare(state: ChessState, from: Int, target: Int): Boolean {
        val p = state.board[from]
        if (p == '.' || from == target) return false
        val fr = from / 8; val fc = from % 8
        val tr = target / 8; val tc = target % 8
        val dr = tr - fr; val dc = tc - fc
        val type = p.lowercaseChar()
        if (type == 'p') {
            val direction = if (p.isUpperCase()) -1 else 1
            return dr == direction && kotlin.math.abs(dc) == 1
        }
        if (type == 'n') return (kotlin.math.abs(dr) == 2 && kotlin.math.abs(dc) == 1) ||
            (kotlin.math.abs(dr) == 1 && kotlin.math.abs(dc) == 2)
        if (type == 'k') return maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc)) == 1
        val diagonal = kotlin.math.abs(dr) == kotlin.math.abs(dc) && dr != 0
        val straight = (dr == 0) xor (dc == 0)
        val allowed = when (type) {
            'b' -> diagonal
            'r' -> straight
            else -> diagonal || straight
        }
        if (!allowed) return false
        val sr = dr.compareTo(0); val sc = dc.compareTo(0)
        var r = fr + sr; var c = fc + sc
        while (r != tr || c != tc) {
            if (state.board[r * 8 + c] != '.') return false
            r += sr; c += sc
        }
        return true
    }

    private fun lineBetween(a: Int, b: Int): List<Int>? {
        val ar = a / 8; val ac = a % 8
        val br = b / 8; val bc = b % 8
        val dr = br - ar; val dc = bc - ac
        if (!(dr == 0 || dc == 0 || kotlin.math.abs(dr) == kotlin.math.abs(dc))) return null
        if (dr == 0 && dc == 0) return null
        val sr = dr.compareTo(0); val sc = dc.compareTo(0)
        val result = mutableListOf<Int>()
        var r = ar + sr; var c = ac + sc
        while (r != br || c != bc) {
            result += r * 8 + c
            r += sr; c += sc
        }
        return result
    }

    private fun nextOnRay(from: Int, toward: Int): Int? {
        val fr = from / 8; val fc = from % 8
        val tr = toward / 8; val tc = toward % 8
        val dr = (fr - tr).compareTo(0); val dc = (fc - tc).compareTo(0)
        val r = fr + dr; val c = fc + dc
        return if (r in 0..7 && c in 0..7) r * 8 + c else null
    }

    private fun pieceName(piece: Char): String = when (piece.lowercaseChar()) {
        'p' -> "pawn"; 'n' -> "knight"; 'b' -> "bishop"; 'r' -> "rook"
        'q' -> "queen"; 'k' -> "king"; else -> "piece"
    }
}
