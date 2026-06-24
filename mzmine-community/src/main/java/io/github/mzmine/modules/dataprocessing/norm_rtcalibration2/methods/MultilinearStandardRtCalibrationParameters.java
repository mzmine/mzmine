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

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.methods;

import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.ImportTypeParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameWithExampleExportParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;

public class MultilinearStandardRtCalibrationParameters extends SimpleParameterSet {

  public static final PercentParameter correctionBandwidth = new PercentParameter(
      "Interpolation bandwidth", "", 0.1d, 0.01d, 1d);

  public static final RTToleranceParameter standardTolerance = new RTToleranceParameter(
      "Standard RT tolerance", "RT tolerance to search for standards (pre correction).",
      new RTTolerance(0.1f, Unit.MINUTES));

  public static final FileNameParameter standardsList = new FileNameWithExampleExportParameter(
      "Internal standards list", "A file that specifies the internal standards.",
      ExtensionFilters.CSV_TSV_IMPORT, MultilinearStandardRtCalibrationParameters::exportFile);

  public static final List<ImportType<?>> importTypeList = List.of(
      new ImportType<>(true, new MZType().getUniqueID(), new PrecursorMZType()), //
      new ImportType<>(true, new RTType().getUniqueID(), new RTType()), //
      new ImportType<>(false, new CompoundNameType().getUniqueID(), new CompoundNameType()));

  public static final ImportTypeParameter importTypes = new ImportTypeParameter("Column headers",
      "Set the column headers in your quantification table.", importTypeList);

  public MultilinearStandardRtCalibrationParameters() {
    super(correctionBandwidth, standardTolerance, standardsList, importTypes);
  }

  public static final String mzHeader = new MZType().getUniqueID();
  public static final String rtHeader = new RTType().getUniqueID();
  public static final String example = """
      %s\t%s
      499.0412\t7.9
      513.0412\t4.6
      """.formatted(mzHeader, rtHeader);

  public static void exportFile(File file) {
    try (var w = Files.newBufferedWriter(file.toPath(), WriterOptions.REPLACE.toOpenOption())) {
      w.write(example);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);

    final boolean allEnabled = getValue(importTypes).stream().allMatch(ImportType::isSelected);
    if (!allEnabled) {
      errorMessages.add("Precursor mz and RT columns must be selected");
    }

    final boolean fileOk =
        getValue(standardsList) != null && !getValue(standardsList).getName().isBlank() && getValue(
            standardsList).exists();
    if (!fileOk) {
      errorMessages.add("Standards file invalid or does not exist: " + getValue(standardsList));
    }
    return allEnabled && superCheck && fileOk;
  }
}
