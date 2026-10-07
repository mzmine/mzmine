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

package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Sealed hierarchy of events fired by the quality pane that hosts (e.g. the compound dashboard)
/// can listen to via {@link CompoundRowQualityController#onQualityCheckEventProperty()}. New event
/// types can be added as records implementing this interface; consumers should use exhaustive
/// {@code switch} on the sealed permits list to handle them.
public sealed interface QualityCheckEvent {

  /// Fired when the user clicks a fragment-scan group in the MS2-available check. Identifies the
  /// member row, the activation energy, and the activation method that selected scans should match.
  /// The listener is expected to focus the row and pick a matching scan from its fragment scans.
  /// {@code energy} is nullable because some scans carry no activation energy (then the listener
  /// should match on method + missing-energy scans).
  record FragmentEnergyMethodSelectedEvent(@NotNull FeatureListRow row, @Nullable Float energy,
                                           @NotNull ActivationMethod method) implements
      QualityCheckEvent {

  }

  /// Fired when the user requests the full detail view for a compound annotation match (e.g. by
  /// double-clicking the structure preview or pressing the detail icon button in the compound
  /// annotation check). Carries the matching annotation and the member row that owns it so the
  /// listener can navigate to the right place (e.g. open the IIMN network on this row).
  record AnnotationDetailRequestedEvent(@NotNull FeatureAnnotation annotation,
                                        @NotNull FeatureListRow row) implements QualityCheckEvent {

  }

  /// Fired when the user double-clicks a structure preview in the annotation-agreement check.
  /// Carries the annotation that was clicked and the member {@link FeatureListRow} that contributed
  /// it; the dashboard listener typically promotes the row to its selected adduct row so the EICs /
  /// MS1 / structure preview follow.
  record AnnotationStructureSelectedEvent(@NotNull FeatureAnnotation annotation,
                                          @NotNull FeatureListRow row) implements
      QualityCheckEvent {

  }

  /// Fired when the user double-clicks an {@code AnnotationSummaryChart} (the bar-chart cell next
  /// to a structure) in the annotation-agreement sub pane. Carries the member row that contributed
  /// the annotation plus the rendered {@link AnnotationSummary}. The dashboard listener typically
  /// routes this to {@code AnnotationSummaryType.getDoubleClickAction(...)} so the matching detail
  /// tab (compound DB / spectral library / lipid QC) opens.
  record AnnotationSummaryActivated(@NotNull FeatureListRow row,
                                    @NotNull AnnotationSummary annotationSummary) implements
      QualityCheckEvent {

  }
}
