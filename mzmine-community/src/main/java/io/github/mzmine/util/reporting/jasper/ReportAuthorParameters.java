/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.reporting.jasper;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.TextParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import java.io.File;
import java.util.List;
import java.util.Map;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.validation.constraints.NotNull;

public class ReportAuthorParameters extends SimpleParameterSet {

  public static final StringParameter vendorCompany = new StringParameter("Company/Institute",
      "Your company/institute name.");

  public static final TextParameter contact = new TextParameter("Author contact", """
      Your Job description/contact address, e.g.:
      <Your name>
      <Your job title>
      <Your company name>
      <Your address>
      <email/tel. number>
      """);

  public static final FileNameParameter logoPath = new FileNameParameter("Logo",
      "Select the logo of your company/institute.",
      List.of(new ExtensionFilter("Image files", "*.jpg", "*.png", "*.svg")),
      FileSelectionType.OPEN, true);

  public ReportAuthorParameters() {
    super(vendorCompany, contact, logoPath);
  }

  public void addToMetadata(@NotNull Map<String, Object> jasperParameters) {
    jasperParameters.put("META_COMPANY", getValue(ReportAuthorParameters.vendorCompany));
    jasperParameters.put("META_LAB_DESCRIPTION", getValue(ReportAuthorParameters.contact));
    final File logoPath = getValue(ReportAuthorParameters.logoPath);
    jasperParameters.put("META_LOGO_PATH", logoPath != null ? logoPath.getAbsolutePath() : null);
  }
}
