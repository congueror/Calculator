package net.congueror.calculator.structure;

import net.congueror.calculator.Equation;
import net.congueror.calculator.Expression;
import net.congueror.calculator.OperationStep;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ActionTree implements Cloneable {
    private int depth;
    private TokenPair encOperator = null;
    private TokenPair value;
    private ExtendedList<ActionTree> children = new ExtendedList<>();

    public ActionTree(TokenPair value) {
        this.value = value;
    }

    public TokenPair value() {
        return value;
    }

    public ActionTree getChild() {
        return children.get(0);
    }

    public boolean findAny(String typeQuery) {
        if (this.value.is(typeQuery)) return true;
        for (ActionTree child : children) {
            if (child.value.is(typeQuery))
                return true;
            child.findAny(typeQuery);
        }
        return false;
    }

    public void insert(ActionTree node) {
        if (node == this) return;
        children.add(node);
        node.depth = depth + 1;
    }

    public void insert(int index, ActionTree node) {
        if (node == this) return;
        children.add(index, node);
        node.depth = depth + 1;
    }

    public void insert(List<ActionTree> nodes) {
        for (ActionTree node : nodes) {
            insert(node);
        }
    }

    public void print() {
        print(0);
    }

    private void print(int height) {
        System.out.println(value.toString());
        ++height;
        for (ActionTree child : children) {
            System.out.print("  ".repeat(Math.max(0, height)));
            child.print(height);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionTree that = (ActionTree) o;
        return value().equals(that.value()) && children.equals(that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, children);
    }

    @Override
    public String toString() {
        return (!children.isEmpty() ? "{" + value + ": " + children + "}" : value.toString());
    }

    public ActionTree _clone() {
        try {
            return (ActionTree) this.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ActionTree t = (ActionTree) super.clone();
        t.value = this.value._clone();
        t.children = new ExtendedList<>();
        for (ActionTree child : this.children) {
            t.children.add(child._clone());
        }
        return t;
    }

    private static boolean compareProducts(ActionTree tree1, ActionTree tree2) {
        if (tree1.value().equals("op", "\\cdot") || tree2.value().equals("op", "\\cdot")) {
            if (!tree1.value().equals("op", "\\cdot"))
                return Set.of(tree1).equals(tree2.children.get(a -> !a.value.is("num")).collect(Collectors.toSet()));
            if (!tree2.value().equals("op", "\\cdot"))
                return tree1.children.get(a -> !a.value.is("num")).collect(Collectors.toSet()).equals(Set.of(tree2));
            return tree1.children.get(a -> !a.value.is("num")).collect(Collectors.toSet()).equals(tree2.children.get(a -> !a.value.is("num")).collect(Collectors.toSet()));
        }
        return tree1.equals(tree2);
    }

    private static String getProductValue(ActionTree tree) {
        if (tree.value().equals("op", "\\cdot"))
            return tree.children.get(a -> a.value.is("num")).map(a -> a.value().value()).collect(ExtendedList.toList()).getOr(0, "1.0");
        return "1.0";
    }

    public String toLatex() {
        if (this.value.is("root")) {
            return this.children.get(0).toLatex();
        }

        StringBuilder ltx = new StringBuilder();
        Expression.EncapsulationOperator op = this.encOperator != null ? ((Expression.EncapsulationOperator) Equation.EXPRESSIONS.get(this.encOperator.value())) : null;

        if (op != null) {
            ltx.append(this.encOperator.value());
        }

        if (this.value.is("num")) {
            var yes = new DecimalFormat("#.##");
            ltx.append(yes.format(Double.parseDouble(this.value.value())));
        } else if (this.value.isOr("const"))
            ltx.append(this.value.value());
        else if (this.value.isOr("delOp", "trigFun")) {
            ltx.append(this.value.value()).append(this.children.get(0).toLatex());
        } else if (this.value.is("encOp")) {
            Expression.EncapsulationOperator op1 = ((Expression.EncapsulationOperator) Equation.EXPRESSIONS.get(this.value.value()));
            ltx.append(this.value.value()).append(this.children.get(0).toLatex()).append(op1.counterpart().value());
        } else if (this.value.is("struct")) {
            ltx.append(this.value.value());
            for (ActionTree child : this.children) {
                ltx.append(child.toLatex());
            }
        } else if (this.value.is("op")) {
            for (int i = 0; i < this.children.size(); i++) {
                ActionTree child = this.children.get(i);
                if (i > 0 && !child.value.is("delOp") && (!child.value.is("num") || !child.value.value().contains("-"))) {
                    ltx.append(this.value.value()).append(" ");
                }
                ltx.append(child.toLatex());
            }
        }

        if (op != null)
            ltx.append(op.counterpart().value());

        return ltx.toString();
    }

    public void solveOperation(ActionTree root, ExtendedList<OperationStep> steps) {
        if (this.value.is("root")) {
            ActionTree rootCl = root._clone();
            rootCl.omitParentheses();
            rootCl.applyNegativeSigns();
            steps.add(new OperationStep(rootCl, ""));
            this.children.get(0).solveOperation(root, steps);
            return;
        }

        root.execute((a, b) -> steps.add(new OperationStep(a._clone(), b)));
    }

    private void execute(BiConsumer<ActionTree, String> onChange) {
        StringBuilder message = new StringBuilder();
        omitParentheses();
        applyNegativeSigns();
        for (int i = 0; i < 1; i++) {

            message = new StringBuilder();
            if (applyOperatorIdentities(message)) {
                omitParentheses();
                applyNegativeSigns();
                onChange.accept(this, message.toString());
                i = -1;
                continue;
            }

            if (divisionsToFractions()) {
                omitParentheses();
                applyNegativeSigns();
                onChange.accept(this, "Convert division operators to fractions for better calculations.");
                i = -1;
                continue;
            }

            message = new StringBuilder();
            if (simplifyFraction(message)) {
                omitParentheses();
                applyNegativeSigns();
                onChange.accept(this, message.toString());
                i = -1;
                continue;
            }

            message = new StringBuilder();
            if (executeOperators(message)) {
                omitParentheses();
                applyNegativeSigns();
                onChange.accept(this, message.toString());
                i = -1;
                continue;
            }

            message = new StringBuilder();
            if (executeTrigonometricFunctions(message)) {
                omitParentheses();
                applyNegativeSigns();
                onChange.accept(this, message.toString());
                i = -1;
                continue;
            }

            message = new StringBuilder();
            if (constToNum(message)) {
                omitParentheses();
                applyNegativeSigns();
                onChange.accept(this, message.toString());
                i = -1;
            }
        }
    }

    /**
     * Iterates through children and omits parentheses.
     */
    private void omitParentheses() {
        this.children.get(a -> a.value.is("encOp")).forEach(a -> {
            a.encOperator = a.value;
            a.value = a.children.get(0).value;
            a.children = a.children.get(0).children;
        });

        this.children.forEach(ActionTree::omitParentheses);
    }

    private void applyNegativeSigns() {

        //factor out negative sign in multiplication
        for (int i = 0; i < this.children.size(); i++) {
            var a = this.children.get(i);
            if (a.value.equals("op", "\\cdot") && a.children.stream().anyMatch(a1 -> a1.value.equals("delOp", "-"))) {
                for (int j = 0; j < a.children.size(); j++) {
                    ActionTree child = a.children.get(j);
                    if (child.value.equals("delOp", "-")) {
                        if (this.value.equals("delOp", "-")) {
                            child.value = child.children.get(0).value;
                            child.children = child.children.get(0).children;
                            this.value = a.value;
                            this.children = a.children;
                        } else {
                            var newN = new ActionTree(new TokenPair("delOp", "-"));

                            child.value = child.children.get(0).value;
                            child.children = child.children.get(0).children;

                            newN.insert(a);
                            this.children.removeRange(i, 1);
                            this.children.add(i, newN);
                        }
                    }
                }
            }
        }

        //apply negative sign to numbers
        this.children.get(a -> a.value.equals("delOp", "-")).forEach(a -> {
            var child = a.children.get(0).value;
            if (child.is("num")) {
                double val = -Double.parseDouble(child.value());
                a.children.clear();
                a.value = new TokenPair(val);
            }
        });

        this.children.forEach(ActionTree::applyNegativeSigns);
    }

    private boolean applyOperatorIdentities(StringBuilder message) {
        AtomicBoolean changed = new AtomicBoolean();

        if (this.value.is("op")) {
            for (int i = 0; i < this.children.size(); i++) {
                if (this.value.equals("op", "\\cdot") && this.children.get(i).value.equalsNumber(1)) {
                    this.children.removeIndices(i);
                    message.append("In multiplication, 1 is ignored such that <mth-f> x \\cdot 1 = x </mth-f>");
                    changed.set(true);
                    break;
                } else if (this.value.equals("op", "\\cdot") && this.children.get(i).value.equalsNumber(0)) {
                    this.value = new TokenPair("num", "0.0");
                    this.children.clear();
                    message.append("When any number is multiplied by 0, the resulting product is always 0 such that <mth-f> x \\cdot 0 = 0 </mth-f>");
                    changed.set(true);
                    break;
                } else if (this.value.equals("op", "+") && this.children.get(i).value.equalsNumber(0)) {
                    this.children.removeIndices(i);
                    message.append("In addition, 0 is ignored such that <mth-f> x + 0 = x </mth-f>");
                    changed.set(true);
                    break;
                }
            }
        }

        this.children.forEach(a -> {
            if (!changed.get() && a.applyOperatorIdentities(message))
                changed.set(true);
        });

        return changed.get();
    }

    private boolean divisionsToFractions() {
        AtomicBoolean changed = new AtomicBoolean();

        if (this.value.equals("op", "\\cdot")) {
            var indexes = new ArrayList<Integer>();
            this.children.forI(i -> {
                var a = this.children.get(i);
                if (a.value.equals("delOp", "\\div"))
                    indexes.add(i);
            });

            if (!indexes.isEmpty()) {
                var fraction = new ActionTree(new TokenPair("struct", "\\frac"));

                var numerator = new ActionTree(new TokenPair("encOp", "{"));
                var numeratorMult = new ActionTree(new TokenPair("op", "\\cdot"));
                var denominator = new ActionTree(new TokenPair("encOp", "{"));
                var denominatorMult = new ActionTree(new TokenPair("op", "\\cdot"));

                for (int i = 0; i < this.children.size(); i++) {
                    if (!indexes.contains(i)) {
                        numeratorMult.insert(this.children.get(i));
                    } else {
                        denominatorMult.insert(this.children.get(i).children);
                    }
                }

                numerator.insert(numeratorMult);
                denominator.insert(denominatorMult);
                fraction.insert(numerator);
                fraction.insert(denominator);

                this.value = fraction.value;
                this.children = fraction.children;
                changed.set(true);
            }
        }

        this.children.forEach(a -> {
            if (!changed.get() && a.divisionsToFractions())
                changed.set(true);
        });

        return changed.get();
    }

    /**
     * Iterates through children and cancels any pairs of inverse numbers
     *
     * @return True if any inverse pairs were successfully found and cancelled.
     */
    private boolean simplifyFraction(StringBuilder message) {
        AtomicBoolean changed = new AtomicBoolean();

        for (int i = 0; i < this.children.size(); i++) {
            var a = this.children.get(i);
            if (a.value.equals("struct", "\\frac")) {
                var num = a.children.get(0);
                var den = a.children.get(1);
                if (den.value().is("num") && Double.parseDouble(den.value().value()) == 1) {
                    a.value = num.value();
                    a.children = num.children;

                    message.append("A denominator of 1 can be ignored in division such that <mth-f> \\frac{x}{1} = x </mth-f>");
                    changed.set(true);
                    break;
                } else if (num.value().equals("op", "\\cdot") && den.value().equals("op", "\\cdot")) {
                    ArrayList<Integer> ints = new ArrayList<>();
                    for (int j = 0; j < num.children.size(); j++) {
                        if (den.children.contains(num.children.get(j))) {
                            ints.add(j);
                        }
                    }

                    ArrayList<Integer> ints1 = new ArrayList<>();
                    for (int j = 0; j < den.children.size(); j++) {
                        if (num.children.contains(den.children.get(j))) {
                            ints1.add(j);
                        }
                    }

                    if (!ints.isEmpty() || !ints1.isEmpty()) {
                        num.children.removeIndices(ints);
                        den.children.removeIndices(ints1);

                        message.append("Cancel out inverse pairs using <mth-f>x \\cdot \\frac{1}{x} = 1</mth-f> rule.");
                        changed.set(true);
                        break;
                    }
                }
            }
        }

        this.children.forEach(a -> {
            if (!changed.get() && a.simplifyFraction(message))
                changed.set(true);
        });

        return changed.get();
    }

    private boolean executeOperators(StringBuilder message) {
        AtomicBoolean changed = new AtomicBoolean();

        if (this.value.is("op")) {
            Expression.Operator op = null;
            var nums = this.children.get(a -> a.value.is("num")).collect(Collectors.toCollection(ExtendedList::new));
            while (nums.size() > 1) {
                ActionTree child1 = null;
                ActionTree child2 = null;
                for (ActionTree at : nums) {
                    if (child1 == null)
                        child1 = at;
                    else if (child2 == null)
                        child2 = at;
                    else
                        break;
                }

                if (child1 == null || child2 == null)
                    break;
                else if (child1.value.is("num") && child2.value.is("num")) {
                    double val1 = Double.parseDouble(child1.value.value());
                    double val2 = Double.parseDouble(child2.value.value());

                    if (!this.children.remove(child1))
                        System.out.println("Child did not exist in list, so remove function is SHITE");
                    if (!this.children.remove(child2))
                        System.out.println("Child did not exist in list, so remove function is SHITE");

                    op = (Expression.Operator) Equation.EXPRESSIONS.get(this.value.value());
                    this.children.add(new ActionTree(new TokenPair(op.apply(val1, val2))));

                    if (this.children.size() == 1) {
                        this.value = this.children.get(0).value;
                        this.children.clear();
                        if (this.value.is("num") && !this.value.value().contains("-"))
                            this.encOperator = null;
                    }
                    nums = this.children.get(a -> a.value.is("num")).collect(Collectors.toCollection(ExtendedList::new));
                    changed.set(true);
                }
            }

            if (!changed.get()) {
                var non_nums = this.children.get(a -> !a.value.is("num")).collect(Collectors.toCollection(ExtendedList::new));
                int last1 = 0;
                int last2 = -1;
                while (non_nums.size() > last1) {
                    //4e+ pi + e
                    ActionTree child1 = null;
                    ActionTree child2 = null;

                    for (int i = last1; i < non_nums.size(); i++) {
                        if (child1 == null) {
                            child1 = non_nums.get(i);
                            last1 = i;
                        }
                        for (int j = last2 + 1; j < non_nums.size(); j++) {
                            if (j == last1) continue;
                            if (child2 == null) {
                                child2 = non_nums.get(j);
                                last2 = j;
                                if (j == non_nums.size() - 1) {
                                    last1++;
                                    last2 = 0;
                                }
                            }
                        }
                    }

                    if (child1 == null || child2 == null)
                        break;
                    else if (this.value().has("+")) {
                        if (compareProducts(child1, child2)) {
                            double val1 = Double.parseDouble(getProductValue(child1));
                            double val2 = Double.parseDouble(getProductValue(child2));

                            var at = new ActionTree(new TokenPair("op", "\\cdot"));
                            at.insert(new ActionTree(new TokenPair("num", "" + (val1 + val2))));
                            if (child1.value().equals("op", "\\cdot"))
                                child1.children.get(a -> !a.value.is("num")).forEach(at::insert);
                            else if (child2.value().equals("op", "\\cdot"))
                                child2.children.get(a -> !a.value.is("num")).forEach(at::insert);
                            else
                                at.insert(child1);

                            this.children.remove(child1);
                            this.children.remove(child2);
                            this.children.add(at);

                            if (this.children.size() == 1) {
                                this.value = this.children.get(0).value;
                                this.children = this.children.get(0).children;
                                if (this.value.is("num") && !this.value.value().contains("-"))
                                    this.encOperator = null;
                            }

                            non_nums = this.children.get(a -> !a.value.is("num")).collect(Collectors.toCollection(ExtendedList::new));
                            changed.set(true);
                            op = (Expression.Operator) Equation.EXPRESSIONS.get("+");
                        }
                    } else {
                        break;
                    }
                }
            }

            if (changed.get())
                message.append("Calculate the ").append(op.verbose());
        }

        this.children.forEach(a ->

        {
            if (!changed.get() && a.executeOperators(message))
                changed.set(true);
        });

        return changed.get();
    }

    private boolean executeTrigonometricFunctions(StringBuilder message) {
        AtomicBoolean changed = new AtomicBoolean();

        this.children.get(a -> a.value.is("trigFun")).forEach(a -> {
            if (!changed.get()) {
                Expression.TrigonometricFunction fun = ((Expression.TrigonometricFunction) Equation.EXPRESSIONS.get(a.value.value()));
                var input = a.children.get(0);
                input.execute((o1, o2) -> {
                });
                if (input.value.is("num")) {
                    Equation val = fun.execute(Double.parseDouble(input.value.value()));
                    a.children.clear();
                    a.value = val.getEquation().value();
                    a.children = val.getEquation().children;
                    changed.set(true);
                    message.append("Calculate the trigonometric ").append(fun.name()).append(" function");
                }
            }
        });

        this.children.forEach(a -> {
            if (!changed.get() && a.executeTrigonometricFunctions(message))
                changed.set(true);
        });

        return changed.get();
    }

    private boolean executeMathematicalConstruct(StringBuilder message) {
        AtomicBoolean changed = new AtomicBoolean();

        this.children.get(a -> a.value.is("struct")).forEach(a -> {
            if (!changed.get()) {

            }
        });

        this.children.forEach(a -> {
            if (!changed.get() && a.executeMathematicalConstruct(message))
                changed.set(true);
        });

        return changed.get();
    }

    /**
     * Iterates through children and converts constants to their numbers.
     *
     * @return True if any constants were successfully found and converted.
     */
    private boolean constToNum(StringBuilder message) {
        AtomicBoolean changed = new AtomicBoolean();
        List<String> conversions = new ArrayList<>();

        this.children.get(a -> (a.value.is("delOp") && a.children.get(0).value.is("const")) || a.value.is("const")).forEach(a -> {
            a = a.value.is("const") ? a : a.children.get(0);
            Expression.Constant cons = ((Expression.Constant) Equation.EXPRESSIONS.get(a.value.value()));
            changed.set(true);
            conversions.add(a.value.value());
            a.value = new TokenPair(cons.getValue());
        });

        if (conversions.size() > 0) {
            message.append("Convert constant").append(conversions.size() > 1 ? "s" : "");
            for (int i = 0; i < conversions.size(); i++) {
                String s = conversions.get(i);
                message.append(" <mth-f> ").append(s).append(" </mth-f> ");
                if (i + 1 < conversions.size()) {
                    message.append("and");
                }
            }
            message.append("to a number.");
        }

        this.children.forEach(a -> {
            if (!changed.get() && a.constToNum(message))
                changed.set(true);
        });

        return changed.get();
    }
}
