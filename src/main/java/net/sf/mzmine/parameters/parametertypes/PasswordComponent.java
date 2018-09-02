/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.parameters.parametertypes;

import javax.swing.JPanel;
import javax.swing.JPasswordField;

/**
 * Password component
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class PasswordComponent extends JPanel {

  /*
   * Parameter component to enter a password for e-Mail error messages
   */
  private static final long serialVersionUID = 1L;

  private final JPasswordField passwordField;

  public PasswordComponent(int inputsize) {
    passwordField = new JPasswordField(inputsize);
    add(passwordField);
  }

  public void setText(String text) {
    passwordField.setText(text);
  }

  public String getText() {
    String passText = new String(passwordField.getPassword());
    return passText;
  }

  @Override
  public void setToolTipText(String toolTip) {
    passwordField.setToolTipText(toolTip);
  }
}
