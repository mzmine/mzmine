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

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.RtLimitsFilter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.objects.ObjectUtils;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GroupMS2SubParameters extends SimpleParameterSet {

  public GroupMS2SubParameters() {
    super(new Parameter[]{GroupMS2Parameters.mzTol, GroupMS2Parameters.rtFilter,
            GroupMS2Parameters.minimumRelativeFeatureHeight, GroupMS2Parameters.minRequiredSignals,
            GroupMS2Parameters.limitMobilityByFeature,

            // TIMS specific
            GroupMS2Parameters.combineTimsMsMs, GroupMS2Parameters.minImsRawSignals,
            GroupMS2Parameters.advancedParameters},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_ms2_scan_pairing/ms2_scan_pairing.html");
  }


  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public int getVersion() {
    return 2;
  }

  public static GroupMS2SubParameters create(@NotNull MZTolerance mzTol,
      @NotNull RtLimitsFilter rtLimits, @Nullable Double minRelHeight,
      @Nullable Integer minSignalsInMs2, boolean limitByIms, boolean combineTims,
      int imsMinOccurrence, boolean advanced, @Nullable Double timsOutputNoiseLevel,
      @Nullable Double timsOutputNoiseRelative, @Nullable String iterativeMs2ColName) {
    final GroupMS2SubParameters param = (GroupMS2SubParameters) new GroupMS2SubParameters().cloneParameterSet();
    param.setParameter(GroupMS2Parameters.mzTol, mzTol);
    param.setParameter(GroupMS2Parameters.rtFilter, rtLimits);
    param.setParameter(GroupMS2Parameters.minimumRelativeFeatureHeight, minRelHeight != null,
        Objects.requireNonNullElse(minRelHeight, GroupMS2Parameters.DEFAULT_MIN_REL_HEIGHT));
    param.setParameter(GroupMS2Parameters.minRequiredSignals, minSignalsInMs2 != null,
        Objects.requireNonNullElse(minSignalsInMs2, GroupMS2Parameters.DEFAULT_MIN_SIGNALS));
    param.setParameter(GroupMS2Parameters.limitMobilityByFeature, limitByIms);
    param.setParameter(GroupMS2Parameters.combineTimsMsMs, combineTims);
    param.setParameter(GroupMS2Parameters.minImsRawSignals, imsMinOccurrence);

    final GroupMs2AdvancedParameters advancedParam = GroupMs2AdvancedParameters.create(
        timsOutputNoiseLevel, timsOutputNoiseRelative, iterativeMs2ColName);
    param.setParameter(GroupMS2Parameters.advancedParameters, advanced &&
        ObjectUtils.countNonNull(timsOutputNoiseLevel, timsOutputNoiseRelative, iterativeMs2ColName)
            > 0);
    param.getParameter(GroupMS2Parameters.advancedParameters).setEmbeddedParameters(advancedParam);

    return param;
  }

  public static GroupMS2SubParameters createDefault() {
    return create(new MZTolerance(0.01, 10), GroupMS2Parameters.DEFAULT_RT_FILTER,
        GroupMS2Parameters.DEFAULT_MIN_REL_HEIGHT, GroupMS2Parameters.DEFAULT_MIN_SIGNALS,
        GroupMS2Parameters.DEFAULT_LIMIT_IMS, GroupMS2Parameters.DEFAULT_COMBINE_TIMS,
        GroupMS2Parameters.DEFAULT_IMS_MIN_SIGNALS, false, null, null, null);
  }
}
