package eu.europeana.metis.utils;

import static eu.europeana.metis.utils.TempFileUtils.createSecureTempFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains functionality to extract archives.
 */
public class CompressedFileHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(CompressedFileHandler.class);

  private static final String MAC_TEMP_FOLDER = "__MACOSX";
  private static final String MAC_TEMP_FILE = ".DS_Store";
  public static final String FILE_NAME_BANNED_CHARACTERS = "% $:?&#<>|*," + Character.MIN_VALUE;

  /**
   * Extract a file from a compressed file/archive.
   * <p>
   * Supports extraction of files from compressed files/archives.
   * <p>
   * The extraction is based on the apache common compress library and is generic to support all files that that library
   * supports.
   * <p>
   * We do though limit the support using what we have defined in the {@link CompressedFileExtension} enum.
   *
   * @param compressedFile The compressed file.
   * @param destinationFolder The destination folder.
   * @throws IOException If there was a problem with the extraction.
   */
  public static void extractFile(Path compressedFile, Path destinationFolder) throws IOException {
    Objects.requireNonNull(compressedFile, "compressedFile cannot be null");
    Objects.requireNonNull(destinationFolder, "destinationFolder cannot be null");

    final CompressedFileExtension compressingExtension = CompressedFileExtension.forPath(compressedFile);
    if (compressingExtension == null) {
      throw new IOException("Can't process archive of this type: " + compressedFile);
    }
    extractInternal(compressedFile, destinationFolder);
  }

  private static void extractInternal(Path compressedFile, Path destinationFolder) throws IOException {
    Files.createDirectories(destinationFolder);

    final List<Path> nestedArchives = new ArrayList<>();

    try (InputStream inputStream = Files.newInputStream(compressedFile);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        ArchiveInputStream<?> archiveInputStream = createArchiveInputStream(bufferedInputStream, compressedFile)) {

      if (archiveInputStream == null) {
        handleSingleCompressedFile(compressedFile, destinationFolder);
        return;
      }
      nestedArchives.addAll(extractEntriesAndReturnNestedArchives(archiveInputStream, destinationFolder));
    }
    extractNestedArchives(nestedArchives);
  }

  private static ArchiveInputStream<?> createArchiveInputStream(BufferedInputStream bis, Path compressedFile) {
    try {
      return new ArchiveStreamFactory().createArchiveInputStream(bis);
    } catch (ArchiveException e) {
      LOGGER.debug("File {} is not a recognized archive format: {}", compressedFile, e.getMessage());
      return null;
    }
  }

  private static List<Path> extractEntriesAndReturnNestedArchives(ArchiveInputStream<?> archiveInputStream,
      Path destinationFolder)
      throws IOException {
    final List<Path> nestedArchives = new ArrayList<>();
    ArchiveEntry entry;
    while ((entry = archiveInputStream.getNextEntry()) != null) {
      if (skipMacFiles(entry)) {
        continue;
      }

      final String entryName = replaceBannedCharacters(entry.getName());
      final Path entryPath = zipSlipVulnerabilityProtect(entryName, destinationFolder);

      if (entry.isDirectory()) {
        createDirectories(entryPath);
      } else {
        extractFileEntry(archiveInputStream, entryPath);
        if (CompressedFileExtension.hasCompressedFileExtension(entryName)) {
          nestedArchives.add(entryPath);
        }
      }
    }
    return nestedArchives;
  }

  private static void extractFileEntry(ArchiveInputStream<?> ais, Path entryPath)
      throws IOException {
    createParentDirectories(entryPath);
    Files.copy(ais, entryPath, StandardCopyOption.REPLACE_EXISTING);
  }


  private static void createDirectories(Path path) throws IOException {
    if (Files.notExists(path)) {
      Files.createDirectories(path);
    }
  }

  private static void createParentDirectories(Path path) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      createDirectories(parent);
    }
  }

  private static void extractNestedArchives(List<Path> nestedArchives) throws IOException {
    for (Path nested : nestedArchives) {
      extractInternal(nested, nested.getParent());
    }
  }

  private static boolean skipMacFiles(ArchiveEntry entry) {
    return entry.getName().startsWith(MAC_TEMP_FOLDER) || entry.getName().endsWith(MAC_TEMP_FILE);
  }

  private static void handleSingleCompressedFile(Path compressedFile, Path destinationFolder)
      throws IOException {

    Path outFile = CompressedFileExtension
        .removeAndNormalizeLastExtension(destinationFolder.resolve(compressedFile.getFileName()));

    Files.createDirectories(outFile.getParent());

    try (InputStream fis = Files.newInputStream(compressedFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        InputStream cis = new CompressorStreamFactory().createCompressorInputStream(bis);
        OutputStream out = Files.newOutputStream(outFile)) {
      IOUtils.copy(cis, out);

      if (CompressedFileExtension.hasCompressedFileExtension(outFile.toString())) {
        extractInternal(outFile, outFile.getParent());
        cleanupIntermediateFile(outFile);
      }

    } catch (CompressorException e) {
      LOGGER.debug("File {} is not a recognized compressed format: {}, probably we reached a leaf regular file", compressedFile,
          e.getMessage());
    }
  }

  private static void cleanupIntermediateFile(Path outFile) {
    try {
      Files.deleteIfExists(outFile);
    } catch (IOException e) {
      LOGGER.warn("Could not delete intermediate archive {}: {}", outFile, e.getMessage());
    }
  }

  private static String replaceBannedCharacters(String entryName) {
    return entryName.replaceAll("[" + FILE_NAME_BANNED_CHARACTERS + "]", "_");
  }

  private static Path zipSlipVulnerabilityProtect(String entryName, Path targetDir)
      throws IOException {
    // https://snyk.io/research/zip-slip-vulnerability
    Path targetDirResolved = targetDir.resolve(entryName);
    // make sure the normalized file still has targetDir as its prefix else throw exception
    Path normalizePath = targetDirResolved.normalize();
    if (!normalizePath.startsWith(targetDir)) {
      throw new IOException("Entry is outside of the target dir: " + entryName);
    }
    return normalizePath;
  }

  /**
   * This method extracts all files from a ZIP file and returns them as byte arrays. This method only considers files in the main
   * directory. This method creates (and then removes) a temporary file.
   *
   * @param providedZipFile Input stream containing the zip file. This method is not responsible for closing the stream.
   * @return A list of records.
   * @throws IOException In case of problems with the temporary file or with reading the zip file.
   */
  public List<ByteArrayInputStream> getContentFromZipFile(InputStream providedZipFile)
      throws IOException {
    try (final ZipFile zipFile = createInMemoryZipFileObject(providedZipFile)) {
      final List<InputStream> streams = getContentFromZipFile(zipFile);
      final List<ByteArrayInputStream> result = new ArrayList<>(streams.size());
      for (InputStream stream : streams) {
        result.add(new ByteArrayInputStream(IOUtils.toByteArray(stream)));
      }
      return result;
    }
  }

  private ZipFile createInMemoryZipFileObject(InputStream content) throws IOException {
    final File tempFile = createSecureTempFile(content.getClass().getSimpleName(), ".zip").toFile();
    FileUtils.copyInputStreamToFile(content, tempFile);
    LOGGER.info("Temp file: {} created.", tempFile);
    return new ZipFile(tempFile, ZipFile.OPEN_READ | ZipFile.OPEN_DELETE);
  }


  /**
   * Retrieves records from a given ZIP file. Each record is extracted from the files inside the ZIP and converted to a string
   * with UTF-8 encoding.
   *
   * @param zipFile The ZIP file from which records will be retrieved.
   * @return A list of strings, where each string represents the contents of a file from the ZIP.
   * @throws IOException If an issue occurs while accessing or reading the ZIP file.
   */
  public List<String> getRecordsFromZipFile(ZipFile zipFile) throws IOException {
    final List<InputStream> streams = getContentFromZipFile(zipFile);
    final List<String> result = new ArrayList<>(streams.size());
    for (InputStream stream : streams) {
      result.add(IOUtils.toString(stream, StandardCharsets.UTF_8));
    }
    return result;
  }

  private List<InputStream> getContentFromZipFile(ZipFile zipFile) throws IOException {
    final List<InputStream> result = new ArrayList<>();
    final Iterator<? extends ZipEntry> entries = zipFile.stream().iterator();
    while (entries.hasNext()) {
      final ZipEntry zipEntry = entries.next();
      if (zipEntry.isDirectory() || skipMacFiles(zipEntry)) {
        continue;
      }
      result.add(zipFile.getInputStream(zipEntry));
    }
    return result;
  }

  private boolean skipMacFiles(ZipEntry zipEntry) {
    return zipEntry.getName().startsWith(MAC_TEMP_FOLDER) || zipEntry.getName().endsWith(MAC_TEMP_FILE);
  }
}
