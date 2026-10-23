/**
 * Created by Davide on 2026-10-23 .
 */
package move_with_shortcuts;


import dutil.value_holder.ObjectAndDescr;
import jakarta.validation.constraints.NotNull;

import java.nio.file.InvalidPathException;

import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.string.TextUtilities.dq;


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
   * @return The request in {@link ObjectAndDescr#object object}, or a human-readable error in {@link
   *         ObjectAndDescr#description description}.
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
   * Builds the human-readable description of a validation failure.
   *
   * @param e The failure.
   *
   * @return The description.
   */
  private static String errorDescription(@NotNull RuntimeException e) {
  
    final String message = e instanceof InvalidPathException
                           ? dq(e.getLocalizedMessage()) + " is not a valid path."
                           : e.getLocalizedMessage();
                           
    final String result = message == null || message.isBlank() ? getFullDescriptionWithRootCause(e) : message;
    return result;
  }
}
