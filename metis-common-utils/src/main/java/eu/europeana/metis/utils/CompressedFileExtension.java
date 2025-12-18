package eu.europeana.metis.utils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Collection of supported file compressions.
 */
public enum CompressedFileExtension {

  /**
   * ZIP files.
   **/
  ZIP(".zip"),

  /**
   * GZIP files (that are not also tar files)
   **/
  GZIP(".gz"),

  /**
   * TAR files (only tar files)
   */
  TAR(".tar"),

  /**
   * Tarred GZIP files.
   **/
  TAR_GZ(".tar.gz", true),

  /**
   * Tarred GZIP files.
   **/
  TGZIP(".tgz", true);

  private static final List<CompressedFileExtension> SORTED_EXTENSIONS =
      Arrays.stream(values())
            .sorted(Comparator.comparingInt(compressedFileExtension -> -compressedFileExtension.getExtension().length()))
            .toList();

  private final String extension;
  private final UnaryOperator<String> normalizeExtensionFunction;

  CompressedFileExtension(String extension) {
    this(extension, false);
  }

  CompressedFileExtension(String extension, boolean addTarAfterStripping) {
    this.extension = extension;
    this.normalizeExtensionFunction = addTarAfterStripping
        ? (name -> stripExtension(name, extension) + ".tar")
        : (name -> stripExtension(name, extension));
  }

  /**
   * @return The extension associated with this file compression (including the separator).
   */
  public final String getExtension() {
    return extension;
  }

  public Function<String, String> getNormalizeExtensionFunction() {
    return normalizeExtensionFunction;
  }

  private boolean matchesFileExtension(String fileName) {
    return fileName.endsWith(getExtension());
  }

  /**
   * Find the compression for the given file based on the file's extension.
   *
   * @param file The file for which to return the compression.
   * @return The compression, or null if the file extension does not reflect a supported compression.
   */
  public static CompressedFileExtension forPath(Path file) {
    return SORTED_EXTENSIONS.stream()
                            .filter(compressedFileExtension -> compressedFileExtension.matchesFileExtension(file.toString()))
                            .findFirst()
                            .orElse(null);
  }

  /**
   * Removes from the provided path the last extension and normalizes the extension(if applicable) if it belongs to a supported
   * compressed file format.
   * <p>
   * Examples:
   * <ul>
   *   <li>If the path is .tar.gz, the extension will be removed and the path will be .tar</li>
   *   <li>If the path is .tgz, the extension will be removed and the path will be .tar</li>
   *   <li>If the path is .tar, .zip, .gz, the extension will be removed</li>
   * </ul>
   *
   * @param path the file path to normalize and modify. Must not be null and must have a filename.
   * @return the normalized path with the last compression-related extension removed, or the original path if no supported
   * compression extension is present.
   * @throws NullPointerException if the provided path or its filename is null.
   */
  public static Path removeAndNormalizeLastExtension(Path path) {
    Objects.requireNonNull(path, "Path cannot be null");
    Objects.requireNonNull(path.getFileName(), "Path must have a filename");

    CompressedFileExtension compressedFileExtension = forPath(path);
    if (compressedFileExtension == null) {
      return path;
    }

    String newName = compressedFileExtension.getNormalizeExtensionFunction().apply(path.getFileName().toString());
    return Optional.ofNullable(path.getParent())
                   .map(parent -> parent.resolve(newName))
                   .orElseGet(() -> Path.of(newName));
  }

  private static String stripExtension(String name, String extension) {
    return name.substring(0, name.length() - extension.length());
  }

  /**
   * Remove the extension of one of the supported compressions from the file.
   *
   * @param file The file from which to remove the extension.
   * @return The path without the file extension.
   * @throws IllegalArgumentException If the file does not have a supported file extension.
   */
  public static Path removeExtension(Path file) {
    final CompressedFileExtension compressedFileExtension = forPath(file);
    if (compressedFileExtension == null || file.getFileName() == null) {
      throw new IllegalArgumentException("File " + file + " is not a recognised compressed file.");
    }
    final String fileName = file.getFileName().toString();
    final String newFileName = stripExtension(fileName, compressedFileExtension.getExtension());
    return Optional.ofNullable(file.getParent()).map(parent -> parent.resolve(newFileName))
                   .orElseGet(() -> Path.of(newFileName));
  }

  /**
   * Checks whether the file has a supported compressed file extension.
   *
   * @param file The file.
   * @return Whether the file has a supported compressed file extension.
   */
  public static boolean hasCompressedFileExtension(Path file) {
    return file.getFileName() != null && hasCompressedFileExtension(file.getFileName().toString());
  }

  /**
   * Checks whether the file name has a supported compressed file extension.
   *
   * @param fileName The file name.
   * @return Whether the file name has a supported compressed file extension.
   */
  public static boolean hasCompressedFileExtension(String fileName) {
    return Stream.of(values()).anyMatch(compressedFileExtension -> compressedFileExtension.matchesFileExtension(fileName));
  }
}
