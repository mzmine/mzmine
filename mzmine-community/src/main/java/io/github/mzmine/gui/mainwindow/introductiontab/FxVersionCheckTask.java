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

import io.github.mzmine.gui.NewVersionCheck;
import io.github.mzmine.gui.NewVersionCheck.CheckType;
import io.github.mzmine.gui.VersionCheckResultType;
import io.github.mzmine.gui.mainwindow.VersionCheckResult;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import java.util.logging.Logger;

public class FxVersionCheckTask extends FxUpdateTask<IntroductionTabModel> {

  private static final Logger logger = Logger.getLogger(FxVersionCheckTask.class.getName());

  private VersionCheckResult result = null;
  private final NewVersionCheck check = new NewVersionCheck(CheckType.DESKTOP);

  protected FxVersionCheckTask(IntroductionTabModel model) {
    super("Checking for mzmine updates", model);
  }

  @Override
  protected void process() {
    check.run();
    result = check.getResult();
    logger.finest("Version check finished for IntroductionTab. Result: %s".formatted(
        result != null ? result.type() : "failed"));
  }

  @Override
  protected void updateGuiModel() {
    if (result != null && result.type() == VersionCheckResultType.NEW_AVAILALABLE) {
      model.setNewVersionAvailable(true);
    }
  }

  @Override
  public String getTaskDescription() {
    return getName();
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }
}
