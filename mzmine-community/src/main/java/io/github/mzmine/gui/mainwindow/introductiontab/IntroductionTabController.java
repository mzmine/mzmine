/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.gui.mainwindow.introductiontab;

import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class IntroductionTabController extends FxController<IntroductionTabModel> {

  private static final Logger logger = Logger.getLogger(IntroductionTabController.class.getName());

  private final IntroductionTabBuilder introductionTabBuilder;

  protected IntroductionTabController() {
    super(new IntroductionTabModel());
    introductionTabBuilder = new IntroductionTabBuilder(model);
    runVersionCheck();
  }

  @Override
  protected @NotNull FxViewBuilder<IntroductionTabModel> getViewBuilder() {
    return introductionTabBuilder;
  }

  private void runVersionCheck() {
    onTaskThread(new FxVersionCheckTask(model));
  }
}
