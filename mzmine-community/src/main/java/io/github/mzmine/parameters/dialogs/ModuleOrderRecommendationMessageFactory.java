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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.batchmode.order.ModuleOrderRecommendation;
import io.github.mzmine.modules.batchmode.order.ModuleOrderRule;
import io.github.mzmine.parameters.ParameterSet;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adds processing-order recommendations to the standard parameter dialog message area.
 */
public final class ModuleOrderRecommendationMessageFactory {

  private ModuleOrderRecommendationMessageFactory() {
  }

  public static @Nullable Region combineWithExistingMessage(
      @NotNull final ParameterSet parameterSet, @Nullable final Region existingMessage) {
    final MZmineProcessingModule module = MZmineCore.getModuleForParameterSetIfUnique(parameterSet)
        .filter(MZmineProcessingModule.class::isInstance).map(MZmineProcessingModule.class::cast)
        .orElse(null);
    if (module == null || module.getModuleOrderRecommendations().isEmpty()) {
      return existingMessage;
    }

    final Region recommendationMessage = createRecommendationMessage(module);
    if (existingMessage == null) {
      return recommendationMessage;
    }
    return FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, existingMessage,
        recommendationMessage);
  }

  private static @NotNull Region createRecommendationMessage(
      @NotNull final MZmineProcessingModule module) {
    final List<Node> messageNodes = new ArrayList<>();
    final List<ModuleOrderRecommendation> recommendations = module.getModuleOrderRecommendations();
    messageNodes.add(FxTexts.boldText(module.getName()));
    messageNodes.add(FxTexts.linebreak());
    for (int i = 0; i < recommendations.size(); i++) {
      final ModuleOrderRecommendation recommendation = recommendations.get(i);
      messageNodes.add(FxTexts.text(recommendation.rationale() + "\n"));
      final ModuleOrderRule rule = recommendation.rule();
      messageNodes.add(FxTexts.text("\u2022 " + rule.description() + "\n"));
      if (i + 1 < recommendations.size()) {
        messageNodes.add(FxTexts.linebreak());
      }
    }
    return FxTextFlows.newTextFlowInAccordion("Processing order", false,
        messageNodes.toArray(Node[]::new));
  }
}
