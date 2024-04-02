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

package io.github.mzmine.main;

import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.impl.MZmineConfigurationImpl;
import io.github.mzmine.parameters.Parameter;

public final class ConfigService {

  private static final MZmineConfiguration config = new MZmineConfigurationImpl();

  public static MZmineConfiguration getConfiguration() {
    return config;
  }

  public static MZminePreferences getPreferences() {
    return config.getPreferences();
  }

  public static <V, T extends Parameter<V>> V getPreference(T parameter) {
    return config.getPreferences().getValue(parameter);
  }

  public static NumberFormats getExportFormats() {
    return config.getExportFormats();
  }

  public static NumberFormats getGuiFormats() {
    return config.getGuiFormats();
  }
}
