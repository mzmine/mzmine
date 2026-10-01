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

package io.github.mzmine.modules.tools.tools_autoparam.preclassification;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * All classifiers decided without conflict.
 *
 * @param parameters       the decided settings, see {@link PreclassificationParameters}
 * @param choiceParameters the parameters of {@link #parameters()} the user still has to choose. The
 *                         same instances, so a dialog on them changes {@link #parameters()}
 * @param choiceMessages   explain why the user has to choose
 */
public record PreclassificationResolved(@NotNull ParameterSet parameters,
                                        @NotNull List<@NotNull Parameter<?>> choiceParameters,
                                        @NotNull List<@NotNull String> choiceMessages) implements
    PreclassificationResolution {

  public PreclassificationResolved {
    choiceParameters = List.copyOf(choiceParameters);
    choiceMessages = List.copyOf(choiceMessages);
  }

  public boolean needsUserChoice() {
    return !choiceParameters.isEmpty();
  }
}
