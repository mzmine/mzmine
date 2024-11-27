/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.mzio.mzmine.datamodel.parameters.EmbeddedParameterSet;
import io.mzio.mzmine.datamodel.parameters.Parameter;
import io.mzio.mzmine.datamodel.parameters.ParameterSet;
import io.mzio.mzmine.datamodel.parameters.UserParameter;
import io.mzio.mzmine.datamodel.parameters.impl.EmbeddedParameter;
import io.mzio.mzmine.datamodel.parameters.impl.HiddenParameter;
import java.io.File;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Apply a new base file to all output parameters
 */
public class ChangeOutputFilesUtils {

  /**
   * @param baseFile file path and base filename usually without extension to be set to all
   *                 parameters of type {@link FileNameSuffixExportParameter} with their suffix.
   * @return number of changed parameters
   */
  public static int applyTo(
      @Nullable final List<MZmineProcessingStep<MZmineProcessingModule>> steps,
      final File baseFile) {
    if (steps == null) {
      return 0;
    }

    int changed = 0;
    for (var step : steps) {
      ParameterSet params = step.getParameterSet();
      if (params == null) {
        continue;
      }
      for (final Parameter<?> parameter : params.getParameters()) {
        changed += applyTo(parameter, baseFile);
      }
    }
    return changed;
  }

  /**
   * @param baseFile file path and base filename usually without extension to be set to all
   *                 parameters of type {@link FileNameSuffixExportParameter} with their suffix.
   * @return number of changed parameters
   */
  public static int applyTo(@Nullable final ParameterSet params, final File baseFile) {
    if (params == null) {
      return 0;
    }

    int changed = 0;
    for (final Parameter<?> parameter : params.getParameters()) {
      changed += applyTo(parameter, baseFile);
    }
    return changed;
  }

  private static int applyTo(final Parameter<?> parameter, final File baseFile) {
    if (parameter == null) {
      return 0;
    }
    if (parameter instanceof FileNameSuffixExportParameter outParam) {
      outParam.setValueAppendSuffix(baseFile);
      return 1;
    }
    // search for embedded parameters
    if (parameter instanceof EmbeddedParameterSet<?, ?> parent) {
      ParameterSet embedded = parent.getEmbeddedParameters();
      return applyTo(embedded, baseFile);
    }
    // optional?
    if (parameter instanceof EmbeddedParameter<?, ?, ?> parent) {
      UserParameter<?, ?> embedded = parent.getEmbeddedParameter();
      return applyTo(embedded, baseFile);
    }
    if (parameter instanceof HiddenParameter<?> parent) {
      var embedded = parent.getEmbeddedParameter();
      return applyTo(embedded, baseFile);
    }
    return 0;
  }
}
