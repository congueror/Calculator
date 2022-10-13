package net.congueror.calculator.util;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.handler.CefContextMenuHandler;

public class ContextMenuHandler implements CefContextMenuHandler {
    @Override
    public void onBeforeContextMenu(CefBrowser cefBrowser, CefFrame cefFrame, CefContextMenuParams cefContextMenuParams, CefMenuModel model) {
        model.clear();

    }

    @Override
    public boolean onContextMenuCommand(CefBrowser cefBrowser, CefFrame cefFrame, CefContextMenuParams cefContextMenuParams, int i, int i1) {
        return true;
    }

    @Override
    public void onContextMenuDismissed(CefBrowser cefBrowser, CefFrame cefFrame) {

    }
}
