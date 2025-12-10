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

package io.github.mzmine.datamodel.identities;

import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IonParts {

  private static final Logger logger = Logger.getLogger(IonParts.class.getName());

  /**
   * The only part allowed with empty name
   */
  public static final IonPart SILENT_CHARGE = IonPart.ofNamed("", 0d, 1);

  public static final IonPart M_MINUS = IonPart.ofNamed("e", IonUtils.ELECTRON_MASS, -1);
  public static final IonPart M_PLUS = M_MINUS.flipCount();

  public static final IonPart H = new IonPart("H", 1);
  public static final IonPart H2_PLUS = H.withCount(2);
  public static final IonPart H3_PLUS = H.withCount(3);
  public static final IonPart NA = new IonPart("Na", 1);
  public static final IonPart NA2_PLUS = NA.withCount(2);
  public static final IonPart NH4 = IonPart.ofFormula("NH4", "NH4", 1);
  public static final IonPart K = new IonPart("K", 1);
  public static final IonPart CA = new IonPart("Ca", 2);
  public static final IonPart MG = new IonPart("Mg", 2);
  public static final IonPart FEII = new IonPart("Fe", 2);
  public static final IonPart FEIII = new IonPart("Fe", 3);

  // negative
  public static final IonPart H_MINUS = H.flipCount();
  public static final IonPart F = new IonPart("F", -1);
  public static final IonPart CL = new IonPart("Cl", -1);
  public static final IonPart BR = new IonPart("Br", -1);
  public static final IonPart I = new IonPart("I", -1);
  public static final IonPart FORMATE_FA = new IonPart("CHO2", -1);
  public static final IonPart ACETATE_AC = new IonPart("C2H3O2", -1);
  // complex
  public static final IonPart METHANOL = new IonPart("CH2OH", 0);
  public static final IonPart ETHANOL = new IonPart("C2H6O", 0);
  public static final IonPart ACN = new IonPart("CH3CN", 0);
  public static final IonPart FORMIC_ACID = new IonPart("CH2O2", 0);
  public static final IonPart ACETIC_ACID = new IonPart("CH3COOH", 0);
  public static final IonPart ISO_PROPANOL = new IonPart("C3H8O", 0);
  // loss;
  public static final IonPart CO = new IonPart("CO", 0, -1);
  public static final IonPart CO2 = new IonPart("CO2", 0, -1);
  public static final IonPart NH3 = IonPart.ofFormula("NH3", "NH3", 0, -1);
  public static final IonPart H2 = new IonPart("H2", 0, -1);
  public static final IonPart C2H4 = new IonPart("C2H4", 0, -1);
  public static final IonPart HCL = IonPart.ofFormula("HCl", "HCl", 0, -1);
  public static final IonPart H2O = new IonPart("H2O", 0, -1);
  public static final IonPart H2O_2 = H2O.withCount(-2);
  public static final IonPart H2O_3 = H2O.withCount(-3);
  public static final IonPart H2O_4 = H2O.withCount(-4);
  public static final IonPart H2O_5 = H2O.withCount(-5);

  // default modifications
  public static final List<IonPart> DEFAULT_VALUES_MODIFICATIONS = List.of(H2O, H2O_2, H2O_3, H2O_4,
      H2O_5, HCL, NH3, CO, CO2, H2, C2H4, FORMIC_ACID, ACETIC_ACID, ACN, METHANOL, ETHANOL,
      ISO_PROPANOL);

  public static final List<IonPart> PREDEFINED_PARTS = List.of(H2O, H2O_2, H2O_3, H2O_4, H2O_5, HCL,
      C2H4, H2, NH3, CO, CO2, FORMIC_ACID, ACETIC_ACID, ACN, METHANOL, ETHANOL, ISO_PROPANOL,
      ACETATE_AC, FORMATE_FA, I, BR, CL, F, H_MINUS, FEII, FEIII, MG, CA, K, NH4, NA2_PLUS, NA, H,
      H2_PLUS, H3_PLUS, M_MINUS, M_PLUS);

  /**
   * @param nameOrFormula structure or common name
   * @param charge
   * @return an IonPart either predefined by name, common name {@link CompoundsByNames}, by
   * structure. Otherwise, {@link IonPart#unknown(String, int)}
   */
  @NotNull
  public static IonPart findPartByNameOrFormula(@NotNull String nameOrFormula, int count,
      Integer charge) {

    nameOrFormula = nameOrFormula.trim();
    // search predefined by mzmine and by user
    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();
    final List<IonPartDefinition> definitionsForName = global.findPartsByName(nameOrFormula);

    IonPartDefinition best = null;
    for (final IonPartDefinition predefined : definitionsForName) {
      if (predefined.name().equals(nameOrFormula)) {
        // directly found the correct part
        if (charge == null || Objects.equals(charge, predefined.charge())) {
          return predefined.withCount(count);
        }
        if (best == null) {
          best = predefined;
        }
      }
    }
    if (best != null) {
      // mismatch in charge
      // like when the current list of ion parts defines Fe+2 but not Fe+3 which was loaded
      // create a new instance with that charge
      return best.withCount(count).withSingleCharge(charge);
    }

    // map names or structure by internal known names
    Optional<IonPart> part = CompoundsByNames.getIonPartByName(nameOrFormula);
    if (part.isPresent()) {
      return part.get().withCount(count).withSingleCharge(charge);
    }

    // try with formula or return unknown
    return IonPart.ofFormula(nameOrFormula, charge, count);
  }


  /**
   * merges duplicate parts (all matching properties excluding the count). If after merging a part
   * has count==0 it is removed from the list. Like when adding and removing a 'Na'.
   *
   * @param parts may contain duplicates
   * @return unmodifiable list of ion parts
   */
  public static List<IonPart> mergeDuplicates(@NotNull IonPart... parts) {
    return mergeDuplicates(List.of(parts));
  }

  /**
   * merges duplicate parts (all matching properties excluding the count). If after merging a part
   * has count==0 it is removed from the list. Like when adding and removing a 'Na'.
   *
   * @param parts may contain duplicates
   * @return unmodifiable list of ion parts
   */
  @NotNull
  public static List<IonPart> mergeDuplicates(@Nullable Collection<IonPart> parts) {
    if (parts == null) {
      return List.of();
    }
    // use trick to create a copy with count 1 to group all parts based on the other properties but not count
    final Collection<List<IonPart>> groupedDuplicates = parts.stream().filter(Objects::nonNull)
        .collect(Collectors.groupingBy(type -> type.withCount(1))).values();
    // merge duplicates into one and return new list
    //noinspection DataFlowIssue,OptionalGetWithoutIsPresent
    return groupedDuplicates.stream()
        .map(duplicates -> duplicates.stream().reduce(IonPart::merge).get())
        // require count !=0 to only keep parts that matter
        .filter(part -> part.count() != 0).toList();
  }
}
