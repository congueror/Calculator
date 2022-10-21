package net.congueror.calculator.helpers;

import net.congueror.calculator.Equation;

public final class MathHelper {

    private MathHelper() {}



    public static double sin(double a) {
        final double precision = 1e-14;

        while (a > Math.PI / 2 || a < -Math.PI / 2) {
            if (a > 0)
                a -= Math.PI;
            else
                a += Math.PI;
        }

        if (Math.abs(a) < precision)
            return 0d;
        if (Math.abs(a - Math.PI / 2) < precision)
            return a > 0 ? 1d : -1d;
        return Math.sin(a);
    }

    public static double cos(double a) {
        final double precision = 1e-14;

        while (a > Math.PI || a < 0) {
            if (a > 0)
                a -= 2 * Math.PI;
            if (a < 0)
                a += 2 * Math.PI;
        }

        if (Math.abs(a) < precision)
            return 1d;
        if (Math.abs(a - Math.PI / 2) < precision)
            return 0d;
        if (Math.abs(a - Math.PI) < precision)
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
}
