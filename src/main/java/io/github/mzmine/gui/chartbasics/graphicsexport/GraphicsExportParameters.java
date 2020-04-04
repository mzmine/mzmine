/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui.chartbasics.graphicsexport;

import io.github.mzmine.parameters.parametertypes.colorpalette.ColorPaletteParameter;
import java.awt.Dimension;
import java.io.File;
import java.text.DecimalFormat;
import org.jfree.chart.JFreeChart;
import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ColorParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.DimensionUnitUtil;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.DimensionUnitUtil.DimUnit;
import io.github.mzmine.util.files.FileAndPathUtil;
import javafx.application.Platform;
import javafx.scene.paint.Color;

public class GraphicsExportParameters extends SimpleParameterSet {

  public enum FixedSize {
    Chart, Plot;
  }

  public static final FileNameParameter path =
      new FileNameParameter("Path", "The file path", FileSelectionType.SAVE);

  public static final DoubleParameter width = new DoubleParameter("Width",
      "Uses fixed width for the chart or plot", DecimalFormat.getInstance(), 15.0);

  public static final DoubleParameter dpi = new DoubleParameter("Resolution (dpi)",
      "dots per inch resolution (for print usually 300 up to 600 dpi)", DecimalFormat.getInstance(),
      300.0);

  public static final OptionalParameter<DoubleParameter> height =
      new OptionalParameter<DoubleParameter>(new DoubleParameter("Height",
          "Only uses width if height is unchecked. Otherwise uses fixed height for the chart or plot",
          DecimalFormat.getInstance(), 8.0));

  public static final ComboParameter<String> exportFormat = new ComboParameter<String>("Format",
      "The image export format", new String[] {"PDF", "EMF", "EPS", "SVG", "JPG", "PNG"}, "PNG");

  public static final ComboParameter<FixedSize> fixedSize =
      new ComboParameter<FixedSize>("Fixed size for",
          "Fixed size for the plot (the data space without axes and titles) or the whole chart.",
          FixedSize.values(), FixedSize.Chart);

  public static final ColorParameter color =
      new ColorParameter("Background", "Background color", Color.WHITE);
  public static final DoubleParameter alpha = new DoubleParameter("Transparency",
      "Transparency from 0.0-1.0 (fully visible)", DecimalFormat.getInstance(), 1.0, 0.0, 1.0);

  public static final ComboParameter<DimUnit> unit = new ComboParameter<>("Unit",
      "Unit for width and height dimensions", DimUnit.values(), DimUnit.CM);

  public static final ParameterSetParameter chartParameters = new ParameterSetParameter(
      "Chart parameters", "Manually set the chart parameters", new ChartThemeParameters());

  public static final ColorPaletteParameter colorPalette = new ColorPaletteParameter("Color palette", "The color palette used for export.");

  public GraphicsExportParameters() {
    super(new Parameter[] {path, unit, exportFormat, fixedSize, width, height, dpi, color, alpha,
        chartParameters, colorPalette});
    height.setValue(true);
  }


  public ExitCode showSetupDialog(boolean valueCheckRequired, JFreeChart chart) {

    assert Platform.isFxApplicationThread();

    if ((getParameters() == null) || (getParameters().length == 0))
      return ExitCode.OK;
    GraphicsExportDialogFX dialog = new GraphicsExportDialogFX(valueCheckRequired, this, chart);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  /**
   * height is unchecked - use only width for size calculations
   *
   * @return
   */
  public boolean isUseOnlyWidth() {
    return !this.getParameter(height).getValue();
  }

  public FixedSize getFixedSize() {
    return this.getParameter(fixedSize).getValue();
  }

  /**
   * height in unit
   *
   * @return
   */
  public double getHeight() {
    return this.getParameter(height).getEmbeddedParameter().getValue();
  }

  /**
   * width in unit
   *
   * @return
   */
  public double getWidth() {
    return this.getParameter(width).getValue();
  }

  /**
   * Pixel size
   *
   * @return
   */
  public Dimension getPixelSize() {
    return new Dimension((int) getWidthPixel(), (int) getHeightPixel());
  }

  /**
   * Pixel width
   *
   * @return
   */
  public float getWidthPixel() {
    return DimensionUnitUtil.toPixel((float) getWidth(), getUnit());
  }

  /**
   * Pixel height
   *
   * @return
   */
  public float getHeightPixel() {
    return DimensionUnitUtil.toPixel((float) getHeight(), getUnit());
  }

  public double getDPI() {
    return this.getParameter(dpi).getValue();
  }

  public Color getColor() {
    return this.getParameter(color).getValue();
  }

  public Color getColorWithAlpha() {
    Color c = getColor();
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), getTransparency());
  }

  public DimUnit getUnit() {
    return this.getParameter(unit).getValue();
  }

  public String getFormat() {
    return this.getParameter(exportFormat).getValue();
  }

  public double getTransparency() {
    return this.getParameter(alpha).getValue();
  }

  // file
  public String getPath() {
    return this.getParameter(path).getValue().getParent();
  }

  public File getPathAsFile() {
    return this.getParameter(path).getValue().getParentFile();
  }

  public String getFilename() {
    return this.getParameter(path).getValue().getName();
  }

  /**
   * path/filename.format
   *
   * @return
   */
  public File getFullpath() {
    return FileAndPathUtil.getRealFilePath(getPathAsFile(), getFilename(), getFormat());
  }

  /**
   * Converts the pixel value to the specified unit
   *
   * @param value as pixel
   */
  public void setWidthPixel(double value) {
    this.getParameter(width).setValue(DimensionUnitUtil.pixelToUnit(value, getUnit()));
  }

  /**
   * Converts the pixel value to the specified unit
   *
   * @param value as pixel
   */
  public void setHeightPixel(double value) {
    this.getParameter(height).getEmbeddedParameter()
        .setValue(DimensionUnitUtil.pixelToUnit(value, getUnit()));
  }

  /**
   * Converts the pixel value to the specified unit
   *
   * @param value as pixel
   */
  public void setPixelSize(Dimension size) {
    setWidthPixel(size.getWidth());
    setHeightPixel(size.getHeight());
  }

  /**
   * Converts the pixel value to the specified unit
   *
   * @param value as pixel
   */
  public void setPixelSize(double w, double h) {
    setWidthPixel(w);
    setHeightPixel(h);
  }
}
