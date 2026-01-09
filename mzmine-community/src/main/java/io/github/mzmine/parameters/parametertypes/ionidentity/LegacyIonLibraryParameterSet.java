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


import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import java.util.logging.Level;
import java.util.logging.Logger;

@Deprecated
class LegacyIonLibraryParameterSet extends SimpleParameterSet {

  private static final Logger logger = Logger.getLogger(
      LegacyIonLibraryParameterSet.class.getName());
  public static final IntegerParameter MAX_CHARGE = new IntegerParameter("Maximum charge",
      "Maximum charge to be used for adduct search.", 2, 1, 100);
  public static final IntegerParameter MAX_MOLECULES = new IntegerParameter(
      "Maximum molecules/cluster", "Maximum molecules per cluster (f.e. [2M+Na]+).", 3, 1, 10);

  public static final LegacyIonModificationParameter ADDUCTS = new LegacyIonModificationParameter(
      "Adducts",
      "List of adducts, each one refers a specific distance in m/z axis between related peaks");

  public LegacyIonLibraryParameterSet() {
    super(MAX_CHARGE, MAX_MOLECULES, ADDUCTS);
  }

  public void setAll(int maxCharge, int maxMolecules, final LegacyIonModification[] adductChoices,
      final LegacyIonModification[] modificationChoices, final LegacyIonModification[][] selected) {
    setParameter(MAX_MOLECULES, maxMolecules);
    setParameter(MAX_CHARGE, maxCharge);
    var ionLib = getParameter(ADDUCTS);
    ionLib.setChoices(adductChoices, modificationChoices);
    ionLib.setValue(selected);
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public ParameterSet cloneParameterSet() {
    return this.cloneParameterSet(false);
  }

  /**
   * Need to override as this class is package private and super uses constructor to clone
   */
  @Override
  public ParameterSet cloneParameterSet(boolean keepSelection) {
    try {
      LegacyIonLibraryParameterSet newSet = new LegacyIonLibraryParameterSet();
      // Make a deep copy of the parameters
      newSet.parameters = ParameterUtils.cloneParameters(parameters, keepSelection);
      newSet.setModuleNameAttribute(this.getModuleNameAttribute());
      newSet.helpUrl = helpUrl;

      return newSet;
    } catch (Throwable e) {
      logger.log(Level.WARNING, "While cloning parameters: " + e.getMessage(), e);
      return null;
    }
  }
}
