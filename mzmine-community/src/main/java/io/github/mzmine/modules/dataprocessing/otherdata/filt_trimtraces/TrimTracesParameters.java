/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.otherdata.filt_trimtraces;

import com.google.common.collect.Range;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherRawOrProcessed;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherTraceSelection;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherTraceSelectionParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class TrimTracesParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter files = new RawDataFilesParameter();

  public static final OtherTraceSelectionParameter traces = new OtherTraceSelectionParameter(
      "Detector traces", "Select the traces you want to be trimmed.", OtherTraceSelection.rawUv());

  public static final RTRangeParameter rtRange = new RTRangeParameter("Keep RT range",
      "Select the rt range you want to keep.", true, Range.closed(0d, 100d));

  public static final ComboParameter<OtherRawOrProcessed> saveResultsAs = new ComboParameter<>(
      "Save results as", """
      Select if you want to trim the raw traces or save a copy as processed feature.
            
      Saving as raw will require you to re-import the file if you want to make changes, but will 
      show the trimmed result as raw trace in plots.
            
      Saving as processed will not alter the raw data, but plots such as the Correlation Dashboard 
      will still show the raw chromatogram.
      """, OtherRawOrProcessed.values(), OtherRawOrProcessed.PROCESSED);

  public TrimTracesParameters() {
    super(files, traces, rtRange, saveResultsAs);
  }
}
