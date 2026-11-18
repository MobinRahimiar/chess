package com.chessmasterai

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Parsed UCI principal variation returned by Stockfish. */
data class EngineLine(
    val rank: Int,
    val depth: Int,
    val score: String,
    val scoreIsMate: Boolean,
    val nodes: Long,
    val nps: Long,
    val pv: List<String>
) {
    val bestMove: String get() = pv.firstOrNull().orEmpty()
}

data class EngineReport(val lines: List<EngineLine>, val bestMove: String, val depth: Int)

/**
 * Small UCI process adapter. Put the official Android Stockfish executable at
 * app/src/main/jniLibs/<abi>/libstockfish.so. Android extracts it to nativeLibraryDir.
 * No fake evaluation is produced when the executable is absent.
 */
class StockfishUci(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)
    @Volatile private var process: Process? = null
    @Volatile private var writer: BufferedWriter? = null
    @Volatile private var reader: BufferedReader? = null

    fun engineFile(): File = File(appContext.applicationInfo.nativeLibraryDir, "libstockfish.so")
    fun isAvailable(): Boolean = engineFile().let { it.isFile && it.canExecute() }

    fun analyze(
        fen: String,
        depth: Int = 14,
        multiPv: Int = 3,
        onStatus: (String) -> Unit,
        onReport: (EngineReport) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!running.compareAndSet(false, true)) {
            main.post { onStatus("Analysis is already running") }
            return
        }
        worker.execute {
            try {
                val binary = engineFile()
                if (!binary.isFile || !binary.canExecute()) {
                    throw IllegalStateException(
                        "Stockfish binary not installed for this device. Add libstockfish.so to app/src/main/jniLibs/<device-ABI>/."
                    )
                }
                main.post { onStatus("Starting Stockfish…") }
                val p = ProcessBuilder(binary.absolutePath).redirectErrorStream(true).start()
                process = p
                writer = BufferedWriter(OutputStreamWriter(p.outputStream))
                reader = BufferedReader(InputStreamReader(p.inputStream))
                send("uci")
                if (!readUntil("uciok", 12)) throw IllegalStateException("Stockfish did not complete UCI handshake")
                send("setoption name Threads value 2")
                send("setoption name Hash value 48")
                send("setoption name MultiPV value ${multiPv.coerceIn(1, 5)}")
                send("isready")
                if (!readUntil("readyok", 8)) throw IllegalStateException("Stockfish is not ready")
                send("ucinewgame")
                send("position fen $fen")
                send("go depth ${depth.coerceIn(8, 24)}")
                main.post { onStatus("Analyzing position…") }
                val lines = linkedMapOf<Int, EngineLine>()
                var best = ""
                var reachedDepth = 0
                while (true) {
                    val line = reader?.readLine() ?: break
                    if (line.startsWith("info ")) {
                        parseUciInfoLine(line)?.let {
                            lines[it.rank] = it
                            reachedDepth = maxOf(reachedDepth, it.depth)
                            main.post { onReport(EngineReport(lines.values.sortedBy { x -> x.rank }, best, reachedDepth)) }
                        }
                    } else if (line.startsWith("bestmove ")) {
                        best = line.split(Regex("\\s+" )).getOrNull(1).orEmpty()
                        break
                    }
                }
                if (best.isBlank() || best == "(none)") throw IllegalStateException("Engine returned no legal best move")
                main.post {
                    onReport(EngineReport(lines.values.sortedBy { it.rank }, best, reachedDepth))
                    onStatus("Analysis complete")
                }
            } catch (t: Throwable) {
                main.post { onError(t.message ?: "Unknown Stockfish error") }
            } finally {
                try { send("quit") } catch (_: Exception) { }
                try { process?.destroy() } catch (_: Exception) { }
                process = null
                writer = null
                reader = null
                running.set(false)
            }
        }
    }

    fun stop() {
        try { send("stop") } catch (_: Exception) { }
    }

    fun close() {
        stop()
        try { send("quit") } catch (_: Exception) { }
        try { process?.destroy() } catch (_: Exception) { }
        process = null
        worker.shutdownNow()
    }

    private fun send(command: String) {
        synchronized(this) {
            writer?.apply { write(command); newLine(); flush() }
        }
    }

    private fun readUntil(expected: String, seconds: Long): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds)
        while (System.nanoTime() < deadline) {
            val line = reader?.readLine() ?: return false
            if (line.trim() == expected) return true
        }
        return false
    }


}

/** Parses one UCI `info` line; exposed internally so the protocol parser can be unit-tested. */
internal fun parseUciInfoLine(line: String): EngineLine? {
    val tokens = line.trim().split(Regex("\\s+"))
    fun value(key: String): String? = tokens.indexOf(key).takeIf { it >= 0 && it + 1 < tokens.size }?.let { tokens[it + 1] }
    val depth = value("depth")?.toIntOrNull() ?: return null
    val rank = value("multipv")?.toIntOrNull() ?: 1
    val scoreIndex = tokens.indexOf("score")
    if (scoreIndex < 0 || scoreIndex + 2 >= tokens.size) return null
    val mate = tokens[scoreIndex + 1] == "mate"
    val rawScore = tokens[scoreIndex + 2].toIntOrNull() ?: return null
    val score = if (mate) "Mate ${if (rawScore > 0) "+" else ""}$rawScore" else {
        val pawns = rawScore / 100.0
        "${if (pawns > 0) "+" else ""}${"%.2f".format(java.util.Locale.US, pawns)}"
    }
    val pvIndex = tokens.indexOf("pv")
    val pv = if (pvIndex >= 0 && pvIndex + 1 < tokens.size) tokens.drop(pvIndex + 1) else emptyList()
    return EngineLine(rank, depth, score, mate, value("nodes")?.toLongOrNull() ?: 0L, value("nps")?.toLongOrNull() ?: 0L, pv)
}
