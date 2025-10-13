package eu.europeana.metis.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CompressedFileExtensionTest {

  @ParameterizedTest
  @CsvSource({
      "fileName.zip,ZIP,fileName",
      "fileName.gz,GZIP,fileName",
      "fileName.tar.gz,TAR_GZ,fileName.tar",
      "fileName.tgz,TGZIP,fileName.tar",
      "fileName.tar,TAR,fileName"
  })
  void testForPathAndRemoveLastExtension(String fileName, CompressedFileExtension expectedExtension, String expectedPrefix) {
    Path path = Path.of(fileName);
    assertEquals(expectedExtension, CompressedFileExtension.forPath(path));
    assertEquals(expectedPrefix, CompressedFileExtension.removeAndNormalizeLastExtension(path).getFileName().toString());
  }
}