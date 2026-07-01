/**
 * Created by Davide on 20:21 2026-06-29 .
 */
package rename_with_links;


import jakarta.validation.constraints.NotNull;

import java.io.File;
import java.util.List;


/**
 * Object that processes a {@link List} of shortcut files, updating their target based on the logic implemented in it.
 */
public interface IShortcutsTargetUpdater {
  
  
  /**
   * Updates the target of the given shortcuts based on the implemented logic.
   *
   * @param shortcuts The shortcut files to process.<br>
   *
   * @param oldTarget The target that - if present in a shortcut - must be updated to {@code newTarget}.<br>
   *
   * @param newTarget The target to set into the shortcuts that have it equal to {@code oldTarget}.
   *
   * @return Whether the user interrupted the processing.
   */
  boolean updateShortcuts(@NotNull List<File> shortcuts, @NotNull File oldTarget
                                                       , @NotNull File newTarget);
  
}
