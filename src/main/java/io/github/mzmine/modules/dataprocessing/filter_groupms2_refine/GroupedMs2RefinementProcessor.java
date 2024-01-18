/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.operations.AbstractTaskSubProcessor;
import io.github.mzmine.util.FeatureSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Filters out feature list rows.
 */
public class GroupedMs2RefinementProcessor extends AbstractTaskSubProcessor {

  private static final Logger logger = Logger.getLogger(
      GroupedMs2RefinementProcessor.class.getName());

  private final FeatureList featureList;
  private final Double minAbsFeatureHeight;
  private final Double minRelFeatureHeight;
  private final AtomicLong totalFeatures = new AtomicLong(0);
  private final AtomicLong processedFeatures = new AtomicLong(0);
  private final AtomicLong removedScans = new AtomicLong(0);
  private final AtomicLong totalScans = new AtomicLong(0);
  private final AtomicLong totalUniqueScans = new AtomicLong(0);
  private final @NotNull String description;

  public GroupedMs2RefinementProcessor(@Nullable AbstractTask parentTask,
      final FeatureList featureList, final double minRelFeatureHeight,
      final double minAbsFeatureHeight) {
    super(parentTask);
    this.featureList = featureList;
    this.minRelFeatureHeight = minRelFeatureHeight;
    this.minAbsFeatureHeight = minAbsFeatureHeight;

    description = "Refining grouped MS2 scans in list " + featureList.getName();
  }

  /**
   * Refine fragmentation scans of the selected feature list of this object. This is done for all
   * {@link RawDataFile}.
   */
  @Override
  public void process() {
    if (featureList.getRawDataFiles().size() == 1) {
      processDataFile(featureList.getRawDataFiles().get(0));
    } else {
      // cannot use forEach in parallel stream as this may not block the carrier thread
      long success = featureList.getRawDataFiles().parallelStream() //
          .filter(this::processDataFile) // if success then true
          .count();
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
   * @param raw file to process
   * @return true on success
   */
  private boolean processDataFile(final RawDataFile raw) {
    // create map
    List<ModularFeature> features = featureList.getFeatures(raw);
    totalFeatures.addAndGet(features.size());
    features.sort(new FeatureSorter(SortingProperty.Height, SortingDirection.Descending));

    // map scan to the highest feature height
    Object2FloatOpenHashMap<Scan> scanToHeightMap = new Object2FloatOpenHashMap<>();

    for (final ModularFeature feature : features) {
      if (isCanceled()) {
        return false;
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
    return true;
  }

  @Override
  public @NotNull String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return totalFeatures.get() == 0 ? 0.0 : processedFeatures.get() / (double) totalFeatures.get();
  }

}
