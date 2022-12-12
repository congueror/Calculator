package net.congueror.calculator.helpers;

import net.congueror.calculator.Equation;
import net.congueror.calculator.structure.ActionTree;
import net.congueror.calculator.structure.TokenPair;

public final class MathHelper {
    private static final double precision = 1e14;
    private static final double epsilon = 1e-14;

    private MathHelper() {}

    public static boolean equals(double a, double b) {
        return Math.abs(a - b) < epsilon;
    }

    public static double gcd(double a, double b) {
        a *= precision;
        b *= precision;

        while (b != 0) {
            double temp = a;
            a = b;
            b = temp % b;
        }
        return a / precision;
    }

    public static double sin(double a) {

        while (a > Math.PI / 2 || a < -Math.PI / 2) {
            if (a > 0)
                a -= Math.PI;
            else
                a += Math.PI;
        }

        if (Math.abs(a) < epsilon)
            return 0d;
        if (Math.abs(a - Math.PI / 2) < epsilon)
            return a > 0 ? 1d : -1d;
        return Math.sin(a);
    }

    public static double cos(double a) {
        while (a > Math.PI || a < 0) {
            if (a > 0)
                a -= 2 * Math.PI;
            if (a < 0)
                a += 2 * Math.PI;
        }

        if (Math.abs(a) < epsilon)
            return 1d;
        if (Math.abs(a - Math.PI / 2) < epsilon)
            return 0d;
        if (Math.abs(a - Math.PI) < epsilon)
            return -1d;
        return Math.cos(a);
    }

    public static double tan(double a) {
        return sin(a) / cos(a);
    }

    public static Equation arcsin(Double a) {
        if (a == -1 || a == 1)
            return new Equation(a == 1 ? "\\frac{\\pi }{2}" : "-\\frac{\\pi }{2}");
        return new Equation(String.valueOf(Math.asin(a)));
    }

    public static Equation arccos(Double a) {
        if (a == -1 || a == 1)
            return new Equation(a == 1 ? "0" : "\\pi");
        if (a == 0)
            return new Equation("\\frac{\\pi }{2}");
        return new Equation(String.valueOf(Math.acos(a)));
    }

    public static Equation arctan(Double a) {
        if (a == 1)
            return new Equation("\\frac{\\pi }{4}");
        if (Double.isInfinite(a))
            return new Equation(a < 0 ? "-\\frac{\\pi }{2}" : "\\frac{\\pi }{2}");
        return new Equation(String.valueOf(Math.atan(a)));
    }

    public static ActionTree fraction(ActionTree a, ActionTree b) {
        if (a.value().is("num") && b.value().is("num")) {
            double val = a.value().getAsNum() / b.value().getAsNum();
            return new ActionTree(new TokenPair(val));
        }
        return null;
    }
}
