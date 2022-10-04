/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.javafx;

import io.github.mzmine.main.MZmineCore;
import java.awt.Desktop;
import java.net.URL;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebView;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

/**
 * Open an HTML link in a WebView in the systems default browser.
 * <p>
 * webView.getEngine().getLoadWorker().stateProperty().addListener(new
 * HtmlLinkOpenExternalBrowserListener(webView));
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class HtmlLinkOpenExternalBrowserListener implements ChangeListener<State>, EventListener {

  private static final String CLICK_EVENT = "click";
  private static final String ANCHOR_TAG = "a";

  private final WebView webView;

  public HtmlLinkOpenExternalBrowserListener(WebView webView) {
    this.webView = webView;
  }

  @Override
  public void changed(ObservableValue<? extends State> observable, Worker.State oldValue,
      Worker.State newValue) {
    if (Worker.State.SUCCEEDED.equals(newValue)) {
      Document document = webView.getEngine().getDocument();
      NodeList anchors = document.getElementsByTagName(ANCHOR_TAG);
      for (int i = 0; i < anchors.getLength(); i++) {
        Node node = anchors.item(i);
        EventTarget eventTarget = (EventTarget) node;
        eventTarget.addEventListener(CLICK_EVENT, this, false);
      }
    }
  }

  @Override
  public void handleEvent(Event event) {
    HTMLAnchorElement anchorElement = (HTMLAnchorElement) event.getCurrentTarget();
    String href = anchorElement.getHref();

    if (Desktop.isDesktopSupported()) {
      openLinkInSystemBrowser(href);
    } else {
      // LOGGER.warn("OS does not support desktop operations like browsing. Cannot open link '{}'.", href);
    }

    event.preventDefault();
  }

  private void openLinkInSystemBrowser(String url) {
    try {
      MZmineCore.getDesktop().openWebPage(new URL(url));
    } catch (Throwable e) {
    }
  }
}