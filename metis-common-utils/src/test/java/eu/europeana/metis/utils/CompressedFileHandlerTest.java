package eu.europeana.metis.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link CompressedFileHandler}
 */

class CompressedFileHandlerTest {

  private static final Path DESTINATION_DIR = Path.of("src", "test", "resources", "__files");
  private static final String XML_TYPE = "xml";
  private static final String NON_COMPRESSED_FILE = "non_compressed_file.txt";
  private static final String UNSUPPORTED_TAR_BZ2 = "archive.tar.bz2";

  // GZ/TAR test files
  private static final String TAR_GZ_FILE1 = "gzFile.tgz";
  private static final String TAR_GZ_FILE2 = "gzFileWithCompressedGZFiles.tar.gz";
  private static final String TAR_GZ_FILE3 = "gzFilesWithMixedCompressedFiles.tar.gz";
  private static final String TAR_GZ_FILE4 = "gzFileWithSubdirContainingSpaceInName.tar.gz";
  private static final String TAR_FILE = "tarFileWithSubdirContainingSpaceInName.tar";

  // ZIP test files
  private static final String ZIP_FILE1 = "zipFileWithNestedZipFiles.zip";
  private static final String ZIP_FILE2 = "zipFileWithNestedFolders.zip";
  private static final String ZIP_FILE3 = "ZipFilesWithMixedCompressedFiles.zip";
  private static final String ZIP_FILE4 = "zipFileWithSubdirContainingSpaceInName.zip";
  private static final String ZIP_FILE5 = "zipWithDirectories.zip";

  @AfterEach
  void cleanUp() {
    Stream.of(
              getPrefix(TAR_GZ_FILE1),
              getPrefix(TAR_GZ_FILE2),
              getPrefix(TAR_GZ_FILE3),
              getPrefix(TAR_GZ_FILE4),
              getPrefix(TAR_FILE),
              getPrefix(ZIP_FILE1),
              getPrefix(ZIP_FILE2),
              getPrefix(ZIP_FILE3),
              getPrefix(ZIP_FILE4),
              getPrefix(ZIP_FILE5)
          ).map(name -> DESTINATION_DIR.resolve(name).toFile())
          .filter(File::exists)
          .forEach(file -> {
            try {
              FileUtils.forceDelete(file);
            } catch (IOException e) {
              throw new RuntimeException("Failed to delete " + file, e);
            }
          });
  }

  @ParameterizedTest(name = "Extract {0} {1} → {2}")
  @MethodSource
  void shouldExtractCompressedFilesRecursively(String fileName, String outputDir, int expectedCount,
      boolean checkBannedChars) throws IOException {
    Path compressedFile = DESTINATION_DIR.resolve(fileName);

    CompressedFileHandler compressedFileHandler = new CompressedFileHandler(10, 2);
    compressedFileHandler.extract(compressedFile, DESTINATION_DIR);

    Collection<File> files = FileUtils.listFiles(
        DESTINATION_DIR.resolve(outputDir).toFile(), new String[]{XML_TYPE}, true);

    assertNotNull(files);
    assertEquals(expectedCount, files.size());

    if (checkBannedChars) {
      files.forEach(file -> assertEquals(
          -1,
          StringUtils.indexOfAny(file.getName(), CompressedFileHandler.FILE_NAME_BANNED_CHARACTERS)
      ));
    }
  }

  @ParameterizedTest(name = "Expect failure for non-compressed file: {0}")
  @MethodSource
  void shouldFailWhenProvidedFileIsNotCompressed(String fileName) {
    Assertions.assertThrows(IOException.class,
        () -> new CompressedFileHandler().extract(DESTINATION_DIR.resolve(fileName), DESTINATION_DIR));
  }

  private static Stream<Arguments> shouldExtractCompressedFilesRecursively() {
    return Stream.of(
        // GZ/TAR tests
        Arguments.of(TAR_GZ_FILE1, getPrefix(TAR_GZ_FILE1), 13, false),
        Arguments.of(TAR_GZ_FILE2, getPrefix(TAR_GZ_FILE2), 13, false),
        Arguments.of(TAR_GZ_FILE3, getPrefix(TAR_GZ_FILE3), 13, false),
        Arguments.of(TAR_GZ_FILE4, getPrefix(TAR_GZ_FILE4), 10, true),
        Arguments.of(TAR_FILE, getPrefix(TAR_FILE), 10, true),

        // ZIP tests
        Arguments.of(ZIP_FILE1, getPrefix(ZIP_FILE1), 13, false),
        Arguments.of(ZIP_FILE2, getPrefix(ZIP_FILE2), 13, false),
        Arguments.of(ZIP_FILE3, getPrefix(ZIP_FILE3), 13, false),
        Arguments.of(ZIP_FILE4, getPrefix(ZIP_FILE4), 10, true),
        Arguments.of(ZIP_FILE5, getPrefix(ZIP_FILE5), 50, true)
    );
  }

  private static Stream<Arguments> shouldFailWhenProvidedFileIsNotCompressed() {
    return Stream.of(
        Arguments.of(NON_COMPRESSED_FILE),
        Arguments.of(UNSUPPORTED_TAR_BZ2)
    );
  }

  private static String getPrefix(String fileName) {
    return fileName.substring(0, fileName.indexOf("."));
  }

  @ParameterizedTest
  @MethodSource
  void testConvertPartitionedPathToOriginal(Path input, Path expected) {
    assertEquals(expected, CompressedFileHandler.convertPartitionedPathToOriginal(input));
  }

  private static Stream<Arguments> testConvertPartitionedPathToOriginal() {
    return Stream.of(
        Arguments.of(
            Path.of("directory1/part_0/file.txt"),
            Path.of("directory1/file.txt")
        ),
        Arguments.of(
            Path.of("directory1/part_0/directory2/part_1/file1.txt"),
            Path.of("directory1/directory2/file1.txt")
        ),
        Arguments.of(
            Path.of("gzFile/part_0/gzFile/part_1/xml/part_0/xml/part_0/anotherZip/part_0/anotherZip/part_0/jedit-4.5.0.xml"),
            Path.of("gzFile/gzFile/xml/xml/anotherZip/anotherZip/jedit-4.5.0.xml")
        )
    );
  }
}
