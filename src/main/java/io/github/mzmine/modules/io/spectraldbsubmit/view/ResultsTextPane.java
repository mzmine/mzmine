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

package io.github.mzmine.modules.io.spectraldbsubmit.view;

import java.awt.Color;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class ResultsTextPane extends JTextPane {
  private static final long serialVersionUID = 1L;
  private Style error;
  private Style succed;
  private Style info;

  public ResultsTextPane() {
    super();
    createStyles();
  }

  public ResultsTextPane(StyledDocument doc) {
    super(doc);
    createStyles();
    setEditorKit(JTextPane.createEditorKitForContentType("text/html"));
    setEditable(false);
    addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          if (Desktop.isDesktopSupported()) {
            try {
              Desktop.getDesktop().browse(e.getURL().toURI());
            } catch (IOException | URISyntaxException e1) {
              e1.printStackTrace();
            }
          }
        }
      }
    });
  }

  private void createStyles() {
    error = this.addStyle("error", null);
    StyleConstants.setForeground(error, Color.red);
    succed = this.addStyle("succed", null);
    StyleConstants.setForeground(succed, Color.green);
    info = this.addStyle("succed", null);
    StyleConstants.setForeground(info, Color.black);
  }

  public void appendText(String text, Style style) {
    StyledDocument doc = getStyledDocument();
    try {
      doc.insertString(doc.getLength(), text + "\n", style);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  public void appendHyperlink(String text, String url, Style style) {
    appendText(url, style);

    // does not work
    String display = text == null || text.isEmpty() ? url : text;
    String hyper = "<a href=\"" + url + "\">" + display + "</a>";

    StyledDocument doc = getStyledDocument();
    try {
      doc.insertString(doc.getLength(), hyper + "\n", style);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  public void appendErrorLink(String text, String url) {
    appendHyperlink(text, url, error);
  }

  public void appendSuccedLink(String text, String url) {
    appendHyperlink(text, url, succed);
  }

  public void appendInfoLink(String text, String url) {
    appendHyperlink(text, url, info);
  }

  public void appendErrorText(String text) {
    appendText(text, error);
  }

  public void appendSuccedText(String text) {
    appendText(text, succed);
  }

  public void appendInfoText(String text) {
    appendText(text, info);
  }

  public void appendColoredText(String text, Color color) {
    StyledDocument doc = getStyledDocument();

    Style style = addStyle("Color Style", null);
    StyleConstants.setForeground(style, color);
    try {
      doc.insertString(doc.getLength(), text, style);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

}
