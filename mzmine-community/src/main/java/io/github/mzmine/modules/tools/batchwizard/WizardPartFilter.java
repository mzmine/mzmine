package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * A filter to restrict wizard workflow selections.
 */
public interface WizardPartFilter {

  /**
   * @param allowed The allowed steps
   */
  static WizardPartFilter allow(Collection<WizardParameterFactory> allowed) {
    final HashSet<WizardParameterFactory> set = new HashSet<>(allowed);
    return set::contains;
  }

  /**
   * @param allowed The allowed steps
   */
  static WizardPartFilter allow(WizardParameterFactory... allowed) {
    return allow(Arrays.asList(allowed));
  }

  /**
   * @param denied The denied steps
   */
  static WizardPartFilter deny(Collection<WizardParameterFactory> denied) {
    final HashSet<WizardParameterFactory> set = new HashSet<>(denied);
    return part -> !set.contains(part);
  }

  /**
   * @param denied The denied steps
   */
  static WizardPartFilter deny(WizardParameterFactory... denied) {
    return deny(Arrays.stream(denied).collect(Collectors.toSet()));
  }

  static WizardPartFilter combine(WizardPartFilter... filters) {
    return part -> Arrays.stream(filters).allMatch(filter -> filter.accept(part));
  }

  /**
   * @param part The wizard parameter factory to check if it is allowed.
   * @return true or false
   */
  boolean accept(@NotNull WizardParameterFactory part);
}
