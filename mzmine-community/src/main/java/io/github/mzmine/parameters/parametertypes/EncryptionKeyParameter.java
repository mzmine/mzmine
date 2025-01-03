/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.util.StringCrypter;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;
import org.w3c.dom.Element;

public class EncryptionKeyParameter implements Parameter<StringCrypter> {

  private StringCrypter value;

  @Override
  public String getName() {
    return "AES Encryption Key";
  }

  @Override
  public StringCrypter getValue() {
    return value;
  }

  @Override
  public void setValue(StringCrypter newValue) {
    value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    try {
      final String nuVal = xmlElement.getTextContent();
      if (nuVal == null || nuVal.isEmpty()) {
        return;
      }
      value = new StringCrypter(nuVal);
    } catch (IOException e) {
      Logger.getLogger(this.getClass().getName()).warning("Could not load Encryption key! "
                                                          + "Encrypted parameters in the config file might not be decryptable.");
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    xmlElement.setTextContent(value.toString());
  }

  @Override
  public Parameter<StringCrypter> cloneParameter() {
    EncryptionKeyParameter newP = new EncryptionKeyParameter();
    if (this.value != null) {
      newP.setValue(new StringCrypter(value.toBytes()));
    }
    return newP;
  }

  @Override
  public boolean isSensitive() {
    return true;
  }
}
