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

package io.github.mzmine.javafx.components.skins;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.skin.ComboBoxListViewSkin;

/**
 * Automatically scales a ComboBox text field. This is important when using editable combobox.
 * Otherwise, the entered text will somehow add to the preferred width that is calculated from the
 * list view in the default skin.
 *
 * @param <T>
 */
public class DynamicWidthComboBoxSkin<T> extends ComboBoxListViewSkin<T> {


  private final DynamicTextFieldSkin skin;

  /**
   * @param minColumnCount -1 to deactivate
   * @param maxColumnCount -1 to deactivate
   */
  public DynamicWidthComboBoxSkin(ComboBox<T> combo, int minColumnCount, int maxColumnCount) {
    super(combo);
    combo.setEditable(true);
    skin = new DynamicTextFieldSkin(getEditor(), minColumnCount, maxColumnCount);
    // no need to set skin. it is not updated somehow so need to calc pref width in method manually
//    getEditor().setSkin(skin);
  }

  @Override
  protected double computePrefWidth(double height, double topInset, double rightInset,
      double bottomInset, double leftInset) {
    final Insets i = getEditor().getInsets();
    final double displayNodeWidth = skin.computePrefWidth(getEditor().getHeight(), i.getTop(),
        i.getRight(), i.getBottom(), i.getLeft());
    final int buttonSize = 25;
    final double totalWidth = displayNodeWidth + buttonSize;
    return leftInset + totalWidth + rightInset;
  }
}
