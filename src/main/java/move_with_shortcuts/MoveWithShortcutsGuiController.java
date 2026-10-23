/**
 * Created by Davide on 2026-10-23 .
 */
package move_with_shortcuts;


import dutil.value_holder.ObjectAndDescr;
import jakarta.validation.constraints.NotNull;

import java.io.File;
import java.nio.file.InvalidPathException;

import static dfile.file.FileUtilities.getCanonicalPath;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.string.TextUtilities.dq;
import static org.apache.commons.lang3.StringUtils.isNotBlank;


// @formatter:off


/**
 * Controller that validates the values entered in the MWS GUI and builds the corresponding request, without creating
 * any Swing component. It is therefore testable in a headless environment.
 */
final class MoveWithShortcutsGuiController {


  /**
   * Private constructor to prevent instantiation.
   */
  private MoveWithShortcutsGuiController() {}
  
  
  /**
   * Validates the given paths and builds the corresponding move / rename request.
   *
   * @param originalPath The value of the original-path field.<br>
   *
   * @param destinationPath The value of the destination-path field.<br>
   *
   * @param searchPath The value of the search-path field.
   *
   * @return The request in {@link ObjectAndDescr#object object}, or a human-readable error in {@link ObjectAndDescr#description
   *         description}.
   */
  static ObjectAndDescr<GuiMoveRequest> buildRequest(@NotNull String originalPath
                                                   , @NotNull String destinationPath
                                                   , @NotNull String searchPath) {
                                                   
    ObjectAndDescr<GuiMoveRequest> result;
    
    try {
    
      final GuiMoveRequest request = MoveWithShortcuts.resolveGuiRequest(
                                                             originalPath, destinationPath, searchPath);
                                                             
      result = new ObjectAndDescr<>(request, null);
    }
    catch (RuntimeException e) {
    
      result = new ObjectAndDescr<>(null, errorDescription(e));
    }
    return result;
  }
  
  /**
   * Returns the default value for the search-folder field : the parent folder of the selected item, or the user's home
   * folder when no item was selected.
   *
   * @param originalPath The path received from Explorer, or {@code null} when started standalone.
   *
   * @return The default search-folder path.
   */
  static String defaultSearchPath(String originalPath) {
  
    String result = System.getProperty("user.home");
    
    if (isNotBlank(originalPath)) {
    
      final File original = new File(originalPath);
      
      final File parent = original.getParentFile();
      
      if (parent != null) {
      
        try {
        
          result = getCanonicalPath(parent);
        }
        catch (RuntimeException e) {
        
          result = parent.getPath();
        }
      }
    }
    return result;
  }
  
  /**
   * Builds the human-readable description of a validation failure.
   *
   * @param e The failure.
   *
   * @return The description.
   */
  private static String errorDescription(@NotNull RuntimeException e) {
  
    final String message = e instanceof InvalidPathException ? dq(e.getLocalizedMessage()) + " is not a valid path."
                                                             : e.getLocalizedMessage();
                                                             
    final String result = message == null || message.isBlank() ? getFullDescriptionWithRootCause(e) : message;
    
    return result;
  }
  
}
