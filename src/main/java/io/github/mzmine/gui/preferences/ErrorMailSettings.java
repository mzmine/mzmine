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

package io.github.mzmine.gui.preferences;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PasswordParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;

/**
 * Error mail settings
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ErrorMailSettings extends SimpleParameterSet {

  // we use the same address to send and receive emails
  public static final StringParameter eMailAddress =
      new StringParameter("E-mail address", "Enter your e-Mail address", "", true);

  public static final PasswordParameter eMailPassword =
      new PasswordParameter("E-mail password", "Enter your e-Mail password", "",true);

  public static final StringParameter smtpHost =
      new StringParameter("Host server smtp", "Enter host server smtp, e.g. smtp.gmail.com", "", true);

  public static final IntegerParameter smtpPort =
      new IntegerParameter("smtp port", "Enter smtp port, for gmail 465",487, true);

  public ErrorMailSettings() {
    super(new Parameter[] {eMailAddress, eMailPassword, smtpHost, smtpPort});
  }

}
