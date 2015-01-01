/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA -1301 USA
 */

package net.sf.mzmine.util.components;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ToolTipUI;

public class MultiLineToolTipUI extends ToolTipUI {

    static MultiLineToolTipUI SINGLETON = new MultiLineToolTipUI();
    int inset = 3;
    Graphics g;

    private MultiLineToolTipUI() {
    }

    public static ComponentUI createUI(JComponent c) {
	return SINGLETON;
    }

    public void installUI(JComponent c) {
	LookAndFeel.installColorsAndFont(c, "ToolTip.background",
		"ToolTip.foreground", "ToolTip.font");
	LookAndFeel.installBorder(c, "ToolTip.border");
    }

    public void uninstallUI(JComponent c) {
	LookAndFeel.uninstallBorder(c);
    }

    public Dimension getPreferredSize(JComponent c) {
	Font font = c.getFont();
	String tipText = ((JToolTip) c).getTipText();
	JToolTip mtt = (JToolTip) c;
	FontMetrics fontMetrics = mtt.getFontMetrics(font);
	int fontHeight = fontMetrics.getHeight();

	if (tipText == null)
	    return new Dimension(0, 0);

	String lines[] = tipText.split("\n");
	int num_lines = lines.length;

	int width, height, onewidth;
	height = num_lines * fontHeight;
	width = 0;
	for (int i = 0; i < num_lines; i++) {
	    onewidth = fontMetrics.stringWidth(lines[i]);
	    width = Math.max(width, onewidth);
	}
	return new Dimension(width + inset * 2, height + inset * 2);
    }

    public Dimension getMinimumSize(JComponent c) {
	return getPreferredSize(c);
    }

    public Dimension getMaximumSize(JComponent c) {
	return getPreferredSize(c);
    }

    public void paint(Graphics g, JComponent c) {
	Font font = c.getFont();
	JToolTip mtt = (JToolTip) c;
	FontMetrics fontMetrics = mtt.getFontMetrics(font);
	Dimension dimension = c.getSize();
	int fontHeight = fontMetrics.getHeight();
	int fontAscent = fontMetrics.getAscent();
	String tipText = ((JToolTip) c).getTipText();
	if (tipText == null)
	    return;
	String lines[] = tipText.split("\n");
	int num_lines = lines.length;
	int height;
	int i;

	g.setColor(c.getBackground());
	g.fillRect(0, 0, dimension.width, dimension.height);
	g.setColor(c.getForeground());
	for (i = 0, height = 2 + fontAscent; i < num_lines; i++, height += fontHeight) {
	    g.drawString(lines[i], inset, height);
	}
    }

}
