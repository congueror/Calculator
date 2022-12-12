package net.congueror.calculator;

import net.congueror.calculator.helpers.MathHelper;
import net.congueror.calculator.structure.ActionTree;
import net.congueror.calculator.structure.ExtendedList;
import net.congueror.calculator.structure.TokenPair;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class Expression {

    final String type;

    public Expression(String type) {
        this.type = type;
    }

    public static void registerExpressions() {
        Equation.EXPRESSIONS.clear();
        Equation.EXPRESSIONS.put("=", new ComparisonOperator(d -> MathHelper.equals(d[0], d[1])));
        Equation.EXPRESSIONS.put("\\ne", new ComparisonOperator(d -> !MathHelper.equals(d[0], d[1])));
        Equation.EXPRESSIONS.put("\\approx", new ComparisonOperator(d -> MathHelper.equals(d[0], d[1])));
        Equation.EXPRESSIONS.put(">", new ComparisonOperator(d -> d[0] > d[1]));
        Equation.EXPRESSIONS.put("<", new ComparisonOperator(d -> d[0] < d[1]));
        Equation.EXPRESSIONS.put("\\ge", new ComparisonOperator(d -> d[0] >= d[1]));
        Equation.EXPRESSIONS.put("\\le", new ComparisonOperator(d -> d[0] <= d[1]));

        Equation.EXPRESSIONS.put("\\implies", new LogicOperator());

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

        Equation.EXPRESSIONS.put("\\frac", new Construct("fraction", 2, ats -> MathHelper.fraction(ats[0], ats[1])));

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

    public abstract void toLatex(StringBuilder ltx, TokenPair value, ExtendedList<ActionTree> children);

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

        @Override
        public void toLatex(StringBuilder ltx, TokenPair value, ExtendedList<ActionTree> children) {
            for (int i = 0; i < children.size(); i++) {
                ActionTree child = children.get(i);
                if (i > 0 && !child.value().is("delOp") && (!child.value().is("num") || !child.value().value().contains("-"))) {
                    ltx.append(value.value()).append(" ");
                }
                ltx.append(child.toLatex());
            }
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

        @Override
        public void toLatex(StringBuilder ltx, TokenPair value, ExtendedList<ActionTree> children) {
            ltx.append(value.value()).append(children.get(0).toLatex());
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

        @Override
        public void toLatex(StringBuilder ltx, TokenPair value, ExtendedList<ActionTree> children) {
            Expression.EncapsulationOperator op1 = ((Expression.EncapsulationOperator) Equation.EXPRESSIONS.get(value.value()));
            ltx.append(value.value()).append(children.get(0).toLatex()).append(op1.counterpart().value());
        }
    }

    /**
     * childrenAmount == 2
     */
    public static class ComparisonOperator extends Expression {

        private final Predicate<double[]> predicate;
        public ComparisonOperator(Predicate<double[]> predicate) {
            super("comparison");
            this.predicate = predicate;
        }

        public boolean execute(double a, double b) {
            return predicate.test(new double[] {a, b});
        }

        @Override
        public void toLatex(StringBuilder ltx, TokenPair value, ExtendedList<ActionTree> children) {
            ltx.append(children.get(0).toLatex());
            ltx.append(value.value());
            ltx.append(children.get(1).toLatex());
        }
    }

    public static class LogicOperator extends Expression {

        public LogicOperator() {
            super("logic");
        }

        @Override
        public void toLatex(StringBuilder ltx, TokenPair value, ExtendedList<ActionTree> children) {
            ltx.append(children.get(0).toLatex());
            ltx.append(value.value()).append(" ");
            ltx.append(children.get(1).toLatex());
        }
    }

    public static class Construct extends Expression {

        private final String verbose;
        private final int inputs;
        private final Function<ActionTree[], ActionTree> fun;
        public Construct(String verbose, int inputs, Function<ActionTree[], ActionTree> fun) {
            super("struct");
            this.verbose = verbose;
            this.inputs = inputs;
            this.fun = fun;
        }

        public String verbose() {
            return verbose;
        }

        public int inputs() {
            return inputs;
        }

        @Nullable
        public ActionTree execute(ActionTree[] inputs) {
            if (inputs.length != inputs()) {
                throw new ArithmeticException("Invalid number of inputs in construct, " + verbose);
            }
            return fun.apply(inputs);
        }

        @Override
        public void toLatex(StringBuilder ltx, TokenPair value, ExtendedList<ActionTree> children) {
            ltx.append(value.value());
            for (ActionTree child : children) {
                ltx.append(child.toLatex());
            }
        }
    }

    /**
     * childrenAmount == 1
     */

    public static class TrigonometricFunction extends Expression {

        private final String name;
        private final Function<Double, Equation> fun;
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

        @Override
        public void toLatex(StringBuilder ltx, TokenPair value, ExtendedList<ActionTree> children) {
            ltx.append(value.value()).append(children.get(0).toLatex());
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

        @Override
        public void toLatex(StringBuilder ltx, TokenPair value, ExtendedList<ActionTree> children) {
            ltx.append(value.value());
        }
    }
}
