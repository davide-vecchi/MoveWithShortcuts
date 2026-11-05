/**
 * Created by Davide on 2026-11-04 .
 */
package move_with_shortcuts;


import jakarta.validation.constraints.NotNull;

import java.io.File;

import static dfile.file.FileUtilities.getCanonicalPath;
import static dutil.string.TextUtilities.dq;


// @formatter:off


/**
 * Resolves the default folder to scan for shortcuts from a configured value, falling back to the user's home folder.
 */
final class SearchPathDefaults {


  /**
   * The outcome of resolving a default search folder.
   *
   * @param path The resolved folder path.<br>
   *
   * @param warning A warning when the configured value was unusable, or {@code null}.
   */
  record Resolution(@NotNull String path, String warning) {}
  
  
  /**
   * Private constructor to prevent instantiation.
   */
  private SearchPathDefaults() {}
  
  
  /**
   * Resolves the given configured value into a usable default search folder, warning when it is unusable.
   *
   * @param configuredPath The configured or command-line value, or {@code null} when it is absent.<br>
   *
   * @param sourceDescr The description of the source of the value.
   *
   * @return The resolved default and an optional warning.
   */
  static Resolution resolveDefaultSearchPath(String configuredPath, @NotNull String sourceDescr) {
  
    final String userHome = System.getProperty("user.home");
    
    Resolution result;
    
    if (configuredPath == null) {
    
      result = new Resolution(userHome, null);
    }
    else if (configuredPath.isBlank()) {
    
      result = new Resolution(userHome, "The " + sourceDescr + " is blank; using " + dq(userHome)
                                    + " as the default search folder.");
    }
    else {
    
      final File configuredFile = new File(configuredPath);
      
      try {
      
        final String canonicalPath = getCanonicalPath(configuredFile);
        
        if (configuredFile.isDirectory()) {
        
          result = new Resolution(canonicalPath, null);
        }
        else {
        
          result = new Resolution(userHome, "The " + sourceDescr + " " + dq(canonicalPath)
                                        + " is not an existing folder; using " + dq(userHome)
                                        + " as the default search folder.");
        }
      }
      catch (RuntimeException e) {
      
        result = new Resolution(userHome, "The " + sourceDescr + " " + dq(configuredPath)
                                      + " is not a valid path (" + e.getLocalizedMessage() + "); using "
                                      + dq(userHome) + " as the default search folder.");
      }
    }
    return result;
  }
  
}
