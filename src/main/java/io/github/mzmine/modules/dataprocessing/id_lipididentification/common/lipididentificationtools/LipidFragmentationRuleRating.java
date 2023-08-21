package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools;

public enum LipidFragmentationRuleRating {
  MINOR("minor", "Fragmentation rule alone is not enough to identify a lipid"),//
  MAJOR("major", "Fragmentation rule can identify lipid");//

  private final String name;
  private final String description;

  LipidFragmentationRuleRating(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }
}

