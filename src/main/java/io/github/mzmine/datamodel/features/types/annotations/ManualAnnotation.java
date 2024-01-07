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

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  public ManualAnnotation(List<FeatureIdentity> identities, String comment, String compoundName,
      String ion, String formula, String smiles, String inchi) {
    this.identities = identities;
    this.comment = comment;
    this.compoundName = compoundName;
    this.ion = ion;
    this.formula = formula;
    this.smiles = smiles;
    this.inchi = inchi;
  }

  @Override
  public String toString() {
    return Stream.of(compoundName, ion, formula, smiles, inchi).filter(Objects::nonNull)
        .collect(Collectors.joining(": "));
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
