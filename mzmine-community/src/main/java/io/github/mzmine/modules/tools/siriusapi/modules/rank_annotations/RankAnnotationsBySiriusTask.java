/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.tools.siriusapi.modules.rank_annotations;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.tools.siriusapi.JobWaiterTask;
import io.github.mzmine.modules.tools.siriusapi.MzmineToSirius;
import io.github.mzmine.modules.tools.siriusapi.Sirius;
import io.github.mzmine.modules.tools.siriusapi.SiriusToMzmine;
import io.github.mzmine.modules.tools.siriusapi.modules.fingerid.SiriusFingerIdModule;
import io.github.mzmine.modules.tools.siriusapi.modules.fingerid.SiriusFingerIdParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureTableFXUtil;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.sirius.ms.sdk.model.ConfidenceMode;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.JobSubmission;
import io.sirius.ms.sdk.model.SearchableDatabase;
import io.sirius.ms.sdk.model.StructureDbSearch;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankAnnotationsBySiriusTask extends AbstractFeatureListTask {

  private final @NotNull String idStr;
  private final @NotNull ModularFeatureList flist;
  private String description = "Ranking annotations using Sirius.";

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   * @param parameters
   * @param moduleClass
   */
  protected RankAnnotationsBySiriusTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    idStr = parameters.getOptionalValue(SiriusFingerIdParameters.rowIds).orElse("");
    flist = parameters.getValue(SiriusFingerIdParameters.flist).getMatchingFeatureLists()[0];
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {
    final List<FeatureListRow> rows = (idStr.isBlank() ? flist.getRows()
        : FeatureUtils.idStringToRows(flist, idStr)).stream()
        .filter(r -> !r.getCompoundAnnotations().isEmpty()).toList();
    totalItems = rows.size();

    try (Sirius sirius = new Sirius()) {

      for (FeatureListRow row : rows) {

        final List<CompoundDBAnnotation> annotations = row.getCompoundAnnotations();

        if (annotations.isEmpty()) {
          return;
        }

        // inchi may be there but not the smiles
        annotations.forEach(CompoundDBAnnotation::enrichMetadata);

        // this exports all annotations to a single file, in general use for mzmine
        // it may be better to create a single database per row, to not mix up structures
        // from different calls, however.
        final SearchableDatabase customDatabase = MzmineToSirius.toCustomDatabase(annotations,
            sirius);

        final String siriusId = MzmineToSirius.exportToSiriusUnique(sirius, List.of(row))
            .get(row.getID());

        JobSubmission config = sirius.api().jobs().getDefaultJobConfig(false, false, true);
        StructureDbSearch structureDbParams = config.getStructureDbSearchParams();
        structureDbParams.setStructureSearchDBs(List.of(customDatabase.getDatabaseId()));
        structureDbParams.setExpansiveSearchConfidenceMode(
            ConfidenceMode.OFF); // no fallback to pubchem for this application

        final PolarityType polarity = FeatureUtils.extractBestPolarity(row)
            .orElse(PolarityType.POSITIVE);

        switch (polarity) {
          case NEGATIVE -> config.setFallbackAdducts(List.of("[M-H]-"));
          default -> config.setFallbackAdducts(List.of("[M+H]+", "[M+NH4]+", "[M+Na]+"));
        }

        config.setAlignedFeatureIds(List.of(siriusId));
        final Job job = sirius.api().jobs()
            .startJob(sirius.getProject().getProjectId(), config, List.of(JobOptField.PROGRESS));
        JobWaiterTask task = new JobWaiterTask(SiriusFingerIdModule.class, Instant.now(),
            SiriusFingerIdParameters.of(List.of(row)), () -> sirius.api().jobs()
            .getJob(sirius.getProject().getProjectId(), job.getId(),
                List.of(JobOptField.PROGRESS)), () -> {});

        task.run();

        final List<CompoundDBAnnotation> rankedAnnotations = SiriusToMzmine.getSiriusAnnotations(
            sirius, siriusId, row);

        row.setCompoundAnnotations(rankedAnnotations);

        FeatureTableFXUtil.updateCellsForFeatureList(flist);
        finishedItems.getAndIncrement();

        if (isCanceled()) {
          cancel();
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getTaskDescription() {
    return description;
  }
}
