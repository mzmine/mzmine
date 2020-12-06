/*
 *
 *  * Copyright 2006-2020 The MZmine Development Team
 *  *
 *  * This file is part of MZmine.
 *  *
 *  * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  * General Public License as published by the Free Software Foundation; either version 2 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  * Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  * USA
 *
 *
 */

package io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing;

import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SavitzkyGolayFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithMobilogramPreview;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;

public class MobilogramSmootherSetupDialog extends ParameterSetupDialogWithMobilogramPreview {

  private int previousDataSetNum;

  public MobilogramSmootherSetupDialog(boolean valueCheckRequired,
      ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    previousDataSetNum = 0;

  }

  @Override
  protected void parametersChanged() {
    super.parametersChanged();

    updateParameterSetFromComponents();
    Mobilogram mobilogram = controller.getSelectedMobilogram();

    List<String> errorMessages = new ArrayList<>();
    if (!parameterSet.checkParameterValues(errorMessages) || mobilogram == null) {
      return;
    }

    final double[] weights = SavitzkyGolayFilter.getNormalizedWeights(
        parameterSet.getParameter(MobilogramSmootherParameters.filterWidth).getValue());
    Mobilogram smoothed = MobilogramSmootherTask.sgSmoothMobilogram(mobilogram, weights);

    if (previousDataSetNum != -1) {
      controller.getMobilogramChart().removeDataSet(previousDataSetNum);
    }

    PreviewMobilogram preview = new PreviewMobilogram(smoothed);
    previousDataSetNum = controller.getMobilogramChart().addDataset(preview);
  }

  @Override
  public void onMobilogramSelectionChanged(Mobilogram newMobilogram) {
    parametersChanged();
  }
}
