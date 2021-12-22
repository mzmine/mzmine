/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.features.types.numbers.scores;

import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreCalculator;
import org.jetbrains.annotations.NotNull;

/**
 * The MS/MS score is used during molecular formula prediction to score how many signals are
 * described by the molecular formula candidate. The score is calculated in {@link
 * MSMSScoreCalculator#evaluateMSMS}
 */
public class MsMsScoreType extends ScoreType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "msms_score";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "MS/MS score";
  }

}
