/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_features_all_speclib_matches;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectralMatchPanelFX;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.awt.Dimension;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExportAllIdsGraphicalTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ExportAllIdsGraphicalTask.class.getName());

  private final FeatureList[] flists;
  private final File dir;
  private long totalIds = 1;
  private int numIds = 1;
  private String desc = "Exporting all identifications.";
  private int processed = 0;

  protected ExportAllIdsGraphicalTask(ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    flists = parameters.getValue(ExportAllIdsGraphicalParameters.flists).getMatchingFeatureLists();
    dir = parameters.getValue(ExportAllIdsGraphicalParameters.dir);
    numIds = parameters.getValue(ExportAllIdsGraphicalParameters.numMatches);
  }

  @Override
  public String getTaskDescription() {
    return desc;
  }

  @Override
  public double getFinishedPercentage() {
    return processed / (double) totalIds;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    totalIds = Arrays.stream(flists).flatMap(FeatureList::stream)
        .filter(row -> !row.getSpectralLibraryMatches().isEmpty()).count();

    final NumberFormats form = MZmineCore.getConfiguration().getExportFormats();

    for (FeatureList flist : flists) {
      final File flistFolder = new File(dir, flist.getName());
      flistFolder.mkdirs();
      for (FeatureListRow row : flist.getRows()) {
        final List<SpectralDBAnnotation> matches = row.getSpectralLibraryMatches();
        if (matches.isEmpty()) {
          continue;
        }

        for (int i = 0; i < Math.min(matches.size(), numIds); i++) {
          SpectralDBAnnotation match = matches.get(i);
          final SpectralMatchPanelFX spectralMatchPanelFX = new SpectralMatchPanelFX(match);
          var fileName = new File(flistFolder,
              "%d_mz-%s_score-%s_match-%d".formatted(row.getID(), form.mz(row.getAverageMZ()),
                  form.score(match.getScore()), i));

          Dimension dimension = new Dimension(700, 500);
          try {
            dimension = spectralMatchPanelFX.exportToGraphics("pdf",
                new File(fileName.getAbsolutePath() + ".pdf"));
          } catch (InterruptedException | InvocationTargetException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
          }

          /*final WritableImage img = new WritableImage((int) dimension.getWidth(),
              (int) dimension.getHeight());
          Platform.runLater(() -> {
            spectralMatchPanelFX.snapshot(new SnapshotParameters(), img);
            try {
              ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png",
                  new File(fileName.getAbsoluteFile() + ".png"));
            } catch (IOException e) {
              logger.log(Level.WARNING, e.getMessage(), e);
            }
          });*/
        }

        processed++;
        desc = "Exporting all annotations. %d/%d".formatted(processed, totalIds);
        if (isCanceled()) {
          return;
        }

      }
    }

    setStatus(TaskStatus.FINISHED);
  }
}
