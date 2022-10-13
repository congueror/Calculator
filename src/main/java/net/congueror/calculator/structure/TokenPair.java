package net.congueror.calculator.structure;

import java.util.Objects;

public final class TokenPair implements Cloneable {
    private final String type;
    private final String value;

    public TokenPair(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public TokenPair(double value) {
        this.type = "num";
        this.value = String.valueOf(value);
    }

    public String type() {
        return type;
    }

    public String value() {
        return value;
    }

    public boolean is(String type) {
        return Objects.equals(this.type, type);
    }

    public boolean isOr(String... types) {
        for (String s : types) {
            if (this.is(s)) {
                return true;
            }
        }
        return false;
    }

    public boolean has(String value) {
        return Objects.equals(this.value, value);
    }

    public boolean hasOr(String... values) {
        for (String s : values) {
            if (this.has(s)) {
                return true;
            }
        }
        return false;
    }

    public boolean equals(String type, String value) {
        return this.is(type) && this.has(value);
    }

    public boolean equalsOr(String type, String... values) {
        return this.is(type) && this.hasOr(values);
    }

    public boolean equalsNumber(double num) {
        if (this.is("num")) {
            return Double.parseDouble(this.value()) == num;
        }
        return false;
    }

    @Override
    public String toString() {
        return "[" + type + ": \"" + value + "\"]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TokenPair) obj;
        return this.equals(that.type, that.value);
    }

    public TokenPair _clone() {
        try {
            return (TokenPair) this.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

}
