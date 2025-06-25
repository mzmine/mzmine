/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.submodules;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import java.util.Optional;

public interface ModuleOptionsEnum<MODULE extends MZmineModule> {

  Class<? extends MODULE> getModuleClass();

  /**
   * Stable ID for save and load. Should not change
   *
   * @return a readable ID
   */
  String getStableId();

  /**
   * @param enumClass the enum class
   * @param id        {@link ModuleOptionsEnum#getStableId()}
   * @return an optional of the enum option with matching stable ID
   */
  static <T extends Enum<?> & ModuleOptionsEnum> Optional<T> parse(Class<T> enumClass, String id) {
    T[] enumConstants = enumClass.getEnumConstants();
    for (T e : enumConstants) {
      if (e.getStableId().equalsIgnoreCase(id)) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }


  /**
   * @param id {@link ModuleOptionsEnum#getStableId()}
   * @return an optional of the enum option with matching stable ID
   */
  default Optional<ModuleOptionsEnum> parse(String id) {
    ModuleOptionsEnum[] enumConstants = getClass().getEnumConstants();
    for (ModuleOptionsEnum e : enumConstants) {
      if (e.getStableId().equalsIgnoreCase(id)) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }

  default MODULE getModuleInstance() {
    return MZmineCore.getModuleInstance(getModuleClass());
  }

  /**
   * Usually returns a clone of the module parameters to separate this from different modules using
   * the same parameter
   *
   * @return parameters
   */
  default ParameterSet getModuleParameters() {
    return ConfigService.getConfiguration().getModuleParameters(getModuleClass())
        .cloneParameterSet();
  }

}
