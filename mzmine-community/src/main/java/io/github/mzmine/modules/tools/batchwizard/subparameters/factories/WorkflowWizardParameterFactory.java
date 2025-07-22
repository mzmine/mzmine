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

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardPartFilter;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WizardWorkflows;
import io.mzio.users.autorisation.ServiceRestricted;
import io.mzio.users.service.UserActiveService;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * the defaults should not change the name of enum values. if strings are needed, override the
 * toString method
 * <p>
 * All implementations must not override their equals and hash code methods.
 */
public abstract class WorkflowWizardParameterFactory implements WizardParameterFactory,
    ServiceRestricted {

  public static WorkflowWizardParameterFactory[] values() {
    return WizardWorkflows.values();
  }

  @Override
  public final int hashCode() {
    return getUniqueID().hashCode();
  }

  @Override
  public final boolean equals(Object o) {
    return o instanceof WorkflowWizardParameterFactory f && f.getUniqueID().equals(getUniqueID());
  }

  /**
   * @return A map that either allows or restricts specific selections in the wizard. If a
   * {@link WizardPart} has no values set, all part {@link WizardParameterFactory}s are allowed.
   */
  public abstract Map<WizardPart, WizardPartFilter> getStepFilters();

  /**
   * Create workflow builder for workflow steps
   *
   * @param steps workflow
   * @return the builder
   */
  public abstract @NotNull WizardBatchBuilder getBatchBuilder(@NotNull final WizardSequence steps)
      throws UnsupportedOperationException;

  @Override
  public abstract @NotNull Set<@NotNull UserActiveService> getUnlockingServices();
}
