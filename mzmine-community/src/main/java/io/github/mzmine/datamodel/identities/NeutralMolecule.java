
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

package io.github.mzmine.datamodel.identities;

import io.github.mzmine.util.FormulaUtils;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class NeutralMolecule {

  @Nullable
  protected final IMolecularFormula cdkFormula;
  @Nullable
  protected final String molFormula;
  protected final double mass;
  @NotNull
  protected String name;

  public NeutralMolecule(@NotNull String name, double mass) {
    this(name, null, mass);
  }

  public NeutralMolecule(@NotNull String name, @Nullable String molFormula, double mass) {
    this.name = name;
    this.molFormula = molFormula;
    if (molFormula != null && molFormula.length() > 0) {
      cdkFormula = FormulaUtils.createMajorIsotopeMolFormula(molFormula);
    } else {
      cdkFormula = null;
    }
    this.mass = mass;
  }

  public double getMass() {
    return mass;
  }

  public double getAbsMass() {
    return Math.abs(mass);
  }

  /**
   * the raw name
   */
  @NotNull
  public String getName() {
    return name;
  }

  /**
   * The parsed name (default + or - the name depending on the mass difference)
   */
  public String parseName() {
    String sign = this.getMass() < 0 ? "-" : "+";
    return sign + getName();
  }

  @Nullable
  public String getMolFormula() {
    return molFormula;
  }

  @Nullable
  public IMolecularFormula getCDKFormula() {
    return cdkFormula;
  }


  @Override
  public int hashCode() {
    return Objects.hash(name, mass);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!obj.getClass().equals(getClass())) {
      return false;
    }
    NeutralMolecule other = (NeutralMolecule) obj;
    return Objects.equals(name, other.name) && Objects.equals(mass, other.getMass());
  }
}
