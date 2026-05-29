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
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class RecomputeTask extends FxUpdateTask<CompoundRowQualityModel> {

  private final CompoundRowQualityInteractor interactor;
  private final CompoundRow row;
  private final RTTolerance rtTol;
  private final MZTolerance mzTol;
  private final MZTolerance ms2Tol;
  private final @Nullable SimpleColorPalette palette;
  private final @Nullable ObjectProperty<@Nullable FeatureListRow> selectedMemberRow;
  private final @Nullable Consumer<@NotNull QualityCheckEvent> onEvent;
  private final @Nullable ParameterSet checkParameters;
  private final @Nullable Consumer<@NotNull ParameterSet> onCheckParametersUpdate;
  private @Nullable List<QualityCheckResult> results;

  RecomputeTask(@NotNull CompoundRowQualityModel model,
      @NotNull CompoundRowQualityInteractor interactor, @NotNull CompoundRow row,
      @NotNull RTTolerance rtTol, @NotNull MZTolerance mzTol, @NotNull MZTolerance ms2Tol,
      @Nullable SimpleColorPalette palette,
      @Nullable ObjectProperty<@Nullable FeatureListRow> selectedMemberRow,
      @Nullable Consumer<@NotNull QualityCheckEvent> onEvent,
      @Nullable ParameterSet checkParameters,
      @Nullable Consumer<@NotNull ParameterSet> onCheckParametersUpdate) {
    super("compoundrow_quality_update", model);
    this.interactor = interactor;
    this.row = row;
    this.rtTol = rtTol;
    this.mzTol = mzTol;
    this.ms2Tol = ms2Tol;
    this.palette = palette;
    this.selectedMemberRow = selectedMemberRow;
    this.onEvent = onEvent;
    this.checkParameters = checkParameters;
    this.onCheckParametersUpdate = onCheckParametersUpdate;
  }

  @Override
  public String getTaskDescription() {
    return "Computing CompoundRow quality checks";
  }

  @Override
  public double getFinishedPercentage() {
    return results == null ? 0.0 : 1.0;
  }

  @Override
  protected void process() {
    results = interactor.compute(row, rtTol, mzTol, ms2Tol, palette, selectedMemberRow, onEvent,
        checkParameters, onCheckParametersUpdate);
  }

  @Override
  protected void updateGuiModel() {
    if (results != null) {
      model.getResults().setAll(results);
    }
    model.computingProperty().set(false);
  }
}
