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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.util.javafx.DraggableListCell;
import java.util.function.IntFunction;
import javafx.scene.control.Tooltip;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

final class BatchStepListCell extends
    DraggableListCell<MZmineProcessingStep<MZmineProcessingModule>> {

  private final IntFunction<@Nullable String> validationMessageProvider;
  private final FontIcon infoIcon;
  private final Tooltip validationTooltip = new Tooltip();

  BatchStepListCell(@NotNull final IntFunction<@Nullable String> validationMessageProvider) {
    this.validationMessageProvider = validationMessageProvider;
    infoIcon = FxIconUtil.getFontIcon(FxIcons.INFO_CIRCLE, FxIconUtil.LIST_ICON_SIZE,
        ConfigService.getDefaultColorPalette().getPositiveColor());
    validationTooltip.setWrapText(true);
    validationTooltip.setMaxWidth(600);
  }

  @Override
  protected void updateItem(@Nullable final MZmineProcessingStep<MZmineProcessingModule> item,
      final boolean empty) {
    super.updateItem(item, empty);
    if (empty || item == null) {
      setText(null);
      setGraphic(null);
      setTooltip(null);
      return;
    }

    setText("%d. %s".formatted(getIndex() + 1, item.getModule().getName()));
    final String validationMessage = validationMessageProvider.apply(getIndex());
    if (validationMessage == null) {
      setGraphic(null);
      setTooltip(null);
      return;
    }

    validationTooltip.setText(validationMessage);
    setGraphic(infoIcon);
    setTooltip(validationTooltip);
  }
}
