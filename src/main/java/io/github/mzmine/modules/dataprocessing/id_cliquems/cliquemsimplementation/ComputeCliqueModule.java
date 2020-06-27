package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

public class ComputeCliqueModule {

  AnClique anClique;

  ComputeCliqueModule(AnClique anClique){
    this.anClique = anClique;
  }

  public AnClique getClique(){


    return anClique;
  }

  private void createNetwork(){

  }

}
