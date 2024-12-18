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

package io.github.mzmine.modules.dataprocessing.filter_groupms2;

import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.FeatureLimitOptions;
import io.github.mzmine.parameters.parametertypes.combowithinput.RtLimitsFilter;
import io.github.mzmine.parameters.parametertypes.combowithinput.RtLimitsFilterParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GroupMS2Parameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  // refine grouped MS2 to only those where relative feature height is at least X %
  public static final OptionalParameter<PercentParameter> minimumRelativeFeatureHeight = new OptionalParameter<>(
      new PercentParameter("Minimum relative feature height",
          "If an MS2 was assigned to multiple features, only keep the feature assignments where feature height is at least X% of the highest feature.",
          0.25, 0d, 1d), true);


  public static final OptionalParameter<IntegerParameter> minRequiredSignals = new OptionalParameter<>(
      new IntegerParameter("Minimum required signals",
          "Only assign fragmentation spectra with at least n signals in the filtered spectrum after mass detection etc.",
          1), true);

  public static final MZToleranceParameter mzTol = new MZToleranceParameter(
      "MS1 to MS2 precursor tolerance (m/z)",
      "Describes the tolerance between the precursor ion in a MS1 scan and the precursor "
          + "m/z assigned to the MS2 scan.", 0.01, 10);


  public static final RtLimitsFilterParameter rtFilter = new RtLimitsFilterParameter(
      new RtLimitsFilter(FeatureLimitOptions.USE_FEATURE_EDGES,
          new RTTolerance(0.2f, Unit.MINUTES)));

  // TIMS specific parameters
  public static final BooleanParameter limitMobilityByFeature = new BooleanParameter(
      "Limit by ion mobility edges", """
      If checked, only mobility scans from the mobility range of the feature will be merged.
      This is usually not needed. However, if isomeres/isobares elute at the same retention time
      and are close in mobility, the MS/MS window might be larger than the peak in mobility dimension
      and thus cause chimeric MS/MS spectra. This can be investigated in hte "All MS MS" window""",
      false);

  public static final BooleanParameter combineTimsMsMs = new BooleanParameter(
      "Merge MS/MS spectra (TIMS)", """
      If checked, all assigned MS/MS spectra with the same collision energy will be merged into a single MS/MS spectrum.
       These merged spectra will also be merged to a consensus spectrum across all collision energies.
      """, false);

  public static final IntegerParameter minImsRawSignals = new IntegerParameter(
      "Minimum detections in IMS dimension", """
      During data-dependent IMS-resolved MS2 acquisition, e.g., PASEF, multiple spectra are acquired for each precursor during a single IMS ramp.
      This allows filtering for reoccurring signals with the same m/z. Since electrical noise is distributed randomly,
      this filter will remove statistical noise generated from the merged MS2, even at higher intensities,
      but can retain actual fragment ion signals at low intensities (if they were detected more than this number).
      Default value: 2. Typical values: 1-4, the higher, the less likely noise signals are, but also removes real ion signals if set too high.
      To reproduce behavior of mzmine <=4.4, use 1.
      Note: The signal occurrence is checked on a per-frame basis (before merging CEs and across CEs).""",
      2, 1, Integer.MAX_VALUE);

  public static final AdvancedParametersParameter<GroupMs2AdvancedParameters> advancedParameters = new AdvancedParametersParameter<>(
      new GroupMs2AdvancedParameters(), false);

  public GroupMS2Parameters() {
    super(new Parameter[]{PEAK_LISTS, mzTol, rtFilter, minimumRelativeFeatureHeight,
            minRequiredSignals, limitMobilityByFeature,
            // TIMS specific
            minImsRawSignals, combineTimsMsMs, advancedParameters},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_ms2_scan_pairing/ms2_scan_pairing.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public int getVersion() {
    return 3;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 3 -> """
          In mzmine version >= 4.4.3, the IMS-related MS2 grouping parameters %s and %s were moved \
          into an advanced parameter set (see drop down) and disabled by default.
          Additionally, the parameter %s was added as a more sophisticated option to improve \
          the quality of DDA-IMS-MS2 spectra.
          """.formatted(inQuotes(GroupMs2AdvancedParameters.outputNoiseLevel.getName()),
          inQuotes(GroupMs2AdvancedParameters.outputNoiseLevelRelative.getName()),
          inQuotes(GroupMS2Parameters.minImsRawSignals.getName()));
      default -> null;
    };
  }
}
