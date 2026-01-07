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

package io.github.mzmine.gui.mainwindow.introductiontab;

import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.mzio.users.user.CurrentUserService;
import io.mzio.users.user.UserChangedSubscription;
import java.util.logging.Logger;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

public class IntroductionTabController extends FxController<IntroductionTabModel> {

  private static final Logger logger = Logger.getLogger(IntroductionTabController.class.getName());

  private final IntroductionTabBuilder introductionTabBuilder;
  private UserChangedSubscription userListener;

  protected IntroductionTabController() {
    super(new IntroductionTabModel());
    addBindings();
    introductionTabBuilder = new IntroductionTabBuilder(model);
    runVersionCheck();
  }

  private void addBindings() {
    model.setIsDarkMode(ConfigService.getConfiguration().isDarkMode());
    model.isDarkModeProperty().subscribe((_, isDark) -> {
      ConfigService.setDarkMode(isDark);
    });
    userListener = CurrentUserService.subscribe(user -> model.setNeedsUserLogin(user == null));
  }

  @Override
  protected @NotNull FxViewBuilder<IntroductionTabModel> getViewBuilder() {
    return introductionTabBuilder;
  }

  private void runVersionCheck() {
    onTaskThreadDelayed(new FxVersionCheckTask(model), new Duration(5000));
  }

  @Override
  public void close() {
    super.close();
    userListener.unsubscribe();
  }
}
