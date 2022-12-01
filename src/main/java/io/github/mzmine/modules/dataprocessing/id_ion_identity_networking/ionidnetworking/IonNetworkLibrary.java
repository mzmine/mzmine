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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking;


import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonModificationType;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IonNetworkLibrary {

  private static final Logger LOG = Logger.getLogger(IonNetworkLibrary.class.getName());

  public enum CheckMode {
    AVGERAGE, ONE_FEATURE, ALL_FEATURES;

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

  private MZTolerance mzTolerance;
  // adducts
  private final IonModification[] selectedAdducts;
  private final IonModification[] selectedMods;
  private List<IonType> allAdducts = new ArrayList<>();
  private final boolean isPositive;
  private final int maxCharge;
  private final int maxMolecules;

  /**
   * Set mztolerance later
   */
  public IonNetworkLibrary(IonLibraryParameterSet parameterSet) {
    this(parameterSet, null);
  }

  /**
   * For simple setup
   */
  public IonNetworkLibrary(IonLibraryParameterSet parameterSet, MZTolerance mzTolerance) {
    this.mzTolerance = mzTolerance;
    this.maxCharge = parameterSet.getParameter(IonLibraryParameterSet.MAX_CHARGE).getValue();
    // adducts stuff
    isPositive = parameterSet.getParameter(IonLibraryParameterSet.POSITIVE_MODE).getValue()
        .equals("POSITIVE");
    maxMolecules = parameterSet.getParameter(IonLibraryParameterSet.MAX_MOLECULES).getValue();

    selectedAdducts = parameterSet.getParameter(IonLibraryParameterSet.ADDUCTS).getValue()[0];
    selectedMods = parameterSet.getParameter(IonLibraryParameterSet.ADDUCTS).getValue()[1];

    createAllAdducts(isPositive, maxMolecules, maxCharge);
  }

  /**
   * For simple setup
   */
  public IonNetworkLibrary(MZTolerance mzTolerance, int maxCharge, boolean isPositive,
      int maxMolecules, IonModification[] selectedAdducts, IonModification[] selectedMods) {
    this.mzTolerance = mzTolerance;
    this.maxCharge = maxCharge;
    this.isPositive = isPositive;
    // adducts stuff
    this.maxMolecules = maxMolecules;
    this.selectedAdducts = selectedAdducts;
    this.selectedMods = selectedMods;

    createAllAdducts(isPositive, maxMolecules, maxCharge);
  }

  /**
   * create all possible adducts
   */
  private void createAllAdducts(boolean positive, int maxMolecules, int maxCharge) {
    // normal primary adducts
    allAdducts.clear();
    // add all [M+?]c+ as references to neutral loss
    // [M-H2O+?]c+
    for (int c = 1; c <= maxCharge; c++) {
      allAdducts.add(new IonType(1, IonModification.getUndefinedforCharge(positive ? c : -c)));
    }

    for (IonModification a : selectedAdducts) {
      if ((a.getCharge() > 0 && positive) || (a.getCharge() < 0 && !positive)) {
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
   * Does find all possible adduct combinations
   */
  public @NotNull
  List<IonIdentity[]> findAdducts(final FeatureList featureList, final FeatureListRow row1,
      final FeatureListRow row2, final CheckMode mode, final double minHeight) {
    return findAdducts(featureList, row1, row2, row1.getRowCharge(), row2.getRowCharge(), mode,
        minHeight);
  }

  /**
   * Does find all possible adducts between row1 and row2
   *
   * @param z1 -1 or 0 if not set (charge state always positive)
   * @param z2 -1 or 0 if not set (charge state always positive)
   * @return returns list of adducts for [row1, row2]
   */
  public @NotNull
  List<IonIdentity[]> findAdducts(final FeatureList featureList, final FeatureListRow row1,
      final FeatureListRow row2, int z1, int z2, final CheckMode mode, final double minHeight) {
    z1 = Math.abs(z1);
    z2 = Math.abs(z2);
    List<IonIdentity[]> list = new ArrayList<>();
    // check all combinations of adducts
    for (IonType adduct : allAdducts) {
      for (IonType adduct2 : allAdducts) {
        if (adduct.equals(adduct2)) {
          continue;
        }

        // do not check if MOL = MOL and MOL>1
        // only one can be modified
        // check charge state if absCharge is not -1 or 0 (no charge detected)
        if (checkMolCount(adduct, adduct2) //
            && checkMaxMod(adduct, adduct2) //
            && checkChargeStates(adduct, adduct2, z1, z2) //
            && checkMultiChargeDifference(adduct, adduct2) //
            && checkSameAdducts(adduct, adduct2)) {
          // checks each raw file - only true if all m/z are in range
          if (checkAdduct(featureList, row1, row2, adduct, adduct2, mode, minHeight)) {
            // is a2 a modification of a1? (same adducts - different mods
            if (adduct2.isModificationOf(adduct)) {
              IonType mod = adduct2.subtractMods(adduct);
              IonType undefined =
                  new IonType(IonModification.getUndefinedforCharge(adduct.getCharge()));
              list.add(IonIdentity.addAdductIdentityToRow(mzTolerance, row1, undefined, row1, mod));
            } else if (adduct.isModificationOf(adduct2)) {
              IonType mod = adduct.subtractMods(adduct2);
              IonType undefined =
                  new IonType(IonModification.getUndefinedforCharge(adduct2.getCharge()));
              list.add(IonIdentity.addAdductIdentityToRow(mzTolerance, row1, mod, row2, undefined));
            } else {
              // Add adduct identity and notify GUI.
              // only if not already present
              list.add(
                  IonIdentity.addAdductIdentityToRow(mzTolerance, row1, adduct, row2, adduct2));
            }
          }
        }
      }
    }
    // no adduct to be found
    return list;
  }


  /**
   * Searches for an IonType for row that matches in network
   *
   * @param row row to check against neutral mass of ionNet
   * @param ionNet for neutral mass
   * @return IonIdentity or null if already present in network or if no match available
   */
  @Nullable
  public IonIdentity findAdducts(FeatureListRow row, IonNetwork ionNet) {
    // already contained
    if (ionNet.containsKey(row)) {
      return null;
    }

    int z = Math.abs(row.getBestFeature().getCharge());
    List<IonIdentity> list = new ArrayList<>();
    // check all combinations of adducts
    for (IonType adduct : allAdducts) {
      if (!adduct.isUndefinedAdduct()) {
        if (z == 0 || adduct.getAbsCharge() == z) {
          double neutralMass = ionNet.getNeutralMass();
          double mz = row.getAverageMZ();
          double rowMass = adduct.getMass(mz);
          if (mzTolerance.checkWithinTolerance(neutralMass, rowMass)) {
            // add identity
            IonIdentity a = new IonIdentity(adduct);
            ionNet.put(row, a);
            row.addIonIdentity(a, false);
            return a;
          }
        }
        // no adduct to be found
      }
    }
    return null;
  }


  /**
   * Do not allow adduct overlap: Only if both are of type undefined ?
   *
   * @return
   */
  private boolean checkSameAdducts(IonType a, IonType b) {
    // no adduct overlap (with none being undefined) (or both undefined_adduct)
    return (!a.hasAdductOverlap(b)
            && !a.getAdduct().getType().equals(IonModificationType.UNDEFINED_ADDUCT)
            && !b.getAdduct().getType().equals(IonModificationType.UNDEFINED_ADDUCT))
           // all beeing M+?
           || (a.getAdduct().getType().equals(IonModificationType.UNDEFINED_ADDUCT)
               && b.getAdduct().getType().equals(IonModificationType.UNDEFINED_ADDUCT));
  }

  /**
   * [yM+X]+ and [yM+X-H]+ are only different by -H. if any adduct part or modification equals,
   * return false. Charge is different
   *
   * @return only true if charge is different or no modification or adduct sub part equals
   */
  private boolean checkMultiChargeDifference(IonType a, IonType b) {
    return a.getCharge() != b.getCharge()
           || (!a.hasModificationOverlap(b) && !a.hasAdductOverlap(b));
  }

  /**
   * MOL != MOL or MOL==1
   *
   * @return
   */
  private boolean checkMolCount(IonType a, IonType b) {
    return a.getMolecules() != b.getMolecules() || (a.getMolecules() == 1 && b.getMolecules() == 1);
  }

  /**
   * True if a charge state was not detected or if it fits to the adduct
   *
   * @param adduct
   * @param adduct2
   * @param z1
   * @param z2
   * @return
   */
  private boolean checkChargeStates(IonType adduct, IonType adduct2, int z1, int z2) {
    return (z1 == 0 || adduct.getAbsCharge() == z1) && (z2 == 0 || adduct2.getAbsCharge() == z2);
  }

  /**
   * Only one adduct can have modifications
   *
   * @param adduct
   * @param adduct2
   * @return
   */
  private boolean checkMaxMod(IonType adduct, IonType adduct2) {
    return !(adduct.getModCount() > 0 && adduct2.getModCount() > 0);
  }

  /**
   * Check if candidate peak is a given type of adduct of given main peak. is not checking retention
   * time (has to be checked before)
   *
   * @param row1
   * @param row2
   * @param adduct
   * @param adduct2
   * @param minHeight exclude smaller peaks as they can have a higher mz difference
   * @return false if one peak pair with height>=minHeight is outside of mzTolerance
   */
  private boolean checkAdduct(final FeatureList featureList, final FeatureListRow row1,
      final FeatureListRow row2, final IonType adduct, final IonType adduct2, final CheckMode mode,
      double minHeight) {
    // averarge mz
    if (mode.equals(CheckMode.AVGERAGE)) {
      double m1 = adduct.getMass(row1.getAverageMZ());
      double m2 = adduct2.getMass(row2.getAverageMZ());
      return mzTolerance.checkWithinTolerance(m1, m2);
    } else {
      // feature comparison
      // for each peak[rawfile] in row
      boolean hasCommonFeature = false;
      //
      for (RawDataFile raw : featureList.getRawDataFiles()) {
        Feature f1 = row1.getFeature(raw);
        Feature f2 = row2.getFeature(raw);
        // check for minimum height. Small peaks have a higher delta mz
        if (f1 != null && f2 != null && f1.getHeight() >= minHeight
            && f2.getHeight() >= minHeight) {
          hasCommonFeature = true;
          double m1 = adduct.getMass(f1.getMZ());
          double m2 = adduct2.getMass(f2.getMZ());
          boolean sameMZ = mzTolerance.checkWithinTolerance(m1, m2);

          // short cut
          switch (mode) {
            case ONE_FEATURE:
              if (sameMZ) {
                return true;
              }
              break;
            case ALL_FEATURES:
              if (!sameMZ) {
                return false;
              }
              break;
          }
        }
      }
      // directly returns false if not in range
      // so if has common peak = isAdduct
      return mode.equals(CheckMode.ALL_FEATURES) && hasCommonFeature;
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

  public boolean isPositive() {
    return isPositive;
  }

  public int getMaxMolecules() {
    return maxMolecules;
  }

  public int getMaxCharge() {
    return maxCharge;
  }

}
