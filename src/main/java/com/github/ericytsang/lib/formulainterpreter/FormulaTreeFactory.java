package com.github.ericytsang.lib.formulainterpreter;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

/**
 * Created by surpl on 8/4/2016.
 */
public class FormulaTreeFactory<FormulaTree>
{
    public final TokenInterpreter tokenInterpreter;
    public final OperandFactory<FormulaTree> operandFactory;

    public FormulaTreeFactory(TokenInterpreter tokenInterpreter,OperandFactory<FormulaTree> operandFactory)
    {
        this.tokenInterpreter = tokenInterpreter;
        this.operandFactory = operandFactory;
    }

    public FormulaTree parse(List<String> words)
    {
        List<String> inputTokens = toPrefixNotation(words);

        Stack<FormulaTree> operandStack = new Stack<FormulaTree>();

        for (String word : inputTokens)
        {
            Symbol symbol = tokenInterpreter.parse(word);

            try
            {
                switch(symbol.type)
                {
                    case OPERATOR:
                        List<FormulaTree> operands = new ArrayList<FormulaTree>(symbol.arity);
                        for(int i = 0; i < symbol.arity; i++)
                        {
                            operands.add(0,operandStack.pop());
                        }
                        operandStack.push(operandFactory.parse(word,operands));
                        break;
                    case OPERAND:
                        operandStack.push(operandFactory.parse(word));
                        break;
                    case OPENING_PARENTHESIS:
                    case CLOSING_PARENTHESIS:
                        throw new IllegalArgumentException("unexpected token...string has been put into reverse polish notation, and should not have any parentheses");
                }
            }
            catch (EmptyStackException ex)
            {
                throw new IllegalArgumentException("missing operand for operator: $word");
            }
        }

        FormulaTree result = operandStack.pop();
        if (!operandStack.isEmpty())
        {
            throw new IllegalArgumentException("too many operands for operators");
        }
        return result;
    }

    public interface TokenInterpreter
    {
        Symbol parse(String word);
    }

    public interface OperandFactory<FormulaTree>
    {
        FormulaTree parse(String word);
        FormulaTree parse(String word,List<FormulaTree> operands);
    }

    public static class Symbol
    {
        /**
         * specifies if this [Symbol] represents an operator or an operand.
         */
        private final Type type;

        /**
         * number of operands this operator takes.
         *
         * e.g.: + has 2 and - has 2
         *
         * this field will only be accessed by the [FormulaTreeFactory] if
         * [type] is set to [Type.OPERATOR].
         */
        private final Integer arity;

        /**
         * precedence of this operator.
         *
         * e.g.: ^ is 3, * is 2, / is 2, + is 1, - is 1
         *
         * this field will only be accessed by the [FormulaTreeFactory] if
         * [type] is set to [Type.OPERATOR].
         */
        private final Integer precedence;

        public Symbol(Type type,Integer arity,Integer precedence)
        {
            this.type = type;
            this.arity = arity;
            this.precedence = precedence;
        }

        public Type getType()
        {
            return type;
        }

        public Integer getArity()
        {
            return arity;
        }

        public Integer getPrecedence()
        {
            return precedence;
        }
    }

    public enum Type {OPERAND, OPERATOR, OPENING_PARENTHESIS, CLOSING_PARENTHESIS }

    /**
     * shunting yard algorithm
     */
    private List<String> toPrefixNotation(List<String> inputTokens)
    {
        List<String> outputQueue = new ArrayList<String>(inputTokens.size());
        Stack<String> operatorStack = new Stack<String>();

        for (String word : inputTokens)
        {
            Symbol operand = tokenInterpreter.parse(word);
            switch(operand.type)
            {
                case OPERATOR:
                    while (!operatorStack.isEmpty() && operand.precedence <= tokenInterpreter.parse(operatorStack.peek()).precedence)
                    {
                        outputQueue.add(operatorStack.pop());
                    }
                    operatorStack.push(word);
                    break;

                case OPERAND:
                    outputQueue.add(word);
                    break;

                case OPENING_PARENTHESIS:
                    operatorStack.push(word);
                    break;

                case CLOSING_PARENTHESIS:
                    // pop all words from the stack, and add them into the output
                    // queue until the matching opening parenthesis is found. but do
                    // not add it to the output queue.
                    while (true)
                    {
                        // pop the next word from the stack. if there is no word,
                        // throw an exception
                        if (operatorStack.isEmpty())
                        {
                            throw new IllegalArgumentException("there is an uneven amount of parenthesis...");
                        }
                        String poppedWord = operatorStack.pop();

                        // push all popped words into the output queue unless it is
                        // the matching opening parenthesis
                        if (tokenInterpreter.parse(poppedWord).type == Type.OPENING_PARENTHESIS)
                        {
                            break;
                        }
                        else
                        {
                            outputQueue.add(poppedWord);
                        }
                    }
                    break;
            }
        }

        while (!operatorStack.isEmpty())
        {
            String word = operatorStack.pop();
            if (tokenInterpreter.parse(word).type == Type.OPENING_PARENTHESIS)
            {
                throw new IllegalArgumentException("there is an uneven amount of parenthesis...");
            }
            else
            {
                outputQueue.add(word);
            }
        }

        return outputQueue;
    }
}
