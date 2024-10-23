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

package panama;

import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class MemoryMapTest {

  public MemorySegment memMap() throws IOException {
    MemorySegment memorySegment = null;
    var arena = Arena.ofAuto();
    memorySegment = FileAndPathUtil.memoryMapSparseTempFile("mzm", ".tmp",
        Path.of("D:\\mzminetemp2\\"), arena, 100000);
    new File("D:\\mzminetemp2\\mzm.tmp").delete();
    memorySegment.set(ValueLayout.JAVA_INT, 0, 15);
    var i = memorySegment.get(ValueLayout.JAVA_INT, 0);
    System.out.println(i);
    return memorySegment;
  }

  @Test
  void delete() throws IOException {
    Arrays.stream(Path.of("D:\\mzminetemp2\\").toFile().listFiles())
        .filter(f -> f.getName().startsWith("mzmine")).forEach(file -> {
          try {
            Files.delete(file.toPath());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void test() throws IOException {
    var memorySegment = memMap();
    new File("D:\\mzminetemp2\\mzm.tmp").delete();
    System.out.println("mapped");
    Assertions.assertEquals(15, memorySegment.get(ValueLayout.JAVA_INT, 0));
    System.gc();
    System.gc();
    System.out.println("After gc");
    Assertions.assertEquals(15, memorySegment.get(ValueLayout.JAVA_INT, 0));
    memorySegment = null;
    System.gc();
    System.gc();
    System.out.println("After gc");

  }
}
