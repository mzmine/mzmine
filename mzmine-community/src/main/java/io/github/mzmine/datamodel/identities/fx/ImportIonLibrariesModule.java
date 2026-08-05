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
import io.github.mzmine.datamodel.identities.global.IonLibraryImportResult.MergePolicy;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.impl.AbstractMZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImportIonLibrariesModule extends AbstractMZmineModule {

  private static final Logger logger = Logger.getLogger(ImportIonLibrariesModule.class.getName());

  public ImportIonLibrariesModule() {
    super("Import ion libraries", ImportIonLibrariesParameters.class);
  }

  public static void showDialog() {
    final ParameterSet parameters = ConfigService.getConfiguration()
        .getModuleParameters(ImportIonLibrariesModule.class);

    try {
      ExitCode exitCode = parameters.showSetupDialog(true);
      if (exitCode != ExitCode.OK) {
        return;
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    File[] files = parameters.getValue(
        ImportIonLibrariesParameters.filename);
    final MergePolicy mergePolicy = parameters.getValue(ImportIonLibrariesParameters.mergePolicy);

    if (files!=null && files.length>0) {
    // this uses the last selected files to open file chooser at right directory
      GlobalIonLibraryService.getGlobalLibrary().addPresetFiles(List.of(files), mergePolicy);
    }
  }

}
