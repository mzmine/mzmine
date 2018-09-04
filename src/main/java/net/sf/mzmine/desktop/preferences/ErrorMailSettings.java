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

package net.sf.mzmine.desktop.preferences;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.PasswordParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

/**
 * Error mail settings
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ErrorMailSettings extends SimpleParameterSet {

  // we use the same address to send and receive emails
  public static final StringParameter eMailAddress =
      new StringParameter("E-mail address", "Enter your e-Mail address");

  public static final PasswordParameter eMailPassword =
      new PasswordParameter("E-mail password", "Enter your e-Mail password", true);

  public static final StringParameter smtpHost =
      new StringParameter("Host server smtp", "Enter host server smtp, e.g. smtp.gmail.com");

  public static final IntegerParameter smtpPort =
      new IntegerParameter("smtp port", "Enter smtp port, for gmail 465");


  public ErrorMailSettings() {
    super(new Parameter[] {eMailAddress, eMailPassword, smtpHost, smtpPort});
  }

}
