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

package io.github.mzmine.gui.chartbasics.graphicsexport;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.colorpalette.ColorPaletteParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.util.DimensionUnitUtil;
import io.github.mzmine.util.DimensionUnitUtil.DimUnit;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.awt.Dimension;
import java.io.File;
import java.text.DecimalFormat;
import javafx.application.Platform;
import org.jfree.chart.JFreeChart;

public class GraphicsExportParameters extends SimpleParameterSet {

  public static final FileNameParameter path = new FileNameParameter("Path", "The file path",
      FileSelectionType.SAVE);
  public static final OptionalParameter<DoubleParameter> height = new OptionalParameter<DoubleParameter>(
      new DoubleParameter("Height",
          "Only uses width if height is unchecked. Otherwise uses fixed height for the chart or plot",
          DecimalFormat.getInstance(), 8.0));

  public static final DoubleParameter width = new DoubleParameter("Width",
      "Uses fixed width for the chart or plot", DecimalFormat.getInstance(), 15.0);

  public static final DoubleParameter dpi = new DoubleParameter("Resolution (dpi)",
      "dots per inch resolution (for print usually 300 up to 600 dpi)", DecimalFormat.getInstance(),
      300.0);
  public static final ComboParameter<String> exportFormat = new ComboParameter<String>("Format",
      "The image export format", new String[]{"PDF", "EMF", "EPS", "SVG", "JPG", "PNG"}, "PNG");
  public static final ComboParameter<FixedSize> fixedSize = new ComboParameter<FixedSize>(
      "Fixed size for",
      "Fixed size for the plot (the data space without axes and titles) or the whole chart.",
      FixedSize.values(), FixedSize.Chart);
  public static final ColorPaletteParameter colorPalette = new ColorPaletteParameter(
      "Color palette", "The color palette used for export.");

  public static final DoubleParameter alpha = new DoubleParameter("Transparency",
      "Transparency from 0.0-1.0 (fully visible)", DecimalFormat.getInstance(), 1.0, 0.0, 1.0);

  public static final ComboParameter<DimUnit> unit = new ComboParameter<>("Unit",
      "Unit for width and height dimensions", DimUnit.values(), DimUnit.CM);

  public static final ParameterSetParameter chartParameters = new ParameterSetParameter(
      "Chart parameters", "Manually set the chart parameters", (new ExportChartThemeParameters()));

  public GraphicsExportParameters() {
    super(path, unit, exportFormat, fixedSize, width, height, dpi, alpha, chartParameters,
        colorPalette);
    height.setValue(true);
  }

  public ExitCode showSetupDialog(boolean valueCheckRequired, JFreeChart chart) {

    assert Platform.isFxApplicationThread();

    if ((getParameters() == null) || (getParameters().length == 0)) {
      return ExitCode.OK;
    }
    GraphicsExportDialogFX dialog = new GraphicsExportDialogFX(valueCheckRequired, this, chart);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  /**
   * Converts the pixel value to the specified unit
   *
   * @param size as pixel
   */
  public void setPixelSize(Dimension size) {
    setWidthPixel(size.getWidth());
    setHeightPixel(size.getHeight());
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
   * @param w as pixel
   * @param h as pixel
   */
  public void setPixelSize(double w, double h) {
    setWidthPixel(w);
    setHeightPixel(h);
  }

  public enum FixedSize {
    Chart, Plot
  }
}
