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

package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.modules.dataprocessing.group_compoundgrouper.intensityrepresentation.CompoundIntensityRepresentation;
import io.github.mzmine.modules.dataprocessing.group_compoundgrouper.intensityrepresentation.ConfigCompoundRepresentationModule;
import io.github.mzmine.modules.dataprocessing.group_compoundgrouper.intensityrepresentation.ConfigCompoundRepresentationParameters;
import io.github.mzmine.parameters.ParameterUtils;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aggregates per-raw-file intensity (height, area, normalized variants) onto a
 * {@link ModularCompoundRow} according to the {@link CompoundIntensityRepresentation} mode last
 * applied via {@link ConfigCompoundRepresentationModule}.
 * <p>
 * Stateless. Reads the latest applied method on every {@link #apply} call so the active
 * configuration is always taken from the source feature list. If no applied method exists, the
 * binding behaves as {@link CompoundIntensityRepresentation#REPRESENTATIVE} — the compound's
 * features are cleared and the existing fallback in {@link ModularCompoundRow#getFeature} returns
 * the preferred row's feature.
 */
public final class CompoundIntensitySumBinding implements ComplexCompoundRowBinding {

  private static final @NotNull IonIdentityListType ION_IDENTITY_TYPE = DataTypes.get(
      IonIdentityListType.class);
  private static final @NotNull AreaType AREA_TYPE = DataTypes.get(AreaType.class);
  private static final @NotNull HeightType HEIGHT_TYPE = DataTypes.get(HeightType.class);
  private static final @NotNull NormalizedAreaType NORMALIZED_AREA_TYPE = DataTypes.get(
      NormalizedAreaType.class);
  private static final @NotNull NormalizedHeightType NORMALIZED_HEIGHT_TYPE = DataTypes.get(
      NormalizedHeightType.class);

  private static final List<DataType<?>> MEMBER_FEATURE_TYPES = List.of(AREA_TYPE, HEIGHT_TYPE,
      NORMALIZED_AREA_TYPE, NORMALIZED_HEIGHT_TYPE);

  // re-aggregate when ion identity changes (matters for SUM_ION_IDENTITY_MEMBERS filter)
  private static final List<DataType<?>> ION_IDENTITY_TYPES = List.of(ION_IDENTITY_TYPE);

  @Override
  public @NotNull List<DataType<?>> getMemberRowTypes() {
    return ION_IDENTITY_TYPES;
  }

  @Override
  public @NotNull List<DataType<?>> getCompoundRowTypes() {
    return ION_IDENTITY_TYPES;
  }

  @Override
  public @NotNull List<DataType<?>> getMemberFeatureTypes() {
    return MEMBER_FEATURE_TYPES;
  }

  @Override
  public @NotNull List<DataType<?>> getCompoundFeatureTypes() {
    // nested compound case: when an inner ion compound's feature is recomputed, the outer
    // compound that owns the inner ion needs to re-aggregate.
    return MEMBER_FEATURE_TYPES;
  }

  @Override
  public void apply(@NotNull final ModularCompoundRow compoundRow) {
    final ModularFeatureList flist = compoundRow.getFeatureList();
    if (flist == null) {
      return;
    }
    final CompoundIntensityRepresentation mode = readMode(flist);

    if (mode == CompoundIntensityRepresentation.REPRESENTATIVE) {
      compoundRow.clearFeatures(false);
      return;
    }

    final List<FeatureListRow> contributors = switch (mode) {
      case SUM_ALL_MEMBERS -> compoundRow.getMemberRows();
      case SUM_ION_IDENTITY_MEMBERS ->
          compoundRow.getCompoundMembers().stream().map(CompoundFeatureMember::row)
              .filter(FeatureListRow::hasIonIdentity).toList();
      case REPRESENTATIVE -> List.of(compoundRow.getPreferredRow()); // unreachable, handled above
    };

    for (final RawDataFile raw : flist.getRawDataFiles()) {
      float areaSum = 0f;
      float heightSum = 0f;
      float normAreaSum = 0f;
      float normHeightSum = 0f;

      for (final FeatureListRow contributor : contributors) {
        final Feature feature = contributor.getFeature(raw);
        if (feature == null || feature.getFeatureStatus() == FeatureStatus.UNKNOWN) {
          continue;
        }
        if (!(feature instanceof ModularFeature mf)) {
          // assumption: only ModularFeature carries the typed columns we need to sum
          continue;
        }
        areaSum += nullableToFloat(mf.get(AREA_TYPE));
        heightSum += nullableToFloat(mf.get(HEIGHT_TYPE));
        normAreaSum += nullableToFloat(mf.get(NORMALIZED_AREA_TYPE));
        normHeightSum += nullableToFloat(mf.get(NORMALIZED_HEIGHT_TYPE));
      }

      if (heightSum <= 0f) {
        // no contributor → make sure no stale compound feature lingers for this raw file
        compoundRow.removeFeature(raw, false);
        continue;
      }

      final ModularCompoundFeature compoundFeature = new ModularCompoundFeature(
          compoundRow.getCompoundList(), compoundRow, raw);

      compoundFeature.set(AREA_TYPE, areaSum);
      compoundFeature.set(HEIGHT_TYPE, heightSum);
      if (normAreaSum > 0d) {
        compoundFeature.set(NORMALIZED_AREA_TYPE, normAreaSum);
      }
      if (normHeightSum > 0d) {
        compoundFeature.set(NORMALIZED_HEIGHT_TYPE, normHeightSum);
      }
      compoundRow.addFeature(raw, compoundFeature, false);
    }
  }

  private static float nullableToFloat(@Nullable final Number value) {
    return value == null ? 0f : value.floatValue();
  }

  /**
   * Reads the last {@link ConfigCompoundRepresentationModule} applied method on the feature list
   * and returns the chosen mode. Defaults to {@link CompoundIntensityRepresentation#REPRESENTATIVE}
   * when no applied method is present.
   */
  private static @NotNull CompoundIntensityRepresentation readMode(
      @NotNull final ModularFeatureList flist) {
    return ParameterUtils.getValueFromAppliedMethods(flist.getAppliedMethods(),
            ConfigCompoundRepresentationParameters.class,
            ConfigCompoundRepresentationParameters.INTENSITY_REPRESENTATION)
        .orElse(CompoundIntensityRepresentation.REPRESENTATIVE);
  }
}
