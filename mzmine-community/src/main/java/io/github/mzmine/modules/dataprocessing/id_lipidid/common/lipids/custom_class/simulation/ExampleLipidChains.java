package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

/**
 * Carbon and double bond numbers of each chain of an example lipid used to preview and simulate a
 * lipid class.
 *
 * @param carbons carbons per chain in the order of the chain types
 * @param dbes    double bond equivalents per chain in the order of the chain types
 */
public record ExampleLipidChains(int @NotNull [] carbons, int @NotNull [] dbes) {

  // common chains: 18:1, 16:0, 18:2, 20:4
  private static final int[] DEFAULT_CARBONS = {18, 16, 18, 20};
  private static final int[] DEFAULT_DBES = {1, 0, 2, 4};

  /**
   * Deterministic examples so the preview does not change between updates.
   *
   * @param chainTypes chains of the lipid class
   * @return common example chains, a sphingoid base is 18:1
   */
  public static @NotNull ExampleLipidChains defaultsFor(@NotNull LipidChainType[] chainTypes) {
    final int[] carbons = new int[chainTypes.length];
    final int[] dbes = new int[chainTypes.length];
    int acylIndex = 0;
    for (int i = 0; i < chainTypes.length; i++) {
      if (chainTypes[i].name().startsWith("SPHINGOLIPID_")) {
        carbons[i] = 18;
        dbes[i] = 1;
      } else {
        carbons[i] = DEFAULT_CARBONS[acylIndex % DEFAULT_CARBONS.length];
        dbes[i] = DEFAULT_DBES[acylIndex % DEFAULT_DBES.length];
        acylIndex++;
      }
    }
    return new ExampleLipidChains(carbons, dbes);
  }

  public int totalCarbons() {
    return Arrays.stream(carbons).sum();
  }

  public int totalDbes() {
    return Arrays.stream(dbes).sum();
  }

  public int size() {
    return carbons.length;
  }
}
