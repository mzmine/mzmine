package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.datamodel.features.FeatureListRow;
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
}
