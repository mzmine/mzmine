/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
