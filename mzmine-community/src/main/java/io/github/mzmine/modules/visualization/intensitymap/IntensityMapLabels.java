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

package io.github.mzmine.modules.visualization.intensitymap;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapLabel;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapPeak;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapLayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Labels of feature apexes for the overlays of the visualizer, from the rows of a feature list.
 * Compounds of the compound grouping get one label named after the compound at their representative
 * ion; their other ions get secondary labels with their ion notation. Rows without a compound are
 * labeled by their annotation or m/z, with the ion notation if present, and grouped by their ion
 * identity network. Every label carries the further isotopes of its feature for the hover
 * highlight.
 */
final class IntensityMapLabels {

  // m/z values closer than this are the same ion
  private static final double SAME_MZ = 1e-6;
  // names shown in an image title before the rest is counted
  private static final int MAX_TITLE_NAMES = 2;

  private final List<IntensityMapLayer> layers;
  private final IntensityMapDimensions mode;
  private final @Nullable Range<Float> frameTimes;
  private final NumberFormats formats;
  private final List<IntensityMapLabel> labels = new ArrayList<>();
  // rows with a label, e.g. as ion of a compound
  private final Set<FeatureListRow> labeled = Collections.newSetFromMap(new IdentityHashMap<>());

  private IntensityMapLabels(@NotNull final List<IntensityMapLayer> layers,
      @NotNull final IntensityMapDimensions mode, @Nullable final Range<Float> frameTimes,
      @NotNull final NumberFormats formats) {
    this.layers = layers;
    this.mode = mode;
    this.frameTimes = frameTimes;
    this.formats = formats;
  }

  /**
   * @param frameTimes retention times of the shown mobility frames, null if unknown
   * @return labels of LC-MS and mobility frame data, none for images, see {@link #titles}
   */
  static @NotNull List<IntensityMapLabel> of(@NotNull final FeatureList list,
      @NotNull final List<IntensityMapLayer> layers, @NotNull final IntensityMapDimensions mode,
      @Nullable final Range<Float> frameTimes, @NotNull final NumberFormats formats) {
    return switch (mode) {
      case IMAGING -> List.of();
      case LC_MS, AUTOMATIC, MOBILITY_FRAME ->
          new IntensityMapLabels(layers, mode, frameTimes, formats).build(list);
    };
  }

  /**
   * Annotations of images from a feature list: images get no peak labels, the known annotations of
   * the features in the m/z range of an overlay extend its title instead. Features without an
   * annotation add nothing, the title already shows the m/z.
   *
   * @return annotation text by layer id, only for layers with annotated features
   */
  static @NotNull Map<String, String> titles(@NotNull final FeatureList list,
      @NotNull final List<IntensityMapLayer> layers) {
    final Map<FeatureListRow, String> compounds = compoundNames(list);
    final Map<String, String> titles = new HashMap<>();
    for (final IntensityMapLayer layer : layers) {
      if (layer.fullRange()) {
        // every annotation of the sample would match the complete m/z range
        continue;
      }
      final List<String> names = list.getRows().stream()
          .filter(row -> usableFeature(row, layer) != null).sorted(Comparator.comparingDouble(
              (FeatureListRow row) -> usableFeature(row, layer).getHeight()).reversed())
          .map(row -> titleName(row, compounds)).filter(Objects::nonNull).distinct().toList();
      if (!names.isEmpty()) {
        titles.put(layer.id(), joinNames(names));
      }
    }
    return titles;
  }

  /**
   * @return the names, the first ones in full and the rest counted
   */
  static @NotNull String joinNames(@NotNull final List<String> names) {
    final String shown = String.join(", ",
        names.subList(0, Math.min(MAX_TITLE_NAMES, names.size())));
    return names.size() > MAX_TITLE_NAMES ? shown + " +" + (names.size() - MAX_TITLE_NAMES) : shown;
  }

  /**
   * @return the detected feature of the row in the sample of the layer, null if there is none or it
   * lies outside the m/z range of the layer
   */
  private static @Nullable Feature usableFeature(@NotNull final FeatureListRow row,
      @NotNull final IntensityMapLayer layer) {
    final Feature feature = row.getFeature(layer.file());
    return feature != null && feature.getFeatureStatus() != FeatureStatus.UNKNOWN
        && feature.getMZ() != null && feature.getHeight() != null && layer.mzRange()
        .contains(feature.getMZ()) ? feature : null;
  }

  /**
   * @return the compound or annotation name with the ion notation, null without a name
   */
  private static @Nullable String titleName(@NotNull final FeatureListRow row,
      @NotNull final Map<FeatureListRow, String> compounds) {
    final String name = compounds.getOrDefault(row, row.getPreferredAnnotationName());
    return name == null || name.isBlank() ? null : text(name, ion(row), "");
  }

