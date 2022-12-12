package net.congueror.calculator;

import net.congueror.calculator.structure.ActionTree;
import net.congueror.calculator.structure.ExtendedList;
import net.congueror.calculator.structure.TokenPair;

import java.util.HashMap;
import java.util.Map;

public class Equation {

    public static final Map<String, Expression> EXPRESSIONS = new HashMap<>();

    private final ExtendedList<TokenPair> tokens = new ExtendedList<>();
    private final ActionTree root;
    private boolean comparing;
    private boolean hasVars;

    public Equation(String equation) {
        this.tokenize(equation);

        this.root = new ActionTree(new TokenPair("root", "root"));
        this.parse(this.tokens, this.root, new ExtendedList<>());

        System.out.println();
        this.root.print();
    }

    private void tokenize(String equation) {
        char[] chars = equation.toCharArray();
        final String funChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ()|{}\\";
        final String numChars = "1234567890";
        int pos = 0;
        int absDepth = 0;
        int parenthesisDepth = 0;
        int curliesDepth = 0;
        System.out.println(equation);
        while (pos < chars.length) {
            StringBuilder value = new StringBuilder();

            if (chars[pos] == '\\' && pos + 1 < chars.length) {
                if (chars[pos + 1] == ' ') {
                    pos += 2;
                    continue;
                }

                while (funChars.contains(Character.toString(chars[pos]))) {
                    value.append(chars[pos]);
                    switch (chars[pos]) {
                        case '(' -> parenthesisDepth++;
                        case ')' -> {
                            if (parenthesisDepth > 0)
                                parenthesisDepth--;
                            else
                                throw new ArithmeticException("Found unexpected )");
                        }
                        case '|' -> {
                            if (value.toString().equals("\\left|"))
                                absDepth++;
                            else if (value.toString().equals("\\right|")) {
                                if (absDepth > 0)
                                    absDepth--;
                                else
                                    throw new ArithmeticException("Found unexpected closing |");
                            }
                        }
                        case '{' -> curliesDepth++;
                        case '}' -> {
                            if (curliesDepth > 0)
                                curliesDepth--;
                            else
                                throw new ArithmeticException("Found unexpected }");
                        }
                    }
                    pos++;
                    if (pos != chars.length) {
                        if (chars[pos] == '\\' || chars[pos] == '{' || chars[pos] == '}')
                            break;
                    } else
                        break;
                }

                Expression op = EXPRESSIONS.get(value.toString());
                if (op != null)
                    tokens.add(new TokenPair(op.type, value.toString()));
                else
                    throw new ArithmeticException("Cannot recognize expression " + value);
            } else if (numChars.contains(Character.toString(chars[pos]))) {
                while (numChars.contains(Character.toString(chars[pos])) ||
                        (value.length() > 0 && chars[pos] == '.')) {
                    value.append(chars[pos]);
                    pos++;
                    if (pos == chars.length) {
                        break;
                    }
                }

                tokens.add(new TokenPair("num", value.toString()));
            } else if (EXPRESSIONS.containsKey(Character.toString(chars[pos]))) {
                tokens.add(new TokenPair(EXPRESSIONS.get(Character.toString(chars[pos])).type, Character.toString(chars[pos])));
                pos++;
            } else if (funChars.contains(Character.toString(chars[pos]))) {
                tokens.add(new TokenPair("var", Character.toString(chars[pos])));
                hasVars = true;
                pos++;
            } else if (chars[pos] == ' ') {
                pos++;
            } else {
                throw new ArithmeticException("Invalid character, " + chars[pos]);
            }
        }

        if (parenthesisDepth != 0) {
            throw new ArithmeticException("Expected ), but never found.");
        }
        if (absDepth != 0) {
            throw new ArithmeticException("Expected opening |, but never found.");
        }

        tokens.forEach(System.out::println);
    }

