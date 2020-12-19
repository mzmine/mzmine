package io.github.mzmine.datamodel.identities;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.mzmine.util.FormulaUtils;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class NeutralMolecule {

  protected @Nullable IMolecularFormula cdkFormula;
  protected @Nullable String molFormula;
  protected @Nonnull String name;
  protected double mass;

  public NeutralMolecule(String name, double mass) {
    this(name, "", mass);
  }

  public NeutralMolecule(String name, String molFormula, double mass) {
    super();
    this.name = name;
    this.molFormula = molFormula;
    cdkFormula = FormulaUtils.createMajorIsotopeMolFormula(molFormula);
    this.mass = mass;
  }

  public double getMass() {
    return mass;
  }

  public double getAbsMass() {
    return Math.abs(mass);
  }

  public String getName() {
    return name;
  }

  public String parseName() {
    String sign = this.getMass() < 0 ? "-" : "+";
    return sign + getName();
  }

  public String getMolFormula() {
    return molFormula;
  }

  public IMolecularFormula getCDKFormula() {
    return cdkFormula;
  }


  @Override
  public int hashCode() {
    return Objects.hash(name, mass);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!obj.getClass().equals(getClass()))
      return false;
    if (!(obj instanceof NeutralMolecule))
      return false;
    NeutralMolecule other = (NeutralMolecule) obj;
    if (!name.equals(other.name))
      return false;
    if (!Objects.equals(mass, other.getMass()))
      return false;
    return true;
  }
}
