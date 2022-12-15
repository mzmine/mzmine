/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

/**
 * @author https://github.com/SteffenHeu
 */
package io.github.mzmine.modules.dataprocessing.filter_mobilitymzregionextraction;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.visualization.ims_mobilitymzplot.PlotType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.awt.geom.Point2D;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author https://github.com/SteffenHeu
 */
public class MobilityMzRegionExtractionModule implements MZmineProcessingModule {

  public static final String NAME = "m/z mobility region extraction";

  /*public static void extractFeaturesToNewFeatureList(Collection<ModularFeature> features,
      List<List<Point2D>> pointsLists) {

    List<Path2D> regions = new ArrayList<>();
    pointsLists.forEach(list -> regions.add(MzMobilityRegionExtractionTask.getShape(list)));

    List<ModularFeature> extracted = IonMobilityUtils.getFeaturesWithinRegion(features, regions);

    ModularFeatureList flist = new ModularFeatureList("Selected features region extraction");

    // todo: how to deal with alighed feature lists? features would no longer be aligned
  }*/

  public static void runExtractionForFeatureList(ModularFeatureList featureList,
      List<List<Point2D>> region, String suffix, PlotType ccsOrMobility) {
    ParameterSet parameterSet = MZmineCore.getConfiguration()
        .getModuleParameters(MobilityMzRegionExtractionModule.class).cloneParameterSet();

    parameterSet.getParameter(MobilityMzRegionExtractionParameters.regions).setValue(region);
    parameterSet.getParameter(MobilityMzRegionExtractionParameters.suffix).setValue(suffix);
    parameterSet.getParameter(MobilityMzRegionExtractionParameters.ccsOrMobility)
        .setValue(ccsOrMobility);

    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    Task task = new MobilityMzRegionExtractionTask(parameterSet, featureList,
        MZmineCore.getProjectManager().getCurrentProject(), storage, Instant.now());
    MZmineCore.getTaskController().addTask(task);
  }

  @NotNull
  @Override
  public String getName() {
    return NAME;
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return MobilityMzRegionExtractionParameters.class;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Extracts regions of m/z-mobility regions ";
  }

  @NotNull
  @Override
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    ModularFeatureList[] featureLists = parameters
        .getParameter(MobilityMzRegionExtractionParameters.featureLists).getValue()
        .getMatchingFeatureLists();

    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    for (ModularFeatureList featureList : featureLists) {
      Task task = new MobilityMzRegionExtractionTask(parameters, featureList, project, storage, moduleCallDate);
      tasks.add(task);
    }

    return ExitCode.OK;
  }

  @NotNull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELISTFILTERING;
  }
}
