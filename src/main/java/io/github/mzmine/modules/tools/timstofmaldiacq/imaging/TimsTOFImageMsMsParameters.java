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

package io.github.mzmine.modules.tools.timstofmaldiacq.imaging;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.NumberListParameter;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeDouble;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeDoubleParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import javafx.application.Platform;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

public class TimsTOFImageMsMsParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();
  public static final DirectoryParameter savePathDir = new DirectoryParameter("Data location",
      "Path to where acquired measurements shall be saved.",
      "D:" + File.separator + "Data" + File.separator + "User" + File.separator + "MZmine_3");
  public static final FileNameParameter acquisitionControl = new FileNameParameter(
      "Path to msmsmaldi.exe", "", List.of(new ExtensionFilter("executable", "*.exe")),
      FileSelectionType.OPEN);
  public static final NumberListParameter collisionEnergies = new NumberListParameter(
      "Collision energies", "List of collision energies separated by ','.", List.of(20d, 30d, 40d),
      new DecimalFormat("0.0"));
  public static final IntegerParameter numMsMs = new IntegerParameter("Number of MS/MS spectra",
      "The number of MS/MS spectra to be acquired per feature and collision energy.", 3, 1,
      Integer.MAX_VALUE);
  public static final IntegerParameter minimumDistance = new IntegerParameter(
      "Minimum distance of MS/MS spectra",
      "The minimum distance between two MS/MS spots for a single feature.", 30);
  public static final DoubleParameter minimumPurity = new DoubleParameter("Minimum purity score",
      """
          Evaluates the chimerity of the isolation window and only selects pixels with low chimerity for fragmentation.
          The precursor intensity is divided by the summed intensity in the isolation window.
          This means that low values (e.g. 0.1) indicate high chimerity and high values (e.g. 0.9) indicate
          low chimerity.Thereby MS/MS spectra with higher quality are produced.""",
      new DecimalFormat("0.00"), 0.80, 0d, 1d);
  public static final AbsoluteAndRelativeDoubleParameter minimumIntensity = new AbsoluteAndRelativeDoubleParameter(
      "Minimum MS1 intensity", """
      Minimum intensity of a MS1 pixel to be eligible for an MS/MS spectrum.
      Note that every feature in this feature list will be fragmented.
      This parameter applies to individual pixels in the MS1 image.
      If that intensity is greater than the threshold, an MS2 may be acquired adjacent to that pixel.""",
      "a.u.", new AbsoluteAndRelativeDouble(1E4, 0.05),
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final BooleanParameter exportOnly = new BooleanParameter("Export MS/MS lists only",
      "Will only export MS/MS lists and not start an acquisition.", false);

  public static final AdvancedParametersParameter<AdvancedImageMsMsParameters> advancedParameters = new AdvancedParametersParameter<>(
      new AdvancedImageMsMsParameters());

  public TimsTOFImageMsMsParameters() {
    super(flists, savePathDir, acquisitionControl, numMsMs, collisionEnergies, minimumDistance,
        minimumIntensity, minimumPurity, advancedParameters, exportOnly);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }
    ParameterSetupDialog dialog = new TimsTOFImageMsMsDialog(valueCheckRequired, this, null);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  /*@Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("Minimum chimerity score", minimumPurity);
    return nameParameterMap;
  }*/
}
