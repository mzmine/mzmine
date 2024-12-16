package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public interface WizardPartFilter {

  boolean accept(@NotNull WizardParameterFactory part);

  static WizardPartFilter allow(Collection<WizardParameterFactory> allowed) {
    return part -> new HashSet<>(allowed).contains(part);
  }

  static WizardPartFilter allow(WizardParameterFactory... allowed) {
    return allow(Arrays.asList(allowed));
  }

  static WizardPartFilter deny(Collection<WizardParameterFactory> deny) {
    return part -> !new HashSet<>(deny).contains(part);
  }

  static WizardPartFilter deny(WizardParameterFactory... deny) {
    return deny(Arrays.stream(deny).collect(Collectors.toSet()));
  }

  static WizardPartFilter deny(@NotNull WizardParameterFactory selection,
      Collection<WizardParameterFactory> denied) {
    return part -> selection.equals(part) && deny(denied).accept(part);
  }

  static WizardPartFilter deny(@NotNull WizardParameterFactory selection,
      WizardParameterFactory... denied) {
    return deny(selection, Arrays.asList(denied));
  }
}
