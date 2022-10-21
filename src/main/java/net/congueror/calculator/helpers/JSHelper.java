package net.congueror.calculator.helpers;

public final class JSHelper {

    private JSHelper() {}

    public static String writeJS(String code, Object... objs) {
        String processed = code.replaceAll("\n", "");
        for (int i = 1; i <= objs.length; i++) {
            String obj = objs[i - 1].toString().replace("\\", "\\\\");
            processed = processed.replace("!@#" + i, obj);
        }
        return processed;
    }
}