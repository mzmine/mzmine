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

package io.github.mzmine.modules.visualization.dash_lipidqc.isotope;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Background task that computes measured and theoretical isotope pattern datasets for the
 * {@link IsotopePane} in the lipid QC dashboard.
 */
final class IsotopeComputationTask extends FxUpdateTask<IsotopePane> {

  private final @Nullable FeatureListRow row;
  private @NotNull IsotopeComputationResult result = new IsotopeComputationResult(
      "Select a row with an isotope pattern.", null, null);

  IsotopeComputationTask(final @NotNull IsotopePane model, final @Nullable FeatureListRow row) {
    super("Compute isotope pane", model);
    this.row = row;
  }

  @Override
  protected void process() {
    result = IsotopePane.computeResult(row);
  }

  @Override
  protected void updateGuiModel() {
    model.applyComputationResult(result);
  }

  @Override
  public @NotNull String getTaskDescription() {
    return "Calculating isotope pane datasets";
  }

  @Override
  public double getFinishedPercentage() {
    return 0d;
  }
}
