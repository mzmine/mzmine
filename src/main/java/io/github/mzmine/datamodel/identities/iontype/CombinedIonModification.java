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

package io.github.mzmine.datamodel.identities.iontype;

import io.github.mzmine.datamodel.identities.NeutralMolecule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Combination of multiple IonModifications. Use the static create method to combine modifications
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class CombinedIonModification extends IonModification {

  /**
   * Modification parts
   */
  @NotNull
  private final IonModification[] mods;

  /**
   * Use the create method to construct combined modifications
   *
   * @param mods    list of modifications
   * @param type    modification type
   * @param deltaMZ delta m/z
   * @param charge  charge state
   */
  protected CombinedIonModification(@NotNull IonModification[] mods, IonModificationType type,
      double deltaMZ, int charge) {
    super(type, deltaMZ, charge);
    this.mods = mods;
    this.parsedName = parseName();
  }

  /**
   * Creates a combination of all argument IonModifications
   *
   * @param modifications all modifications
   * @return a combined ion modification or if the argument modification if only one was provided
   */
  public static IonModification create(@NotNull IonModification... modifications) {
    if (modifications.length == 1) {
      return modifications[0];
    }
    // all modifications
    List<IonModification> allModList = new ArrayList<>();
    for (IonModification modification : modifications) {
      Collections.addAll(allModList, modification.getModifications());
    }
    // sort
    IonModification[] allMods = allModList.toArray(IonModification[]::new);
    Arrays.sort(allMods);
    IonModificationType type = IonModificationType.getType(allMods);

    double deltaMZ = 0;
    int charge = 0;
    for (IonModification mod : allMods) {
      deltaMZ += mod.getMass();
      charge += mod.getCharge();
    }
    return new CombinedIonModification(allMods, type, deltaMZ, charge);
  }

  public static IonModification create(List<IonModification> modifications) {
    return create(modifications.toArray(IonModification[]::new));
  }

  @NotNull
  @Override
  public IonModification[] getModifications() {
    return mods;
  }

  @Override
  public int getNumberOfModifications() {
    return mods.length;
  }

  @Override
  public IonModification createOpposite() {
    IonModification[] mod =
        Arrays.stream(mods).map(IonModification::createOpposite).toArray(IonModification[]::new);
    return CombinedIonModification.create(mod);
  }

  /**
   * @return array of names
   */
  @Override
  public String[] getRawNames() {
    return Arrays.stream(mods).map(IonModification::getName).toArray(String[]::new);
  }

  @Override
  public String parseName() {
    StringBuilder nname = new StringBuilder();
    String s = null;
    int counter = 0;
    for (int i = 0; i < mods.length; i++) {
      String cs = mods[i].getName();
      if (s == null) {
        s = cs;
        counter = 1;
      } else if (s.equals(cs)) {
        counter++;
      } else {
        String sign = (mods[i - 1].getMass() < 0 ? "-" : "+");
        String counterS = counter > 1 ? String.valueOf(counter) : "";
        nname.append(sign).append(counterS).append(s);
        s = cs;
        counter = 1;
      }
    }
    String sign = (mods[mods.length - 1].getMass() < 0 ? "-" : "+");
    String counterS = counter > 1 ? String.valueOf(counter) : "";
    nname.append(sign).append(counterS).append(s);
    return nname.toString();
  }

  @Override
  public IonModification remove(IonModification type) {
    List<IonModification> newList = new ArrayList<>();
    for (IonModification m : this.getModifications()) {
      newList.add(m);
    }

    for (IonModification m : type.getModifications()) {
      newList.remove(m);
    }

    if (newList.isEmpty()) {
      return null;
    } else if (newList.size() == 1) {
      return newList.get(0);
    } else {
      return CombinedIonModification.create(newList);
    }
  }


  /**
   * this or any sub modification (for combined) equals to mod?
   *
   * @param mod
   * @return
   */
  @Override
  public boolean contains(IonModification mod) {
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
    map.put("Type", streamModifications().map(IonModification::getType).map(Enum::name)
        .collect(Collectors.joining(";")));
    map.put("Charge", streamModifications().map(IonModification::getCharge).map(String::valueOf)
        .collect(Collectors.joining(";")));
    map.put("Formula",
        streamModifications().map(IonModification::getMolFormula).collect(Collectors.joining(";")));
    return map;
  }
}
