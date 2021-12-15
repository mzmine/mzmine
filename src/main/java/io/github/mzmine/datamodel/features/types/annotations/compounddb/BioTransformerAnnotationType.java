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

package io.github.mzmine.datamodel.features.types.annotations.compounddb;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.modules.dataprocessing.id_biotransformer.BioTransformerAnnotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public class BioTransformerAnnotationType extends ListWithSubsType<BioTransformerAnnotation> {

  @Override
  public @NotNull String getUniqueID() {
    return "biotransformer_annotations";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Bio transformer";
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return new ArrayList<>(
        List.of(new InChIStructureType(), new SmilesStructureType(), new FormulaType(),
            new ALogPType(), new ReactionType(), new EnzymeType()));
  }

  @Override
  protected Map<Class<? extends DataType>, Function<BioTransformerAnnotation, Object>> getMapper() {
    return null;
  }
}
