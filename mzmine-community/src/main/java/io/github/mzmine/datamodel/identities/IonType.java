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

package io.github.mzmine.datamodel.identities;

import io.github.mzmine.datamodel.identities.IonPart.IonStringFlavor;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Whole definition of an adduct, insource fragment, insource cluster of 2M etc. e.g., [2M+Na]+,
 * [M-H2O+2H]+2
 *
 * @param name        parsed name from all parts
 * @param parts       unmodifiable list of ion parts
 * @param totalMass   total mass from ion parts
 * @param totalCharge total charge from all ion parts
 * @param molecules   number of M molecules in cluster, e.g., M or 2M
 */
public record IonType(@NotNull String name, @NotNull List<@NotNull IonPart> parts, double totalMass,
                      int totalCharge, int molecules) {

  public static IonType create(@NotNull List<@NotNull IonPart> parts, int molecules) {
    // requires to merge all the same IonParts into single objects by adding up their count multiplier
    HashMap<IonPart, IonPart> unique = HashMap.newHashMap(parts.size());
    for (final IonPart part : parts) {
      unique.merge(part, part, IonPart::merge);
    }

    // sort so that name will be correct
    parts = unique.values().stream().sorted(IonPart.DEFAULT_ION_ADDUCT_SORTER).toList();

    double totalMass = 0;
    int totalCharge = 0;
    for (final IonPart part : parts) {
      totalMass += part.getTotalMass();
      totalCharge += part.getTotalCharge();
    }

    // generate name
    String ionParts = parts.stream().map(p -> p.toString(IonStringFlavor.SIMPLE_NO_CHARGE))
        .collect(Collectors.joining());
    String name = "[" + molecules + ionParts + "]" + IonUtils.getChargeString(totalCharge);
    return new IonType(name, parts, totalMass, totalCharge, molecules);
  }

  @Override
  public String toString() {
    return name;
  }
}
