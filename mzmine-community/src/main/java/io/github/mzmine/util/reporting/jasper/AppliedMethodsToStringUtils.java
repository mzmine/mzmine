/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.reporting.jasper;

import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import io.github.mzmine.parameters.parametertypes.FontParameter;
import io.github.mzmine.parameters.parametertypes.FontSpecs;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MobilityRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class AppliedMethodsToStringUtils {

  static String parameterToString(Parameter<?> parameter, @Nullable String prefix) {
    String name = parameter.getName();
    StringBuilder sb = new StringBuilder();
    if (prefix != null) {
      sb.append(prefix);
    }
    sb.append(name);
    sb.append(":\t");

    final NumberFormats formats = ConfigService.getGuiFormats();

    sb = switch (parameter) {
      // intentional to name a single file
      case FileNameParameter fnp -> appendParameterValue(fnp.getValue(), sb);
      case FileNamesParameter fnp -> appendParameterValue(fnp.getValue().length + " files", sb);
      case RawDataFilesParameter rfp ->
          appendParameterValue(rfp.getValue().getEvaluationResult().length + " MS data files", sb);
      case FeatureListsParameter flp ->
          appendParameterValue(flp.getValue().getEvaluatedSelection().length + " feature lists",
              sb);
      case ModuleOptionsEnumComboParameter<?> moe -> {
        // no summary for most embedded parameter sets, but here we need the selected value.
        sb = appendParameterValue(moe.getValue().toString(), sb);
        yield appedEmbeddedParameters(prefix, moe, sb);
      }
      case EmbeddedParameterSet<?, ?> eps -> {
        if (eps instanceof OptionalModuleParameter<?> omp) {
          sb = appendParameterValue("(" + omp.getValue() + ")", sb);
          if (!omp.getValue()) { // no need to print embedded parameters
            yield sb;
          }
        }
        sb = appedEmbeddedParameters(prefix, eps, sb);
        yield sb;
      }
      case OptionalParameter<?> op -> {
        sb = appendParameterValue(op.getValue(), sb);
        if (op.getValue()) {
          // only print if selected
          yield appendParameterValue(", " + op.getEmbeddedParameter().getValue(), sb);
        }
        yield sb;
      }
      case RTRangeParameter rtrp -> appendParameterValue(formats.rt(rtrp.getValue()), sb);
      case MZRangeParameter mzrp -> appendParameterValue(formats.mz(mzrp.getValue()), sb);
      case MobilityRangeParameter mrp -> appendParameterValue(formats.mobility(mrp.getValue()), sb);
      case PercentParameter pp -> appendParameterValue(formats.percent(pp.getValue()), sb);
      case RTToleranceParameter rtp -> appendParameterValue(
          formats.rt(rtp.getValue().getTolerance()) + " " + rtp.getValue().getUnit(), sb);
      case FontParameter fp -> {
        FontSpecs value = fp.getValue();
        if (value == null) {
          yield appendParameterValue(null, sb);
        }
        final String fontName = value.getFont().getName();
        final double size = value.getFont().getSize();
        final String style = value.getFont().getStyle();
        yield appendParameterValue(fontName + ", " + style + ", " + ((int) size), sb);
      }
      default -> appendParameterValue(parameter.getValue(), sb);
    };

    return sb.toString();
  }

  private static StringBuilder appedEmbeddedParameters(@Nullable String prefix,
      @NotNull EmbeddedParameterSet<?, ?> eps, @NotNull StringBuilder sb) {
    ParameterSet parameterSet = eps.getEmbeddedParameters();
    for (Parameter<?> parameter1 : parameterSet.getParameters()) {
      sb.append("\n\t");
      sb.append(parameterToString(parameter1, prefix != null ? prefix + "\t" : "\t"));
    }
    return sb;
  }

  @NotNull
  private static StringBuilder appendParameterValue(@Nullable Object value,
      @NotNull StringBuilder sb) {
    if (value == null) {
      return sb.append("<not set>");
    } else if (value.getClass().isArray()) {
      return sb.append(Arrays.toString((Object[]) value));
    } else {
      return sb.append(value.toString());
    }
  }
}
