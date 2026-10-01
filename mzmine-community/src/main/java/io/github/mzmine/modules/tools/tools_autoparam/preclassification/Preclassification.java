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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Runs all {@link RawDataClassifier}s on the imported raw data and collects their decisions in
 * {@link PreclassificationParameters}.
 */
public final class Preclassification {

  /**
   * Register new classifiers here and add their parameter to {@link PreclassificationParameters}.
   */
  private static final List<RawDataClassifier<?>> CLASSIFIERS = List.of(new PolarityClassifier());

  private Preclassification() {
  }

  /**
   * Pure logic, safe to call from a task thread.
   *
   * @param files  the imported raw data files that are used for estimation and optimization
   * @param wizard the wizard sequence with the user's settings, usually a copy
   */
  public static @NotNull PreclassificationResolution resolve(
      @NotNull List<@NotNull RawDataFile> files, @NotNull WizardSequence wizard) {
    return resolve(files, wizard, CLASSIFIERS,
        new PreclassificationParameters().cloneParameterSet());
  }

  /**
   * @param parameters receives the decisions, must contain the parameters of all classifiers
   */
  static @NotNull PreclassificationResolution resolve(@NotNull List<@NotNull RawDataFile> files,
      @NotNull WizardSequence wizard, @NotNull List<RawDataClassifier<?>> classifiers,
      @NotNull ParameterSet parameters) {
    final List<Parameter<?>> choiceParameters = new ArrayList<>();
    final List<String> choiceMessages = new ArrayList<>();
    final List<String> conflicts = new ArrayList<>();
    for (final RawDataClassifier<?> classifier : classifiers) {
      apply(classifier, files, wizard, parameters, choiceParameters, choiceMessages, conflicts);
    }
    // decision: collect the conflicts of all classifiers, so the user sees every problem at once
    if (!conflicts.isEmpty()) {
      return new PreclassificationConflicts(conflicts);
    }
    return new PreclassificationResolved(parameters, choiceParameters, choiceMessages);
  }

  private static <T> void apply(@NotNull RawDataClassifier<T> classifier,
      @NotNull List<@NotNull RawDataFile> files, @NotNull WizardSequence wizard,
      @NotNull ParameterSet parameters, @NotNull List<Parameter<?>> choiceParameters,
      @NotNull List<String> choiceMessages, @NotNull List<String> conflicts) {
    final UserParameter<T, ?> parameter = parameters.getParameter(classifier.parameter());
    switch (classifier.decide(files, wizard)) {
      case PreclassificationFixed<T> fixed -> parameter.setValue(fixed.value());
      case PreclassificationChoice<T> choice -> {
        // limit the combo box to the valid values
        if (parameter instanceof ComboParameter<?> combo) {
          // the combo holds the value type of the classifier's parameter
          @SuppressWarnings("unchecked") final ComboParameter<T> typedCombo = (ComboParameter<T>) combo;
          typedCombo.getChoices().setAll(choice.options());
        }
        parameter.setValue(choice.preselected());
        choiceParameters.add(parameter);
        choiceMessages.add(choice.message());
      }
      case PreclassificationConflict<T> conflict -> conflicts.add(conflict.message());
    }
  }
}
