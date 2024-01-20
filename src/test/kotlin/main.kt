import io.github.wadoon.logic.GrammarGraph
import io.github.wadoon.logic.Token
import javax.xml.stream.events.Characters
import kotlin.test.Test

class Tests {
    @Test
    fun test() {
        val graph = GrammarGraph()
        graph.define("parens", 10, "( _ )")

        graph.define("times", 80, "_ * _")
        graph.define("div", 80, "_ / _")


        graph.define("addition", 90, "_ + _")
        graph.define("substract", 90, "_ - _")


        graph.define("lt", 100, "_ < _")
        graph.define("gt", 100, "_ > _")
        graph.define("gte", 100, "_ >= _")
        graph.define("lte", 100, "_ <= _")

        graph.define("equality", 100, "_ == _")

        graph.define("and", 170, "_ & _")
        graph.define("or", 180, "_ | _")

        graph.define("imp", 200, "_ -> _")
        graph.define("equiv", 200, "_ <-> _")

        val expr = graph.construct()
        for ((k, v) in expr.rules) {
            println("$k ::= $v")
        }

        println(expr.parse(tokenize("a + b")))
    }


    @JvmInline
    value class CToken(override val text: String) : Token

    fun tokenize(s: String): List<Token> {
        return s.split(" +".toRegex()).map { CToken(it) }
        /*var remaining = s
        while (remaining.isNotBlank()) {
            while (remaining.isNotEmpty() && Character.isWhitespace(remaining.first())) {
                remaining = remaining.substring(1)
            }
            if (remaining.isEmpty()) break

            val c = remaining.first()
            when {
                Character.isDigit(c) -> {
                    var s = "" + c
                    while (Character.isDigit(remaining.first()) && remaining.isNotEmpty()) {
                        s += c
                        remaining = remaining.substring(1)
                    }
                    seq.add(CToken(s))
                }

                Character.isAlphabetic(c.code) -> {
                    var s = "" + c
                    while (remaining.isNotEmpty() &&
                        (Character.isDigit(remaining.first()) || Character.isAlphabetic(remaining.first().code))
                    ) {
                        s += c
                        remaining = remaining.substring(1)
                    }
                    seq.add(CToken(s))
                }


            }
        }*/
    }
}