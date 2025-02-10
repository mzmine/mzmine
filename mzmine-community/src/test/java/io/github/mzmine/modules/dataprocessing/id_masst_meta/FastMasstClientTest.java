package io.github.mzmine.modules.dataprocessing.id_masst_meta;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;

class FastMasstClientTest {

  void testLocalFile() throws IOException {
    try (var stream = getClass().getClassLoader()
        .getResourceAsStream("modules\\id_masst_meta\\microbemasst.html")) {

    }
  }

}