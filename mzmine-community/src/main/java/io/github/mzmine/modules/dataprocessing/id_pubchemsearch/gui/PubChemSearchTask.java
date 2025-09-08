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

package io.github.mzmine.modules.dataprocessing.id_pubchemsearch.gui;

import com.google.common.collect.Lists;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.CompoundData;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.PubChemApiClient;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.PubChemApiClient.PubChemApiException;
import io.github.mzmine.modules.dataprocessing.id_pubchemsearch.PubChemSearch;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.IOException;
import java.util.List;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PubChemSearchTask extends FxUpdateTask<PubChemResultsModel> {

  private final SearchType searchType;
  private final @Nullable MZTolerance tolerance;
  private double progress = 0d;
  private final @Nullable FeatureListRow rowToScoreAgainst;
  private long totalResults = 0;

  enum SearchType {
    MASS, FORMULA;
  }

  PubChemSearchTask(@NotNull String taskName, PubChemResultsModel model, SearchType searchType) {
    super(taskName, model);
    this.searchType = searchType;
    tolerance = model.getMzTolerance();
    rowToScoreAgainst = model.getSelectedRow();
  }

  @Override
  public boolean checkPreConditions() {

    return switch (searchType) {
      case MASS -> tolerance != null && model.getMassToSearch() != null;
      case FORMULA -> model.getFormulaToSearch() != null;
    };
  }


  @Override
  protected void process() {
    final PubChemSearch searchConfig = switch (searchType) {
      case MASS -> // nullability of tolerance checked in checkPreConditions
          PubChemSearch.byMassRange(model.getMassToSearch(), tolerance);
      case FORMULA -> PubChemSearch.byFormula(model.getFormulaToSearch());
    };

    try (final PubChemApiClient client = new PubChemApiClient()) {
      final List<String> cids = client.findCids(searchConfig);
      totalResults = cids.size();
      progress = 0.2;
      final List<List<String>> chunks = Lists.partition(cids,
          PubChemApiClient.DEFAULT_CID_CHUNK_SIZE);

      final double step = (1 - progress) / chunks.size();

      for (List<String> chunk : chunks) {
        if (isCanceled()) {
          return;
        }
        final List<CompoundData> compoundData = client.fetchPropertiesForChunk(searchConfig, chunk);
        final List<TreeItem<CompoundDBAnnotation>> newItems = compoundData.stream()
            .map(cd -> cd.convertAndScore(rowToScoreAgainst, model.getIonType())).map(TreeItem::new)
            .toList();

        progress += step;
        FxThread.runLater(() -> {
          // don't use the updateGuiModel method, because this way we can already add intermediate results
          model.compoundsProperty().addAll(newItems);
        });
      }
    } catch (PubChemApiException e) {
      error("PubChem API error: " + e.getMessage(), e);
    } catch (IOException e) {
      error("IOException during PubChem search: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      error("Search interrupted.");
    }
  }

  @Override
  protected void updateGuiModel() {

  }

  @Override
  public String getTaskDescription() {
    return totalResults != 0 ? "Searching PubChem - downloading %d results...".formatted(
        totalResults) : "Searching PubChem...";
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }
}
