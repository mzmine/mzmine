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

package io.github.mzmine.modules.tools.tools_autoparam;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class AutoParamParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter RAW_DATA_FILES = new RawDataFilesParameter();

  public static final ComboParameter<PolarityType> POLARITY = new ComboParameter<>("Polarity",
      "Only MS1 scans of this polarity are used for the statistics. Required for polarity "
          + "switching data, where alternating polarities would break the traces.",
      new PolarityType[]{PolarityType.ANY, PolarityType.POSITIVE, PolarityType.NEGATIVE},
      PolarityType.ANY);

  public AutoParamParameters() {
    super(RAW_DATA_FILES, POLARITY);
  }

  public static ParameterSet of(List<RawDataFile> files) {
    return of(files, PolarityType.ANY);
  }

  /**
   * @param polarity only MS1 scans of this polarity are used, {@link PolarityType#ANY} for all
   */
  public static @NotNull ParameterSet of(@NotNull List<RawDataFile> files,
      @NotNull PolarityType polarity) {
    final ParameterSet parameterSet = new AutoParamParameters().cloneParameterSet();
    parameterSet.setParameter(RAW_DATA_FILES,
        new RawDataFilesSelection(files.toArray(RawDataFile[]::new)));
    parameterSet.setParameter(POLARITY, polarity);
    return parameterSet;
  }
}
