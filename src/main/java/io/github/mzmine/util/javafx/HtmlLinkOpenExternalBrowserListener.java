/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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