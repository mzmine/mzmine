package io.github.mzmine.modules.dataprocessing.id_masst_meta;

public record SpecialMasst(String prefix, String root, String treeFile, String metadataFile, String treeNodeKey, String metadataKey) {

  public static SpecialMasst MICROBE_MASST = new SpecialMasst(
      "microbe",
      "microbes",
      "../data/microbe_masst_tree.json",
      "../data/microbe_masst_table.csv",
      "NCBI",
      "Taxa_NCBI"
      );
  public static SpecialMasst FOOD_MASST = new SpecialMasst(
      "food",
      "food",
      "../data/food_masst_tree.json",
      "../data/food_masst_table.csv",
      "name",
      "node_id"
      );
  public static SpecialMasst PLANT_MASST = new SpecialMasst(
      "plant",
      "plants",
      "../data/plant_masst_tree.json",
      "../data/plant_masst_table.csv",
      "NCBI",
      "Taxa_NCBI"
      );
  public static SpecialMasst TISSUE_MASST = new SpecialMasst(
      "tissue",
      "tissue",
      "../data/tissue_masst_tree.json",
      "../data/tissue_masst_table.csv",
      "ID",
      "ID"
      );
  public static SpecialMasst PERSONALCAREPRODUCT_MASST = new SpecialMasst(
      "personalCareProduct",
      "personal care product",
      "../data/personalCareProduct_masst_tree.json",
      "../data/personalCareProduct_masst_table.csv",
      "name",
      "node_id"
      );
  public static SpecialMasst MICROBIOME_MASST = new SpecialMasst(
      "microbiome",
      "microbiome",
      "../data/microbiome_masst_tree.json",
      "../data/microbiome_masst_table.tsv",
      "name",
      "node_id"
      );
}
