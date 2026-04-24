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

package io.github.mzmine.datamodel.identities.fx;

import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.impl.AbstractMZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class ExportIonLibrariesModule extends AbstractMZmineModule {

  private static final Logger logger = Logger.getLogger(ExportIonLibrariesModule.class.getName());

  public ExportIonLibrariesModule() {
    super("Export ion libraries", ExportIonLibrariesParameters.class);
  }

  public static void showExportDialog(@NotNull List<IonLibrary> libraries) {
    if (libraries.isEmpty()) {
      return;
    }
    libraries = libraries.stream().filter(ionLibrary -> !ionLibrary.isInternalLibrary()).toList();
    if (libraries.isEmpty()) {
      DialogLoggerUtil.showInfoNotification("Cannot export internal mzmine ion libraries",
          "The selected ion library is mzmine internal and cannot be exported. Create a copy first.");
      return;
    }

    try {
      final ParameterSet parameters = ConfigService.getConfiguration()
          .getModuleParameters(ExportIonLibrariesModule.class);

      final DirectoryParameter param = parameters.getParameter(
          ExportIonLibrariesParameters.directory);
      final File selectedDirectory = param.showChooseDirectoryDialog();
      if (selectedDirectory != null) {
        // save library presets
        GlobalIonLibraryService.getGlobalLibrary().exportPresetsTo(selectedDirectory, libraries);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
  }

}
