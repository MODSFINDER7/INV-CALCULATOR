package com.modsfinder.invcalculator;
final class ExpressionParser {
    private final String input;
    private int position;
    private ExpressionParser(String input) {
        this.input = input.replace('×', '*').replace('÷', '/').replace('−', '-');
    }
    static double evaluate(String expression) {
        ExpressionParser parser = new ExpressionParser(expression);
        double value = parser.expression();
        parser.skipSpaces();
        if (parser.position != parser.input.length()) throw new IllegalArgumentException("Unexpected character");
        if (!Double.isFinite(value)) throw new ArithmeticException("Result is not finite");
        return value;
    }
    private double expression() {
        double value = term();
        while (true) {
            skipSpaces();
            if (take('+')) value += term();
            else if (take('-')) value -= term();
            else return value;
        }
    }
    private double term() {
        double value = unary();
        while (true) {
            skipSpaces();
            if (take('*')) value *= unary();
            else if (take('/')) {
                double divisor = unary();
                if (divisor == 0.0) throw new ArithmeticException("Cannot divide by zero");
                value /= divisor;
            } else return value;
        }
    }
    private double unary() {
        skipSpaces();
        if (take('+')) return unary();
        if (take('-')) return -unary();
        return primary();
    }
    private double primary() {
        skipSpaces();
        if (take('(')) {
            double value = expression();
            skipSpaces();
            if (!take(')')) throw new IllegalArgumentException("Missing closing bracket");
            return value;
        }
        int start = position;
        boolean decimal = false;
        while (position < input.length()) {
            char c = input.charAt(position);
            if (Character.isDigit(c)) position++;
            else if (c == '.' && !decimal) { decimal = true; position++; }
            else break;
        }
        if (start == position || input.substring(start, position).equals(".")) throw new IllegalArgumentException("Number expected");
        return Double.parseDouble(input.substring(start, position));
    }
    private boolean take(char expected) {
        if (position < input.length() && input.charAt(position) == expected) { position++; return true; }
        return false;
    }
    private void skipSpaces() {
        while (position < input.length() && Character.isWhitespace(input.charAt(position))) position++;
    }
}
