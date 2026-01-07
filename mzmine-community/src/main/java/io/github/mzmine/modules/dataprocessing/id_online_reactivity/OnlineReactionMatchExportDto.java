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

package io.github.mzmine.modules.dataprocessing.id_online_reactivity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReaction.Type;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.GnpsFbmnExportAndSubmitTask;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;


/**
 * Used to export matches in SIRIUS and GNPS mgf: {@link SiriusExportTask}
 * {@link GnpsFbmnExportAndSubmitTask}
 *
 * @param reaction  the reaction
 * @param linkedIds all linked {@link FeatureListRow} IDs
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
record OnlineReactionMatchExportDto(Type compoundType,
                                           @JsonUnwrapped OnlineReaction reaction,
                                           int[] linkedIds) {

}
