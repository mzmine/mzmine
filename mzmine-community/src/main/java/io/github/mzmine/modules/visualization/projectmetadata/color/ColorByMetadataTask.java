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

package io.github.mzmine.modules.visualization.projectmetadata.color;

import com.google.common.collect.Lists;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.Themes;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.visualization.projectmetadata.RawDataByMetadataSorter;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractRawDataFileTask;
import io.github.mzmine.util.color.ColorUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

public class ColorByMetadataTask extends AbstractRawDataFileTask {

  private final List<RawDataFile> raws;
  private final Map<RawDataFile, Color> map = new HashMap<>();
  private final String colorColumn;
  private final double brightnessPercentRange;
  private final boolean separateBlankQcs;
  private final boolean applySorting;

  private final SimpleColorPalette colors;
  // mark colors as used when they colored a group
  private final Set<Color> usedColors = new HashSet<>();
  private final ColorByMetadataConfig config;

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

    // metadata options like column and how to scale colors
    final ColorByMetadataColumnParameters columnSelection = parameters.getEmbeddedParametersIfSelectedOrElse(
        ColorByMetadataParameters.columnSelection, null);
    if (columnSelection != null) {
      colorColumn = columnSelection.getValue(ColorByMetadataColumnParameters.colorByColumn);
      var transform = columnSelection.getValue(ColorByMetadataColumnParameters.gradientTransform);
      var numericOption = columnSelection.getValue(
          ColorByMetadataColumnParameters.colorNumericValues);
      config = new ColorByMetadataConfig(numericOption, transform);
    } else {
      colorColumn = null;
      config = ColorByMetadataConfig.createDefault();
    }

    separateBlankQcs = parameters.getValue(ColorByMetadataParameters.separateBlankQcs);
    applySorting = parameters.getValue(ColorByMetadataParameters.applySorting);
    // as a percentage of the maximum
    brightnessPercentRange = ColorUtils.maxBrightnessWidth() * parameters.getValue(
        ColorByMetadataParameters.brightnessPercentRange);

    colors = config.cloneResetCategoryPalette();
  }

  @Override
  protected void process() {
    // color blanks and QCs - they may be recolored right after if they also belong to groups
    // make blanks gray monochrome
    List<RawDataFile> blanks = SampleTypeFilter.blank().filterFiles(raws);
    colorFadeLighter(blanks, colors.getNeutralColor(), brightnessPercentRange);

    // #882255
//    Color qcColor = Color.web("#770940");
//    Color qcColor = Color.web("#8e1be1");
    // use the last color that is not dark or light (to keep previous behaviour in mose cases)
    Color qcColor = Lists.reverse(colors).stream()
        .filter(clr -> !ColorUtils.isDark(clr) && !ColorUtils.isLight(clr)).findFirst()
        .orElse(Color.web("#bf2c84")); // positive, negative, or last color?
    List<RawDataFile> qcs = SampleTypeFilter.qc().filterFiles(raws);
    colorFadeLighter(qcs, qcColor, brightnessPercentRange);

    final Themes theme = ConfigService.getPreferences().getValue(MZminePreferences.theme);
    // exclude light colors in light mode and dark colors in dark mode
    final List<Color> excluded = colors.stream().filter(
        clr -> (theme.isDark() && ColorUtils.isDark(clr)) || (!theme.isDark() && ColorUtils.isLight(
            clr))).toList();
    usedColors.addAll(excluded);

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
    List<RawDataFile> filteredRaws = raws;
    if (separateBlankQcs) {
      filteredRaws = SampleTypeFilter.sample().filterFiles(raws);
      // need to skip the black/white color - already used for blanks
      colors.removeFirst();
    }

    MetadataTable metadata = ProjectService.getMetadata();
    MetadataColumn<?> column = metadata.getColumnByName(colorColumn);
    if (column == null) {
      // Do not handle this as an exception / error - this would crash batches
      // but this issue can be resolved later - just show a dialog
      DialogLoggerUtil.showErrorDialog("Missing metadata column",
          "Recoloring: No such metadata column named %s, make sure to import metadata after samples are imported. Open the metadata from the project menu.".formatted(
              colorColumn));
      return;
    }

    final var grouping = ColorByMetadataUtils.colorByColumn(column, filteredRaws, config);

    for (ColorByMetadataGroup group : grouping.groups()) {
      colorFadeLighter(group.group().files(), group.color(), brightnessPercentRange);
    }
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

    usedColors.add(base);
    var colors = ColorUtils.colorFadeLighter(raws.size(), base, bRange);

    for (int i = 0; i < raws.size(); i++) {
      map.put(raws.get(i), colors.get(i)); // actual recoloring
    }
  }


  @Override
  protected void addAppliedMethod() {
    if (Objects.equals(ColorByMetadataModule.class, getModuleClass())) {
      // module was run by itself
      super.addAppliedMethod();
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
