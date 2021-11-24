/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import java.util.List;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class ManualAnnotation {

  private List<FeatureIdentity> identities;
  private String comment;
  private String compoundName;
  private String ion;
  private String formula;
  private String smiles;
  private String inchi;

  public ManualAnnotation() {
  }

  public ManualAnnotation(List<FeatureIdentity> identities, String comment,
      String compoundName, String ion, String formula, String smiles, String inchi) {
    this.identities = identities;
    this.comment = comment;
    this.compoundName = compoundName;
    this.ion = ion;
    this.formula = formula;
    this.smiles = smiles;
    this.inchi = inchi;
  }

  public List<FeatureIdentity> getIdentities() {
    return identities;
  }

  public void setIdentities(List<FeatureIdentity> identities) {
    this.identities = identities;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getCompoundName() {
    return compoundName;
  }

  public void setCompoundName(String compoundName) {
    this.compoundName = compoundName;
  }

  public String getIon() {
    return ion;
  }

  public void setIon(String ion) {
    this.ion = ion;
  }

  public String getFormula() {
    return formula;
  }

  public void setFormula(String formula) {
    this.formula = formula;
  }

  public String getSmiles() {
    return smiles;
  }

  public void setSmiles(String smiles) {
    this.smiles = smiles;
  }

  public String getInchi() {
    return inchi;
  }

  public void setInchi(String inchi) {
    this.inchi = inchi;
  }

  public Object get(DataType sub) {
    if (sub instanceof CommentType) {
      return comment;
    }
    if (sub instanceof SmilesStructureType) {
      return smiles;
    }
    if (sub instanceof InChIStructureType) {
      return inchi;
    }
    if (sub instanceof IdentityType) {
      return identities;
    }
    if (sub instanceof FormulaType) {
      return formula;
    }
    if (sub instanceof IonAdductType) {
      return ion;
    }
    if (sub instanceof CompoundNameType) {
      return compoundName;
    }
    throw new IllegalArgumentException(
        String.format("Subtype %s of class %s is not handled", sub, sub.getClass().getName()));
  }

  public void set(DataType sub, Object o) {
    if (sub instanceof CommentType) {
      setComment((String) o);
      return;
    } else if (sub instanceof SmilesStructureType) {
      setSmiles((String) o);
      return;
    } else if (sub instanceof InChIStructureType) {
      setInchi((String) o);
      return;
    } else if (sub instanceof IdentityType) {
      setIdentities((List<FeatureIdentity>) o);
      return;
    } else if (sub instanceof FormulaType) {
      setFormula((String) o);
      return;
    } else if (sub instanceof IonAdductType) {
      setIon((String) o);
      return;
    } else if (sub instanceof CompoundNameType) {
      setCompoundName((String) o);
      return;
    }
    throw new IllegalArgumentException(
        String.format("Subtype %s of class %s is not handled", sub, sub.getClass().getName()));
  }
}
