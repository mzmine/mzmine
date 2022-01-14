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

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.ImportTypeParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class LocalCSVDatabaseSearchParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final FileNameParameter dataBaseFile = new FileNameParameter("Database file",
      "Name of file that contains information for peak identification", FileSelectionType.OPEN);

  public static final StringParameter fieldSeparator = new StringParameter("Field separator",
      "Character(s) used to separate fields in the database file", ",");
  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();
  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();
  public static final MobilityToleranceParameter mobTolerance = new MobilityToleranceParameter(
      new MobilityTolerance(0.01f));
  public static final PercentParameter ccsTolerance = new PercentParameter("CCS tolerance (%)",
      "Maximum allowed difference (in per cent) for two ccs values.", 0.05);
  public static final OptionalModuleParameter<IonLibraryParameterSet> ionLibrary = new OptionalModuleParameter<>(
      "Use adducts",
      "If enabled, m/z values for multiple adducts will be calculated and matched against the feature list.",
      (IonLibraryParameterSet) new IonLibraryParameterSet().cloneParameterSet());
  private static final List<ImportType> importTypes = List.of(
      new ImportType(true, "neutral mass", new NeutralMassType()),
      new ImportType(true, "mz", new PrecursorMZType()), //
      new ImportType(true, "rt", new RTType()), new ImportType(true, "formula", new FormulaType()),
      new ImportType(true, "smiles", new SmilesStructureType()),
      new ImportType(false, "name", new CompoundNameType()),
      new ImportType(false, "CCS", new CCSType()),
      new ImportType(false, "mobility", new MobilityType()),
      new ImportType(true, "comment", new CommentType()),
      new ImportType(false, "adduct", new IonAdductType()),
      new ImportType(false, "PubChemCID", new PubChemIdType()));
  public static final ImportTypeParameter columns = new ImportTypeParameter("Columns",
      "Select the columns you want to import from the library file.", importTypes);

  public LocalCSVDatabaseSearchParameters() {
    super(
        new Parameter[]{peakLists, dataBaseFile, fieldSeparator, columns, mzTolerance, rtTolerance,
            mobTolerance, ccsTolerance, ionLibrary});
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    final boolean superCheck = super.checkParameterValues(errorMessages);

    final List<ImportType> selectedTypes = getParameter(columns).getValue().stream()
        .filter(ImportType::isSelected).toList();

    boolean compoundNameSelected = true;
    if (!importTypeListContainsType(selectedTypes, new CompoundNameType())) {
      compoundNameSelected = false;
      errorMessages.add(new CompoundNameType().getHeaderString() + " must be selected.");
    }

    boolean canDetermineMz = false;
    if (importTypeListContainsType(selectedTypes, new NeutralMassType()) && getValue(ionLibrary)) {
      canDetermineMz = true;
    } else if (importTypeListContainsType(selectedTypes, new PrecursorMZType())) {
      canDetermineMz = true;
    } else if (importTypeListContainsType(selectedTypes, new FormulaType()) && getValue(
        ionLibrary)) {
      canDetermineMz = true;
    } else if (importTypeListContainsType(selectedTypes, new SmilesStructureType()) && getValue(
        ionLibrary)) {
      canDetermineMz = true;
    }

    if (!canDetermineMz) {
      errorMessages.add("Cannot determine precursor mz with currently selected data types.");
    }

    return superCheck && compoundNameSelected && canDetermineMz;
  }

  private boolean importTypeListContainsType(List<ImportType> importTypes, DataType<?> type) {
    return importTypes.stream().anyMatch(importType -> importType.getDataType().equals(type));
  }
}
