package net.congueror.calculator;

import net.congueror.calculator.structure.ActionTree;

public class OperationStep {
    private final ActionTree step;
    private final String message;
    private String prefix = "";
    private boolean hasConstants;

    public OperationStep(ActionTree step, String message) {
        this.step = step;
        this.message = message;

        if (step.findAny("const")) {
            hasConstants = true;
        }
    }

    public OperationStep(ActionTree step, String message, String prefix) {
        this(step, message);
        this.prefix = prefix;
    }

    public ActionTree step() {
        return step;
    }

    public String message() {
        return message;
    }

    public String prefix() {
        return prefix;
    }
}
