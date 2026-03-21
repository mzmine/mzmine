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

package io.github.mzmine.util.reporting.jasper.reporttypes;

import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import io.github.mzmine.util.reporting.jasper.ReportingParameters;

/**
 * Sub parameters in {@link ReportingParameters}
 */
public class AlignedFeatureReportParameters extends SimpleParameterSet {

  public static final BooleanParameter includeSummaryTable = new BooleanParameter(
      "Include summary table", "Select if the summary table should be included.", true);

  public static final BooleanParameter includeEvidencePages = new BooleanParameter(
      "Include evidence",
      "Select if feature evidence shall be included, e.g., EICs, mobilograms, spectral library matches, lipid annotations, MS1 spectrum, MS2 spectrum, ...",
      true);

  public static final MetadataGroupingParameter grouping = new MetadataGroupingParameter(
      "Metadata grouping (Boxplot)",
      "Select the metadata grouping for the generation of the box plot.",
      MetadataColumn.SAMPLE_TYPE_HEADER);

  public AlignedFeatureReportParameters() {
    super(includeSummaryTable, includeEvidencePages, grouping);
  }

}
