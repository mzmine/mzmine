package io.github.mzmine.modules.tools.siriusapi;

import com.beust.jcommander.internal.Nullable;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.FeatureUtils;
import org.jetbrains.annotations.NotNull;

public class AlignedFeatureDoesNotExistException extends RuntimeException {

  public AlignedFeatureDoesNotExistException(@NotNull String siriusFeatureId,
      @NotNull String siriusProject) {
    super(
        "The aligned feature with %s does not exist in the Sirius project %s. Is the correct project selected?".formatted(
            siriusFeatureId, siriusProject));
  }

  public AlignedFeatureDoesNotExistException(@NotNull String siriusFeatureId,
      @NotNull String siriusProject, @NotNull FeatureListRow row) {
    super(
        "The aligned feature with %s for mzmine feature %s does not exist in the Sirius project %s. Is the correct project selected?".formatted(
            siriusFeatureId, FeatureUtils.rowToString(row), siriusProject));
  }
}
