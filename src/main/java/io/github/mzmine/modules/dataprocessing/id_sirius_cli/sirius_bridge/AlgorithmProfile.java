package io.github.mzmine.modules.dataprocessing.id_sirius_cli.sirius_bridge;

public enum AlgorithmProfile {
  QTOF("qtof"), ORBITRAP("orbitrap"), FTICR("orbitrap");

  private final String str;

  private AlgorithmProfile(String str) {

    this.str = str;
  }

  public String commandName() {
    return str;
  }
}