  /**
   * @return the name of the compound of every grouped row that has a named compound
   */
  private static @NotNull Map<FeatureListRow, String> compoundNames(
      @NotNull final FeatureList list) {
    final Map<FeatureListRow, String> names = new IdentityHashMap<>();
    if (!list.hasCompoundList()) {
      return names;
    }
    for (final ModularCompoundRow compound : list.getCompoundList().getRows()) {
      final String name = compound.getPreferredAnnotationName();
      if (name != null && !name.isBlank()) {
        names.put(leaf(compound), name);
        forEachMember(compound, (_, row) -> names.putIfAbsent(row, name));
      }
    }
    return names;
  }

  /**
   * Visits the members of a compound and of its nested compounds.
   *
   * @param action receives the member and its feature list row
   */
  private static void forEachMember(@NotNull final CompoundRow compound,
      @NotNull final BiConsumer<CompoundFeatureMember, FeatureListRow> action) {
    for (final CompoundFeatureMember member : compound.getCompoundMembers()) {
      action.accept(member, leaf(member.row()));
      if (member.row() instanceof CompoundRow nested) {
        forEachMember(nested, action);
      }
    }
  }

  /**
   * @return the annotation name or m/z, followed by the ion notation if present
   */
  static @NotNull String text(@Nullable final String name, @Nullable final String ion,
      @NotNull final String mz) {
    final String base = name != null && !name.isBlank() ? name : "m/z " + mz;
    return ion == null || ion.isBlank() ? base : base + " " + ion;
  }

  /**
   * @return the ion notation of the row: the adduct of its preferred annotation, otherwise its best
   * ion identity
   */
  private static @Nullable String ion(@NotNull final FeatureListRow row) {
    final FeatureAnnotation annotation = row.getPreferredAnnotation();
    if (annotation != null && annotation.getAdductType() != null) {
      return annotation.getAdductType().toString();
    }
    final IonIdentity identity = row.getBestIonIdentity();
    return identity == null ? null : identity.toString();
  }

  /**
   * @return the feature list row of a possibly nested compound row
   */
  private static @NotNull FeatureListRow leaf(@NotNull final FeatureListRow row) {
    FeatureListRow current = row;
    while (current instanceof CompoundRow compound) {
      current = compound.getPreferredRow();
    }
    return current;
  }

  private @NotNull List<IntensityMapLabel> build(@NotNull final FeatureList list) {
    final CompoundList compounds = list.hasCompoundList() ? list.getCompoundList() : null;
    if (compounds != null) {
      for (final ModularCompoundRow compound : compounds.getRows()) {
        addCompound(compound);
      }
    }
    for (final FeatureListRow row : list.getRows()) {
      if (!labeled.contains(row)) {
        final String text = text(row.getPreferredAnnotationName(), ion(row),
            formats.mz(row.getAverageMZ()));
        add(row, network(row), row.getPreferredAnnotationName(), text, text,
            row.getPreferredAnnotation() != null, false);
      }
    }
    return labels;
  }

  private void addCompound(@NotNull final ModularCompoundRow compound) {
    final FeatureListRow representative = leaf(compound);
    final String name = compound.getPreferredAnnotationName();
    final String prefix =
        "Compound " + compound.getCompoundId() + (name == null || name.isBlank() ? ""
            : ": " + name);
    final String text = text(name, ion(representative), formats.mz(representative.getAverageMZ()));
    final String group = "compound " + compound.getCompoundId();
    add(representative, group, name, text, prefix + " · " + ion(representative, "representative"),
        compound.getPreferredAnnotation() != null, false);
    forEachMember(compound, (member, row) -> {
      if (row != representative && !labeled.contains(row)) {
        final CompoundMemberRole role = member.role();
        final String ion = ion(row);
        add(row, group, name, ion != null ? ion : role.getLabel(),
            prefix + " · " + ion(row, role.getLabel().toLowerCase()), false, true);
      }
    });
  }

  /**
   * @return group key of a row without compound: its ion identity network, null if none
   */
  private static @Nullable String network(@NotNull final FeatureListRow row) {
    final IonIdentity identity = row.getBestIonIdentity();
    return identity == null || identity.getNetwork() == null ? null
        : "network " + identity.getNetID();
  }

  /**
   * @return m/z and intensity of the further isotopes of the feature inside the m/z range of the
   * overlay, from the isotope pattern of the feature or else of the row; intensities are scaled so
   * that the signal closest to the feature m/z matches the feature height
   */
  private static @NotNull List<double[]> isotopes(@NotNull final Feature feature,
      @NotNull final FeatureListRow row, @NotNull final IntensityMapLayer layer) {
    final IsotopePattern pattern = feature.getIsotopePattern() != null ? feature.getIsotopePattern()
        : row.getBestIsotopePattern();
    final Float height = feature.getHeight();
    final Double mz = feature.getMZ();
    if (pattern == null || height == null || mz == null || pattern.getNumberOfDataPoints() < 2) {
      return List.of();
    }
    // assumption: the signal closest to the feature m/z is the feature itself
    int self = 0;
    for (int i = 1; i < pattern.getNumberOfDataPoints(); i++) {
      if (Math.abs(pattern.getMzValue(i) - mz) < Math.abs(pattern.getMzValue(self) - mz)) {
        self = i;
      }
    }
    final double reference = pattern.getIntensityValue(self);
    final List<double[]> isotopes = new ArrayList<>();
    for (int i = 0; i < pattern.getNumberOfDataPoints(); i++) {
      final double isotopeMz = pattern.getMzValue(i);
      if (i != self && layer.mzRange().contains(isotopeMz)) {
        isotopes.add(new double[]{isotopeMz,
            reference > 0 ? height * pattern.getIntensityValue(i) / reference : height});
      }
    }
    return isotopes;
  }

