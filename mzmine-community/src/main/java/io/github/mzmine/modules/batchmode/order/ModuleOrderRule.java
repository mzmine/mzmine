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

package io.github.mzmine.modules.batchmode.order;

import io.github.mzmine.modules.MZmineProcessingModule;
import org.jetbrains.annotations.NotNull;

/**
 * One ordering requirement for a processing module.
 */
public sealed interface ModuleOrderRule permits RelativeModuleOrderRule {

  default @NotNull String description(@NotNull final String selfName) {
    return ModuleOrderTextFormatter.describeRule(this, selfName);
  }

  public static @NotNull ModuleOrderRule mustRunBefore(
      @NotNull final Class<? extends MZmineProcessingModule> anchorModule) {
    return mustRunBefore(new ModuleClassOrderCondition(anchorModule));
  }

  public static @NotNull ModuleOrderRule mustRunBefore(
      @NotNull final ModuleOrderCondition anchorCondition) {
    return new RelativeModuleOrderRule(anchorCondition, ModuleOrderPosition.BEFORE,
        ModuleOrderAnchorRequirement.REQUIRED, ModuleOrderLevel.MUST);
  }

  public static @NotNull ModuleOrderRule mustRunAfter(
      @NotNull final Class<? extends MZmineProcessingModule> anchorModule) {
    return mustRunAfter(new ModuleClassOrderCondition(anchorModule));
  }

  public static @NotNull ModuleOrderRule mustRunAfter(
      @NotNull final ModuleOrderCondition anchorCondition) {
    return new RelativeModuleOrderRule(anchorCondition, ModuleOrderPosition.AFTER,
        ModuleOrderAnchorRequirement.REQUIRED, ModuleOrderLevel.MUST);
  }

  public static @NotNull ModuleOrderRule shouldRunBefore(
      @NotNull final Class<? extends MZmineProcessingModule> anchorModule) {
    return shouldRunBefore(new ModuleClassOrderCondition(anchorModule));
  }

  public static @NotNull ModuleOrderRule shouldRunBefore(
      @NotNull final ModuleOrderCondition anchorCondition) {
    return new RelativeModuleOrderRule(anchorCondition, ModuleOrderPosition.BEFORE,
        ModuleOrderAnchorRequirement.REQUIRED, ModuleOrderLevel.SHOULD);
  }

  public static @NotNull ModuleOrderRule shouldRunAfter(
      @NotNull final Class<? extends MZmineProcessingModule> anchorModule) {
    return shouldRunAfter(new ModuleClassOrderCondition(anchorModule));
  }

  public static @NotNull ModuleOrderRule shouldRunAfter(
      @NotNull final ModuleOrderCondition anchorCondition) {
    return new RelativeModuleOrderRule(anchorCondition, ModuleOrderPosition.AFTER,
        ModuleOrderAnchorRequirement.REQUIRED, ModuleOrderLevel.SHOULD);
  }

  public static @NotNull ModuleOrderRule ifPresentMustRunBefore(
      @NotNull final Class<? extends MZmineProcessingModule> anchorModule) {
    return ifPresentMustRunBefore(new ModuleClassOrderCondition(anchorModule));
  }

  public static @NotNull ModuleOrderRule ifPresentMustRunBefore(
      @NotNull final ModuleOrderCondition anchorCondition) {
    return new RelativeModuleOrderRule(anchorCondition, ModuleOrderPosition.BEFORE,
        ModuleOrderAnchorRequirement.IF_PRESENT, ModuleOrderLevel.MUST);
  }

  public static @NotNull ModuleOrderRule ifPresentMustRunAfter(
      @NotNull final Class<? extends MZmineProcessingModule> anchorModule) {
    return ifPresentMustRunAfter(new ModuleClassOrderCondition(anchorModule));
  }

  public static @NotNull ModuleOrderRule ifPresentMustRunAfter(
      @NotNull final ModuleOrderCondition anchorCondition) {
    return new RelativeModuleOrderRule(anchorCondition, ModuleOrderPosition.AFTER,
        ModuleOrderAnchorRequirement.IF_PRESENT, ModuleOrderLevel.MUST);
  }

  public static @NotNull ModuleOrderRule ifPresentShouldRunBefore(
      @NotNull final Class<? extends MZmineProcessingModule> anchorModule) {
    return ifPresentShouldRunBefore(new ModuleClassOrderCondition(anchorModule));
  }

  public static @NotNull ModuleOrderRule ifPresentShouldRunBefore(
      @NotNull final ModuleOrderCondition anchorCondition) {
    return new RelativeModuleOrderRule(anchorCondition, ModuleOrderPosition.BEFORE,
        ModuleOrderAnchorRequirement.IF_PRESENT, ModuleOrderLevel.SHOULD);
  }

  public static @NotNull ModuleOrderRule ifPresentShouldRunAfter(
      @NotNull final Class<? extends MZmineProcessingModule> anchorModule) {
    return ifPresentShouldRunAfter(new ModuleClassOrderCondition(anchorModule));
  }

  public static @NotNull ModuleOrderRule ifPresentShouldRunAfter(
      @NotNull final ModuleOrderCondition anchorCondition) {
    return new RelativeModuleOrderRule(anchorCondition, ModuleOrderPosition.AFTER,
        ModuleOrderAnchorRequirement.IF_PRESENT, ModuleOrderLevel.SHOULD);
  }
}
