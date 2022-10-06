/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.util.components;

import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import com.google.common.collect.Range;

/**
 * Component with two textboxes to specify a range
 */
public class RangeComponent extends GridBagPanel {

  private static final long serialVersionUID = 1L;

  public static final int TEXTFIELD_COLUMNS = 8;

  private JFormattedTextField minTxtField, maxTxtField;

  public RangeComponent(NumberFormat format) {
    minTxtField = new JFormattedTextField(format);
    maxTxtField = new JFormattedTextField(format);
    minTxtField.setColumns(TEXTFIELD_COLUMNS);
    maxTxtField.setColumns(TEXTFIELD_COLUMNS);
    add(minTxtField, 0, 0, 1, 1, 1, 0);
    add(new JLabel(" - "), 1, 0, 1, 1, 0, 0);
    add(maxTxtField, 2, 0, 1, 1, 1, 0);
  }

  /**
   * @return Returns the current values
   */
  public Range<Double> getRangeValue() {
    double minValue = ((Number) minTxtField.getValue()).doubleValue();
    double maxValue = ((Number) maxTxtField.getValue()).doubleValue();
    return Range.closed(minValue, maxValue);
  }

  public void setRangeValue(Range<Double> value) {
    minTxtField.setValue(value.lowerEndpoint());
    maxTxtField.setValue(value.upperEndpoint());
  }

  public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
    minTxtField.addPropertyChangeListener(property, listener);
    maxTxtField.addPropertyChangeListener(property, listener);
  }

  public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
    minTxtField.removePropertyChangeListener(property, listener);
    maxTxtField.removePropertyChangeListener(property, listener);
  }

  public void setNumberFormat(NumberFormat format) {
    DefaultFormatterFactory fac = new DefaultFormatterFactory(new NumberFormatter(format));
    minTxtField.setFormatterFactory(fac);
    maxTxtField.setFormatterFactory(fac);
  }

}
