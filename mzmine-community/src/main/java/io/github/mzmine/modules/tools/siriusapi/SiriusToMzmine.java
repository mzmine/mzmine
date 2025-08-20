/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.tools.siriusapi;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ALogPType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.FormulaUtils;
import io.sirius.ms.sdk.model.StructureCandidateFormula;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class SiriusToMzmine {

  public static @Nullable CompoundDBAnnotation toMzmine(
      @Nullable StructureCandidateFormula structure) {
    if (structure == null) {
      return null;
    }

    final NumberFormats formats = ConfigService.getGuiFormats();

    final CompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();

    final IonType ionType = IonTypeParser.parse(structure.getAdduct());
    annotation.put(IonTypeType.class, ionType);
    final IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula(
        structure.getMolecularFormula());
    final double neutralMass = FormulaUtils.getMonoisotopicMass(formula);
    final double mz = ionType.getMZ(neutralMass);

    annotation.putIfNotNull(CompoundNameType.class, structure.getStructureName());
    annotation.putIfNotNull(FormulaType.class, structure.getMolecularFormula());
    annotation.putIfNotNull(SmilesStructureType.class, structure.getSmiles());
    annotation.putIfNotNull(InChIKeyStructureType.class, structure.getInchiKey());
    annotation.putIfNotNull(PrecursorMZType.class, mz);
    annotation.putIfNotNull(NeutralMassType.class, neutralMass);
    annotation.put(CommentType.class,
        "Imported from Sirius. CSI score: %s".formatted(formats.score(structure.getCsiScore())));
//    annotation.putIfNotNull(DatabaseNameType.class,
//        structure.getDbLinks().isEmpty() ? null : structure.getDbLinks().getFirst().getName());
    annotation.putIfNotNull(ALogPType.class, structure.getXlogP().floatValue());


    return annotation;
  }
}
