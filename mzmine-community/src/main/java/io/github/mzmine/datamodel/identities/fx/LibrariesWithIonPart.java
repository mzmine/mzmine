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

package io.github.mzmine.datamodel.identities.fx;

import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonType.IonTypeStringFlavor;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

record LibrariesWithIonPart(IonPart searchedPart, Map<IonLibrary, List<IonType>> libraries,
                            List<IonType> types) {

  @Override
  public @NotNull String toString() {
    final String allLibs = libraries.keySet().stream().map(IonLibrary::name).sorted()
        .collect(Collectors.joining(", "));

    Set<IonType> allTypes = new HashSet<>();
    allTypes.addAll(types);
//    for (var type : types) {
//      allTypes.add(type.toString(IonTypeStringFlavor.SIMPLE_DEFAULT));
//    }
    for (List<IonType> list : libraries.values()) {
      allTypes.addAll(list);
    }

    final String allTypesStr = allTypes.stream().sorted()
        .map(t -> t.toString(IonTypeStringFlavor.SIMPLE_DEFAULT)).collect(Collectors.joining(", "));

    return """
        Libraries with part:
        %s
        
        All ion types with part:
        %s""".formatted(allLibs, allTypesStr);
  }
}
