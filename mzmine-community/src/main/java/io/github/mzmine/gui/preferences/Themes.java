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

package io.github.mzmine.gui.preferences;

import java.util.List;
import javafx.collections.ObservableList;

public enum Themes {
  //  MZMINE_LIGHT("MZmine light", List.of("themes/MZmine_default.css", "themes/MZmine_light.css"),
//      false), //
//  MZMINE_DARK("MZmine dark", List.of("themes/MZmine_default.css", "themes/MZmine_dark.css"),
//      true), //
  /*MZMINE_AKK("MZmine AKK", List.of("themes/MZmine_AKK.css"), true), *///
  JABREF_LIGHT("Light", List.of("themes/jabref_light.css", "themes/jabref_additions_common.css",
      "themes/jabref_additions_light.css"), false), //
  JABREF_DARK("Dark", List.of("themes/jabref_light.css", "themes/jabref_dark.css",
      "themes/jabref_additions_common.css", "themes/jabref_additions_dark.css"), true), //
  JABREF_DARK_CUSTOM("Dark (High contrast)",
      List.of("themes/jabref_light.css", "themes/jabref_dark.css", "themes/jabref_dark_2.css",
          "themes/jabref_additions_common.css", "themes/jabref_additions_dark.css"), true), //
  DARK_GREY("Dark grey",
      List.of("themes/jabref_light.css", "themes/jabref_dark_grey.css",
      "themes/jabref_additions_common.css", "themes/jabref_additions_dark.css"), true) //
  ;

  private final String name;
  private final List<String> stylesheets;
  private final boolean isDark;

  Themes(String name, List<String> stylesheets, boolean isDark) {
    this.name = name;
    this.stylesheets = stylesheets;
    this.isDark = isDark;
  }

  public void apply(ObservableList<String> sheets) {
    sheets.clear();
    sheets.addAll(stylesheets);
  }

  public boolean isDark() {
    return isDark;
  }

  @Override
  public String toString() {
    return name;
  }

}
