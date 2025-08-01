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

package io.github.mzmine.modules.batchmode.change_outfiles;

import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Apply a new base file to all output parameters
 */
public class ChangeOutputFilesUtils {

  private final Map<String, Integer> exportFileCounter = new HashMap<>();

  private ChangeOutputFilesUtils() {
  }

  /**
   * @param baseFile file path and base filename usually without extension to be set to all
   *                 parameters of type {@link FileNameSuffixExportParameter} with their suffix.
   * @return number of changed parameters
   */
  public static int applyTo(
      @Nullable final List<MZmineProcessingStep<MZmineProcessingModule>> steps,
      final File baseFile) {
    return new ChangeOutputFilesUtils().applyToInternal(steps, baseFile);
  }

  private int applyToInternal(
      @Nullable final List<MZmineProcessingStep<MZmineProcessingModule>> steps,
      final File baseFile) {

    if (steps == null) {
      return 0;
    }

    int changed = 0;
    for (var step : steps) {
      ParameterSet params = step.getParameterSet();
      // go through all embedded parameters
      changed += ParameterUtils.streamParametersDeep(params, FileNameSuffixExportParameter.class)
          .mapToInt(outParam -> setOutputFilename(baseFile, outParam)).sum();
    }
    return changed;
  }

  private int setOutputFilename(final File baseFile, final FileNameSuffixExportParameter outParam) {
    File file = null;
    Integer duplicateCounter = null;
    // have to name it once then check for duplicates
    outParam.setValueAppendSuffix(baseFile, null);

    // may also be a duplicate so check
    while ((file = outParam.getValue()) != null) {
      duplicateCounter = exportFileCounter.get(file.getName());
      if (duplicateCounter == null) {
        exportFileCounter.put(file.getName(), 1); // next will be named with suffix 1
        return 1;
      } else {
        exportFileCounter.put(file.getName(), ++duplicateCounter);
      }

      outParam.setValueAppendSuffix(baseFile, duplicateCounter.toString());
    }
    // apparently does not set the file
    return 0;
  }

}
