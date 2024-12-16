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

package io.github.mzmine.modules.tools.batchwizard.subparameters.factories;

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowDDA;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowDIA;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowDeconvolution;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowImaging;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowLibraryGeneration;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowMs1Only;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowTargetPlate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * the defaults should not change the name of enum values. if strings are needed, override the
 * toString method
 *
 * All implementations must override their equals method in this manner:
 *
 * {@code
 * @Override
 *   public boolean equals(Object o) {
 *     return o instanceof WorkflowWizardParameterFactory fac && fac.getUniqueID()
 *         .equals(this.getUniqueID());
 *   }
 * }
 */
public abstract class WorkflowWizardParameterFactory implements WizardParameterFactory {

  private static final Set<WorkflowWizardParameterFactory> values = new LinkedHashSet<>();

  static {
    values.addAll(List.of(new WorkflowDDA(), new WorkflowDIA(), new WorkflowDeconvolution(),
        new WorkflowLibraryGeneration(), new WorkflowImaging(), new WorkflowTargetPlate(),
        new WorkflowMs1Only()));
  }

  public static WorkflowWizardParameterFactory[] values() {
    return values.toArray(WorkflowWizardParameterFactory[]::new);
  }

  @Override
  public int hashCode() {
    return getClass().getName().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof WorkflowWizardParameterFactory f && f.getUniqueID().equals(getUniqueID());
  }
}
