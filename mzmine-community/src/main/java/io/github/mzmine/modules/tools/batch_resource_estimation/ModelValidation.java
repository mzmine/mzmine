package io.github.mzmine.modules.tools.batch_resource_estimation;

import org.jetbrains.annotations.NotNull;

/**
 * Validation of the extrapolation with an extra subset run that was not used to fit the model.
 *
 * @param predicted extrapolation of the model for the files of the validation run
 * @param measured  measurements of the validation run
 * @param totals    comparison of the batch totals
 */
public record ModelValidation(@NotNull BatchResourcePrediction predicted,
                              @NotNull BatchResourcePrediction measured, @NotNull FitCheck totals) {

}
