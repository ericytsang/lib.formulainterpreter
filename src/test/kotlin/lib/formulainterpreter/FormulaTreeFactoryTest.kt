package lib.formulainterpreter

import lib.formulainterpreter.FormulaTreeFactory.Symbol.*
import org.junit.Test

/**
 * Created by surpl on 5/5/2016.
 */
class InterpreterTest
{
    @Test
    fun test1()
    {
        val factory = FormulaTreeFactory(tokenInterpreter,operandFactory)
        val tree = factory.parse("3 * 7 + ( 6 - 4 )".split(" "))
        println(tree)
        assert(tree.toString() == "((3*7)+(6-4))")
    }
}

interface Composite

class AtomicComposite(val string:String):Composite
{
    override fun toString():String = string
}

class CompositeComposite(val string:String,val leftChild:Composite,val rightChild:Composite):Composite
{
    override fun toString():String = "($leftChild$string$rightChild)"
}

val operandFactory = object:FormulaTreeFactory.OperandFactory<Composite>
{
    override fun parse(word:String):Composite
    {
        return AtomicComposite(word)
    }
    override fun parse(word:String,operands:List<Composite>):Composite
    {
        return CompositeComposite(word,operands.first(),operands.last())
    }
}

val tokenInterpreter = object:FormulaTreeFactory.TokenInterpreter
{
    override fun parse(word:String):FormulaTreeFactory.Symbol = when (word)
    {
        "+","-" -> FormulaTreeFactory.Symbol(Type.OPERATOR,2,1)
        "*","/" -> FormulaTreeFactory.Symbol(Type.OPERATOR,2,2)
        "(" -> FormulaTreeFactory.Symbol(Type.OPENING_PARENTHESIS,0,0)
        ")" -> FormulaTreeFactory.Symbol(Type.CLOSING_PARENTHESIS,0,0)
        else -> FormulaTreeFactory.Symbol(Type.OPERAND,0,0)
    }
}
