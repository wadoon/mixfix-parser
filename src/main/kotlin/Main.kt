package io.github.wadoon.logic


sealed class Part {

    abstract fun asPExpr(other: PExpr): PExpr
}

data class NamePart(val name: String) : Part() {

    override fun toString() = name

    override fun asPExpr(other: PExpr): PExpr = asPExpr()
    fun asPExpr() = Symbol(name)
}

object HolePart : Part() {
    override fun toString() = "_"

    override fun asPExpr(other: PExpr) = other

}

interface Operator {
    val name: String
    val precedence: Int
    val schema: List<Part>
    fun generate(topLevel: NonTerminal, nextLevel: NonTerminal): PExpr
    fun repr() = schema.joinToString(" ") { it.toString() }
}

data class PrefixOperator(override val name: String, override val precedence: Int, override val schema: List<Part>) :
    Operator {
    internal val operatorName: NamePart = schema.first() as NamePart

    init {
        require(schema.size == 2)
        require(schema[2] is HolePart)
    }

    override fun generate(topLevel: NonTerminal, nextLevel: NonTerminal): PExpr =
        Sequence(listOf(operatorName.asPExpr().repeat1(), nextLevel))
}

data class Postfix(override val name: String, override val precedence: Int, override val schema: List<Part>) :
    Operator {
    internal val operatorName: NamePart = schema[1] as NamePart

    init {
        require(schema.size == 2)
        require(schema.first() is HolePart)
    }

    override fun generate(topLevel: NonTerminal, nextLevel: NonTerminal): PExpr =
        Sequence(listOf(nextLevel, operatorName.asPExpr().repeat1()))
}

data class Closed(override val name: String, override val precedence: Int, override val schema: List<Part>) :
    Operator {
    internal val open: NamePart = schema.first() as NamePart
    internal val close: NamePart = schema.last() as NamePart

    init {
        require(schema.size >= 3)
    }

    override fun generate(topLevel: NonTerminal, nextLevel: NonTerminal): PExpr {
        val intern = schema.map { it.asPExpr(topLevel) }
        return Sequence(intern)
    }
}

data class MixfixOperator(override val name: String, override val precedence: Int, override val schema: List<Part>) :
    Operator {

    init {
        require(schema.last() is HolePart)
        require(schema.first() is HolePart)
    }

    override fun generate(topLevel: NonTerminal, nextLevel: NonTerminal): PExpr {
        val intern = schema.subList(1, schema.size - 1).map { it.asPExpr(topLevel) }
        return Sequence(listOf(schema.first().asPExpr(nextLevel)) + intern + listOf(schema.last().asPExpr(nextLevel)))
    }
}


class GrammarGraph(val ops: MutableList<Operator> = arrayListOf()) {
    fun buckets() =
        ops.map { it.precedence }.toSortedSet()

    fun next(precedence: Int): List<Operator> {
        val b = buckets()
        val next = b.tailSet(precedence + 1).first()
        return ops.filter { it.precedence == next }
    }

    fun lower(precedence: Int): List<Operator> {
        val b = buckets()
        val next = b.headSet(precedence - 1).last()
        return ops.filter { it.precedence == next }
    }

    fun define(name: String, precedence: Int, parts: String): Operator {
        val p = parts.split(" ")
            .map {
                when (it) {
                    "_" -> HolePart
                    else -> NamePart(it)
                }
            }

        return try {
            Postfix(name, precedence, p).also { ops.add(it) }
        } catch (e: IllegalArgumentException) {
            try {
                PrefixOperator(name, precedence, p).also { ops.add(it) }
            } catch (e: IllegalArgumentException) {
                try {
                    Closed(name, precedence, p).also { ops.add(it) }
                } catch (e: IllegalArgumentException) {
                    MixfixOperator(name, precedence, p).also { ops.add(it) }
                }
            }
        }
    }

    fun construct(): PegParser {
        val buckets = ops.groupBy { it.precedence }
        val precs = buckets.keys.toSortedSet()
        val rules = mutableMapOf<String, PExpr>()

        val topLevel = NonTerminal("expr")

        for (prec in precs) {
            val pops = buckets[prec]!!
            val names = pops.map { NonTerminal(it.name) }
            rules["expr$prec"] = Choice(names)

            val nextLevel =
                try {
                    val next = precs.headSet(prec - 1).last()
                    NonTerminal("expr$next")
                } catch (e: NoSuchElementException) {
                    NonTerminal("expr")
                }

            for (op in pops) {
                /* val parts = op.schema.map {
                    when (it) {
                        is HolePart -> sub
                        is NamePart -> Symbol(it.name)
                    }
                }*/
                rules[op.name] = op.generate(topLevel, nextLevel)
            }
        }

        //val max = buckets().last()
        rules["expr"] = Choice(ops.sortedBy { it.precedence }.map { NonTerminal(it.name) })
        return PegParser(rules, "expr")
    }
}
