/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public interface BioTransformerAnnotation extends CompoundDBAnnotation {

  Logger logger = Logger.getLogger(BioTransformerAnnotation.class.getName());

  static final int FORMULA_INDEX = 5;

  static final int SMILES_INDEX = 2;

  static final int INCHI_INDEX = 1;

  static final int REACTION_INDEX = 13;

  static final int ENZYME_INDEX = 11;

  static final int ALOGP_INDEX = 7;

  public static BioTransformerAnnotation fromCsvLine(String[] lineValues, IonType ionType) {

    final double mz = 0;
    final String smiles = lineValues[SMILES_INDEX];

    SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
    try {
      final IAtomContainer iAtomContainer = parser.parseSmiles(smiles);
      final IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMolecularFormula(
          iAtomContainer);
      final Integer charge = molecularFormula.getCharge();

      // neutralise the molecular formula
      if (charge != null && charge != 0) {
        logger.finest(() ->"Formula not neutral, adjusting with " + (charge * -1) + " protons.");
        MolecularFormulaManipulator.adjustProtonation(molecularFormula, -charge);
      }

      final String neutralFormula = MolecularFormulaManipulator.getString(molecularFormula);

      ionType.addToFormula(molecularFormula);

      return new BioTransformerAnnotationImpl(neutralFormula, mz, ionType,
          smiles, lineValues[INCHI_INDEX], lineValues[REACTION_INDEX],
          lineValues[REACTION_INDEX], Float.valueOf(lineValues[ALOGP_INDEX]));
    } catch (InvalidSmilesException | CloneNotSupportedException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      return null;
    }
  }
}
