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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AbundanceMeasureParameter;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupSelection;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupSelectionParameter;
import org.jetbrains.annotations.NotNull;

public class CVFilterParameters extends SimpleParameterSet {

  public static final MetadataGroupSelectionParameter grouping = new MetadataGroupSelectionParameter(
      "Sample grouping", "Select the metadata group to calculate the CV for",
      new MetadataGroupSelection(MetadataColumn.SAMPLE_TYPE_HEADER, SampleType.QC.toString()));

  public static final PercentParameter maxCv = new PercentParameter("Maximum CV",
      "Maximum allowed coefficient of variation (relative standard deviation) of a feature inside the the selected group.",
      0.2, 0d, 10d);

  public static final AbundanceMeasureParameter abundanceMeasure = new AbundanceMeasureParameter(
      "Abundance measure",
      "Select the abundance measure (height or area) to use for CV calculation.",
      AbundanceMeasure.values(), AbundanceMeasure.Area);

  public static final BooleanParameter keepUndetected = new BooleanParameter("Keep undetected",
      "Keep features that were not detected in the specified group in the feature list.", false);

  public CVFilterParameters() {
    super(grouping, maxCv, abundanceMeasure, keepUndetected);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
