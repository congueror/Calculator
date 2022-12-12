package net.congueror.calculator;

import net.congueror.calculator.structure.ActionTree;
import net.congueror.calculator.structure.TokenPair;

public class OperationStep {
    private final ActionTree step;
    private final String message;
    private final TokenPair prefix;

    public OperationStep(ActionTree step, String message) {
        this(step, message, new TokenPair("", ""));
    }

    public OperationStep(ActionTree step, String message, TokenPair prefix) {
        this.step = step;
        this.message = message;
        this.prefix = prefix;
    }

    public ActionTree step() {
        return step;
    }

    public String message() {
        return message;
    }

    public TokenPair prefix() {
        return prefix;
    }
}
