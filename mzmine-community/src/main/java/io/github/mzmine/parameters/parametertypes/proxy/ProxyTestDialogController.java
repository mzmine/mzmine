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

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.util.web.ProxyTestUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.NotNull;

public class ProxyTestDialogController extends FxController<ProxyTestDialogModel> {

  private final ProxyTestDialogViewBuilder viewBuilder;

  protected ProxyTestDialogController() {
    super(new ProxyTestDialogModel());
    viewBuilder = new ProxyTestDialogViewBuilder(model);

    onTaskThread(this::runTests);
  }

  private void runTests() {
    final boolean testProxy = ProxyTestUtils.testDefaultClient(true);
    final boolean testNoProxy = ProxyTestUtils.testDefaultClient(false);

    final String results = ProxyTestUtils.testAll();

    FxThread.runLater(() -> {
      model.setProxyTest(testProxy);
      model.setNoProxyTest(testNoProxy);
      model.setMessage(results);
      model.setTestsFinished(true);
    });
  }

  @Override
  protected @NotNull FxViewBuilder<ProxyTestDialogModel> getViewBuilder() {
    return viewBuilder;
  }

  public void showDialog() {
    final Alert dialog = DialogLoggerUtil.createAlert(AlertType.NONE,
        DialogLoggerUtil.getMainWindow(), "Proxy connection test", buildView(), ButtonType.CLOSE);
    dialog.show();
  }
}
