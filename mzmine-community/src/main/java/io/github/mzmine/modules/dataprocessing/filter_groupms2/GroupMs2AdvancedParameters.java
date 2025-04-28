/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_groupms2;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import static io.github.mzmine.util.StringUtils.inQuotes;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class GroupMs2AdvancedParameters extends SimpleParameterSet {

  public static final OptionalParameter<DoubleParameter> outputNoiseLevel = new OptionalParameter<>(
      new DoubleParameter("Minimum signal intensity (absolute, TIMS)",
          "If a TIMS feature is processed, this parameter "
          + "can be used to filter low abundant signals in the MS/MS spectrum, since multiple "
          + "MS/MS mobility scans need to be merged together.",
          MZmineCore.getConfiguration().getIntensityFormat(), 250d, 0d, Double.MAX_VALUE), false);

  public static final OptionalParameter<PercentParameter> outputNoiseLevelRelative = new OptionalParameter<>(
      new PercentParameter("Minimum signal intensity (relative, TIMS)",
          "If an ion mobility spectrometry (TIMS) feature is processed, this parameter "
          + "can be used to filter low abundant peaks in the MS/MS spectrum, since multiple "
          + "MS/MS mobility scans need to be merged together.", 0.01d), true);

  public static final OptionalParameter<MetadataGroupingParameter> iterativeMs2Column = new OptionalParameter<>(
      new MetadataGroupingParameter("Group iterative MS2s", """
          Allows selection of a metadata column that contains the file name of a raw data file to which the scans of a MS2 file shall be paired to.
          For example, the column name is %s then that column name must be listed here.
          For three files A_main, B_MS2, and C_MS2, of which A_main contains MS1 (and MS2) scans,
          and where B_MS2 and C_MS2 only contain MS2 scans, the files B_MS2 and C_MS2 must list
          %s in the column %s. The MS2 scans of file B and C are then paired to the features of file A.
          """.formatted(inQuotes(GroupMS2Processor.DEFAULT_QUANT_FILE_COLUMN_NAME),
          inQuotes("A_main"), inQuotes(GroupMS2Processor.DEFAULT_QUANT_FILE_COLUMN_NAME)),
          GroupMS2Processor.DEFAULT_QUANT_FILE_COLUMN_NAME));

  public GroupMs2AdvancedParameters() {
    super(outputNoiseLevel, outputNoiseLevelRelative, iterativeMs2Column);
  }

  public static GroupMs2AdvancedParameters create(@Nullable Double outputNoiseLevel,
      @Nullable Double outputNoiseLevelRelative, @Nullable String iterativeMs2ColumnName) {
    final GroupMs2AdvancedParameters param = (GroupMs2AdvancedParameters) new GroupMs2AdvancedParameters().cloneParameterSet();

    param.setParameter(GroupMs2AdvancedParameters.outputNoiseLevel, outputNoiseLevel != null,
        outputNoiseLevel);
    param.setParameter(GroupMs2AdvancedParameters.outputNoiseLevelRelative,
        outputNoiseLevelRelative != null, outputNoiseLevelRelative);

    param.setParameter(GroupMs2AdvancedParameters.iterativeMs2Column,
        iterativeMs2ColumnName != null, Objects.requireNonNullElse(iterativeMs2ColumnName,
            GroupMS2Processor.DEFAULT_QUANT_FILE_COLUMN_NAME));

    return param;
  }
}
