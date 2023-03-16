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

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilterParameter;
import io.github.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MobilityRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScanSelectionFiltersParameters extends SimpleParameterSet {

  public static final IntRangeParameter scanNumParameter = new IntRangeParameter("Scan number",
      "Range of included scan numbers", false, null);

  public static final IntegerParameter baseFilteringIntegerParameter = new IntegerParameter(
      "Base Filtering Integer",
      "Enter an integer for which every multiple of that integer in the list will be filtered. (Every Nth element will be shown)",
      null, false);

  public static final RTRangeParameter rtParameter = new RTRangeParameter(false);

  public static final MobilityRangeParameter mobilityParameter = new MobilityRangeParameter(false);

  public static final MsLevelFilterParameter msLevelParameter = new MsLevelFilterParameter();

  public static final StringParameter scanDefinitionParameter = new StringParameter(
      "Scan definition",
      "Include only scans that match this scan definition. You can use wild cards, e.g. *FTMS*",
      null, false);

  public static final ComboParameter<PolarityType> polarityParameter = new ComboParameter<>(
      "Polarity", "Include only scans of this polarity",
      List.of(PolarityType.ANY, PolarityType.POSITIVE, PolarityType.NEGATIVE), PolarityType.ANY);


  public static final ComboParameter<MassSpectrumType> spectrumTypeParameter = new ComboParameter<>(
      "Spectrum type", "Include only spectra of this type",
      List.of(MassSpectrumType.ANY, MassSpectrumType.PROFILE, MassSpectrumType.CENTROIDED,
          MassSpectrumType.THRESHOLDED), MassSpectrumType.ANY);


  public ScanSelectionFiltersParameters() {
    this(null);
  }

  public ScanSelectionFiltersParameters(final ScanSelection scanSelection) {
    super(scanNumParameter, baseFilteringIntegerParameter, rtParameter, mobilityParameter,
        msLevelParameter, scanDefinitionParameter, polarityParameter, spectrumTypeParameter);
    setFilter(scanSelection);
  }

  public void setFilter(@Nullable ScanSelection selection) {
    if (selection == null) {
      setParameter(scanNumParameter, null);
      setParameter(baseFilteringIntegerParameter, null);
      setParameter(rtParameter, null);
      setParameter(mobilityParameter, null);
      setParameter(msLevelParameter, null);
      setParameter(scanDefinitionParameter, null);
      setParameter(polarityParameter, PolarityType.ANY);
      setParameter(spectrumTypeParameter, MassSpectrumType.ANY);
    } else {
      setParameter(scanNumParameter, selection.scanNumberRange());
      setParameter(baseFilteringIntegerParameter, selection.getBaseFilteringInteger());
      setParameter(rtParameter, selection.scanRTRange());
      setParameter(mobilityParameter, selection.scanMobilityRange());
      setParameter(msLevelParameter, selection.msLevel());
      setParameter(scanDefinitionParameter, selection.scanDefinition());
      setParameter(polarityParameter, selection.polarity());
      setParameter(spectrumTypeParameter, selection.spectrumType());
    }
  }

  /**
   * @return ScanSelection from the current dataset
   */
  @NotNull
  public ScanSelection createFilter() {
    Range<Integer> scanNumberRange = getValue(scanNumParameter);
    Integer baseFilteringInteger = getValue(baseFilteringIntegerParameter);
    Range<Double> scanRTRange = getValue(rtParameter);
    Range<Double> scanMobilityRange = getValue(mobilityParameter);
    PolarityType polarity = getValue(polarityParameter);
    MassSpectrumType spectrumType = getValue(spectrumTypeParameter);
    MsLevelFilter msLevelFilter = getValue(msLevelParameter);
    String scanDefinition = getValue(scanDefinitionParameter);

    return new ScanSelection(scanNumberRange, baseFilteringInteger, scanRTRange, scanMobilityRange,
        polarity, spectrumType, msLevelFilter, scanDefinition);
  }

  @Override
  public int getVersion() {
    return 2;
  }
}
