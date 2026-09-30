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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.header;

import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLCVParam;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An element of the mzML header that holds cvParams, userParams, and references to
 * referenceableParamGroups, e.g., sourceFile, sample, software, or an instrument component.
 */
public class MzMLParamContainer {

  private final @NotNull String id;
  private final @NotNull List<MzMLCVParam> cvParams = new ArrayList<>();
  private final @NotNull List<MzMLUserParam> userParams = new ArrayList<>();
  private final @NotNull List<String> paramGroupRefs = new ArrayList<>();

  public MzMLParamContainer(@NotNull final String id) {
    this.id = id;
  }

  /**
   * @return the value of the first userParam with this name (case-insensitive) or null
   */
  public static @Nullable String findUserParamValue(@NotNull final List<MzMLUserParam> params,
      @NotNull final String name) {
    for (final MzMLUserParam param : params) {
      if (param.name().equalsIgnoreCase(name) && param.value() != null) {
        return param.value();
      }
    }
    return null;
  }

  public @NotNull String getId() {
    return id;
  }

  public void addCvParam(@NotNull final MzMLCVParam param) {
    cvParams.add(param);
  }

  public void addUserParam(@NotNull final MzMLUserParam param) {
    userParams.add(param);
  }

  public void addParamGroupRef(@NotNull final String ref) {
    paramGroupRefs.add(ref);
  }

  /**
   * @return the cvParams defined directly in this element, without referenced param groups
   */
  public @NotNull List<MzMLCVParam> getCvParams() {
    return cvParams;
  }

  /**
   * @return the userParams defined directly in this element, without referenced param groups
   */
  public @NotNull List<MzMLUserParam> getUserParams() {
    return userParams;
  }

  public @NotNull List<String> getParamGroupRefs() {
    return paramGroupRefs;
  }
}
