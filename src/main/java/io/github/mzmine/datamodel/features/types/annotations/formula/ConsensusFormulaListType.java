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

package io.github.mzmine.datamodel.features.types.annotations.formula;

import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import org.jetbrains.annotations.NotNull;

/**
 * This annotation type stores a list of formulas that were predicted as consensus formulas for
 * multiple feature list rows (e.g., in Ion Identity Networking, see {@link
 * IonNetwork#getMolFormulas()}) or for multiple different methods
 */
public class ConsensusFormulaListType extends ListDataType<ResultFormula>
    implements AnnotationType, EditableColumnType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "consensus_formulas";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Consensus formula";
  }

}
