package io.github.wadoon.logic


/**
 *
 */
sealed class PExpr {
    abstract fun parse(ps: ParserState): Expr?
}

/*abstract class PExprMem : PExpr() {
    override fun parse(ps: ParserState): Expr? = when (this) {
        in ps.memNone[ps.pos] -> {
            val memErrs = ps.memNone[ps.pos].getValue(this)
            ps.errs = memErrs
            null
        }

        in ps.memResult[ps.pos] -> {
            val (nextI, result) = ps.memResult[ps.pos].getValue(this)
            ps.pos = nextI
            result
        }

        else -> {
            val initI = ps.pos
            parseImpl(ps).also {
                if (it == null) ps.memNone[initI][this] = ArrayDeque(ps.errs)
                else ps.memResult[initI][this] = ps.pos to it
            }
        }
    }

    abstract fun parseImpl(ps: ParserState): Expr?
}*/

data class Symbol(val text: String) : PExpr() {
    override fun parse(ps: ParserState): Expr? {
        val peek = ps.peek()
        return if (peek == null) {
            ps.addErr("EoF reached but I need to match $text")
            null
        } else {
            if (peek.text == text) {
                ps.consume()
                return Expr(text)
            } else {
                ps.addErr("Expected $text but got ${peek.text}")
                null
            }
        }
    }

    override fun toString() = "'$text'"
}

data class NonTerminal(val sym: String) : PExpr() {
    override fun parse(ps: ParserState): Expr? = ps.invokeRule(sym)
    override fun toString() = "<$sym>"
}

data class Repeat(val sub: PExpr, val min: Int = 0) : PExpr() {
    override fun parse(ps: ParserState): Expr? {
        var matched = 0
        while (true) {
            val s = sub.parse(ps)
            if (s != null) {
                // clear errors?
                matched++
            } else {
                if (matched >= min) {
                    return Expr("x")
                }
            }
        }
    }

    override fun toString() = "($sub)" + (if (min == 0) "*" else "+")
}

data class Optional(val sub: PExpr) : PExpr() {
    override fun parse(ps: ParserState): Expr? {
        val pos = ps.pos
        val e = sub.parse(ps)
        if (e == null) {
            ps.errs.clear()
            ps.pos = pos
        }
        return e
    }

    override fun toString() = "($sub)?"
}

data class Sequence(val sub: List<PExpr>) : PExpr() {
    constructor(vararg sub: PExpr) : this(sub.toList())

    override fun parse(ps: ParserState): Expr? {
        for (pExpr in sub) {
            val r = pExpr.parse(ps)
            if (r == null)
                return null
        }
        return null
    }

    override fun toString() = sub.joinToString(" ") { "($it)" }
}

data class Choice(val sub: List<PExpr>) : PExpr() {
    constructor(vararg sub: PExpr) : this(sub.toList())

    override fun parse(ps: ParserState): Expr? {
        for (pExpr in sub) {
            val pos = ps.pos
            val r = pExpr.parse(ps)
            if (r != null) return r
            ps.pos = pos
        }
        ps.addErr("No alternative matched")
        return null
    }

    override fun toString() = sub.joinToString("|") { "( $it )" }
}

data class Expr(val symbol: String, val children: List<Expr> = listOf())
data class ParseError(val message: String, val pos: Int)
data class ParserResult(val expr: Expr?, val error: List<ParseError>?)

interface Token {
    val text: String
}

class ParserState(
    internal val stream: List<Token>,
    internal val rules: MutableMap<String, PExpr>
) {
    /** Current index in `stream` */
    internal var pos: Int = 0

    // Memoization (for packrat)
    internal val memNone: List<MutableMap<PExpr, ArrayDeque<ParseError>>> =
        List(size = stream.size + 1) { mutableMapOf() }

    internal val memResult: List<MutableMap<PExpr, Pair<Int, Expr>>> =
        List(size = stream.size + 1) { mutableMapOf() }


    // Error stack
    internal var errs = ArrayDeque<ParseError>()

    internal fun addErr(errMsg: String) = errs.add(ParseError(errMsg, pos))

    fun invokeRule(name: String): Expr? {
        val r = rules[name]
        if (r != null) return r.parse(this)
        else {
            addErr("Could not find rule $name.")
            return null
        }
    }

    fun peek(): Token? = try {
        stream[pos]
    } catch (e: IndexOutOfBoundsException) {
        null
    }

    fun consume() = ++pos
}

class PegParser(val rules: MutableMap<String, PExpr>, startName: String) {
    private val TEXT_IS_TOO_LONG = "Text was not fully parsed"

    private val startSymbol: PExpr = rules[startName]!!
    fun parse(seq: List<Token>): ParserResult {
        val ps = ParserState(seq, rules)
        val parsedSymbol = startSymbol.parse(ps)
        if (parsedSymbol != null) {
            val x = parsedSymbol.takeIf { ps.pos == seq.size }
            //?: None.also { ps.addErr(TEXT_IS_TOO_LONG) }
            if (x != null) {
                return ParserResult(x, null)
            } else {
                ps.addErr(TEXT_IS_TOO_LONG)
            }
        }
        return ParserResult(null, ps.errs)
    }
}

object EOF : PExpr() {
    override fun parse(ps: ParserState): Expr? {
        return if (ps.pos >= ps.stream.size) Expr("EOF")
        else null
    }
}

//Helpers
fun PExpr.repeat1(): PExpr = Repeat(this, 1)
fun PExpr.repeat0(): PExpr = Repeat(this, 0)

