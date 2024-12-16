package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public interface WizardPartFilter {

  boolean accept(@NotNull WizardParameterFactory part);

  /**
   *
   * @param allowed The allowed steps
   */
  static WizardPartFilter allow(Collection<WizardParameterFactory> allowed) {
    return part -> new HashSet<>(allowed).contains(part);
  }

  /**
   *
   * @param allowed The allowed steps
   */
  static WizardPartFilter allow(WizardParameterFactory... allowed) {
    return allow(Arrays.asList(allowed));
  }

  /**
   *
   * @param denied The denied steps
   */
  static WizardPartFilter deny(Collection<WizardParameterFactory> denied) {
    return part -> !new HashSet<>(denied).contains(part);
  }

  /**
   *
   * @param denied The denied steps
   */
  static WizardPartFilter deny(WizardParameterFactory... denied) {
    return deny(Arrays.stream(denied).collect(Collectors.toSet()));
  }
}
