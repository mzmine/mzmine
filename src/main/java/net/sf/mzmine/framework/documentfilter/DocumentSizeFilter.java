/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.framework.documentfilter;

import java.awt.Toolkit;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class DocumentSizeFilter extends DocumentFilter {

  int maxCharacters;

  public DocumentSizeFilter(int maxChars) {
    maxCharacters = maxChars;
  }

  @Override
  public void insertString(FilterBypass fb, int offs, String str, AttributeSet a)
      throws BadLocationException {
    if ((fb.getDocument().getLength() + str.length()) <= maxCharacters)
      super.insertString(fb, offs, str, a);
    else
      Toolkit.getDefaultToolkit().beep();
  }

  @Override
  public void replace(FilterBypass fb, int offs, int length, String str, AttributeSet a)
      throws BadLocationException {
    if ((fb.getDocument().getLength() + str.length() - length) <= maxCharacters)
      super.replace(fb, offs, length, str, a);
    else
      Toolkit.getDefaultToolkit().beep();
  }
}
