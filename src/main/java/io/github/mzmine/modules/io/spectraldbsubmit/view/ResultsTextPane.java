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
