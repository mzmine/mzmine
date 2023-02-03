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

package io.github.mzmine.modules.dataprocessing.filter_groupms2_refine;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Filters out feature list rows.
 */
public class GroupedMs2RefinementTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(GroupedMs2RefinementTask.class.getName());

  private final FeatureList featureList;
  private final ParameterSet parameters;
  private final Double minAbsFeatureHeight;
  private final Double minRelFeatureHeight;
  private final AtomicLong totalFeatures = new AtomicLong(0);
  private final AtomicLong processedFeatures = new AtomicLong(0);
  private final AtomicLong removedScans = new AtomicLong(0);
  private final AtomicLong totalScans = new AtomicLong(0);
  private final AtomicLong totalUniqueScans = new AtomicLong(0);

  /**
   * @param featureList  feature list to process.
   * @param parameterSet task parameters.
   */
  public GroupedMs2RefinementTask(final FeatureList featureList, final ParameterSet parameterSet,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureList = featureList;

    parameters = parameterSet;
    // RT has two options / tolerance is only provided for second option
    minAbsFeatureHeight = parameters.getValue(
        GroupedMs2RefinementParameters.minimumAbsoluteFeatureHeight);
    minRelFeatureHeight = parameters.getValue(
        GroupedMs2RefinementParameters.minimumRelativeFeatureHeight);
  }

  public GroupedMs2RefinementTask(final FeatureList featureList, final double minRelFeatureHeight,
      final double minAbsFeatureHeight) {
    super(null, Instant.now());
    this.featureList = featureList;
    this.minRelFeatureHeight = minRelFeatureHeight;
    this.minAbsFeatureHeight = minAbsFeatureHeight;
    parameters = MZmineCore.getConfiguration().getModuleParameters(GroupedMs2RefinementModule.class)
        .cloneParameterSet();
    parameters.setParameter(GroupedMs2RefinementParameters.minimumRelativeFeatureHeight,
        minRelFeatureHeight);
    parameters.setParameter(GroupedMs2RefinementParameters.minimumAbsoluteFeatureHeight,
        minAbsFeatureHeight);
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);

      processFeatureList(this);

      if (isCanceled()) {
        return;
      }

      featureList.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(GroupedMs2RefinementModule.class, parameters,
              getModuleCallDate()));
      setStatus(TaskStatus.FINISHED);
      logger.info("Finished refining fragment scans for features in " + featureList.getName());

    } catch (Exception t) {
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
      logger.log(Level.SEVERE,
          "Error while refining fragment scans for features in " + featureList.getName(), t);
    }
  }

  /**
   * Refine fragmentation scans of the selected feature list of this object. This is done for all
   * {@link RawDataFile}.
   *
   * @param parentTask this or the parent task listed in the task controller
   */
  public void processFeatureList(final AbstractTask parentTask) {
    if (featureList.getRawDataFiles().size() == 1) {
      processDataFile(parentTask, featureList.getRawDataFiles().get(0));
    } else {
      featureList.getRawDataFiles().parallelStream()
          .forEach(raw -> processDataFile(parentTask, raw));
    }

    // log statistics
    MZmineConfiguration config = MZmineCore.getConfiguration();
    logger.info("""
        Refinement of fragmentation scan-feature assignment (total=%s; unique scans=%s) has removed %d scans (%s) from lower abundant feature with a relative intensity threshold of %s.
        The absolute minimum intensity threshold of %s does not count to this value.""".formatted(
        totalScans.get(), totalUniqueScans.get(), removedScans.get(),
        config.getPercentFormat().format(removedScans.get() / (double) totalScans.get()),
        config.getPercentFormat().format(minRelFeatureHeight),
        config.getIntensityFormat().format(minAbsFeatureHeight)));
  }

  /**
   * @param parentTask this or the parent task listed in the task controller
   * @param raw        file to process
   */
  private void processDataFile(final AbstractTask parentTask, final RawDataFile raw) {
    // create map
    List<ModularFeature> features = featureList.getFeatures(raw);
    totalFeatures.addAndGet(features.size());
    features.sort(new FeatureSorter(SortingProperty.Height, SortingDirection.Descending));

    // map scan to the highest feature height
    Object2FloatOpenHashMap<Scan> scanToHeightMap = new Object2FloatOpenHashMap<>();

    for (final ModularFeature feature : features) {
      if (parentTask.isCanceled()) {
        return;
      }
      final float height = feature.getHeight();
      if (height < minAbsFeatureHeight) {
        // remove all scans
        feature.setAllMS2FragmentScans(null);
      } else {
        // filter by relative height to max highest feature
        var filteredScans = feature.getAllMS2FragmentScans().stream().filter(ms2 -> {
          float maxHeight = scanToHeightMap.computeIfAbsent(ms2, key -> height);

          boolean keep = height / maxHeight >= minRelFeatureHeight;
          if (!keep) {
            removedScans.incrementAndGet();
          }
          totalScans.incrementAndGet();
          return keep;
        }).toList();

        feature.setAllMS2FragmentScans(filteredScans);
      }
      processedFeatures.incrementAndGet();
    }
    totalUniqueScans.addAndGet(scanToHeightMap.size());
  }

  @Override
  public double getFinishedPercentage() {
    return totalFeatures.get() == 0 ? 0.0 : processedFeatures.get() / (double) totalFeatures.get();
  }

  @Override
  public String getTaskDescription() {
    return "Refine grouped fragmentation spectra for features in " + featureList.getName();
  }
}
