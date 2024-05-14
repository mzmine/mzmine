/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package modules;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.impl.PasefMsMsInfoImpl;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.modules.dataprocessing.filter_maldigroupms2.MaldiGroupMS2Task;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestGroupMaldiMs2 {

  private static Logger logger = Logger.getLogger(TestGroupMaldiMs2.class.getName());

  @Test
  void test() {
    PasefMsMsInfo info1 = new PasefMsMsInfoImpl(500, null, null, null, null, null, null);
    PasefMsMsInfo info2 = new PasefMsMsInfoImpl(501, null, null, null, null, null, null);
    PasefMsMsInfo info3 = new PasefMsMsInfoImpl(502, null, null, null, null, null, null);
    PasefMsMsInfo info4 = new PasefMsMsInfoImpl(503, null, null, null, null, null, null);
    PasefMsMsInfo info5 = new PasefMsMsInfoImpl(504, null, null, null, null, null, null);
    PasefMsMsInfo info6 = new PasefMsMsInfoImpl(505, null, null, null, null, null, null);

    final List<PasefMsMsInfo> infos = List.of(info1, info2, info3, info4, info5, info6);

    final List<PasefMsMsInfo> msMsInfos = MaldiGroupMS2Task.getMsMsInfos(infos,
        Range.closed(501.5, 503.5));

    Assertions.assertEquals(List.of(info3, info4), msMsInfos);
  }
}
