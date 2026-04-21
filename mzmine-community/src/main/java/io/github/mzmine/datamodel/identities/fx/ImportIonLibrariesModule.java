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

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import java.io.File;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImportIonLibrariesModule implements MZmineModule {

  public static void showDialog() {
    final ParameterSet parameters = ConfigService.getConfiguration()
        .getModuleParameters(ImportIonLibrariesModule.class);
    final FileNamesParameter fileParam = parameters.getParameter(
        ImportIonLibrariesParameters.filename);

    // this uses the last selected files to open file chooser at right directory
    final List<File> selectedFiles = fileParam.createEditingComponent()
        .showSelectMultiFilesDialog(null);
    if (selectedFiles != null && !selectedFiles.isEmpty()) {

    }
  }

  @Override
  public @NotNull String getName() {
    return "Import ion libraries";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return ImportIonLibrariesParameters.class;
  }


}