  /**
   * GC-EI deconvolution keeps one feature per compound and stores the grouped features only as a
   * pseudo spectrum of their m/z values and heights at the retention time of the feature.
   *
   * @return m/z and intensity of the ions grouped with the feature inside the m/z range of the
   * overlay, the feature itself excluded; empty without a GC-EI pseudo spectrum
   */
  static @NotNull List<double[]> deconvoluted(@NotNull final Feature feature,
      @NotNull final IntensityMapLayer layer) {
    final Double mz = feature.getMZ();
    if (mz == null) {
      return List.of();
    }
    for (final Scan scan : feature.getAllMS2FragmentScans()) {
      // assumption: other pseudo spectra, e.g. of LC-DIA, hold fragments that are not in the map
      if (scan instanceof PseudoSpectrum pseudo
          && pseudo.getPseudoSpectrumType() == PseudoSpectrumType.GC_EI) {
        final List<double[]> ions = new ArrayList<>();
        for (int i = 0; i < pseudo.getNumberOfDataPoints(); i++) {
          final double ion = pseudo.getMzValue(i);
          // skip the feature itself, the spectrum contains it
          if (Math.abs(ion - mz) > SAME_MZ && layer.mzRange().contains(ion)) {
            ions.add(new double[]{ion, pseudo.getIntensityValue(i)});
          }
        }
        return ions;
      }
    }
    return List.of();
  }

  /**
   * @return the extent of a feature, the apex alone if unknown
   */
  static @NotNull Range<Double> range(@Nullable final Range<? extends Number> range,
      final double apex) {
    return range == null ? Range.singleton(apex)
        : Range.closed(range.lowerEndpoint().doubleValue(), range.upperEndpoint().doubleValue());
  }

  /**
   * @return ion notation and role, e.g. "[M+Na]+ (adduct)"
   */
  private @NotNull String ion(@NotNull final FeatureListRow row, @NotNull final String role) {
    final String ion = IntensityMapLabels.ion(row);
    return (ion == null ? "m/z " + formats.mz(row.getAverageMZ()) : ion) + " (" + role + ")";
  }

  /**
   * Adds a label for every overlay that shows the feature of the row.
   *
   * @param group key of the group of the row, null if not grouped
   * @param name  annotation or compound name for searching
   */
  private void add(@NotNull final FeatureListRow row, @Nullable final String group,
      @Nullable final String name, @NotNull final String text, @NotNull final String description,
      final boolean annotated, final boolean secondary) {
    labeled.add(row);
    for (final IntensityMapLayer layer : layers) {
      final Feature feature = usableFeature(row, layer);
      if (feature == null || feature.getRT() == null) {
        continue;
      }
      final double mz = feature.getMZ();
      final float rt = feature.getRT();
      final String details =
          description + " · m/z " + formats.mz(mz) + " · RT " + formats.rt(rt) + " min";
      switch (mode) {
        case LC_MS, AUTOMATIC -> {
          final List<double[]> deconvoluted = deconvoluted(feature, layer);
          labels.add(new IntensityMapLabel(layer.id(), rt, mz,
              range(feature.getRawDataPointsRTRange(), rt),
              range(feature.getRawDataPointsMZRange(), mz), feature.getHeight(), name, text,
              deconvoluted.isEmpty() ? details
                  : details + " · " + deconvoluted.size() + " ions grouped by deconvolution",
              annotated, secondary, group, isotopes(feature, row, layer).stream()
              .map(isotope -> new IntensityMapPeak(rt, isotope[0], isotope[1])).toList(),
              deconvoluted.stream().map(ion -> new IntensityMapPeak(rt, ion[0], ion[1])).toList()));
        }
        case MOBILITY_FRAME -> {
          final Float mobility = feature.getMobility();
          // only features eluting in the shown frames
          if (mobility != null && frameTimes != null && feature.getRawDataPointsRTRange()
              .isConnected(frameTimes)) {
            labels.add(new IntensityMapLabel(layer.id(), mz, mobility,
                range(feature.getRawDataPointsMZRange(), mz),
                range(feature.getMobilityRange(), mobility), feature.getHeight(), name, text,
                details + " · mobility " + formats.mobility(mobility), annotated, secondary, group,
                isotopes(feature, row, layer).stream()
                    .map(isotope -> new IntensityMapPeak(isotope[0], mobility, isotope[1]))
                    .toList(), List.of()));
          }
        }
        case IMAGING -> {
          // no labels for images
        }
      }
    }
  }
}
