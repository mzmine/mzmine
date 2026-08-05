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

package io.github.mzmine.datamodel.features.compoundlist;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.mzmine.datamodel.features.FeatureListRowID;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMembersType;
import org.jetbrains.annotations.NotNull;

/**
 * JSON DTO for a single compound member: a flat row reference, its role inside the compound, and
 * the member score. Used by {@link CompoundMembersType#getFormattedString} for export.
 * <p>
 * {@code row} is unwrapped, so the {@link FeatureListRowID} components ({@code mode}, {@code id})
 * are emitted directly next to {@code role} and {@code score}.
 */
record CompoundMemberDTO(@JsonUnwrapped @NotNull FeatureListRowID row
// for now do not export role and score. Only once we can explain them better as this might confuse users
//    , @NotNull CompoundMemberRole role, float score
) {

}
