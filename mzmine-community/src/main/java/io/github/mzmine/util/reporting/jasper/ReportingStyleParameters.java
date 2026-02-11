/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.reporting.jasper;

import io.github.mzmine.gui.chartbasics.graphicsexport.ExportChartThemeModule;
import io.github.mzmine.gui.chartbasics.graphicsexport.ExportChartThemeParameters;
import io.github.mzmine.modules.visualization.molstructure.StructureRenderModule;
import io.github.mzmine.modules.visualization.molstructure.StructureRenderParameters;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;

/**
 * Put all style relevant parameters here
 */
public class ReportingStyleParameters extends SimpleParameterSet {

  public static final ParameterSetParameter<ExportChartThemeParameters> chartThemeParam = new ParameterSetParameter<>(
      "Chart theme", "Define the chart theme", new ExportChartThemeParameters(), false,
      ExportChartThemeModule.class);


  /**
   * Uses the same zoom=1 as in mzmine. In {@link ReportingTask} createReportUtils a dpi factor is
   * applied to scale up structures for the report.
   */
  public static final ParameterSetParameter<StructureRenderParameters> structureRendering = new ParameterSetParameter<>(
      "Molecular structures", "Options to control the rendering of molecular structures.",
      new StructureRenderParameters(), StructureRenderModule.class);

  public ReportingStyleParameters() {
    super(chartThemeParam, structureRendering);
  }
}
