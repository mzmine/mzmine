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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class holds annotation related data like list of features in a cliques and the corresponding
 * NUM_ANNO (default value 5) no. of annotations, annotatted masses, annotation scores for
 */
public class OutputAn {

  private int NUM_ANNO = ComputeAdduct.numofAnnotation;

  public List<Integer> features;
  public List<HashMap<Integer, String>> ans = new ArrayList<>();
  public List<HashMap<Integer, Double>> masses = new ArrayList<>();
  public List<HashMap<Integer, Double>> scores = new ArrayList<>();

  public OutputAn(List<Integer> features) {
    this.features = features;

    for (int i = 0; i < NUM_ANNO; i++) {
      HashMap<Integer, String> tempAn = new HashMap<>();
      ans.add(tempAn);
      HashMap<Integer, Double> tempmass = new HashMap<>();
      masses.add(tempmass);
      HashMap<Integer, Double> tempScore = new HashMap<>();
      scores.add(tempScore);
    }

    for (Integer itv : features) {
      for (int i = 0; i < NUM_ANNO; i++) {
        this.ans.get(i).put(itv, "NA");
        this.masses.get(i).put(itv, 0.0);
        this.scores.get(i).put(itv, 0.0);
      }
    }
  }

}
