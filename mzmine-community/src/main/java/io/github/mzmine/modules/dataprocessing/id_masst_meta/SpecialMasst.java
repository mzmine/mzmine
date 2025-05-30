/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_masst_meta;

/**
 * Not used yet but maybe later for direct inclusion of MASST resources
 */
record SpecialMasst(String prefix, String root, String treeFile, String metadataFile,
                    String treeNodeKey, String metadataKey) {

  public static SpecialMasst MICROBE_MASST = new SpecialMasst("microbe", "microbes",
      "../data/microbe_masst_tree.json", "../data/microbe_masst_table.csv", "NCBI", "Taxa_NCBI");
  public static SpecialMasst FOOD_MASST = new SpecialMasst("food", "food",
      "../data/food_masst_tree.json", "../data/food_masst_table.csv", "name", "node_id");
  public static SpecialMasst PLANT_MASST = new SpecialMasst("plant", "plants",
      "../data/plant_masst_tree.json", "../data/plant_masst_table.csv", "NCBI", "Taxa_NCBI");
  public static SpecialMasst TISSUE_MASST = new SpecialMasst("tissue", "tissue",
      "../data/tissue_masst_tree.json", "../data/tissue_masst_table.csv", "ID", "ID");
  public static SpecialMasst PERSONALCAREPRODUCT_MASST = new SpecialMasst("personalCareProduct",
      "personal care product", "../data/personalCareProduct_masst_tree.json",
      "../data/personalCareProduct_masst_table.csv", "name", "node_id");
  public static SpecialMasst MICROBIOME_MASST = new SpecialMasst("microbiome", "microbiome",
      "../data/microbiome_masst_tree.json", "../data/microbiome_masst_table.tsv", "name",
      "node_id");
}
