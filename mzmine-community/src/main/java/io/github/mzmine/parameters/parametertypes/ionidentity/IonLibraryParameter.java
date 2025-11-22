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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import io.github.mzmine.datamodel.identities.IonLibrary;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesModule;
import io.github.mzmine.parameters.AbstractParameter;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 *
 */
public class IonLibraryParameter extends AbstractParameter<IonLibrary, IonLibraryComponent> {

  private IonLibrary library;

  public IonLibraryParameter() {
    this(IonLibrary.MZMINE_DEFAULT_DUAL_POLARITY);
  }

  public IonLibraryParameter(@NotNull IonLibrary defaultValue) {
    super("Ion library",
        "Select an ion library. Ion types and libraries are created in a separate tab, search for module '%s'".formatted(
            GlobalIonLibrariesModule.NAME));
    library = defaultValue;
  }

  @Override
  public IonLibraryComponent createEditingComponent() {
    return new IonLibraryComponent(getValue());
  }

  @Override
  public void setValueFromComponent(IonLibraryComponent comp) {
    setValue(comp.getValue());
  }

  @Override
  public void setValueToComponent(IonLibraryComponent comp, @Nullable IonLibrary newValue) {
    comp.setValue(newValue);
  }

  @Override
  public IonLibrary getValue() {
    return library;
  }

  @Override
  public void setValue(IonLibrary newValue) {
    library = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (library == null) {
      errorMessages.add(name + " is undefined");
      return false;
    }

    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
// TODO
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
//TODO
  }

  @Override
  public UserParameter<IonLibrary, IonLibraryComponent> cloneParameter() {
    return new IonLibraryParameter(library);
  }
}
