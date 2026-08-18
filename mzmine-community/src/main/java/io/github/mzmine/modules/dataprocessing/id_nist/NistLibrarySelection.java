/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 * SPDX-License-Identifier: MIT
 */
package io.github.mzmine.modules.dataprocessing.id_nist;

public enum NistLibrarySelection {
  MAIN("Main EI library", true, false),
  REPLICATE("Replicate EI library", false, true),
  MAIN_AND_REPLICATE("Main + replicate EI libraries", true, true);

  private final String label;
  private final boolean main;
  private final boolean replicate;

  NistLibrarySelection(String label, boolean main, boolean replicate) {
    this.label = label;
    this.main = main;
    this.replicate = replicate;
  }

  public boolean usesMain() {
    return main;
  }

  public boolean usesReplicate() {
    return replicate;
  }

  @Override
  public String toString() {
    return label;
  }
}
