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

package io.github.mzmine.parameters.parametertypes.proxy;

import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.util.web.ProxyTestUtils;

public class ProxyTestUpdateTask extends FxUpdateTask<ProxyTestDialogModel> {

  private boolean testProxy;
  private boolean testNoProxy;
  private String results;
  private String log;
  private double progress = 0;

  protected ProxyTestUpdateTask(ProxyTestDialogModel model) {
    super("Proxy test", model);
  }

  @Override
  protected void process() {
    testProxy = ProxyTestUtils.testDefaultClient(true);
    progress = 0.33;
    testNoProxy = ProxyTestUtils.testDefaultClient(false);
    progress = 0.66;

    results = ProxyTestUtils.testAll();
    log = ProxyTestUtils.logProxyState("Proxy config test: ");
    progress = 1;
  }

  @Override
  protected void updateGuiModel() {
    model.setProxyTest(testProxy);
    model.setNoProxyTest(testNoProxy);
    model.setMessage(results + "\n\n" + log);
    model.setTestsFinished(true);
  }

  @Override
  public String getTaskDescription() {
    return "Running proxy tests";
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }
}
