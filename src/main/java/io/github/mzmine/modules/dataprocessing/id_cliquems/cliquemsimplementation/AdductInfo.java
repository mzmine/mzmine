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

package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import java.util.List;
import org.jetbrains.annotations.Unmodifiable;

/**
 * This class contains information related to isotope, namely adduct annotations, mass value and scores
 */
@Unmodifiable
public class AdductInfo {

  public Integer feature;
  public List<String> annotations;
  public List<Double> masses;
  public List<Double> scores;

  public AdductInfo(Integer feature, List<String> annotations, List<Double> masses, List<Double> scores){
    this.feature = feature;
    this.annotations = annotations;
    this.masses = masses;
    this.scores = scores;
  }
}
