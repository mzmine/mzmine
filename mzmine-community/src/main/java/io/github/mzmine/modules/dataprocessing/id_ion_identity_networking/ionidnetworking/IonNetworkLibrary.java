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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking;


import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.identities.IonLibrary;
import io.github.mzmine.datamodel.identities.SearchableIonLibrary;
import io.github.mzmine.datamodel.identities.SimpleIonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.parameters.parametertypes.ionidentity.legacy.LegacyIonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Replaced by {@link IonLibrary} and {@link SearchableIonLibrary} for searches.
 * <p>
 * Important: Only used for loading of old parameters.
 */
@Deprecated
public class IonNetworkLibrary {

  private static final Logger LOG = Logger.getLogger(IonNetworkLibrary.class.getName());

  @NotNull
  public IonLibrary toNewLibrary() {
    final List<io.github.mzmine.datamodel.identities.IonType> ions = allAdducts.stream()
        .map(IonType::toNewIonType).toList();
    return new SimpleIonLibrary("Legacy library imported", ions);
  }

  private MZTolerance mzTolerance;
  // adducts
  private final IonModification[] selectedAdducts;
  private final IonModification[] selectedMods;
  private final List<IonType> allAdducts = new ArrayList<>();
  private final PolarityType polarity;
  private final int maxCharge;
  private final int maxMolecules;

  /**
   * Set mztolerance later
   */
  public IonNetworkLibrary(LegacyIonLibraryParameterSet parameterSet) {
    this(parameterSet, null);
  }


  public IonNetworkLibrary(LegacyIonLibraryParameterSet parameterSet, MZTolerance mzTolerance) {
    this(parameterSet, PolarityType.ANY, mzTolerance);
  }

  public IonNetworkLibrary(LegacyIonLibraryParameterSet parameterSet, PolarityType polarity,
      MZTolerance mzTolerance) {
    this(mzTolerance, parameterSet.getValue(LegacyIonLibraryParameterSet.MAX_CHARGE), polarity,
        parameterSet.getValue(LegacyIonLibraryParameterSet.MAX_MOLECULES),
        parameterSet.getValue(LegacyIonLibraryParameterSet.ADDUCTS)[0],
        parameterSet.getValue(LegacyIonLibraryParameterSet.ADDUCTS)[1]);
  }

  /**
   * For simple setup
   */
  public IonNetworkLibrary(MZTolerance mzTolerance, int maxCharge, PolarityType polarity,
      int maxMolecules, IonModification[] selectedAdducts, IonModification[] selectedMods) {
    this.mzTolerance = mzTolerance;
    this.maxCharge = maxCharge;
    this.polarity = polarity;
    // adducts stuff
    this.maxMolecules = maxMolecules;
    this.selectedAdducts = selectedAdducts;
    this.selectedMods = selectedMods;

    createAllAdducts(polarity, maxMolecules, maxCharge);
  }

  /**
   * create all possible adducts
   */
  private void createAllAdducts(PolarityType polarity, int maxMolecules, int maxCharge) {
    // normal primary adducts
    allAdducts.clear();
    // add all [M+?]c+ as references to neutral loss
    // [M-H2O+?]c+
    for (int c = 1; c <= maxCharge; c++) {
      if (polarity.includesPositive()) {
        allAdducts.add(new IonType(1, IonModification.getUndefinedforCharge(c)));
      }
      if (polarity.includesNegative()) {
        allAdducts.add(new IonType(1, IonModification.getUndefinedforCharge(-c)));
      }
    }

    for (IonModification a : selectedAdducts) {
      if (polarity.includesCharge(a.getCharge())) {
        if (a.getAbsCharge() <= maxCharge) {
          for (int n = 1; n <= maxMolecules; n++) {
            allAdducts.add(new IonType(n, a));
          }
        }
      }
    }

    addModification();
    // print them out
    for (IonType a : allAdducts) {
      LOG.finest("Adding modification: " + a.toString());
    }
  }

  /**
   * adds modification to the existing adducts
   */
  private void addModification() {
    int size = allAdducts.size();
    for (int i = 0; i < size; i++) {
      for (IonModification a : selectedMods) {
        IonType ion = allAdducts.get(i);
        if (filter(ion, a)) {
          allAdducts.add(ion.createModified(a));
        }
      }
    }
  }


  /**
   * Only true if no filter is negative. Filter out -NH3+NH4
   *
   * @param ion
   * @param mod
   * @return
   */
  private boolean filter(IonType ion, IonModification mod) {
    IonModification add = ion.getAdduct();
    // specific filters
    boolean bad = (add.contains(IonModification.NH4) && mod.contains(IonModification.NH3));

    return !bad;
  }

  public void setMzTolerance(MZTolerance mzTolerance) {
    this.mzTolerance = mzTolerance;
  }

  public MZTolerance getMzTolerance() {
    return mzTolerance;
  }

  public IonModification[] getSelectedAdducts() {
    return selectedAdducts;
  }

  public IonModification[] getSelectedMods() {
    return selectedMods;
  }

  public List<IonType> getAllAdducts() {
    return allAdducts;
  }

  public PolarityType isPositive() {
    return polarity;
  }

  public int getMaxMolecules() {
    return maxMolecules;
  }

  public int getMaxCharge() {
    return maxCharge;
  }

}
