/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.proxy;

import static io.github.mzmine.javafx.components.factories.FxTexts.colored;
import static io.github.mzmine.javafx.components.factories.FxTexts.linebreak;
import static io.github.mzmine.javafx.components.factories.FxTexts.styledText;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;
import static io.github.mzmine.javafx.components.util.FxLayout.newVBox;

import io.github.mzmine.javafx.components.factories.FxLabels.Styles;
import io.github.mzmine.javafx.components.factories.FxTextAreas;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.jetbrains.annotations.NotNull;

public class ProxyTestDialogViewBuilder extends FxViewBuilder<ProxyTestDialogModel> {

  public ProxyTestDialogViewBuilder(@NotNull ProxyTestDialogModel model) {
    super(model);
  }

  @Override
  public Region build() {
    final TextArea mainText = FxTextAreas.newTextArea(100, 50, true, model.messageProperty(),
        "Running connection tests may take a few seconds. If connection issues remain, send this information to info@mzio.io");
    mainText.setEditable(false);
    BorderPane content = FxLayout.newBorderPane(mainText);

    final Color positive = ConfigService.getDefaultColorPalette().getPositiveColor();
    final Color negative = ConfigService.getDefaultColorPalette().getNegativeColor();

    final ObservableValue<Color> proxyColor = model.proxyTestProperty()
        .map(state -> state ? positive : negative).orElse(negative);
    final ObservableValue<Color> noProxyColor = model.noProxyTestProperty()
        .map(state -> state ? positive : negative).orElse(negative);

    final String resolvingInfo = "If these connection issues remain, send this information to info@mzio.io";

    final Text resolvingInfoText = colored(text(resolvingInfo), negative);
    final var anyTestFailed = model.proxyTestProperty().and(model.noProxyTestProperty()).not();
    resolvingInfoText.visibleProperty().bind(anyTestFailed);
    FxLayout.bindManagedToVisible(resolvingInfoText);

    final VBox topChecks = newVBox( //
        FxTextFlows.newTextFlow( //
            styledText("Proxy connection tests:", Styles.BOLD_SEMI_TITLE), //
            linebreak(), //
            text("With proxy connection: "), //
            colored(text(model.proxyTestMessageProperty()), proxyColor), //
            linebreak(), //
            text("Direct connection: "), //
            colored(text(model.noProxyTestMessageProperty()), noProxyColor), //
            linebreak(), //
            resolvingInfoText //
        ));

    final Text runningTests = styledText("Running tests... this may take a few seconds",
        Styles.BOLD_SEMI_TITLE);

    // show running tests as long as not finished
    content.topProperty().bind(
        model.testsFinishedProperty().map(finished -> finished ? topChecks : runningTests)
            .orElse(runningTests));

    return content;
  }
}
