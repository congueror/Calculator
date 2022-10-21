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
                    callback.failure(0, e.getMessage());
                    return false;
                }

                System.out.println(request);
                callback.success(request);
                return true;
            }
        }, true);
        client.addMessageRouter(cmr);
    }

    /*
    2+\left(8-9\right)\cdot4\cdot9\div4+e-\pi-1
    3e+\left(8+\left(4-3\right)\right)2
    \sin\left(180\right)-\cos\left(2\pi\right)\arctan1
    \frac{2\pi }{2e}
    2e+3e+4+5
     */

    public String calculate(String equation) {
        Equation eq = new Equation(equation);
        var steps = eq.solveOperation();

        AtomicInteger yes = new AtomicInteger(0);
        steps.forEach(o -> {
            System.out.println(yes);
            o.step().print();
            yes.incrementAndGet();
        });

        StringBuilder code = new StringBuilder(JSHelper.writeJS("""
                var area = document.getElementById("area");
                area.innerHTML = "<b id='text'>Solve Operation:</b> <br>";
                operationStep(area, "!@#1 = !@#2", "");
                """, steps.get(0).step().toLatex(), steps.get(steps.size() - 1).step().toLatex()));
        for (int i = 1; i < steps.size(); i++) {
            OperationStep step = steps.get(i);
            String js = JSHelper.writeJS("""
                    operationStep(area, "=!@#1", "!@#2");
                    """, step.step().toLatex(), step.message());
            code.append(js);
        }

        return code.toString();
    }
}
