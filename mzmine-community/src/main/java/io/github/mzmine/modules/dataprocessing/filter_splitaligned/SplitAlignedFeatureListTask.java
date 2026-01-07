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

package io.github.mzmine.modules.dataprocessing.filter_splitaligned;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.SamplesGroupedBy;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SplitAlignedFeatureListTask extends AbstractFeatureListTask {

  private final FeatureList flist;
  @NotNull
  private final MZmineProject project;
  private final String gropingColumn;
  private List<FeatureList> resultFlists = new ArrayList<>();

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   */
  protected SplitAlignedFeatureListTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass, FeatureList featureList,
      @NotNull MZmineProject project) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.flist = featureList;
    this.project = project;
    this.gropingColumn = parameters.getOptionalValue(SplitAlignedFeatureListParameters.grouping)
        .orElse(null);
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return resultFlists;
  }

  @Override
  protected void process() {
    final MetadataTable metadata = ProjectService.getMetadata();
    final List<? extends SamplesGroupedBy<?>> sampleGroups = getGroupedSamples(metadata);

    totalItems = (long) flist.getNumberOfRows() * flist.getNumberOfRawDataFiles();

    for (final SamplesGroupedBy<?> sampleGroup : sampleGroups) {

      final ModularFeatureList resultFlist = FeatureListUtils.createCopy(flist, null,
          "_" + (sampleGroup.value() != null ? sampleGroup.valueString()
              : sampleGroup.files().getFirst().getName()), getMemoryMapStorage(), false,
          sampleGroup.files(), false, null, null);

      for (final FeatureListRow row : flist.getRows()) {
        if (sampleGroup.files().stream().noneMatch(row::hasFeature)) {
          finishedItems.getAndAdd(sampleGroup.size());
          continue;
        }

        // specifically do not copy any annotations or so, as we would not know which feature it was based on.
        final ModularFeatureListRow copiedRow = new ModularFeatureListRow(resultFlist, row.getID());

        for (final RawDataFile file : sampleGroup.files()) {
          final Feature feature = row.getFeature(file);
          if (feature == null) {
            finishedItems.getAndIncrement();
            continue;
          }
          copiedRow.addFeature(file, new ModularFeature(resultFlist, feature), false);
          finishedItems.getAndIncrement();
        }

        copiedRow.applyRowBindings();
        resultFlist.addRow(copiedRow);
      }

      resultFlists.add(resultFlist);
    }

    for (FeatureList resultFlist : resultFlists) {
      project.addFeatureList(resultFlist);
    }
  }

  private @NotNull List<? extends SamplesGroupedBy<?>> getGroupedSamples(MetadataTable metadata) {
    List<? extends SamplesGroupedBy<?>> sampleGroups;
    if (gropingColumn != null) {
      sampleGroups = metadata.groupFilesByColumnIncludeNull(
          metadata.getColumnByName(gropingColumn));
    } else {
      sampleGroups = flist.getRawDataFiles().stream()
          .map(file -> new SamplesGroupedBy<>(null, List.of(file), 0)).toList();
    }
    return sampleGroups;
  }

  @Override
  public String getTaskDescription() {
    return "Splitting aligned feature list %s into individual feature lists.".formatted(
        flist.getName());
  }
}
