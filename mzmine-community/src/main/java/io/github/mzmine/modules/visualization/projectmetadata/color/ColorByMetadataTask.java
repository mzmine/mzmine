/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.projectmetadata.color;

import static io.github.mzmine.util.MathUtils.within;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.visualization.projectmetadata.MetadataUtils;
import io.github.mzmine.modules.visualization.projectmetadata.RawDataByMetadataSorter;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractRawDataFileTask;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

public class ColorByMetadataTask extends AbstractRawDataFileTask {

  // make sure brightness works for dark and light mode
  public static final double minBrightness = 0.33;
  public static final double maxBrightness = 0.75;
  public static final double minSaturation = 0.35;

  private final List<RawDataFile> raws;
  private final Map<RawDataFile, Color> map = new HashMap<>();
  private final String colorColumn;
  private final double brightnessPercentRange;
  private final boolean separateBlankQcs;
  private final boolean applySorting;

  public ColorByMetadataTask(final @NotNull Instant moduleCallDate,
      @NotNull final ParameterSet parameters,
      @NotNull final Class<? extends MZmineModule> moduleClass) {
    var raws = parameters.getValue(ColorByMetadataParameters.rawFiles).getMatchingRawDataFiles();
    this(moduleCallDate, parameters, moduleClass, raws);
  }

  public ColorByMetadataTask(final @NotNull Instant moduleCallDate,
      @NotNull final ParameterSet parameters,
      @NotNull final Class<? extends MZmineModule> moduleClass, RawDataFile[] raws) {
    super(null, moduleCallDate, parameters, moduleClass);
    // sort by date
    this.raws = Arrays.stream(raws).sorted(RawDataByMetadataSorter.byDateAndName()).toList();
    colorColumn = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        ColorByMetadataParameters.colorByColumn, null);
    separateBlankQcs = parameters.getValue(ColorByMetadataParameters.separateBlankQcs);
    applySorting = parameters.getValue(ColorByMetadataParameters.applySorting);
    // as a percentage of the maximum
    brightnessPercentRange = maxBrightnessWidth() * parameters.getValue(
        ColorByMetadataParameters.brightnessPercentRange);
  }


  public static double maxBrightnessWidth() {
    return maxBrightness - minBrightness;
  }

  @Override
  protected void process() {
    // color blanks and QCs - they may be recolored right after if they also belong to groups
    // make blanks gray monochrome
    colorFadeGray(SampleTypeFilter.blank().filterFiles(raws));

    // #882255
    Color berry = Color.web("#770940");
    List<RawDataFile> qcs = SampleTypeFilter.qc().filterFiles(raws);
    colorFadeLighter(qcs, berry, brightnessPercentRange);

    // color samples by metadata - this may recolor blanks and QC if they are listed
    if (colorColumn != null) {
      colorByColumn(colorColumn);
    }
    if (isCanceled()) {
      return;
    }

    // finally set colors to raw files
    FxThread.runLater(() -> {
      map.forEach(RawDataFile::setColor);

      if (applySorting) {
        MZmineGUI.sortRawDataFilesAlphabetically();
      }
    });
  }

  /**
   * Color files grouped by metadata column
   *
   * @param colorColumn column to group by
   */
  private void colorByColumn(final String colorColumn) {
    MetadataTable metadata = MetadataUtils.getMetadata();
    MetadataColumn<?> column = metadata.getColumnByName(colorColumn);
    if (column == null) {
      // Do not handle this as an exception / error - this would crash batches
      // but this issue can be resolved later - just show a dialog
      DesktopService.getDesktop().displayErrorMessage(
          "Recoloring: No such metadata column named %s, make sure to import metadata after samples are imported. Open the metadata from the project menu.".formatted(
              colorColumn));
      return;
    }
    List<RawDataFile> filteredRaws = raws;

    SimpleColorPalette colors = ConfigService.getDefaultColorPalette().clone();
    if (separateBlankQcs) {
      filteredRaws = SampleTypeFilter.sample().filterFiles(raws);
      // need to skip the black/white color - already used for blanks
      colors.getNextColor();
    }

    Map<?, List<RawDataFile>> groups = metadata.groupFilesByColumn(filteredRaws, column);
    groups.forEach(
        (_, group) -> colorFadeLighter(group, colors.getNextColor(), brightnessPercentRange));
  }

  /**
   * Color around the base color. Brightness and saturation are scaled
   *
   * @param bRange brightness width
   */
  private void colorFadeLighter(final List<RawDataFile> raws, final Color base,
      final double bRange) {
    if (base.getSaturation() == 0) {
      colorFadeGray(raws);
      return;
    }

    double h = base.getHue();

    // start saturation --> 1  - tend to higher saturations rather than lower
    double sRange = bRange * 1.5;
    double maxS = within(base.getSaturation() + sRange, minSaturation, 1);
    double minS = maxS - sRange;
    if (minS < minSaturation) {
      minS = minSaturation;
      maxS = within(minS + sRange, minSaturation, 1);
    }

    // start in center brightness to allow both light and dark mode
    double minB = within(0.5 - bRange / 2d, minBrightness, maxBrightness);
    double maxB = within(minB + bRange, minBrightness, maxBrightness);

    // first half only increases brightness with max saturation
    double halfN = Math.floor(raws.size() / 2d);
    double stepB = (maxB - minB) / (halfN);
    double b = minB;
    int step = 0;
    for (; step < halfN; step++) {
      map.put(raws.get(step), Color.hsb(h, maxS, b));
      b = within(b + stepB, minB, maxB);
    }

    // reduce saturation
    double stepS = (maxS - minS) / (raws.size() - halfN - 1);
    double s = maxS;
    for (; step < raws.size(); step++) {
      map.put(raws.get(step), Color.hsb(h, s, maxB));
      s = within(s - stepS, minS, maxS); // need to modify after adding the last brightness step
    }
  }

  private void colorFadeGray(final List<RawDataFile> raws) {
    colorFadeGray(raws, brightnessPercentRange);
  }

  /**
   * Color around 50% gray +- bRange/2
   *
   * @param bRange brightness width
   */
  private void colorFadeGray(final List<RawDataFile> raws, double bRange) {
    int n = raws.size() - 1;
    bRange = Math.min(bRange, maxBrightnessWidth());
    double startB = within(0.5 - bRange / 2d, minBrightness, maxBrightness);

    int step = 0;
    for (RawDataFile raw : raws) {
      map.put(raw, Color.hsb(0, 0, startB + bRange * (step / (double) n)));
      step++;
    }
  }

  @Override
  public String getTaskDescription() {
    return "Recoloring data files " + raws.size();
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
    return raws;
  }
}
