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

import io.github.mzmine.main.MZmineCore;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class IntroductionTabModel {

  private final BooleanProperty newVersionAvailable = new SimpleBooleanProperty(false);
  private final BooleanProperty isDarkMode = new SimpleBooleanProperty(
      MZmineCore.getConfiguration().isDarkMode());


  public boolean isIsDarkMode() {
    return isDarkMode.get();
  }

  public BooleanProperty isDarkModeProperty() {
    return isDarkMode;
  }

  public void setIsDarkMode(boolean isDarkMode) {
    this.isDarkMode.set(isDarkMode);
  }

  public boolean isNewVersionAvailable() {
    return newVersionAvailable.get();
  }

  public BooleanProperty newVersionAvailableProperty() {
    return newVersionAvailable;
  }

  public void setNewVersionAvailable(boolean newVersionAvailable) {
    this.newVersionAvailable.set(newVersionAvailable);
  }
}
