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

package io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameWithExampleExportParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ReferenceCCSCalibrationParameters extends SimpleParameterSet {

  public static final OptionalParameter<RawDataFilesParameter> files = new OptionalParameter<>(
      new RawDataFilesParameter("Set to additional raw files", 0, Integer.MAX_VALUE), false);

  public static final FeatureListsParameter flists = new FeatureListsParameter(
      "Feature list (with reference compounds)", 1, Integer.MAX_VALUE);

  public static final FileNameWithExampleExportParameter referenceList = new FileNameWithExampleExportParameter(
      "Reference list", """
      The file containing the reference compounds for m/z and mobility.
      Must contain the columns "mz", "mobility", "ccs", "charge". Columns must be separated by ";".""",
      List.of(ExtensionFilters.CSV, ExtensionFilters.TXT),
      ReferenceCCSCalibrationParameters::exportExample);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "Tolerance for the given reference compound list", 0.005, 5);

  public static final MobilityToleranceParameter mobTolerance = new MobilityToleranceParameter(
      "Mobility tolerance",
      "Tolerance for the given reference compound list in the unit of the respective mobility separation device.",
      new MobilityTolerance(0.1f));

  public static final RTRangeParameter rtRange = new RTRangeParameter(
      "Calibration segment RT range", "The rt range of the calibration segment.", true,
      Range.closed(0d, 60d));

  public static final DoubleParameter minHeight = new DoubleParameter("Minumum height",
      "The minimum intensity of a calibrant feature to be used for calibration.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3);

  public ReferenceCCSCalibrationParameters() {
    super(new Parameter[]{files, flists, referenceList, mzTolerance, mobTolerance, rtRange,
            minHeight},
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_ccs_calibration/ccs_calibration.html#reference-css-calibration");
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    if (!super.checkParameterValues(errorMessages)) {
      return false;
    }

    boolean check = true;

    if (getValue(files)) {
      final RawDataFilesSelection value = getParameter(files).getEmbeddedParameter().getValue();
      final RawDataFilesSelection clone = value.clone();
      final RawDataFile[] files = clone.getMatchingRawDataFiles(); // dont evaluate the real parameter, otherwise we have to reset it.

      ModularFeatureList[] flists = getValue(
          ReferenceCCSCalibrationParameters.flists).getMatchingFeatureLists();

      if (files.length != 0 && flists.length > 1) {
        errorMessages.add(
            "Invalid parameter selection. Either select one feature list and >= 1 raw data file or no raw data files. (Reference calibration)");
        check = false;
      }
    }

    return check;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }

  public static void exportExample(File file) {
    String str = """
        mz;charge;mobility;ccs
        118.0863;1;0.5446;121.30
        322.0481;1;0.7363;153.73
        622.0290;1;0.9915;202.96
        922.0098;1;1.1986;243.64
        1221.9906;1;1.3934;282.20
        1521.9715;1;1.5685;316.96
        1821.9523;1;1.7407;351.25
        2121.9332;1;1.9003;383.03
        2421.9140;1;2.0504;412.96
        2721.8948;1;2.1921;441.21
        301.9981;-1;0.6690;140.04
        601.9790;-1;0.8824;180.77
        1033.9881;-1;1.2582;255.34
        1333.9689;-1;1.4073;284.76
        1633.9498;-1;1.5797;319.03
        1933.9306;-1;1.7479;352.55
        2233.9115;-1;1.8895;380.74
        2533.8923;-1;2.0511;412.99
        2833.8731;-1;2.1498;432.62
        """;

    try (var w = Files.newBufferedWriter(file.toPath(), WriterOptions.REPLACE.toOpenOption())) {
      w.write(str);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
