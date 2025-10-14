package eu.europeana.metis.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link CompressedFileHandler}
 */
class CompressedFileHandlerRecordsTest {


  @TempDir
  File tempDir;

  @Test
  void testGetRecordsFromZipFile() throws IOException {
    File zipFile = new File(tempDir, "test.zip");
    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {
      addTextEntry(zos, "file1.txt", "Hello World");
      addTextEntry(zos, "file2.txt", "Another File");
      addTextEntry(zos, "nested/file3.txt", "Nested File");
    }

    List<String> records;
    try (ZipFile zf = new ZipFile(zipFile, StandardCharsets.UTF_8)) {
      records = new CompressedFileHandler().getRecordsFromZipFile(zf);
    }

    assertEquals(3, records.size());
    assertTrue(records.contains("Hello World"));
    assertTrue(records.contains("Another File"));
    assertTrue(records.contains("Nested File"));
  }

  @Test
  void testGetRecordsFromEmptyZipFile() throws IOException {
    File zipFile = new File(tempDir, "empty.zip");
    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos)) {
      // just close immediately — produces valid empty zip
    }

    try (ZipFile zf = new ZipFile(zipFile)) {
      List<String> records = new CompressedFileHandler().getRecordsFromZipFile(zf);
      assertTrue(records.isEmpty(), "Expected no records for empty ZIP");
    }
  }

  @Test
  void testSkipMacOsEntries() throws IOException {
    File file = new File(tempDir, "macos.zip");
    try (FileOutputStream fos = new FileOutputStream(file);
        ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {

      addTextEntry(zos, "Internal_valid/", "");
      addTextEntry(zos, "__MACOSX/", ""); // directory
      addTextEntry(zos, "__MACOSX/something", "Should be ignored");
      addTextEntry(zos, "folder/.DS_Store", "Should also be ignored");
      addTextEntry(zos, "valid/file1.txt", "Keep Me");
      addTextEntry(zos, "valid/file2.txt", "Also Keep Me");
    }

    List<String> records;
    try (ZipFile zipFile = new ZipFile(file, StandardCharsets.UTF_8)) {
      records = new CompressedFileHandler().getRecordsFromZipFile(zipFile);
    }

    assertEquals(2, records.size());
    assertTrue(records.contains("Keep Me"));
    assertTrue(records.contains("Also Keep Me"));
  }

  private void addTextEntry(ZipOutputStream zos, String name, String content) throws IOException {
    ZipEntry entry = new ZipEntry(name);
    zos.putNextEntry(entry);
    zos.write(content.getBytes(StandardCharsets.UTF_8));
    zos.closeEntry();
  }
}
