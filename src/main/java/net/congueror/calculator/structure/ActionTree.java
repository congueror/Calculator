package net.congueror.calculator.structure;

import com.google.common.collect.HashMultiset;
import net.congueror.calculator.Equation;
import net.congueror.calculator.Expression;
import net.congueror.calculator.OperationStep;
import net.congueror.calculator.helpers.GuavaHelper;
import net.congueror.calculator.helpers.MathHelper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ActionTree implements Cloneable {
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");
    private static final double precision = 1e14;
    private int depth;
    private TokenPair encOperator = null;
    private TokenPair value;
    private ExtendedList<ActionTree> children = new ExtendedList<>();
    private boolean formatNum = true;

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
                return GuavaHelper.ofMultiset(tree1).equals(tree2.children.get(a -> !a.value.is("num")).collect(GuavaHelper.toHashMultiset()));
            if (!tree2.value().equals("op", "\\cdot"))
                return tree1.children.get(a -> !a.value.is("num")).collect(GuavaHelper.toHashMultiset()).equals(GuavaHelper.ofMultiset(tree2));
            var set1 = tree1.children.get(a -> !a.value.is("num")).collect(GuavaHelper.toHashMultiset());
            var set2 = tree2.children.get(a -> !a.value.is("num")).collect(GuavaHelper.toHashMultiset());
            return !set1.isEmpty() && !set2.isEmpty() && set1.equals(set2);
        }
        return tree1.equals(tree2);
    }

    private static String getProductValue(ActionTree tree) {
        if (tree.value().equals("op", "\\cdot"))
            return tree.children.get(a -> a.value.is("num")).map(a -> a.value().value()).collect(ExtendedList.toList()).getOr(0, "1.0");
        return "1.0";
    }

    private static void tryRemoveParent(ActionTree tree) {
        if (tree.children.size() == 1) {
            tree.value = tree.children.get(0).value;
            tree.children = tree.children.get(0).children;
            if (tree.value.is("num") && !tree.value.value().contains("-") && (tree.encOperator == null || tree.encOperator.has("(")))
                tree.encOperator = null;
        }
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
            ltx.append(formatNum ? FORMAT.format(Double.parseDouble(this.value.value())) : this.value.value());
        } else if (this.value.is("var")) {
            ltx.append(this.value.value());
        } else if (this.value.is("text")) {
            ltx.append("\\text{").append(this.value().value()).append("}");
        } else
            Equation.EXPRESSIONS.get(this.value().value()).toLatex(ltx, this.value, this.children);


        if (op != null)
            ltx.append(op.counterpart().value());

        return ltx.toString();
    }

    public void simplifyExpression(ActionTree root, ExtendedList<OperationStep> steps) {
        if (this.value.is("root")) {
            ActionTree rootCl = root._clone();
            rootCl.omitParentheses();
            rootCl.applyNegativeSigns();
            steps.add(new OperationStep(rootCl, ""));
            this.children.get(0).simplifyExpression(root, steps);
            return;
        }

        root.simplify((a, b) -> {
            String prefix = "=";
            if (a.getChild().value().is("num")) {
                prefix = "\\approx";
                formatNum = false;
            }
            steps.add(new OperationStep(a._clone(), b, new TokenPair("comparison", prefix)));
        });
    }

    public void compareExpression(ActionTree root, ExtendedList<OperationStep> steps) {
        if (this.value.is("root")) {
            ActionTree rootCl = root._clone();
            rootCl.omitParentheses();
            rootCl.applyNegativeSigns();
            steps.add(new OperationStep(rootCl, ""));
            this.children.get(0).compareExpression(root, steps);
            return;
        }

        root.simplify((a, b) -> steps.add(new OperationStep(a._clone(), b, new TokenPair("logic", "\\implies"))));
        root.convertComparison();
        steps.add(new OperationStep(root._clone(), "", new TokenPair("logic", "\\implies")));
    }

    public void solveEquation(ActionTree root, ExtendedList<OperationStep> steps) {
        if (this.value.is("root")) {
            ActionTree rootCl = root._clone();
            rootCl.omitParentheses();
            rootCl.applyNegativeSigns();
            steps.add(new OperationStep(rootCl, ""));
            this.children.get(0).compareExpression(root, steps);
            return;
        }

        root.simplify((a, b) -> steps.add(new OperationStep(a._clone(), b, new TokenPair("logic", "\\implies"))));
        root.solve((a, b) -> steps.add(new OperationStep(a._clone(), b, new TokenPair("logic", "\\implies"))));
        steps.add(new OperationStep(root._clone(), "", new TokenPair("logic", "\\implies")));
    }

    private void simplify(BiConsumer<ActionTree, String> onChange) { // \frac{5}{4}\cdot \frac{7}{9}+\frac{7}{6}
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
            if (executeNumOperators(message)) {
                omitParentheses();
                applyNegativeSigns();
                onChange.accept(this, message.toString());
                i = -1;
                continue;
            }

            message = new StringBuilder();
            if (executeNonNumOperators(message)) {
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
            if (executeConstruct(message)) {
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

    private void solve(BiConsumer<ActionTree, String> onChange) {
        StringBuilder message = new StringBuilder();
        omitParentheses();
        applyNegativeSigns();
        for (int i = 0; i < 1; i++) {

        }
    }

    private void convertComparison() {
        if (this.value().is("comparison")) {
            if (this.children.get(0).value().is("num") && this.children.get(1).value().is("num")) {
                var d1 = Double.parseDouble(this.children.get(0).value().value());
                var d2 = Double.parseDouble(this.children.get(1).value().value());
                Expression.ComparisonOperator op = ((Expression.ComparisonOperator) Equation.EXPRESSIONS.get(this.value().value()));
                if (op.execute(d1, d2)) {
                    this.value = new TokenPair("text", "True");
                    this.children.clear();
                    this.encOperator = new TokenPair("encOp", "{");
                } else {
                    this.value = new TokenPair("text", "False");
                    this.children.clear();
                    this.encOperator = new TokenPair("encOp", "{");
                }
            }
        } else
            this.children.forEach(ActionTree::convertComparison);
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

    /**
     * Factors out negative signs from multiplication and applies negative sign to numbers.
     */
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
                a.encOperator = null;
            }
        });

        this.children.forEach(ActionTree::applyNegativeSigns);
    }

    /**
     * Executes operator identities: x * 1 = x, x * 0 = 0, x + 0 = x
     */
    private boolean applyOperatorIdentities(StringBuilder message) {
        AtomicBoolean changed = new AtomicBoolean();

        if (this.value.is("op")) {
            for (int i = 0; i < this.children.size(); i++) {
                if (this.value.equals("op", "\\cdot") && this.children.get(i).value.equalsNumber(1)) {
                    this.children.removeIndices(i);
                    tryRemoveParent(this);
                    message.append("In multiplication, 1 is ignored such that <mth-f> x \\cdot 1 = x </mth-f>");
                    changed.set(true);
                    break;
                } else if (this.value.equals("op", "\\cdot") && this.children.get(i).value.equalsNumber(0)) {
                    this.value = new TokenPair("num", "0.0");
                    this.children.clear();
                    tryRemoveParent(this);
                    message.append("When any number is multiplied by 0, the resulting product is always 0 such that <mth-f> x \\cdot 0 = 0 </mth-f>");
                    changed.set(true);
                    break;
                } else if (this.value.equals("op", "+") && this.children.get(i).value.equalsNumber(0)) {
                    this.children.removeIndices(i);
                    tryRemoveParent(this);
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

    /**
     * Converts \\div operator to fractions
     */
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
     * Omits fraction denominated with 1      &#x09; &#x09;     x/1 = x <br>
     * Cancels any pairs of inverse numbers    &#x09;           x*(1/x) = 1 <br>
     * Long Division (Means and extremes)    &#x09;       (a/b)/(c/d) = ad/bc <br>
     * Simplify with GCD      &#x09;&#x09;                  x/y = (x * gcd(x, y)) / (y * gcd(x, y)) <br>
     */
    private boolean simplifyFraction(StringBuilder message) {
        AtomicBoolean changed = new AtomicBoolean();

        for (int i = 0; i < this.children.size(); i++) {
            var a = this.children.get(i);
            if (a.value().equals("struct", "\\frac")) {
                var num = a.children.get(0);
                var den = a.children.get(1);
                if (den.value().is("num") && Double.parseDouble(den.value().value()) == 1) {
                    a.value = num.value();
                    a.children = num.children;

                    message.append("A denominator of 1 can be ignored in division such that <mth-f> \\frac{x}{1} = x </mth-f>");
                    changed.set(true);
                    break;
                } else if (num.value().has("\\frac") || den.value().has("\\frac")) {
                    ActionTree num1;
                    ActionTree den1;
                    ActionTree num2;
                    ActionTree den2;

                    if (!num.value().has("\\frac")) {
                        num1 = num;
                        den1 = new ActionTree(new TokenPair("num", "1.0"));
                        num2 = den.children.get(0);
                        den2 = den.children.get(1);
                    } else if (!den.value().has("\\frac")) {
                        num1 = num.children.get(0);
                        den1 = num.children.get(1);
                        num2 = den;
                        den2 = new ActionTree(new TokenPair("num", "1.0"));
                    } else {
                        num1 = num.children.get(0);
                        den1 = num.children.get(1);
                        num2 = den.children.get(0);
                        den2 = den.children.get(1);
                    }

                    var newFrac = new ActionTree(new TokenPair("struct", "\\frac"));
                    var newNum = new ActionTree(new TokenPair("op", "\\cdot"));
                    newNum.encOperator = num.encOperator;
                    newNum.insert(num1);
                    newNum.insert(den2);
                    var newDen = new ActionTree(new TokenPair("op", "\\cdot"));
                    newDen.encOperator = num.encOperator;
                    newDen.insert(den1);
                    newDen.insert(num2);
                    newFrac.insert(newNum);
                    newFrac.insert(newDen);

                    this.value = newFrac.value();
                    this.children = newFrac.children;

                    message.append("Simplify long division using <mth-f>\\frac{ \\frac{a}{b} }{ \\frac{c}{d} } = \\frac{ a \\cdot d }{ b \\cdot c }</mth-f> rule.");
                    changed.set(true);
                    break;
                } else if (num.value().equals("op", "\\cdot") && den.value().equals("op", "\\cdot")) {
                    var numerator = GuavaHelper.ofMultiset(num.children.toArray(ActionTree[]::new));
                    var denominator = GuavaHelper.ofMultiset(den.children.toArray(ActionTree[]::new));
                    var crossed = numerator.stream().filter(denominator::contains).collect(GuavaHelper.toHashMultiset());
                    for (ActionTree b : crossed) {
                        num.children.remove(b);
                        den.children.remove(b);
                        message.append("Cancel out inverse pairs using <mth-f>x \\cdot \\frac{1}{x} = 1</mth-f> rule.");
                        changed.set(true);
                        break;
                    }

                    if (!changed.get()) {
                        var nums1 = num.children.get(at -> at.value().is("num")).toList();
                        var nums2 = den.children.get(at -> at.value().is("num")).toList();
                        if (nums1.size() == 1 && nums2.size() == 1) {
                            var num1 = nums1.get(0);
                            var num2 = nums2.get(0);

                            double d1 = Double.parseDouble(num1.value().value());
                            double d2 = Double.parseDouble(num2.value().value());
                            double gcd = MathHelper.gcd(d1, d2);
                            if (gcd != 1) {
                                num1.value = new TokenPair("num", "" + (d1 / gcd));
                                num2.value = new TokenPair("num", "" + (d2 / gcd));
                                message.append("Simplify by dividing the numbers ").append(d1).append(" and ").append(d2).append(" by their GCD(Greatest Common Divisor).");
                                changed.set(true);
                            }
                        }
                    }
                } else if (num.value().equals("op", "\\cdot") || den.value().equals("op", "\\cdot")) {
                    ActionTree root = null;
                    ActionTree opposite = null;

                    if (!num.value().equals("op", "\\cdot")) {
                        root = num;
                        opposite = den;
                    } else if (!den.value().equals("op", "\\cdot")) {
                        root = den;
                        opposite = num;
                    }

                    HashMultiset<ActionTree> set = GuavaHelper.ofMultiset(root);
                    for (ActionTree b : opposite.children.get(set::contains).toList()) {
                        root.value = new TokenPair("num", "1.0");
                        root.children.clear();
                        opposite.children.remove(b);
                        message.append("Cancel out inverse pairs using <mth-f>x \\cdot \\frac{1}{x} = 1</mth-f> rule.");
                        changed.set(true);
                        break;
                    }

                    if (!changed.get()) {
                        var nums = opposite.children.get(at -> at.value().is("num")).toList();
                        if (root.value().is("num") && nums.size() == 1) {
                            var number = nums.get(0);

                            double d1 = Double.parseDouble(root.value().value());
                            double d2 = Double.parseDouble(number.value().value());
                            double gcd = MathHelper.gcd(d1, d2);
                            if (gcd != 1) {
                                root.value = new TokenPair("num", "" + (d1 / gcd));
                                number.value = new TokenPair("num", "" + (d2 / gcd));
                                message.append("Simplify by dividing the numbers ").append(d1).append(" and ").append(d2).append(" by their GCD(Greatest Common Divisor).");
                                changed.set(true);
                            }
                        }
                    }
                } else if (num.equals(den)) {
                    a.value = new TokenPair("num", "1.0");
                    a.children.clear();
                    message.append("Cancel out inverse pairs using <mth-f>x \\cdot \\frac{1}{x} = 1</mth-f> rule.");
                    changed.set(true);
                } else if (num.value().is("num") && den.value().is("num")) {
                    double d1 = Double.parseDouble(num.value().value());
                    double d2 = Double.parseDouble(den.value().value());
                    double gcd = MathHelper.gcd(d1, d2);
                    if (gcd != 1) {
                        num.value = new TokenPair("num", "" + (d1 / gcd));
                        den.value = new TokenPair("num", "" + (d2 / gcd));
                        message.append("Simplify by dividing the numbers ").append(d1).append(" and ").append(d2).append(" by their GCD(Greatest Common Divisor).");
                        changed.set(true);
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

    /**
     * Executes operators for numbers. <br>
     */
    private boolean executeNumOperators(StringBuilder message) {
        AtomicBoolean changed = new AtomicBoolean();

        if (this.value.is("op")) {
            String msg = "";

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

                if (child1 == null || child2 == null) {
                    break;
                } else if (child1.value.is("num") && child2.value.is("num")) {
                    double val1 = Double.parseDouble(child1.value.value());
                    double val2 = Double.parseDouble(child2.value.value());

                    Expression.Operator op = (Expression.Operator) Equation.EXPRESSIONS.get(this.value.value());
                    this.children.add(new ActionTree(new TokenPair(op.apply(val1, val2))));
                    this.children.remove(child1);
                    this.children.remove(child2);

                    tryRemoveParent(this);
                    nums = this.children.get(a -> a.value.is("num")).collect(Collectors.toCollection(ExtendedList::new));
                    changed.set(true);
                    msg = "Calculate the " + op.verbose() + ".";
                }
            }

            if (changed.get())
                message.append(msg);
        }

        this.children.forEach(a -> {
            if (!changed.get() && a.executeNumOperators(message))
                changed.set(true);
        });

        return changed.get();
    }

    /**
     * Addition for common factors       &#x09; &#x09;     ax + bx = (a + b)x <br>
     * Fraction Addition using LCD         &#x09; &#x09;      a/b + c/d = (ad+cb)/bd <br>
     * Fraction Multiplication
     */
    private boolean executeNonNumOperators(StringBuilder message) {
        AtomicBoolean changed = new AtomicBoolean();


        if (this.value.is("op")) {
            String msg = "";

            ActionTree child1 = null;
            ActionTree child2 = null;
            var non_nums = this.children.get(a -> !a.value.is("num")).collect(Collectors.toCollection(ExtendedList::new));
            int last1 = 0;
            int last2 = -1;
            while (non_nums.size() > last1) {

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

                if (child1 == null || child2 == null) {
                    break;
                } else if (this.value().has("+")) {
                    if (child1.value().equals("struct", "\\frac") && child2.value().equals("struct", "\\frac")) {
                        var a = child1.children.get(0);
                        var b = child1.children.get(1);
                        var c = child2.children.get(0);
                        var d = child2.children.get(1);

                        var newFrac = new ActionTree(new TokenPair("struct", "\\frac"));

                        var newNum = new ActionTree(new TokenPair("op", "+"));
                        var ad = new ActionTree(new TokenPair("op", "\\cdot"));
                        ad.insert(a);
                        ad.insert(d);
                        var cb = new ActionTree(new TokenPair("op", "\\cdot"));
                        cb.insert(c);
                        cb.insert(b);
                        newNum.insert(ad);
                        newNum.insert(cb);
                        newNum.encOperator = a.encOperator;

                        var newDen = new ActionTree(new TokenPair("op", "\\cdot"));
                        newDen.insert(b);
                        newDen.insert(d);
                        newDen.encOperator = b.encOperator;

                        newFrac.insert(newNum);
                        newFrac.insert(newDen);

                        this.children.remove(child1);
                        this.children.remove(child2);
                        this.children.add(newFrac);

                        tryRemoveParent(this);

                        non_nums = this.children.get(at -> !at.value.is("num")).collect(Collectors.toCollection(ExtendedList::new));
                        changed.set(true);
                        msg = "Find Least Common Denominator (LCD) and rewrite the fraction sum using <mth-f> \\frac{a}{b} + \\frac{c}{d} = \\frac{a \\cdot d + c \\cdot b}{b \\cdot d} </mth-f> rule.";
                    } else if (compareProducts(child1, child2)) {
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

                        tryRemoveParent(this);

                        non_nums = this.children.get(a -> !a.value.is("num")).collect(Collectors.toCollection(ExtendedList::new));
                        changed.set(true);
                        msg = "Add the coefficients of the common factors.";
                    } else {
                        break;
                    }
                } else if (this.value().has("\\cdot")) {
                    if (child1.value().equals("struct", "\\frac") && child2.value().equals("struct", "\\frac")) {
                        var a = child1.children.get(0);
                        var b = child1.children.get(1);
                        var c = child2.children.get(0);
                        var d = child2.children.get(1);

                        var newFrac = new ActionTree(new TokenPair("struct", "\\frac"));
                        var newNum = new ActionTree(new TokenPair("op", "\\cdot"));
                        newNum.insert(a);
                        newNum.insert(c);
                        newNum.encOperator = a.encOperator;
                        var newDen = new ActionTree(new TokenPair("op", "\\cdot"));
                        newDen.insert(b);
                        newDen.insert(d);
                        newDen.encOperator = b.encOperator;

                        newFrac.insert(newNum);
                        newFrac.insert(newDen);

                        this.children.remove(child1);
                        this.children.remove(child2);
                        this.children.add(newFrac);

                        tryRemoveParent(this);
                        non_nums = this.children.get(at -> !at.value.is("num")).collect(Collectors.toCollection(ExtendedList::new));
                        changed.set(true);
                        msg = "Calculate the product of the fractions.";
                    } else if (child1.value().equals("struct", "\\frac") && child2.value().equals("struct", "\\frac")) {
                        ActionTree a, b, c;
                        if (child1.value().equals("struct", "\\frac")) {
                            a = child1.children.get(0);
                            b = child1.children.get(1);
                            c = child2;
                        } else {
                            a = child2.children.get(0);
                            b = child2.children.get(1);
                            c = child1;
                        }

                        var newFrac = new ActionTree(new TokenPair("struct", "\\frac"));
                        var newNum = new ActionTree(new TokenPair("op", "\\cdot"));
                        newNum.insert(a);
                        newNum.insert(c);
                        newNum.encOperator = a.encOperator;

                        newFrac.insert(newNum);
                        newFrac.insert(b);

                        this.children.remove(child1);
                        this.children.remove(child2);
                        this.children.add(newFrac);

                        tryRemoveParent(this);
                        non_nums = this.children.get(at -> !at.value.is("num")).collect(Collectors.toCollection(ExtendedList::new));
                        changed.set(true);
                        msg = "Calculate the product of the fractions.";
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }

            if (changed.get())
                message.append(msg);
        }

        this.children.forEach(a -> {
            if (!changed.get() && a.executeNonNumOperators(message))
                changed.set(true);
        });

        return changed.get();
    }


    /**
     * Executes trigonometric functions.
     */
    private boolean executeTrigonometricFunctions(StringBuilder message) {
        AtomicBoolean changed = new AtomicBoolean();

        this.children.get(a -> a.value.is("trigFun")).forEach(a -> {
            if (!changed.get()) {
                Expression.TrigonometricFunction fun = ((Expression.TrigonometricFunction) Equation.EXPRESSIONS.get(a.value.value()));
                var input = a.children.get(0);
                input.simplify((o1, o2) -> {
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

    /**
     * TODO
     */
    private boolean executeConstruct(StringBuilder message) {
        AtomicBoolean changed = new AtomicBoolean();

        this.children.get(a -> a.value.is("struct")).forEach(a -> {
            if (!changed.get()) {
                Expression.Construct struct = ((Expression.Construct) Equation.EXPRESSIONS.get(a.value.value()));
                ActionTree[] inputs = a.children.toArray(ActionTree[]::new);
                ActionTree result = struct.execute(inputs);
                if (result != null) {
                    a.children.clear();
                    a.value = result.value();
                    a.children = result.children;
                    changed.set(true);
                    message.append("Calculate the ").append(struct.verbose()).append(".");
                }

            }
        });

        this.children.forEach(a -> {
            if (!changed.get() && a.executeConstruct(message))
                changed.set(true);
        });

        return changed.get();
    }

    /**
     * Converts constants to numbers
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
