package net.congueror.calculator;

import net.congueror.calculator.helpers.JSHelper;
import net.congueror.calculator.util.ContextMenuHandler;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefMessageRouterHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicInteger;

public class Main extends JFrame {

    private static CefBrowser browser;

    public static void main(String[] args) {
        if (!CefApp.startup(args)) {
            System.out.println("Startup initialization failed!");
            return;
        }

        new Main();
    }

    public Main() {

        browser = setupBrowser();

        getContentPane().add(browser.getUIComponent(), BorderLayout.CENTER);

        pack();
        setTitle("Calculator");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                CefApp.getInstance().dispose();
                dispose();
            }
        });

        Expression.registerExpressions();
    }

    private CefBrowser setupBrowser() {
        CefApp.addAppHandler(new CefAppHandlerAdapter(null) {
            @Override
            public void stateHasChanged(CefApp.CefAppState state) {
                // Shutdown the app if the native CEF part is terminated
                if (state == CefApp.CefAppState.TERMINATED) System.exit(0);
            }
        });
        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = false;

        CefApp app = CefApp.getInstance(settings);
        CefClient client = app.createClient();

        jsActive(client);

        return client.createBrowser(Main.class.getResource("/calculator.html").toExternalForm(), false, false);
    }

    public void jsActive(CefClient client) {
        client.addContextMenuHandler(new ContextMenuHandler());

        CefMessageRouter.CefMessageRouterConfig cmrc = new CefMessageRouter.CefMessageRouterConfig("java", "javaCancel");
        CefMessageRouter cmr = CefMessageRouter.create(cmrc);
        cmr.addHandler(new CefMessageRouterHandler() {

            @Override
            public void setNativeRef(String str, long val) {
                //System.out.println(str+"  "+val);
            }

            @Override
            public long getNativeRef(String str) {
                //System.out.println(str);
                return 0;
            }

            @Override
            public void onQueryCanceled(CefBrowser browser, CefFrame frame, long query_id) {
                //System.out.println("Cancel query:"+query_id);
            }

            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request, boolean persistent,
                                   CefQueryCallback callback) {
                //System.out.println("request:"+request+"\nquery_id:"+query_id+"\npersistent:"+persistent);

                try {
                    request = Main.this.calculate(request);
                } catch (ArithmeticException e) {
                    e.printStackTrace();
                    callback.failure(-1, e.getMessage());
                    return false;
                }

                System.out.println(request);
                callback.success(request);
                return true;
            }
        }, true);
        client.addMessageRouter(cmr);
    }

    public String calculate(String equation) {
        Equation eq = new Equation(equation);
        var actions = eq.getActions();
        var eval = new StringBuilder();
        eval.append(JSHelper.writeJS("""
                var area = document.getElementById("area");
                area.innerHTML = "";
                """));

        for (Equation.EquationActions action : actions) {
            if (action.equals(Equation.EquationActions.SIMPLIFY))
                simplify(eval, eq);
            else if (action.equals(Equation.EquationActions.COMPARE))
                compare(eval, eq);
            else if (action.equals(Equation.EquationActions.SOLVE))
                solve(eval, eq);
            eval.append("area.innerHTML += \"<br>\"");
        }

        return eval.toString();
    }

    private void simplify(StringBuilder eval, Equation eq) {
        var steps = eq.simplifyExpression();

        AtomicInteger yes = new AtomicInteger(0);
        steps.forEach(o -> {
            System.out.println(yes);
            o.step().print();
            yes.incrementAndGet();
        });

        eval.append(JSHelper.writeJS("""
                area.innerHTML += "<b id='text'>Simplify Expression:</b> <br>";
                """));
        for (OperationStep step : steps) {
            String js = JSHelper.writeJS("""
                    operationStep(area, "!@#3 !@#1", "!@#2");
                    """, step.step().toLatex(), step.message(), step.prefix().value());
            eval.append(js);
        }
    }

    private void compare(StringBuilder eval, Equation eq) {
        var steps = eq.compareExpression();

        AtomicInteger yes = new AtomicInteger(0);
        steps.forEach(o -> {
            System.out.println(yes);
            o.step().print();
            yes.incrementAndGet();
        });

        eval.append(JSHelper.writeJS("""
                area.innerHTML += "<b id='text'>Compare Expression:</b> <br>";
                """));
        for (OperationStep step : steps) {
            String js = JSHelper.writeJS("""
                    operationStep(area, "!@#3 !@#1", "!@#2");
                    """, step.step().toLatex(), step.message(), step.prefix().value());
            eval.append(js);
        }
    }

    private void solve(StringBuilder eval, Equation eq) {
        var steps = eq.solveEquation();

        AtomicInteger yes = new AtomicInteger(0);
        steps.forEach(o -> {
            System.out.println(yes);
            o.step().print();
            yes.incrementAndGet();
        });

        eval.append(JSHelper.writeJS("""
                area.innerHTML += "<b id='text'>Solve Equation:</b> <br>";
                """));
        for (OperationStep step : steps) {
            String js = JSHelper.writeJS("""
                    operationStep(area, "!@#3 !@#1", "!@#2");
                    """, step.step().toLatex(), step.message(), step.prefix().value());
            eval.append(js);
        }
    }
}
