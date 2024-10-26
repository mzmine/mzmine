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

package io.github.mzmine.modules.io.download;

import io.github.mzmine.util.web.HttpResponseException;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ZenodoServiceTest {

  @Test
  void testRead() throws IOException {
    var file = getClass().getClassLoader().getResource("io/zenodo/zenodo_record.json").getFile();
    var record = ZenodoService.getJsonMapper().readValue(new File(file), ZenodoRecord.class);

    Assertions.assertNotNull(record);
    Assertions.assertEquals("MSnLib Mass spectral libraries (.mgf and .json)", record.title());
    Assertions.assertEquals(4, record.revision());
    Assertions.assertEquals("13898009", record.recid());
    Assertions.assertEquals("cc-by-4.0", record.metadata().license().id());
    Assertions.assertEquals(56, record.files().size());
  }

  @Disabled
  @Test
  void testZenodoApiRedirctRecordID()
      throws HttpResponseException, IOException, InterruptedException {
    var record = ZenodoService.getWebRecord("https://zenodo.org/api/records/12628368");
    Assertions.assertNotNull(record);
  }

  @Disabled
  @Test
  void testZenodoApi() throws HttpResponseException, IOException, InterruptedException {
    var record = ZenodoService.getWebRecord("https://zenodo.org/api/records/13898009");
    Assertions.assertNotNull(record);
    Assertions.assertEquals("MSnLib Mass spectral libraries (.mgf and .json)", record.title());

    Assertions.assertNotNull(record);
    Assertions.assertEquals("MSnLib Mass spectral libraries (.mgf and .json)", record.title());
    Assertions.assertEquals(4, record.revision());
    Assertions.assertEquals("13898009", record.recid());
    Assertions.assertEquals("cc-by-4.0", record.metadata().license().id());
    Assertions.assertEquals(56, record.files().size());
  }

  @Disabled
  @Test
  void testZenodoApiAsset() throws HttpResponseException, IOException, InterruptedException {
    var record = ZenodoService.getWebRecord(
        DownloadAsset.Builder.ofZenodo(ExternalAsset.MS2DEEPSCORE, "12628368").create());
    Assertions.assertNotNull(record);

  }

}