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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.parameters.parametertypes;

import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class TextComponent extends JScrollPane {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

    private final JTextArea textArea;

    public TextComponent() {
	textArea = new JTextArea(5, 30);
	textArea.setFont(smallFont);
	getViewport().add(textArea);
    }

    public void setText(String text) {
	textArea.setText(text);
    }

    public String getText() {
	return textArea.getText();
    }

    @Override
    public void setToolTipText(String toolTip) {
	textArea.setToolTipText(toolTip);
    }
}
