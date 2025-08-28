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

package io.github.mzmine.modules.tools.siriusapi.modules.export;

import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportParameters;
import io.github.mzmine.modules.tools.siriusapi.MzmineToSirius;
import io.github.mzmine.modules.tools.siriusapi.Sirius;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.operations.TaskSubSupplier;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class ExportToSiriusTask extends AbstractFeatureListTask implements
    TaskSubSupplier<Map<Integer, String>> {

  private static final Logger logger = Logger.getLogger(ExportToSiriusTask.class.getName());
  private Map<Integer, String> rowIdToSiriusId = null;
  private ModularFeatureList flist;

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   * @param parameters
   * @param moduleClass
   */
  protected ExportToSiriusTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull ParameterSet parameters, @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    flist = parameters.getValue(ExportToSiriusParameters.flist)
        .getMatchingFeatureLists()[0];
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {
    final String idString = parameters.getOptionalValue(ExportToSiriusParameters.rowIds).orElse("");
    final List<FeatureListRow> rows;
    if (idString == null || idString.isBlank()) {
      rows = flist.getRows();
    } else {
      rows = FeatureUtils.idStringToRows(flist, idString);
    }

    try (Sirius s = new Sirius()) {
      rowIdToSiriusId = MzmineToSirius.exportToSiriusUnique(s, rows);
    } catch (Exception e) {
      switch (e) {
        case WebClientResponseException r -> error(r.getResponseBodyAsString(), r);
        default -> error(e.getMessage(), e);
      }
    }
  }

  @Override
  public String getTaskDescription() {
    return "Exporting features of feature list %s to Sirius".formatted(inQuotes(flist.getName()));
  }

  @Override
  public boolean isCanceled() {
    return super.isCanceled();
  }

  @Override
  public Task getParentTask() {
    return this;
  }

  @Override
  public void setParentTask(@Nullable Task parentTask) {
    return;
  }

  @Override
  public Map<Integer, String> get() {
    if(getStatus() != TaskStatus.FINISHED) {
      throw new RuntimeException("Results were queried before task finished.");
    }
    return rowIdToSiriusId;
  }
}
