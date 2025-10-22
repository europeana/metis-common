package eu.europeana.metis.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains functionality to extract archives.
 */
public class CompressedFileHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String MAC_TEMP_FOLDER = "__MACOSX";
  private static final String MAC_TEMP_FILE = ".DS_Store";
  private static final int DEFAULT_MAX_EXTRACTION_DEPTH = 10;
  private static final int DEFAULT_MAX_ENTRIES_PER_DIR = 10000;

  public static final String FILE_NAME_BANNED_CHARACTERS = "% $:?&#<>|*," + Character.MIN_VALUE;
  public static final String PARTITION_PREFIX = "part_";

  private final int maxExtractionDepth;
  private final int maxEntriesPerDir;

  /**
   * Default constructor for the CompressedFileHandler class. Initializes the instance with default values for maximum extraction
   * depth and maximum entries per directory, as defined by DEFAULT_MAX_EXTRACTION_DEPTH and DEFAULT_MAX_ENTRIES_PER_DIR
   * constants.
   */
  public CompressedFileHandler() {
    this(DEFAULT_MAX_EXTRACTION_DEPTH, DEFAULT_MAX_ENTRIES_PER_DIR);
  }

  /**
   * Constructs a new instance of the CompressedFileHandler class with specific limits for maximum extraction depth and maximum
   * entries per directory.
   *
   * @param maxExtractionDepth the maximum depth to which files can be extracted from nested archives
   * @param maxEntriesPerDir the maximum number of entries that can be extracted into a single directory
   */
  public CompressedFileHandler(int maxExtractionDepth, int maxEntriesPerDir) {
    this.maxExtractionDepth = maxExtractionDepth;
    this.maxEntriesPerDir = maxEntriesPerDir;
  }

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
   * @deprecated Use Constructor and then {@link #extract(Path, Path)} instead.
   */
  @Deprecated(since = "17", forRemoval = true)
  public static void extractFile(Path compressedFile, Path destinationFolder) throws IOException {
    new CompressedFileHandler().extract(compressedFile, destinationFolder);
  }

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
  public void extract(Path compressedFile, Path destinationFolder) throws IOException {
    Objects.requireNonNull(compressedFile, "compressedFile cannot be null");
    Objects.requireNonNull(destinationFolder, "destinationFolder cannot be null");

    final CompressedFileExtension compressingExtension = CompressedFileExtension.forPath(compressedFile);
    if (compressingExtension == null) {
      throw new IOException("Can't process archive of this type: " + compressedFile);
    }

    extractInternal(compressedFile, destinationFolder);
  }

  record ArchiveContext(Path archive, Path destination, int depth) {

  }

  private void extractInternal(Path rootArchive, Path destinationFolder) throws IOException {
    Deque<ArchiveContext> archiveQueue = new ArrayDeque<>();
    archiveQueue.add(new ArchiveContext(rootArchive, destinationFolder, 0));

    while (!archiveQueue.isEmpty()) {
      ArchiveContext archiveContext = archiveQueue.removeFirst();
      Path currentArchive = archiveContext.archive();
      Path parentDestination = archiveContext.destination();
      int depth = archiveContext.depth();

      Path fileNameWithoutExtension = CompressedFileExtension.removeExtension(currentArchive.getFileName());
      Path normalizedDestination;

      if (parentDestination.getFileName() != null && parentDestination.getFileName().equals(fileNameWithoutExtension)) {
        normalizedDestination = parentDestination;
      } else {
        normalizedDestination = parentDestination.resolve(fileNameWithoutExtension);
      }

      Files.createDirectories(normalizedDestination);

      if (depth < maxExtractionDepth) {
        try (InputStream inputStream = Files.newInputStream(currentArchive);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            ArchiveInputStream<?> archiveInputStream = createArchiveInputStream(bufferedInputStream, currentArchive)) {
          handleArchive(archiveInputStream, currentArchive, normalizedDestination, archiveQueue, depth);
        } catch (IOException e) {
          throw new IOException("Error extracting archive: " + currentArchive, e);
        }
      } else {
        LOGGER.warn("Max extraction depth ({}) reached at: {} — skipping deeper extraction", maxExtractionDepth, currentArchive);
      }

      if (!currentArchive.equals(rootArchive)) {
        Files.deleteIfExists(currentArchive);
      }
    }
  }

  private ArchiveInputStream<?> createArchiveInputStream(BufferedInputStream bis, Path compressedFile) {
    try {
      return new ArchiveStreamFactory().createArchiveInputStream(bis);
    } catch (ArchiveException e) {
      LOGGER.debug("File {} is not a recognized archive format: {}", compressedFile, e.getMessage());
      return null;
    }
  }

  private void handleArchive(ArchiveInputStream<?> archiveInputStream, Path currentArchive, Path normalizedDestination,
      Deque<ArchiveContext> archiveQueue, int depth) throws IOException {
    if (archiveInputStream == null) {
      Optional<Path> maybeNestedCompressed = handleSingleCompressedFile(currentArchive, normalizedDestination);
      maybeNestedCompressed.ifPresent(p ->
          archiveQueue.add(new ArchiveContext(p, p.getParent(), depth + 1))
      );
    } else {
      List<Path> nestedArchives = extractEntriesAndReturnNestedArchives(archiveInputStream, normalizedDestination);
      nestedArchives.stream().map(nested -> new ArchiveContext(nested, nested.getParent(), depth + 1))
                    .forEach(archiveQueue::add);
    }
  }

  private Optional<Path> handleSingleCompressedFile(Path compressedFile, Path destinationFolder) throws IOException {

    Path outFile = CompressedFileExtension
        .removeAndNormalizeLastExtension(destinationFolder.resolve(compressedFile.getFileName()));

    Files.createDirectories(outFile.getParent());

    try (InputStream fis = Files.newInputStream(compressedFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        InputStream cis = new CompressorStreamFactory().createCompressorInputStream(bis);
        OutputStream out = Files.newOutputStream(outFile)) {

      IOUtils.copy(cis, out);

      if (CompressedFileExtension.hasCompressedFileExtension(outFile.toString())) {
        // Return this path for the outer loop to process it later
        return Optional.of(outFile);
      }

    } catch (CompressorException e) {
      LOGGER.debug("File {} is not a recognized compressed format: {}", compressedFile, e.getMessage());
    }

    return Optional.empty();
  }

  private List<Path> extractEntriesAndReturnNestedArchives(ArchiveInputStream<?> archiveInputStream, Path destinationFolder)
      throws IOException {

    List<Path> nestedArchives = new ArrayList<>();
    Map<Path, Integer> entriesCounterPerPath = new HashMap<>();
    // Maps the original (logical) directory to its relocated (partitioned) physical path
    Map<Path, Path> relocatedParents = new HashMap<>();
    relocatedParents.put(destinationFolder, destinationFolder); // identity for root

    ArchiveEntry entry;
    while ((entry = archiveInputStream.getNextEntry()) != null) {
      if (skipMacFiles(entry)) {
        continue;
      }

      String sanitizedEntryName = replaceBannedCharacters(entry.getName());
      Path logicalPath = resolveAndValidateEntry(sanitizedEntryName, destinationFolder);

      Path logicalParent = (logicalPath.getParent() != null) ? logicalPath.getParent() : destinationFolder;
      // Where that logical parent was actually created (possibly under part_X/…)
      Path mappedParent = relocatedParents.getOrDefault(logicalParent, logicalParent);

      Path partitionedParent = getPartitionedParent(mappedParent, entriesCounterPerPath);
      Path entryPath = partitionedParent.resolve(logicalPath.getFileName());

      boolean isDirectory = entry.isDirectory();
      if (isDirectory) {
        // Create the relocated directory and remember its new physical path
        Files.createDirectories(entryPath);
        relocatedParents.put(logicalPath, entryPath);
      } else {
        extractFileEntry(archiveInputStream, entryPath);
        if (CompressedFileExtension.hasCompressedFileExtension(sanitizedEntryName)) {
          nestedArchives.add(entryPath);
        }
      }
    }

    return nestedArchives;
  }

  private Path getPartitionedParent(Path parentDir, Map<Path, Integer> counters) throws IOException {
    int index = counters.merge(parentDir, 0, (oldVal, newVal) -> oldVal + 1);
    int partitionNumber = index / maxEntriesPerDir;
    Path partitionDir = parentDir.resolve(PARTITION_PREFIX + partitionNumber);
    Files.createDirectories(partitionDir);
    return partitionDir;
  }

  /**
   * Converts a partitioned file path into its original non-partitioned form by removing artificial partition directories prefixed
   * with a specific keyword and occurring at odd positions within the path structure.
   * <p>
   * The provided partitionedPath is expected to be a relative and a normalized path as well as the result.
   * <p>
   *
   * @param partitionedPath the partitioned path to convert; must not be null
   * @return the original path reconstructed by filtering out artificial partition directories
   * @throws NullPointerException if the given partitionedPath is null
   */
  public static Path convertPartitionedPathToOriginal(Path partitionedPath) {
    Objects.requireNonNull(partitionedPath, "partitionedPath cannot be null");

    List<String> originalPathParts = new ArrayList<>();

    int index = 0;
    for (Path part : partitionedPath.normalize()) {
      String name = part.getFileName().toString();

      // Remove artificial partition directories that occur at odd positions
      if (!(name.startsWith(PARTITION_PREFIX) && index % 2 == 1)) {
        originalPathParts.add(name);
      }
      index++;
    }

    return Path.of("", originalPathParts.toArray(String[]::new));
  }

  private void extractFileEntry(ArchiveInputStream<?> ais, Path entryPath)
      throws IOException {
    createParentDirectories(entryPath);
    Files.copy(ais, entryPath, StandardCopyOption.REPLACE_EXISTING);
  }

  private void createParentDirectories(Path path) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
  }

  private boolean skipMacFiles(ArchiveEntry entry) {
    return entry.getName().startsWith(MAC_TEMP_FOLDER) || entry.getName().endsWith(MAC_TEMP_FILE);
  }

  private String replaceBannedCharacters(String entryName) {
    return entryName.replaceAll("[" + FILE_NAME_BANNED_CHARACTERS + "]", "_");
  }

  private Path resolveAndValidateEntry(String entryName, Path targetDir) throws IOException {
    // https://snyk.io/research/zip-slip-vulnerability
    Path targetDirResolved = targetDir.resolve(entryName);
    // make sure the normalized file still has targetDir as its prefix else throw exception
    Path normalizePath = targetDirResolved.normalize();
    if (!normalizePath.startsWith(targetDir)) {
      throw new IOException("Entry is outside of the target dir: " + entryName);
    }
    return normalizePath;
  }
}
