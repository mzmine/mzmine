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

package io.github.mzmine.modules.dataprocessing.id_addmanualcomp;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ManualCompoundAnnotationModule extends AbstractProcessingModule {

  public ManualCompoundAnnotationModule() {
    super("Manual compound annotation", ManualCompoundAnnotationParameters.class,
        MZmineModuleCategory.ANNOTATION, "Manual annotation of compounds from the feature list.");
  }

  public static void annotate(ModularFeatureListRow row, List<FeatureAnnotation> annotations) {
    annotate(row, annotations, false);
  }

  public static void annotate(ModularFeatureListRow row, List<FeatureAnnotation> annotations,
      boolean runNow) {
    final ManualCompoundAnnotationParameters param = ManualCompoundAnnotationParameters.of(
        row.getFeatureList(), row, annotations);
    if (runNow) {
      new ManualCompoundAnnotationTask(null, Instant.now(), param,
          ManualCompoundAnnotationModule.class).run();
    } else {
      MZmineCore.runMZmineModule(ManualCompoundAnnotationModule.class, param);
    }
  }


  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    final ManualCompoundAnnotationTask manualCompoundAnnotationTask = new ManualCompoundAnnotationTask(
        null, moduleCallDate, parameters, this.getClass());
    tasks.add(manualCompoundAnnotationTask);
    return ExitCode.OK;
  }
}
