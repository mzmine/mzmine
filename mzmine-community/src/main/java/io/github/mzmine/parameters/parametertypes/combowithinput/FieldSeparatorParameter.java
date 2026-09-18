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

package io.github.mzmine.parameters.parametertypes.combowithinput;

import io.github.mzmine.parameters.parametertypes.StringParameter;
import java.util.Collection;
import org.w3c.dom.Element;

/**
 * Column separator of tabular text files. Offers the separators that actually occur in the wild and
 * a custom text field for everything else. Files that are read also offer
 * {@link FieldSeparatorOption#AUTO}, which is the default because tools and locales disagree on the
 * separator, see {@link io.github.mzmine.util.CSVParsingUtils#autoDetermineSeparator(java.io.File)}.
 * <p>
 * Replaces the plain string parameters that were used before. Their values are still loaded, see
 * {@link #loadValueFromXML(Element)}.
 */
public class FieldSeparatorParameter extends
    ComboWithInputParameter<FieldSeparatorOption, FieldSeparator, StringParameter> {

  public static final String DEFAULT_NAME = "Field separator";

  public FieldSeparatorParameter(final String name, final String description,
      final FieldSeparatorOption[] options, final FieldSeparator defaultValue) {
    // the embedded parameter defines name and description of this parameter and is the text field
    // that is shown for the custom option
    super(new StringParameter(name, description, "", false, false, 4), options,
        FieldSeparatorOption.CUSTOM, defaultValue);
  }

  /**
   * Separator of a file that is read, defaults to {@link FieldSeparatorOption#AUTO}.
   */
  public static FieldSeparatorParameter forReading(final String description) {
    return new FieldSeparatorParameter(DEFAULT_NAME, description, FieldSeparatorOption.READ_OPTIONS,
        FieldSeparator.AUTO);
  }

  /**
   * Separator of a file that is written, so without automatic detection.
   */
  public static FieldSeparatorParameter forWriting(final String description,
      final FieldSeparator defaultValue) {
    return forWriting(DEFAULT_NAME, description, defaultValue);
  }

  /**
   * Separator of a file that is written, so without automatic detection.
   */
  public static FieldSeparatorParameter forWriting(final String name, final String description,
      final FieldSeparator defaultValue) {
    return new FieldSeparatorParameter(name, description, FieldSeparatorOption.WRITE_OPTIONS,
        defaultValue);
  }

  @Override
  public FieldSeparator createValue(final FieldSeparatorOption option,
      final StringParameter embeddedParameter) {
    return new FieldSeparator(option, embeddedParameter.getValue());
  }

  @Override
  public FieldSeparatorParameter cloneParameter() {
    final FieldSeparatorParameter clone = new FieldSeparatorParameter(getName(), getDescription(),
        choices.toArray(FieldSeparatorOption[]::new), value);
    clone.setValue(value);
    return clone;
  }

  /**
   * Before this parameter existed, the separator was a plain string parameter that stored the
   * separator as text content, e.g. {@code <parameter name="Field separator">,</parameter>}. Such
   * values are mapped onto the matching option, or onto
   * {@link FieldSeparatorOption#CUSTOM}. Options that this parameter does not offer, like
   * {@link FieldSeparatorOption#AUTO} for an export, fall back to the default value.
   */
  @Override
  public void loadValueFromXML(final Element xmlElement) {
    if (!xmlElement.getAttribute("selected").isEmpty()) {
      super.loadValueFromXML(xmlElement);
      return;
    }

    final FieldSeparator legacy = FieldSeparator.parse(xmlElement.getTextContent());
    if (choices.contains(legacy.option())) {
      setValue(legacy);
    }
  }

  @Override
  public boolean checkValue(final Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(getName() + " is not set properly");
      return false;
    }
    // a blank custom separator is fine, space is a valid separator, but empty is not
    if (useEmbeddedParameter() && value.separator().isEmpty()) {
      errorMessages.add(getName() + ": define the custom column separator.");
      return false;
    }
    return true;
  }
}
