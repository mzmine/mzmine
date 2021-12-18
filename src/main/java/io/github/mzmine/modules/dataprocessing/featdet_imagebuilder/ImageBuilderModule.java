/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderTask;
import io.github.mzmine.modules.visualization.image.ImageVisualizerParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 *
 * The image builder will use the ADAP Chromatogram builder task
 */
public class ImageBuilderModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Image builder";
  private static final String MODULE_DESCRIPTION =
      "This module connects data points from mass lists and builds images.";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    RawDataFile[] files = parameters.getParameter(ImageBuilderParameters.rawDataFiles).getValue()
        .getMatchingRawDataFiles();

    ParameterSet parametersFromImageBuilder = initParameters(parameters);

    MemoryMapStorage storage = MemoryMapStorage.forFeatureList();

    for (RawDataFile file : files) {
      if (!(file instanceof ImagingRawDataFile)) {
        continue;
      }
      Task task = new ModularADAPChromatogramBuilderTask(project, file, parametersFromImageBuilder,
          storage, moduleCallDate);
      tasks.add(task);
    }

    return ExitCode.OK;
  }

  private ParameterSet initParameters(ParameterSet parameters) {
    ParameterSet newParameterSet = MZmineCore.getConfiguration().getModuleParameters(
        ModularADAPChromatogramBuilderModule.class).cloneParameterSet();
    newParameterSet.setParameter(ADAPChromatogramBuilderParameters.scanSelection,
        parameters.getParameter(ImageBuilderParameters.scanSelection).getValue());
    newParameterSet.setParameter(ADAPChromatogramBuilderParameters.minimumScanSpan,
        parameters.getParameter(ImageBuilderParameters.minTotalSignals).getValue());
    newParameterSet.setParameter(ADAPChromatogramBuilderParameters.mzTolerance,
        parameters.getParameter(ImageBuilderParameters.mzTolerance).getValue());
    newParameterSet.setParameter(ADAPChromatogramBuilderParameters.suffix,
        parameters.getParameter(ImageBuilderParameters.suffix).getValue());
    newParameterSet.setParameter(ADAPChromatogramBuilderParameters.IntensityThresh2, 0.0);
    newParameterSet.setParameter(ADAPChromatogramBuilderParameters.startIntensity, 0.0);
    newParameterSet.setParameter(ADAPChromatogramBuilderParameters.allowSingleScans,
        new HashMap<String, Boolean>());
    return newParameterSet;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.EIC_DETECTION;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ImageBuilderParameters.class;
  }

}
