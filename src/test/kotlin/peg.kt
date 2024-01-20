import io.github.wadoon.logic.*
import java.util.StringTokenizer
import kotlin.test.Test
import kotlin.test.assertNotNull


operator fun PExpr.plus(other: PExpr): PExpr {
    return Sequence(this, other)
}

operator fun PExpr.plus(other: String): PExpr {
    return Sequence(this, Symbol(other))
}


infix fun PExpr.or(other: PExpr): PExpr {
    return Choice(this, other)
}

class PegTests {
    val openParen = Symbol("(")

    @Test
    fun testSymbol() {
        val toks = tokenize("(")
        val ps = ParserState(toks, mutableMapOf())
        assertNotNull(openParen.parse(ps))
    }

    @Test
    fun testRepeat1() {
        val seqOfOpen = openParen.repeat1()
        val toks1 = tokenize("((((((((((")
        val ps = ParserState(toks1, mutableMapOf())
        assertNotNull(seqOfOpen.parse(ps))
    }

    @Test
    fun testParenthesis() {
        val start: PExpr = NonTerminal("start")
        val parenthesised: PExpr = Symbol("(") + start + ")"
        val pOrEof = EOF or parenthesised
        val grammar = mapOf("start" to pOrEof)
        val parser = PegParser(grammar.toMutableMap(), "start")

        parser.parse(tokenize("()"))
        parser.parse(tokenize("(())"))
        parser.parse(tokenize("(())"))
        parser.parse(tokenize("(((())))"))
        parser.parse(tokenize("((()))"))
        parser.parse(tokenize("(((((())))))"))
    }

    private fun tokenize(str: String): List<Token> {
        val s = StringTokenizer(str, "() +-*/%", true)
        return s.asSequence().map { it -> Tests.CToken(it.toString()) }.toList()
    }
}