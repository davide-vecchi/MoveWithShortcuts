/**
 * Created by Davide on 2026-10-24 .
 */
package move_with_shortcuts;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.apache.commons.lang3.StringUtils.isNotBlank;


// @formatter:off


/**
 * Creates the per-instance log folder and the log file names inside it, so that several MWS instances can run at the
 * same time without sharing log files.
 */
final class MoveWithShortcutsLogs {


  /**
   * The name of the folder that contains the per-instance log folders.
   */
  static final String LOG_FOLDER_NAME = "LOG";
  
  /**
   * The maximum length of the original leaf used in the log folder name.
   */
  private static final int MAX_LEAF_LENGTH = 60;
  
  /**
   * The characters that cannot appear in a Windows folder name.
   */
  private static final String INVALID_FOLDER_NAME_CHARS = "<>:\"/\\|?*";
  
  /**
   * The leaf to use when the original path is not known or has no usable leaf.
   */
  private static final String FALLBACK_LEAF = "MoveWithShortcuts";
  
  /**
   * The format of the timestamp part of the log folder name. It uses only characters that are valid in a Windows
   * folder name.
   */
  private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd_HH-mm-ss-SSS";
  
  
  /**
   * Private constructor to prevent instantiation.
   */
  private MoveWithShortcutsLogs() {}
  
  
  /**
   * Creates a new per-instance log folder under the given parent log folder.
   *
   * @param logParentFolder The folder that contains the per-instance folders.<br>
   *
   * @param originalPath The original file or folder path, or {@code null} when it is not known.
   *
   * @return The new folder, or {@code null} when it cannot be created.
   */
  static File createInstanceLogFolder(@NotNull File logParentFolder, String originalPath) {
  
    File result = null;
    
    try {
    
      Files.createDirectories(logParentFolder.toPath());
      
      final String baseName = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date())
                            + "_" + leafOf(originalPath);
                            
      int suffix = 1;
      
      while (result == null) {
      
        final File candidate = new File(logParentFolder
                                      , suffix == 1 ? baseName : baseName + "_" + suffix);
                                      
        try {
        
          Files.createDirectory(candidate.toPath());
          
          result = candidate;
        }
        catch (FileAlreadyExistsException e) {
        
          suffix++;
        }
      }
    }
    catch (IOException e) {
    
      result = null;
    }
    return result;
  }
  
  /**
   * Returns the leaf of the original path, sanitized for use in a folder name, or a fallback when it is not usable.
   *
   * @param originalPath The original file or folder path, or {@code null}.
   *
   * @return The leaf to use in the log folder name.
   */
  static String leafOf(String originalPath) {
  
    String result = FALLBACK_LEAF;
    
    if (isNotBlank(originalPath)) {
    
      final String sanitized = sanitize(new File(originalPath).getName());
      
      if (isNotBlank(sanitized)) {
      
        result = sanitized;
      }
    }
    return result;
  }
  
  /**
   * Returns the path to use for a log file inside the given folder.
   *
   * @param logFolder The log folder, or {@code null} to use the bare file name.<br>
   *
   * @param fileName The log file name.
   *
   * @return The log file path.
   */
  static String fileName(File logFolder, @NotBlank String fileName) {
  
    return logFolder == null ? fileName : new File(logFolder, fileName).getPath();
  }
  
  /**
   * Replaces the characters that are not allowed in a Windows folder name, trims the result and limits its length.
   *
   * @param leaf The leaf to sanitize.
   *
   * @return The sanitized leaf.
   */
  private static String sanitize(@NotBlank String leaf) {
  
    final StringBuilder builder = new StringBuilder(leaf.length());
    
    for (int i = 0; i < leaf.length(); i++) {
    
      final char c = leaf.charAt(i);
      
      builder.append(c < ' ' || INVALID_FOLDER_NAME_CHARS.indexOf(c) >= 0 ? '_' : c);
    }
    String result = builder.toString().trim();
    
    while (result.endsWith(".")) {
    
      result = result.substring(0, result.length() - 1);
    }
    if (result.length() > MAX_LEAF_LENGTH) {
    
      result = result.substring(0, MAX_LEAF_LENGTH);
    }
    return result;
  }
}
