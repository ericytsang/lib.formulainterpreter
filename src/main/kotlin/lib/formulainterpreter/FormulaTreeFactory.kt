package lib.formulainterpreter

import java.util.ArrayList
import java.util.EmptyStackException
import java.util.Stack

/**
 * Created by surpl on 5/5/2016.
 */

class FormulaTreeFactory<T>(val tokenInterpreter:TokenInterpreter,val operandFactory:OperandFactory<T>)
{
    fun parse(words:List<String>):T
    {
        val reorderedTokens = toPrefixNotation(words)
        return makeTree(reorderedTokens)
    }

    interface TokenInterpreter
    {
        fun parse(word:String):Symbol
    }

    data class Symbol
    (
        /**
         * specifies if this [Symbol] represents an operator or an operand.
         */
        val type:Type,

        /**
         * number of operands this operator takes.
         *
         * e.g.: + has 2 and - has 2
         *
         * this field will only be accessed by the [FormulaTreeFactory] if
         * [type] is set to [Type.OPERATOR].
         */
        val arity:Int,

        /**
         * precedence of this operator.
         *
         * e.g.: ^ is 3, * is 2, / is 2, + is 1, - is 1
         *
         * this field will only be accessed by the [FormulaTreeFactory] if
         * [type] is set to [Type.OPERATOR].
         */
        val precedence:Int
    )
    {
        enum class Type {OPERAND, OPERATOR, OPENING_PARENTHESIS, CLOSING_PARENTHESIS }
    }

    interface OperandFactory<T>
    {
        fun parse(word:String):T
        fun parse(word:String,operands:List<T>):T
    }

    /**
     * shunting yard algorithm
     */
    private fun toPrefixNotation(inputTokens:List<String>):List<String>
    {
        val outputQueue = ArrayList<String>(inputTokens.size)
        val operatorStack = Stack<String>()

        inputTokens.forEach()
        {
            word ->

            val operand = tokenInterpreter.parse(word)
            when (operand.type)
            {
                FormulaTreeFactory.Symbol.Type.OPERATOR ->
                {
                    while (operatorStack.isNotEmpty() && operand.precedence <= tokenInterpreter.parse(operatorStack.peek()).precedence)
                    {
                        outputQueue.add(operatorStack.pop())
                    }
                    operatorStack.push(word)
                }

                FormulaTreeFactory.Symbol.Type.OPERAND ->
                {
                    outputQueue.add(word)
                }

                FormulaTreeFactory.Symbol.Type.OPENING_PARENTHESIS ->
                {
                    operatorStack.push(word)
                }

                FormulaTreeFactory.Symbol.Type.CLOSING_PARENTHESIS ->
                {
                    // pop all words from the stack, and add them into the output
                    // queue until the matching opening parenthesis is found. but do
                    // not add it to the output queue.
                    while (true)
                    {
                        // pop the next word from the stack. if there is no word,
                        // throw an exception
                        val poppedWord = if (operatorStack.isNotEmpty())
                        {
                            operatorStack.pop()
                        }
                        else
                        {
                            throw IllegalArgumentException("there is an uneven amount of parenthesis...")
                        }

                        // push all popped words into the output queue unless it is
                        // the matching opening parenthesis
                        if (tokenInterpreter.parse(poppedWord).type == FormulaTreeFactory.Symbol.Type.OPENING_PARENTHESIS)
                        {
                            break
                        }
                        else
                        {
                            outputQueue.add(poppedWord)
                        }
                    }
                }
            }
        }

        while (operatorStack.isNotEmpty())
        {
            outputQueue.add(operatorStack.pop())
        }

        return outputQueue
    }

    private fun makeTree(inputTokens:List<String>):T
    {
        val operandStack = Stack<T>()

        inputTokens.forEach()
        {
            word ->
            val symbol = tokenInterpreter.parse(word)

            try
            {
                when (symbol.type)
                {
                    FormulaTreeFactory.Symbol.Type.OPERATOR ->
                    {
                        val operands = ArrayList<T>(symbol.arity)
                        for (i in 1..symbol.arity) { operands.add(operandStack.pop()) }
                        operandStack.push(operandFactory.parse(word,operands.reversed()))
                    }

                    FormulaTreeFactory.Symbol.Type.OPERAND ->
                    {
                        operandStack.push(operandFactory.parse(word))
                    }

                    FormulaTreeFactory.Symbol.Type.OPENING_PARENTHESIS,
                    FormulaTreeFactory.Symbol.Type.CLOSING_PARENTHESIS ->
                    {
                        throw IllegalArgumentException("unexpected token...string has been put into reverse polish notation, and should not have any parentheses")
                    }
                }
            }
            catch (ex:EmptyStackException)
            {
                throw IllegalArgumentException("missing operand for operator: $word")
            }

            Unit
        }

        return operandStack.pop()
    }
}
