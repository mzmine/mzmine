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

package io.github.mzmine.modules.tools.tools_autoparam.estimation;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import org.jetbrains.annotations.NotNull;

/**
 * A wizard preset that is replaced because the raw data fit another preset better.
 *
 * @param part   the wizard part of the preset
 * @param from   the preset selected before
 * @param to     the preset that fits the raw data
 * @param reason the measurement that caused the change
 */
public record PresetChange(@NotNull WizardPart part, @NotNull WizardParameterFactory from,
                           @NotNull WizardParameterFactory to, @NotNull String reason) {

  public @NotNull String describe() {
    return "%s: %s → %s (%s)".formatted(part, from, to, reason);
  }
}
