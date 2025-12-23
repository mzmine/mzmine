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

import io.github.mzmine.datamodel.identities.IonLibraries;
import io.github.mzmine.datamodel.identities.IonLibrary;
import io.github.mzmine.datamodel.identities.SearchableIonLibrary;
import io.github.mzmine.datamodel.identities.fx.GlobalIonLibrariesModule;
import io.github.mzmine.datamodel.identities.io.IonLibraryIO;
import io.github.mzmine.datamodel.identities.io.LoadedIonLibrary;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.parameters.AbstractParameter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.ionidentity.legacy.LegacyIonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import io.github.mzmine.util.StringUtils;
import java.util.Collection;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;


/**
 * An {@link IonLibrary} that can be converted to {@link SearchableIonLibrary} for matching.
 * <p>
 * Replaces the {@link LegacyIonLibraryParameterSet} that was typically used as a
 * {@link ParameterSetParameter} or {@link OptionalModuleParameter} or {@link SubModuleParameter}.
 * The {@link #loadValueFromXML(Element)} handles the loading of the old parameterset so this is a
 * drop in replacement.
 */
public class IonLibraryParameter extends AbstractParameter<IonLibrary, IonLibraryComponent> {

  @Nullable
  private IonLibrary library;

  public IonLibraryParameter() {
    this(IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY_FULL);
  }

  public IonLibraryParameter(@Nullable IonLibrary defaultValue) {
    this("Ion library", "", defaultValue);
  }

  public IonLibraryParameter(String name, String description, @Nullable IonLibrary defaultValue) {
    String fullDescription = "Select an ion library. Ion types and libraries are created in a separate tab, search for module '%s'".formatted(
        GlobalIonLibrariesModule.NAME);
    if (description != null) {
      fullDescription = description + "\n" + fullDescription;
    }

    super(name, fullDescription, defaultValue);
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


  /**
   * Supports loading of {@link LegacyIonLibraryParameterSet} that was typically used as a
   * {@link ParameterSetParameter} or {@link OptionalModuleParameter} or {@link SubModuleParameter}.
   * or
   */
  @Override
  public void loadValueFromXML(Element xmlElement) {
    final String version = xmlElement.getAttribute("libraryVersion");
    if (StringUtils.isBlank(version)) {
      loadLegacyLibraryParameters(xmlElement);
      return;
    }

    final LoadedIonLibrary loaded = IonLibraryIO.loadFromXML(xmlElement);
    setValue(loaded == null ? null : loaded.library());
  }

  private void loadLegacyLibraryParameters(Element xmlElement) {
    final LegacyIonLibraryParameterSet parameters = (LegacyIonLibraryParameterSet) new LegacyIonLibraryParameterSet().cloneParameterSet();
    final Map<String, Parameter<?>> loaded = parameters.loadValuesFromXML(xmlElement);
    if (!loaded.containsKey(LegacyIonLibraryParameterSet.ADDUCTS.getName())) {
      return; // nothing loaded
    }

    // create library from old to new
    final IonLibrary library = new IonNetworkLibrary(parameters).toNewLibrary();
    setValue(library);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    // set libraryVersion to 2 so that we know to load a new {@link IonLibrary} or
    // via the old {@link LegacyIonLibraryParameterSet}
    // old libraries did not use libraryVersion so it is unset in this case
    xmlElement.setAttribute("libraryVersion", "2");
    // save all ions and library name so that a library will reload exactly the same library
    // the local version of this library might change and the component will display a modified symbol
    IonLibraryIO.saveToXML(xmlElement, getValue());
  }

  @Override
  public IonLibraryParameter cloneParameter() {
    return new IonLibraryParameter(library);
  }
}
