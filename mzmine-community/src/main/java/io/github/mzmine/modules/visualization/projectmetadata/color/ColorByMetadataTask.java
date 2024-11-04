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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
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

  private final SimpleColorPalette colors;
  // mark colors as used when they colored a group
  private final Set<Color> usedColors = new HashSet<>();

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

    colors = ConfigService.getDefaultColorPalette().clone();
  }


  public static double maxBrightnessWidth() {
    return maxBrightness - minBrightness;
  }

  public static double centerBrightness() {
    return (maxBrightness + minBrightness) / 2d;
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

    // there might be remaining samples that are not colored
    colorRemainingSamples();

    // finally set colors to raw files
    FxThread.runLater(() -> {
      map.forEach(RawDataFile::setColor);

      if (applySorting) {
        MZmineGUI.sortRawDataFilesAlphabetically(raws);
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
   *
   */
  private void colorRemainingSamples() {
    var remainingRaw = raws.stream().filter(raw -> !map.containsKey(raw)).toList();
    Color[] remainingColors = colors.stream().filter(Predicate.not(usedColors::contains))
        .toArray(Color[]::new);
    if (remainingColors.length == 0) {
      remainingColors = new Color[]{colors.getMainColor()};
    }
    SimpleColorPalette colors = new SimpleColorPalette(remainingColors);
    for (final RawDataFile raw : remainingRaw) {
      colorFadeLighter(List.of(raw), colors.getNextColor(), brightnessPercentRange);
    }
  }

  /**
   * Color around the base color. Brightness and saturation are scaled
   *
   * @param bRange brightness width
   */
  private void colorFadeLighter(final List<RawDataFile> raws, final Color base,
      final double bRange) {
    if (raws.isEmpty()) {
      return;
    }
    if (raws.size() == 1) {
      map.put(raws.getFirst(), base);
      usedColors.add(base); // mark as used
      return;
    }
    if (base.getSaturation() == 0) {
      colorFadeGray(raws);
      return;
    }

    usedColors.add(base); // mark as used

    double h = base.getHue();

    // start saturation --> 1  - tend to higher saturations rather than lower
    double sRange = bRange * 1.5;
    double maxS = within(base.getSaturation() + 0.1 + sRange, minSaturation, 1);
    double minS = maxS - sRange;
    if (minS < minSaturation) {
      minS = minSaturation;
      maxS = within(minS + sRange, minSaturation, 1);
    }

    // start in center brightness to allow both light and dark mode
    // prefer lighter colors
    double maxB = within(base.getBrightness() + bRange * 0.6, minBrightness, maxBrightness);
    double minB = maxB - bRange;
    if (minB < minBrightness) {
      minB = minBrightness;
      maxB = within(minB + bRange, minBrightness, maxBrightness);
    }

    // option to start always in the center - but this makes colors quite dull and they only depend on hue
//    double minB = within(centerBrightness() - bRange / 2d, minBrightness, maxBrightness);
//    double maxB = within(minB + bRange, minBrightness, maxBrightness);

    // first half only increases brightness with max saturation
    double halfN = Math.max(Math.floor(raws.size() / 2d), 1);
    double stepB = (maxB - minB) / (halfN);
    double b = minB;
    int step = 0;
    for (; step < halfN; step++) {
      map.put(raws.get(step), Color.hsb(h, maxS, b));
      b = within(b + stepB, minB, maxB);
    }

    // reduce saturation
    double stepS = (maxS - minS) / Math.max(raws.size() - halfN - 1, 1);
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
    if (raws.isEmpty()) {
      return;
    }
    if (raws.size() == 1) {
      map.put(raws.getFirst(), Color.hsb(0, 0, centerBrightness()));
      return;
    }
    int n = Math.max(raws.size() - 1, 1);
    bRange = Math.min(bRange, maxBrightnessWidth());
    double maxB = within(centerBrightness() + bRange / 2d, minBrightness, maxBrightness);

    int step = 0;
    for (RawDataFile raw : raws) {
      map.put(raw, Color.hsb(0, 0, maxB - bRange * (step / (double) n)));
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
