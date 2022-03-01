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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.StringParser;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_biotransformer.BioTransformerModule;
import java.util.List;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmilesStructureType extends StringType implements EditableColumnType,
    StringParser<String>, AnnotationType {

  private StringConverter<String> converter = new DefaultStringConverter();

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "smiles";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "SMILES";
  }

  @Override
  public String fromString(String s) {
    return s;
  }

  @Override
  public StringConverter<String> getStringConverter() {
    return converter;
  }

  @Override
  public @Nullable Runnable getDoubleClickAction(@NotNull ModularFeatureListRow row,
      @NotNull List<RawDataFile> file, DataType<?> superType, @Nullable final Object value) {
    String compoundName = "";
    if(superType instanceof CompoundDatabaseMatchesType) {
      compoundName = row.getCompoundAnnotations().get(0).getCompundName();
    } else if(superType instanceof SpectralLibraryMatchesType) {
      compoundName = row.getSpectralLibraryMatches().get(0).getName();
    }

    if (value instanceof String smiles) {
      final String finalCompoundName = compoundName;
      return () -> MZmineCore.runLater(() -> BioTransformerModule.runSingleRowPredection(row, smiles,
          finalCompoundName));
    }
    return null;
  }
}
