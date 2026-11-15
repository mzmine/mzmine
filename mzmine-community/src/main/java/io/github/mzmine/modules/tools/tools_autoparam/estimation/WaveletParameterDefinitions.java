/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.tools_autoparam.estimation;

import io.github.mzmine.modules.tools.batchwizard.subparameters.ApplicationScope;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ParameterOverride;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.ChoiceSearchDomain;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.DoubleSearchDomain;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.SearchScale;
import io.github.mzmine.parameters.UserParameter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Optional module reflection is confined to the typed batch binding boundary.
 */
final class WaveletParameterDefinitions {

  private static final String MODULE = "io.mzio.mzminepro.modules.featdet_resolving.wavelet.WaveletResolverModule";
  private static final String PARAMETERS = "io.mzio.mzminepro.modules.featdet_resolving.wavelet.WaveletResolverParameters";
  private static final Set<WizardParameterFactory> PRESETS = Set.of(
      /*IonInterfaceWizardParameterFactory.LC_WAVELET*/);

  private WaveletParameterDefinitions() {
  }

  static @NotNull List<ParameterDefinition<?>> definitions() {
    return List.of(new BatchParameterDefinition<Double>("Wavelet SNR threshold", MODULE, "snr",
            ApplicationScope.FIRST, OptimizationRole.OPTIONAL_SELECTION, PRESETS,
            value -> override("snr", value),
            _ -> new ParameterEstimate<>(4d, ValueOrigin.HEURISTIC,
                new DoubleSearchDomain(3d, 10d, SearchScale.LINEAR),
                ParameterEstimators.FIXED_DEFAULT)),
        enumDefinition("Wavelet noise calculation", "noiseCalculation"),
        enumDefinition("Wavelet baseline method", "baselineMethod"));
  }

  private static @NotNull BatchParameterDefinition<Enum<?>> enumDefinition(@NotNull String name,
      @NotNull String field) {
    return new BatchParameterDefinition<>(name, MODULE, field, ApplicationScope.FIRST,
        OptimizationRole.OPTIONAL_SELECTION, PRESETS, value -> override(field, value), _ -> {
      final Object value = parameter(field).getValue();
      if (!(value instanceof Enum<?> initial)) {
        throw new IllegalStateException("Expected an enum parameter: " + field);
      }
      final List<Enum<?>> choices = Arrays.stream(initial.getDeclaringClass().getEnumConstants())
          .<Enum<?>>map(constant -> constant).toList();
      return new ParameterEstimate<>(initial, ValueOrigin.PRESET_DEFAULT,
          new ChoiceSearchDomain<>(choices), "module default");
    });
  }

  private static @NotNull UserParameter<?, ?> parameter(@NotNull String field) {
    try {
      return (UserParameter<?, ?>) Class.forName(PARAMETERS).getField(field).get(null);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Cannot access optional wavelet parameter " + field, e);
    }
  }

  private static @NotNull ParameterOverride override(@NotNull String field, @NotNull Object value) {
    return override(parameter(field), value);
  }

  private static <T> @NotNull ParameterOverride override(@NotNull UserParameter<T, ?> parameter,
      @NotNull Object value) {
    final Object defaultValue = parameter.getValue();
    if (defaultValue == null || !defaultValue.getClass().isInstance(value)) {
      throw new IllegalArgumentException("Invalid value type for " + parameter.getName());
    }
    // assumption: the optional module's parameter accepts its declared default value type.
    @SuppressWarnings("unchecked") final T typedValue = (T) value;
    return new ParameterOverride(MODULE, "WaveletResolverModule", parameter, typedValue,
        ApplicationScope.FIRST);
  }
}