    private void parse(ExtendedList<TokenPair> tokens, ActionTree root, ExtendedList<ActionTree> nodes) {

        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).is("encOp")) {
                Expression.EncapsulationOperator encOp = ((Expression.EncapsulationOperator) EXPRESSIONS.get(tokens.get(i).value()));
                if (!encOp.isLeft() && i + 1 < tokens.size() && tokens.get(i + 1).is("trigFun")) {
                    tokens.add(i + 1, new TokenPair("op", "\\cdot"));
                    i = 0;
                }
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).is("encOp")) {
                Expression.EncapsulationOperator encOp = ((Expression.EncapsulationOperator) EXPRESSIONS.get(tokens.get(i).value()));
                if (encOp.isLeft()) {
                    final int startIndex = i;
                    ActionTree child = new ActionTree(tokens.get(i));
                    ExtendedList<TokenPair> subTokens = new ExtendedList<>();
                    int parDepth = 0;
                    i++;
                    while (i < tokens.size() && (parDepth > 0 || !tokens.get(i).equals(encOp.counterpart()))) {
                        if (tokens.get(i).equals(tokens.get(startIndex))) {
                            parDepth++;
                        } else if (tokens.get(i).equals(encOp.counterpart())) {
                            parDepth--;
                        }
                        subTokens.add(tokens.get(i));
                        i++;
                    }

                    this.parse(subTokens, child, nodes);

                    nodes.add(child);
                    tokens.removeRange(startIndex, i - startIndex + 1);
                    tokens.add(startIndex, new TokenPair("node", "node" + (nodes.size() - 1)));
                    i = 0;
                }
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals("op", "+") && (i - 1 < 0 || tokens.get(i - 1).is("op"))) {
                tokens.remove(i);
                i = 0;
            } else if (tokens.get(i).isOr("num", "const", "var")) {
                if (i - 1 >= 0 && tokens.get(i - 1).isOr("const", "encOp", "node", "var")) {
                    tokens.add(i, new TokenPair("op", "\\cdot"));
                    i = 0;
                } else if (i + 1 < tokens.size() && tokens.get(i + 1).isOr("const", "encOp", "node", "trigFun", "struct", "var")) {
                    tokens.add(i + 1, new TokenPair("op", "\\cdot"));
                    i = 0;
                }
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).is("struct")) {
                Expression.Construct struct = ((Expression.Construct) EXPRESSIONS.get(tokens.get(i).value()));
                var child = new ActionTree(tokens.get(i));
                int length = struct.inputs();
                for (int j = 1; j <= length; j++) {
                    var token = tokens.get(i + j);

                    ActionTree at;
                    if (token.is("node"))
                        at = nodes.get(Integer.parseInt(token.value().substring(4)));
                    else
                        at = new ActionTree(token);

                    child.insert(at);
                }

                nodes.add(child);

                tokens.removeRange(i, length + 1);
                tokens.add(i, new TokenPair("node", "node" + (nodes.size() - 1)));
                i = 0;
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).is("trigFun")) {
                var child = new ActionTree(tokens.get(i));
                var token = tokens.get(i + 1);

                ActionTree at;

                if (token.is("node"))
                    at = nodes.get(Integer.parseInt(token.value().substring(4)));
                else
                    at = new ActionTree(token);

                child.insert(at);
                nodes.add(child);

                tokens.removeRange(i, 2);
                tokens.add(i, new TokenPair("node", "node" + (nodes.size() - 1)));
                i = 0;
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals("delOp", "-")) {
                var child = new ActionTree(tokens.get(i));
                var pair = tokens.get(i + 1);

                ActionTree at;

                if (pair.is("node"))
                    at = nodes.get(Integer.parseInt(pair.value().substring(4)));
                else
                    at = new ActionTree(pair);

                child.insert(at);
                nodes.add(child);

                tokens.removeRange(i, 2);
                if (!(i - 1 < 0)) {
                    tokens.add(i, new TokenPair("op", "+"));
                    tokens.add(i + 1, new TokenPair("node", "node" + (nodes.size() - 1)));
                } else
                    tokens.add(i, new TokenPair("node", "node" + (nodes.size() - 1)));
                i = 0;
            } else if (tokens.get(i).equals("delOp", "\\div")) {
                var child = new ActionTree(tokens.get(i));
                var pair = tokens.get(i + 1);

                ActionTree at;

                if (pair.is("node"))
                    at = nodes.get(Integer.parseInt(pair.value().substring(4)));
                else
                    at = new ActionTree(pair);

                child.insert(at);
                nodes.add(child);

                tokens.removeRange(i, 2);
                tokens.add(i, new TokenPair("op", "\\cdot"));
                tokens.add(i + 1, new TokenPair("node", "node" + (nodes.size() - 1)));
                i = 0;
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals("op", "\\cdot")) {
                ActionTree child = new ActionTree(tokens.get(i));

                var pair1 = tokens.get(i - 1);
                var pair2 = tokens.get(i + 1);

                ActionTree at1;
                ActionTree at2;

                if (pair1.is("node"))
                    at1 = nodes.get(Integer.parseInt(pair1.value().substring(4)));
                else
                    at1 = new ActionTree(pair1);

                if (pair2.is("node"))
                    at2 = nodes.get(Integer.parseInt(pair2.value().substring(4)));
                else
                    at2 = new ActionTree(pair2);

                if (at1.value().equals(tokens.get(i))) {
                    at1.insert(at2);
                    tokens.removeRange(i, 2);
                } else if (at2.value().equals(tokens.get(i))) {
                    at2.insert(at1);
                    tokens.removeRange(i - 1, 2);
                } else {
                    child.insert(at1);
                    child.insert(at2);
                    nodes.add(child);

                    tokens.removeRange(i - 1, 3);
                    tokens.add(i - 1, new TokenPair("node", "node" + (nodes.size() - 1)));
                }
                i = 0;
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals("op", "+")) {
                ActionTree child = new ActionTree(tokens.get(i));

                var pair1 = tokens.get(i - 1);
                var pair2 = tokens.get(i + 1);

                ActionTree at1;
                ActionTree at2;

                if (pair1.is("node"))
                    at1 = nodes.get(Integer.parseInt(pair1.value().substring(4)));
                else
                    at1 = new ActionTree(pair1);

                if (pair2.is("node"))
                    at2 = nodes.get(Integer.parseInt(pair2.value().substring(4)));
                else
                    at2 = new ActionTree(pair2);

                if (at1.value().equals(tokens.get(i))) {
                    at1.insert(at2);
                    tokens.removeRange(i, 2);
                } else if (at2.value().equals(tokens.get(i))) {
                    at2.insert(at1);
                    tokens.removeRange(i - 1, 2);
                } else {
                    child.insert(at1);
                    child.insert(at2);
                    nodes.add(child);

                    tokens.removeRange(i - 1, 3);
                    tokens.add(i - 1, new TokenPair("node", "node" + (nodes.size() - 1)));
                }
                i = 0;
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).is("comparison")) {
                if (comparing)
                    throw new ArithmeticException("Cannot have more than 1 comparison operator");
                comparing = true;
                try {
                    ActionTree child = new ActionTree(tokens.get(i));

                    var pair1 = tokens.get(i - 1);
                    var pair2 = tokens.get(i + 1);

                    ActionTree at1;
                    ActionTree at2;

                    if (pair1.is("node"))
                        at1 = nodes.get(Integer.parseInt(pair1.value().substring(4)));
                    else
                        at1 = new ActionTree(pair1);

                    if (pair2.is("node"))
                        at2 = nodes.get(Integer.parseInt(pair2.value().substring(4)));
                    else
                        at2 = new ActionTree(pair2);

                    child.insert(at1);
                    child.insert(at2);
                    nodes.add(child);

                    tokens.removeRange(i - 1, 3);
                    tokens.add(i - 1, new TokenPair("node", "node" + (nodes.size() - 1)));
                } catch (IndexOutOfBoundsException e) {
                    comparing = false;
                    tokens.remove(i);
                }
            }
        }

        for (TokenPair token : tokens) {
            ActionTree at;

            if (token.is("node"))
                at = nodes.get(Integer.parseInt(token.value().substring(4)));
            else
                at = new ActionTree(token);

            root.insert(at);
        }
    }

    public ActionTree getEquation() {
        return root.getChild();
    }

    public ExtendedList<EquationActions> getActions() {
        var a = new ExtendedList<EquationActions>();
        if (comparing && hasVars)
            a.add(EquationActions.SOLVE);
        else if (comparing)
            a.add(EquationActions.COMPARE);
        else
            a.add(EquationActions.SIMPLIFY);
        return a;
    }

    public ExtendedList<OperationStep> simplifyExpression() {
        ExtendedList<OperationStep> steps = new ExtendedList<>();

        root.simplifyExpression(root, steps);

        if (!steps.last().prefix().isEmpty()) {
            var beginning = new ActionTree(steps.last().prefix());
            beginning.insert(steps.get(0).step().getChild());
            beginning.insert(steps.last().step().getChild());
            steps.add(0, new OperationStep(beginning, ""));

            steps.remove(1);
        }

        return steps;
    }

    public ExtendedList<OperationStep> compareExpression() {
        ExtendedList<OperationStep> steps = new ExtendedList<>();

        root.compareExpression(root, steps);

        var beginning = new ActionTree(steps.last().prefix());
        beginning.insert(steps.get(0).step().getChild());
        beginning.insert(steps.last().step().getChild());
        steps.add(0, new OperationStep(beginning, ""));

        steps.remove(1);

        return steps;
    }

    public ExtendedList<OperationStep> solveEquation() {
        ExtendedList<OperationStep> steps = new ExtendedList<>();

        root.solveEquation(root, steps);

        var beginning = new ActionTree(steps.last().prefix());
        beginning.insert(steps.get(0).step().getChild());
        beginning.insert(steps.last().step().getChild());
        steps.add(0, new OperationStep(beginning, ""));

        steps.remove(1);

        return steps;
    }

    enum EquationActions {
        SIMPLIFY,
        COMPARE,
        SOLVE
    }
}
