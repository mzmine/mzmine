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

package io.github.mzmine.modules.visualization.molstructure;

import io.github.mzmine.javafx.util.FxFileChooser;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.TxtWriter;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.stage.FileChooser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtomContainer;

public class StructureGraphicsExportModule implements MZmineModule {

  private static final Logger logger = Logger.getLogger(
      StructureGraphicsExportModule.class.getName());

  public static void exportToSvg(@NotNull IAtomContainer mol) {
    final ParameterSet parameters = getGlobalParameters();
    try {
      final var config = parameters.getValue(StructureGraphicsExportParameters.structureRendering)
          .createConfig();
      final String svg = StructureRenderService.getGlobalStructureRenderer()
          .drawStructureToSvgStringFixedSize(mol, config);

      final File lastFile = parameters.getValue(StructureGraphicsExportParameters.fileName);
      final FileChooser fc = FxFileChooser.newFileChooser(List.of(ExtensionFilters.SVG), lastFile,
          "Choose svg file");
      File saveToFile = fc.showSaveDialog(null);
      if (saveToFile != null) {
        saveToFile = FileAndPathUtil.getRealFilePath(saveToFile, "svg");
        TxtWriter.write(svg, saveToFile);
      }
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Cannot export svg. Message: " +ex.getMessage(), ex);
    }
  }

  private static ParameterSet getGlobalParameters() {
    return ConfigService.getConfiguration()
        .getModuleParameters(StructureGraphicsExportModule.class);
  }

  @Override
  public @NotNull String getName() {
    return "Export structure to svg";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return StructureGraphicsExportParameters.class;
  }
}
