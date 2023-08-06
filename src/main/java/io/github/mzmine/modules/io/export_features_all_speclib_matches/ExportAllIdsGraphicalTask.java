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

package io.github.mzmine.modules.io.export_features_all_speclib_matches;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.graphicalnodes.LipidSpectrumChart;
import io.github.mzmine.gui.chartbasics.graphicsexport.GraphicsExportDialogFX;
import io.github.mzmine.gui.chartbasics.graphicsexport.GraphicsExportParameters;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectralMatchPanelFX;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExportAllIdsGraphicalTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ExportAllIdsGraphicalTask.class.getName());

  private final FeatureList[] flists;
  private final File dir;
  private final GraphicsExportParameters exportParameters;
  private final int dpiFactor;
  private final boolean exportPng;
  private final boolean exportPdf;
  private final int numIds;
  private String desc = "Exporting all identifications.";
  private NumberFormats form = MZmineCore.getConfiguration().getExportFormats();
  // buffer png export as this takes long time on fx thread
  final int bufferSize = 100;
  final SpectralMatchPanelFX[] panelBuffer = new SpectralMatchPanelFX[bufferSize];
  final File[] filenameBuffer = new File[bufferSize];
  int nextBufferIndex = 0;

  // progress
  private int totalIds = 1;
  private AtomicInteger processed = new AtomicInteger(0);

  protected ExportAllIdsGraphicalTask(ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    flists = parameters.getValue(ExportAllIdsGraphicalParameters.flists).getMatchingFeatureLists();
    dir = parameters.getValue(ExportAllIdsGraphicalParameters.dir);
    numIds = parameters.getValue(ExportAllIdsGraphicalParameters.numMatches);
    dpiFactor = parameters.getValue(ExportAllIdsGraphicalParameters.dpiScalingFactor);
    exportParameters = parameters.getValue(ExportAllIdsGraphicalParameters.export);
    exportPdf = parameters.getValue(ExportAllIdsGraphicalParameters.exportPdf);
    exportPng = parameters.getValue(ExportAllIdsGraphicalParameters.exportPng);
  }

  @Override
  public String getTaskDescription() {
    return desc;
  }

  @Override
  public double getFinishedPercentage() {
    return totalIds == 0 ? 0 : processed.get() / (double) totalIds;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    totalIds = Arrays.stream(flists).mapToInt(FeatureList::getNumberOfRows).sum();


    if (MZmineCore.isHeadLessMode()) {
      MZmineCore.initJavaFxInHeadlessMode();
    }

    try {
      for (FeatureList flist : flists) {
        final File flistFolder = new File(dir, flist.getName());
        FileAndPathUtil.createDirectory(flistFolder);

        for (FeatureListRow row : flist.getRows()) {

          exportSpectralLibraryMatches(flistFolder, row);
          exportLipidIds(flistFolder, row);

          processed.incrementAndGet();
          desc = "Exporting all annotations. %d/%d".formatted(processed.get(), totalIds);
          if (isCanceled()) {
            return;
          }
        }
      }

      if (exportPng) {
        // save rest of the buffer
        saveSpectralMatchesToPng(panelBuffer, filenameBuffer, nextBufferIndex);
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE,
          "Cannot export graphics for lipids and spectral matches  " + ex.getMessage(), ex);
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void exportSpectralLibraryMatches(File flistFolder, FeatureListRow row) {
    final List<SpectralDBAnnotation> matches = row.getSpectralLibraryMatches();
    if (matches.isEmpty()) {
      return;
    }

    for (int i = 0; i < Math.min(matches.size(), numIds); i++) {
      SpectralDBAnnotation match = matches.get(i);
      final SpectralMatchPanelFX spectralMatchPanelFX = new SpectralMatchPanelFX(match);
      spectralMatchPanelFX.applySettings(null);
      var fileName = new File(flistFolder,
          "speclib_%d_mz-%s_score-%s_match_top_%d".formatted(row.getID(),
              form.mz(row.getAverageMZ()), form.score(match.getScore()), i + 1));

      if (exportPdf) {
        Dimension dimension = new Dimension(700, 500);
        try {
          dimension = spectralMatchPanelFX.exportToGraphics("pdf",
              new File(fileName.getAbsolutePath() + ".pdf"));
        } catch (InterruptedException | InvocationTargetException e) {
          logger.log(Level.WARNING, e.getMessage(), e);
        }
      }

      if (exportPng) {
        saveSpectralMatchesToPngBuffered(spectralMatchPanelFX, fileName);
      }
    }
  }

  private void saveSpectralMatchesToPngBuffered(final SpectralMatchPanelFX match, File fileName) {
    // increase total number of exports
    totalIds++;
    // add to buffer
    panelBuffer[nextBufferIndex] = match;
    filenameBuffer[nextBufferIndex] = fileName;
    nextBufferIndex = (nextBufferIndex + 1) % bufferSize;
    if (nextBufferIndex == 0) {
      // save all to png
      saveSpectralMatchesToPng(panelBuffer, filenameBuffer, bufferSize);
    }
  }

  private void saveSpectralMatchesToPng(final SpectralMatchPanelFX[] panelBuffer,
      final File[] filenameBuffer, int elementsToExport) {
    if (elementsToExport == 0) {
      return;
    }

    MZmineCore.runLaterEnsureFxInitialized(() -> {
      int width = 800;
      int height = 400;

      final SnapshotParameters spa = new SnapshotParameters();
      spa.setFill(Color.WHITE);
      spa.setTransform(javafx.scene.transform.Transform.scale(dpiFactor, dpiFactor));
      // scene is required for image export with css style
      BorderPane root = new BorderPane();
      root.setMaxSize(width, height);
      root.setPrefSize(width, height);
      root.setMinSize(width, height);
      Scene scene = new Scene(root, width, height);
      MZmineCore.getConfiguration().getPreferences().getValue(MZminePreferences.theme)
          .apply(scene.getStylesheets());

      WritableImage[] images = new WritableImage[elementsToExport];

      for (int i = 0; i < elementsToExport; i++) {
        var spectralMatchPanelFX = panelBuffer[i];
        File fileName = filenameBuffer[i];
        spectralMatchPanelFX.applySettings(null);
        spectralMatchPanelFX.setMaxSize(width, height);
        spectralMatchPanelFX.setPrefSize(width, height);
        spectralMatchPanelFX.setMinSize(width, height);
        root.setCenter(spectralMatchPanelFX);

        images[i] = spectralMatchPanelFX.snapshot(spa, null);
      }

      // exit FX thread
      Thread subTask = new Thread(() -> {
        for (int i = 0; i < elementsToExport; i++) {
          try {
            File output = new File(filenameBuffer[i].getAbsoluteFile() + ".png");
            ImageIO.write(SwingFXUtils.fromFXImage(images[i], null), "png", output);

            logger.fine("Exported png image " + output.getAbsolutePath());
          } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
          }
        }
        processed.incrementAndGet();
      });
      subTask.setName("Export images");
      subTask.setDaemon(true);
      subTask.start();
    });
  }

  private void exportLipidIds(File flistFolder, FeatureListRow row) {
    final List<MatchedLipid> matches = row.getLipidMatches();
    if (matches.isEmpty()) {
      return;
    }

    for (int i = 0; i < Math.min(matches.size(), numIds); i++) {
      final MatchedLipid lipid = matches.get(i);
      if (lipid == null || lipid.getMatchedFragments() == null || lipid.getMatchedFragments()
          .isEmpty()) {
        continue;
      }

      final LipidSpectrumChart chart = new LipidSpectrumChart(lipid, new AtomicDouble(0d),
          RunOption.THIS_THREAD);
      final SpectraPlot spectraPlot = chart.getSpectraPlot();

      final String formatStr = exportParameters.getValue(GraphicsExportParameters.exportFormat);
      final File exportFile = new File(flistFolder,
          "lipid_%d_mz-%s_score-%s_match-%d.%s".formatted(row.getID(), form.mz(row.getAverageMZ()),
              form.score(lipid.getMsMsScore()), i, formatStr));

      spectraPlot.getXYPlot().getDomainAxis().setAutoRange(true);
      spectraPlot.getXYPlot().getRangeAxis().setAutoRange(true);
      spectraPlot.setTitle(lipid.getLipidAnnotation().getAnnotation(),
          form.mz(lipid.getAccurateMz()) + " " + Objects.requireNonNullElse(lipid.getComment(),
              ""));
      MZmineCore.runOnFxThreadAndWait(() -> {
        GraphicsExportDialogFX dialog = new GraphicsExportDialogFX(false, exportParameters,
            spectraPlot.getChart());
        if (exportPdf) {
          File file = FileAndPathUtil.getRealFilePath(exportFile, "pdf");
          exportParameters.setParameter(GraphicsExportParameters.path, file);
          exportParameters.setParameter(GraphicsExportParameters.exportFormat, "PDF");
          dialog.setParameterValuesToComponents();
          dialog.export();
        }
        if (exportPng) {
          File file = FileAndPathUtil.getRealFilePath(exportFile, "png");
          exportParameters.setParameter(GraphicsExportParameters.path, file);
          exportParameters.setParameter(GraphicsExportParameters.exportFormat, "PNG");
          dialog.setParameterValuesToComponents();
          dialog.export();
        }
      });
    }
  }
}
