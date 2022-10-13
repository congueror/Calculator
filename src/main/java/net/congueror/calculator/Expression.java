package net.congueror.calculator;

import net.congueror.calculator.structure.TokenPair;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Expression {

    final String type;

    public Expression(String type) {
        this.type = type;
    }

    public static void registerExpressions() {
        Equation.EXPRESSIONS.clear();
        Equation.EXPRESSIONS.put("+", new Operator(Double::sum, "sum"));
        Equation.EXPRESSIONS.put("-", new DelegateOperator(new TokenPair("op", "+")));
        Equation.EXPRESSIONS.put("\\cdot", new Operator((a, b) -> a * b, "product"));
        Equation.EXPRESSIONS.put("\\div", new DelegateOperator(new TokenPair("op", "\\cdot")));
        Equation.EXPRESSIONS.put("\\left(", new EncapsulationOperator(new TokenPair("encOp", "\\right)"), true));
        Equation.EXPRESSIONS.put("\\right)", new EncapsulationOperator(new TokenPair("encOp", "\\left("), false));
        Equation.EXPRESSIONS.put("{", new EncapsulationOperator(new TokenPair("encOp", "}"), true));
        Equation.EXPRESSIONS.put("}", new EncapsulationOperator(new TokenPair("encOp", "{"), false));

        Equation.EXPRESSIONS.put("\\pi", new Constant(Math.PI));
        Equation.EXPRESSIONS.put("e", new Constant(Math.E));

        Equation.EXPRESSIONS.put("\\frac", new Construct(2));

        Equation.EXPRESSIONS.put("\\sin", new TrigonometricFunction("sine", a -> new Equation(String.valueOf(MathHelper.sin(a)))));
        Equation.EXPRESSIONS.put("\\cos", new TrigonometricFunction("cosine", a -> new Equation(String.valueOf(MathHelper.cos(a)))));
        Equation.EXPRESSIONS.put("\\tan", new TrigonometricFunction("tangent", a -> new Equation(String.valueOf(MathHelper.tan(a)))));
        Equation.EXPRESSIONS.put("\\arcsin", new TrigonometricFunction("arcsine", MathHelper::arcsin, true));
        Equation.EXPRESSIONS.put("\\arccos", new TrigonometricFunction("arccosine", MathHelper::arccos, true));
        Equation.EXPRESSIONS.put("\\arctan", new TrigonometricFunction("arctangent", MathHelper::arctan, true));
        Equation.EXPRESSIONS.put("\\csc", new TrigonometricFunction("cosecant", a -> new Equation(String.valueOf(1 / MathHelper.sin(a)))));
        Equation.EXPRESSIONS.put("\\sec", new TrigonometricFunction("secant", a -> new Equation(String.valueOf(1 / MathHelper.cos(a)))));
        Equation.EXPRESSIONS.put("\\cot", new TrigonometricFunction("cotangent", a -> new Equation(String.valueOf(1 / MathHelper.tan(a)))));
        Equation.EXPRESSIONS.put("\\arccsc", new TrigonometricFunction("arccosecant", a -> MathHelper.arcsin(1 / a), true));
        Equation.EXPRESSIONS.put("\\arcsec", new TrigonometricFunction("arcsecant", a -> MathHelper.arccos(1 / a), true));
        Equation.EXPRESSIONS.put("\\arccot", new TrigonometricFunction("arccotangent", a -> MathHelper.arctan(1 / a), true));
    }

    /**
     * childrenAmount >= 2
     */
    public static class Operator extends Expression {

        private final BiFunction<Double, Double, Double> fun;
        private final String verbose;
        public Operator(BiFunction<Double, Double, Double> fun, String verbose) {
            super("op");
            this.fun = fun;
            this.verbose = verbose;
        }

        public double apply(double a, double b) {
            return fun.apply(a, b);
        }

        public String verbose() {
            return verbose;
        }
    }

    /**
     * childrenAmount == 1
     */
    public static class DelegateOperator extends Expression {

        private final TokenPair delegate;
        public DelegateOperator(TokenPair delegate) {
            super("delOp");
            this.delegate = delegate;
        }

        public TokenPair getDelegate() {
            return delegate;
        }
    }

    /**
     * childrenAmount == 1
     */
    public static class EncapsulationOperator extends Expression {

        private final TokenPair counterpart;
        private final boolean isLeft;
        public EncapsulationOperator(TokenPair counterpart, boolean isLeft) {
            super("encOp");
            this.counterpart = counterpart;
            this.isLeft = isLeft;
        }

        public TokenPair counterpart() {
            return counterpart;
        }

        public boolean isLeft() {
            return isLeft;
        }
    }

    public static class Construct extends Expression {

        private final int inputs;
        public Construct(int inputs) {
            super("struct");
            this.inputs = inputs;
        }

        public int inputs() {
            return inputs;
        }
    }

    /**
     * childrenAmount == 1
     */

    public static class TrigonometricFunction extends Expression {

        private final String name;
        private final java.util.function.Function<Double, Equation> fun;
        private final boolean inverse;

        public TrigonometricFunction(String name, Function<Double, Equation> fun, boolean inverse) {
            super("trigFun");
            this.name = name;
            this.fun = fun;
            this.inverse = inverse;
        }

        public TrigonometricFunction(String name, java.util.function.Function<Double, Equation> fun) {
            this(name, fun, false);
        }

        public String name() {
            return name;
        }

        public Equation execute(double input) {
            return fun.apply(input);
        }

        public boolean isInverse() {
            return inverse;
        }
    }

    /**
     * childrenAmount == 0
     */
    public static class Constant extends Expression {

        private final double value;

        public Constant(double value) {
            super("const");
            this.value = value;
        }

        public double getValue() {
            return value;
        }
    }
}
