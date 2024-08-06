/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_features_gnps.masst;

import static io.github.mzmine.javafx.components.factories.FxTexts.boldText;
import static io.github.mzmine.javafx.components.factories.FxTexts.linebreak;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.javafx.components.factories.ArticleReferences;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PasswordParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.ExitCode;
import javafx.scene.layout.Region;

/**
 * Submit MASST search on GNPS
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class GnpsMasstSubmitParameters extends SimpleParameterSet {

  public static final ComboParameter<MasstDatabase> database = new ComboParameter<>(
      "Search in database", "Select databases", MasstDatabase.values(), MasstDatabase.ALL);

  public static final StringParameter description = new StringParameter("Description",
      "Description", "MZmine3 MASST submission", false);

  public static final StringParameter email = new StringParameter("Email",
      "Email address for notifications about the job", "", false, true);

  public static final StringParameter username = new StringParameter("Username",
      "Username to directly post job to.", "", false, true);

  public static final PasswordParameter password = new PasswordParameter(
      "Password (optional; currently not encrypted)",
      "The password is sent without encryption, until the server has has moved to its final destination.",
      "", false);

  public static final DoubleParameter cosineScore = new DoubleParameter("Min cosine score", "",
      MZmineCore.getConfiguration().getScoreFormat(), 0.7, 0d, 1d);

  public static final IntegerParameter minimumMatchedSignals = new IntegerParameter(
      "Minimum matched peaks", "Minimum number of spectral signals to be matched", 6, false, 1,
      Integer.MAX_VALUE);

  public static final DoubleParameter parentMassTolerance = new DoubleParameter(
      "Absolute parent mass tolerance", "Absolute m/z tolerance to match precursors",
      MZmineCore.getConfiguration().getMZFormat(), 1d, 0d, Double.MAX_VALUE);

  public static final DoubleParameter fragmentMassTolerance = new DoubleParameter(
      "Absolute fragment mass tolerance", "Absolute m/z tolerance to match fragment ions",
      MZmineCore.getConfiguration().getMZFormat(), 0.5, 0d, Double.MAX_VALUE);

  public static final BooleanParameter searchAnalogs = new BooleanParameter("Analog search",
      "Search analogs by allowing m/z differences between precursors", false);

  public static final BooleanParameter openWebsite = new BooleanParameter("Open website",
      "Website of GNPS job", false);

  public GnpsMasstSubmitParameters() {
    super(new Parameter[]{cosineScore, minimumMatchedSignals, parentMassTolerance,
        fragmentMassTolerance, searchAnalogs, database, description, email, username, password,
        openWebsite});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    final Region message = FxTextFlows.newTextFlowInAccordion("How to cite",
        boldText("About the Mass Spectrometry Search Tool (MASST) direct submission:"), linebreak(),
        text("When using MASST please cite:"), linebreak(),
        ArticleReferences.MASST.hyperlinkText());

    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

}
