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

package io.github.mzmine.datamodel.features.compoundannotations;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.CompoundAnnotationScoreType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.IonTypeType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public interface CompoundDBAnnotation extends Cloneable {

  Logger logger = Logger.getLogger(CompoundDBAnnotation.class.getName());

  String XML_ELEMENT = "compound_db_annotation";
  String XML_TYPE_ATTRIBUTE = "annotationtype";
  String XML_NUM_ENTRIES_ATTR = "entries";

  static List<CompoundDBAnnotation> buildCompoundsWithAddcuts(
      CompoundDBAnnotation neutralAnnotation, IonNetworkLibrary library) {
    final List<CompoundDBAnnotation> annotations = new ArrayList<>();
    for (IonType adduct : library.getAllAdducts()) {
      CompoundDBAnnotation clone = neutralAnnotation.clone();
      try {
        final double mz = calcMz(clone, adduct);
        clone.put(PrecursorMZType.class, mz);
        clone.put(IonTypeType.class, adduct);
        annotations.add(clone);
      } catch (IllegalStateException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
      }
    }

    return annotations;
  }

  static double calcMz(CompoundDBAnnotation annotation, IonType adduct)
      throws IllegalStateException {
    final Double neutralMass = annotation.get(NeutralMassType.class);
    if (neutralMass != null) {
      return adduct.getMZ(neutralMass);
    }

    final IonType currentAdduct = annotation.get(IonTypeType.class);
    if (currentAdduct != null && annotation.getExactMass() != null) {
      final double neutral = currentAdduct.getMass(annotation.getExactMass());
      return adduct.getMZ(neutral);
    }

    final String formula = annotation.getFormula();
    if (formula != null) {
      final IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMolecularFormula(
          formula, SilentChemObjectBuilder.getInstance());
      final Integer charge = molecularFormula.getCharge();
      if (charge != null && charge != 0) {
        logger.info(() -> "Compound " + annotation + " is not neutral as determined by molFormula "
            + MolecularFormulaManipulator.getString(molecularFormula) + ". charge = " + charge
            + ". Adjusting protonation.");
        MolecularFormulaManipulator.adjustProtonation(molecularFormula, -charge);
      }
      final double mass = MolecularFormulaManipulator.getMass(molecularFormula,
          MolecularFormulaManipulator.MonoIsotopic);
      return adduct.getMZ(mass);
    }

    final String smiles = annotation.getSmiles();
    if (smiles != null) {
      try {
        final IAtomContainer iAtomContainer = new SmilesParser(
            SilentChemObjectBuilder.getInstance()).parseSmiles(smiles);
        IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMolecularFormula(
            iAtomContainer);
        final Integer charge = molecularFormula.getCharge();
        if (charge != null && charge != 0) {
          logger.info(() -> "Compound " + annotation + " is not neutral as determined by smiles "
              + MolecularFormulaManipulator.getString(molecularFormula) + ". charge = " + charge
              + ". Adjusting protonation.");
          MolecularFormulaManipulator.adjustProtonation(molecularFormula, -charge);
        }
        final double mass = MolecularFormulaManipulator.getMass(molecularFormula,
            MolecularFormulaManipulator.MonoIsotopic);
        return adduct.getMZ(mass);
      } catch (InvalidSmilesException e) {
        logger.log(Level.SEVERE, e.getMessage(), e);
        logger.severe(() -> "Invalid smiles string for compound " + annotation);
      }
    }

    throw new IllegalStateException(
        "Cannot determine mz of " + annotation + " with adduct " + adduct
            + ". Neither neutral mass, formula, nor smiles is specified.");
  }

  <T> T get(@NotNull DataType<T> key);

  <T> T get(Class<? extends DataType<T>> key);

  <T> T put(@NotNull DataType<T> key, T value);

  <T> T put(@NotNull Class<? extends DataType<T>> key, T value);

  void saveToXML(@NotNull XMLStreamWriter writer, ModularFeatureList flist,
      ModularFeatureListRow row) throws XMLStreamException;

  static CompoundDBAnnotation loadFromXML(@NotNull final XMLStreamReader reader,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row)
      throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Invalid xml element to load CompoundDBAnnotation from.");
    }

    return switch (reader.getAttributeValue(null, XML_TYPE_ATTRIBUTE)) {
      case SimpleCompoundDBAnnotation.XML_TYPE_NAME -> SimpleCompoundDBAnnotation.loadFromXML(
          reader, flist, row);
//      case BioTransformerAnnotationImpl.XML_TYPE_NAME -> BioTransformerAnnotationImpl.loadFromXML(
//          reader, flist, row);
      default -> null;
    };
  }

  public default Double getExactMass() {
    return get(PrecursorMZType.class);
  }

  public default String getSmiles() {
    return get(SmilesStructureType.class);
  }

  public default String getCompundName() {
    return get(CompoundNameType.class);
  }

  public default String getFormula() {
    return get(FormulaType.class);
  }

  public default IonType getAdductType() {
    return get(IonTypeType.class);
  }

  public default Float getMobility() {
    return get(MobilityType.class);
  }

  public default Float getCCS() {
    return get(CCSType.class);
  }

  public default Float getRT() {
    return get(RTType.class);
  }

  public default String getDatabase() {
    return get(DatabaseNameType.class);
  }

  public default Float getScore() {
    return get(CompoundAnnotationScoreType.class);
  }

  public boolean matches(FeatureListRow row, @Nullable MZTolerance mzTolerance,
      @Nullable RTTolerance rtTolerance, @Nullable MobilityTolerance mobilityTolerance,
      @Nullable Double percentCCSTolerance);

  public Float getScore(FeatureListRow row, @Nullable MZTolerance mzTolerance,
      @Nullable RTTolerance rtTolerance, @Nullable MobilityTolerance mobilityTolerance,
      @Nullable Double percentCCSTolerance);

  CompoundDBAnnotation clone();
}
