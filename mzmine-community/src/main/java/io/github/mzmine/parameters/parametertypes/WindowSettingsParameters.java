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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.util.objects.ObjectUtils;
import org.jetbrains.annotations.NotNull;

public class WindowSettingsParameters extends SimpleParameterSet {

  public static final DoubleParameter x = new DoubleParameter("x", "");
  public static final DoubleParameter y = new DoubleParameter("y", "");
  public static final DoubleParameter width = new DoubleParameter("Width", "");
  public static final DoubleParameter height = new DoubleParameter("Height", "");
  public static final BooleanParameter maximized = new BooleanParameter("Maximized", "", true);
  public static final BooleanParameter autoUpdate = new BooleanParameter("Auto update",
      "Deactivate to fix startup window position to current settings. If active, automatically updates the parameters to reflect the current main window state. Next startup will use this window position and size.",
      true);

  public WindowSettingsParameters() {
    super(x, y, width, height, maximized, autoUpdate);
  }

  @NotNull
  public WindowSettings createSettings() throws IllegalArgumentException {
    Double xValue = getValue(x);
    Double yValue = getValue(y);
    Double widthValue = getValue(width);
    Double heightValue = getValue(height);
    Boolean maximizedValue = getValue(maximized);

    if (ObjectUtils.anyIsNull(xValue, yValue, widthValue, heightValue, maximizedValue)) {
      return WindowSettings.createDefaultMaximized();
    }

    return new WindowSettings(xValue, yValue, widthValue, heightValue, maximizedValue);
  }

  public void setSettings(WindowSettings settings) {
    if (settings == null) {
      setSettings(WindowSettings.createDefaultMaximized());
      return;
    }
    setParameter(x, settings.x());
    setParameter(y, settings.y());
    setParameter(width, settings.width());
    setParameter(height, settings.height());
    setParameter(maximized, settings.maximized());
  }
}
