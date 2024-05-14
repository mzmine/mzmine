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

package io.github.mzmine.modules.tools.batchwizard.io;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WizardSequenceSaveModule implements MZmineModule {

  private static final Logger logger = Logger.getLogger(WizardSequenceSaveModule.class.getName());

  public static void setupAndSave(final WizardSequence workflow) {
    ParameterSet params = MZmineCore.getConfiguration()
        .getModuleParameters(WizardSequenceSaveModule.class);
    if (params.showSetupDialog(true) == ExitCode.OK) {
      MZmineCore.getConfiguration().setModuleParameters(WizardSequenceSaveModule.class, params);

      File directory = params.getValue(WizardSequenceSaveParameters.directory);
      String fileName = params.getValue(WizardSequenceSaveParameters.fileName);
      final var exportParts = Arrays.stream(
          params.getValue(WizardSequenceSaveParameters.exportParts)).collect(Collectors.toSet());
      File file = FileAndPathUtil.getRealFilePath(directory, fileName,
          WizardSequenceIOUtils.FILE_FILTER.getExtensions().get(0).split("\\.")[1]);
      try {
        // only keep parts to export
        var filteredWorkflow = workflow.stream()
            .filter(preset -> exportParts.contains(preset.getPart())).toList();
        WizardSequenceIOUtils.saveToFile(filteredWorkflow, file, true);
      } catch (IOException e) {
        logger.log(Level.WARNING, "Cannot write batch wizard presets to " + file.getAbsolutePath(),
            e);
      }
    }
  }

  @Override
  public @NotNull String getName() {
    return "Save wizard preset";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return WizardSequenceSaveParameters.class;
  }

}
