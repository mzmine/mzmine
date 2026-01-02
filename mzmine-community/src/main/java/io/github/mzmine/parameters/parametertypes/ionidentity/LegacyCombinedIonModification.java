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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import io.github.mzmine.datamodel.identities.IonPart;
import io.github.mzmine.datamodel.identities.NeutralMolecule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * Combination of multiple IonModifications. Use the static create method to combine modifications
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
@Deprecated
class LegacyCombinedIonModification extends LegacyIonModification {

  /**
   * Modification parts
   */
  @NotNull
  private final LegacyIonModification[] mods;

  /**
   * Use the create method to construct combined modifications
   *
   * @param mods    list of modifications
   * @param type    modification type
   * @param deltaMZ delta m/z
   * @param charge  charge state
   */
  protected LegacyCombinedIonModification(@NotNull LegacyIonModification[] mods,
      LegacyIonModificationType type, double deltaMZ, int charge) {
    super(type, deltaMZ, charge);
    this.mods = mods;
    this.parsedName = parseName();
  }

  @Override
  public @NotNull Stream<? extends IonPart> toNewParts() {
    return Arrays.stream(mods).flatMap(LegacyIonModification::toNewParts);
  }

  /**
   * Creates a combination of all argument IonModifications
   *
   * @param modifications all modifications
   * @return a combined ion modification or if the argument modification if only one was provided
   */
  public static LegacyIonModification create(@NotNull LegacyIonModification... modifications) {
    if (modifications.length == 1) {
      return modifications[0];
    }
    // all modifications
    List<LegacyIonModification> allModList = new ArrayList<>();
    for (LegacyIonModification modification : modifications) {
      Collections.addAll(allModList, modification.getModifications());
    }
    // sort
    LegacyIonModification[] allMods = allModList.toArray(LegacyIonModification[]::new);
    Arrays.sort(allMods);
    LegacyIonModificationType type = LegacyIonModificationType.getType(allMods);

    double deltaMZ = 0;
    int charge = 0;
    for (LegacyIonModification mod : allMods) {
      deltaMZ += mod.getMass();
      charge += mod.getCharge();
    }
    return new LegacyCombinedIonModification(allMods, type, deltaMZ, charge);
  }

  public static LegacyIonModification create(List<LegacyIonModification> modifications) {
    return create(modifications.toArray(LegacyIonModification[]::new));
  }

  @NotNull
  @Override
  public LegacyIonModification[] getModifications() {
    return mods;
  }

  @Override
  public int getNumberOfModifications() {
    return mods.length;
  }

  @Override
  public LegacyIonModification createOpposite() {
    LegacyIonModification[] mod = Arrays.stream(mods).map(LegacyIonModification::createOpposite)
        .toArray(LegacyIonModification[]::new);
    return LegacyCombinedIonModification.create(mod);
  }

  /**
   * @return array of names
   */
  @Override
  public String[] getRawNames() {
    return Arrays.stream(mods).map(LegacyIonModification::getName).toArray(String[]::new);
  }

  @Override
  public String parseName() {
    StringBuilder nname = new StringBuilder();
    String s = null;
    int counter = 0;
    int lastNonElectronIndex = 0;
    for (; lastNonElectronIndex < mods.length; lastNonElectronIndex++) {
      String cs = mods[lastNonElectronIndex].getName();
      if ("e".equals(cs)) {
        break; // electrons are always at the end
      }
      if (s == null) {
        s = cs;
        counter = 1;
      } else if (s.equals(cs)) {
        counter++;
      } else {
        // finish previous modification
        String sign = (mods[lastNonElectronIndex - 1].getMass() < 0 ? "-" : "+");
        String counterS = counter > 1 ? String.valueOf(counter) : "";
        nname.append(sign).append(counterS).append(s);
        s = cs;
        counter = 1;
      }
    }
    // finalize last part
    if (s != null && lastNonElectronIndex > 0) {
      String sign = (mods[lastNonElectronIndex - 1].getMass() < 0 ? "-" : "+");
      String counterS = counter > 1 ? String.valueOf(counter) : "";
      nname.append(sign).append(counterS).append(s);
    }
    return nname.toString();
  }

  @Override
  public LegacyIonModification remove(LegacyIonModification type) {
    List<LegacyIonModification> newList = new ArrayList<>();
    Collections.addAll(newList, this.getModifications());

    for (LegacyIonModification m : type.getModifications()) {
      newList.remove(m);
    }

    if (newList.isEmpty()) {
      return null;
    } else if (newList.size() == 1) {
      return newList.get(0);
    } else {
      return LegacyCombinedIonModification.create(newList);
    }
  }


  /**
   * this or any sub modification (for combined) equals to mod?
   *
   * @param mod
   * @return
   */
  @Override
  public boolean contains(LegacyIonModification mod) {
    return Arrays.stream(getModifications()).anyMatch(m -> m.equals(mod));
  }

  /**
   * Number of sub IonModifications
   *
   * @return
   */
  @Override
  public int getModCount() {
    return mods.length;
  }

  @Override
  public Map<String, String> getDataMap() {
    Map<String, String> map = new TreeMap<>();
    map.put("Name",
        streamModifications().map(NeutralMolecule::getName).collect(Collectors.joining(";")));
    map.put("Mass Diff", streamModifications().map(NeutralMolecule::getMass).map(String::valueOf)
        .collect(Collectors.joining(";")));
    map.put("Type", streamModifications().map(LegacyIonModification::getType).map(Enum::name)
        .collect(Collectors.joining(";")));
    map.put("Charge",
        streamModifications().map(LegacyIonModification::getCharge).map(String::valueOf)
            .collect(Collectors.joining(";")));
    map.put("Formula", streamModifications().map(LegacyIonModification::getMolFormula)
        .collect(Collectors.joining(";")));
    return map;
  }

  @Override
  public LegacyIonModification withCharge(final int newCharge) {
    return new LegacyCombinedIonModification(mods, type, mass, newCharge);
  }
}
