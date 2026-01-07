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

package io.github.mzmine.modules.io.download;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mzmine.util.web.HttpResponseException;
import io.github.mzmine.util.web.HttpUtils;
import java.io.IOException;

public class ZenodoService {

  public static ZenodoRecord getWebRecord(ZenodoDownloadAsset asset)
      throws HttpResponseException, IOException, InterruptedException {
    return getWebRecord(asset.url());
  }

  public static ZenodoRecord getWebRecord(String url)
      throws HttpResponseException, IOException, InterruptedException {
    String recordInfoJson = HttpUtils.get(url);
    return getJsonMapper().readValue(recordInfoJson, ZenodoRecord.class);
  }

  public static JsonMapper getJsonMapper() {
    return JsonMapper.builder().addModule(new JavaTimeModule()).build();
  }
}
