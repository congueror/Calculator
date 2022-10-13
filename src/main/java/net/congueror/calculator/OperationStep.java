package net.congueror.calculator;

import net.congueror.calculator.structure.ActionTree;

public class OperationStep {
    private final ActionTree step;
    private final String message;
    private boolean hasConstants;

    public OperationStep(ActionTree step, String message) {
        this.step = step;
        this.message = message;

        if (step.findAny("const")) {
            hasConstants = true;
        }
    }

    public ActionTree step() {
        return step;
    }

    public String message() {
        return message;
    }
}
