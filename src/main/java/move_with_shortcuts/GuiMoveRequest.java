/**
 * Created by Davide on 2026-10-23 .
 */
package move_with_shortcuts;


import jakarta.validation.constraints.NotNull;

import java.io.File;


// @formatter:off


/**
 * A validated move / rename request built from the values entered in the GUI.
 *
 * @param originalFileOrFolder The file or folder to rename / move.<br>
 *
 * @param destinationFileOrFolder The destination file or folder.<br>
 *
 * @param mustMove Whether the rename / move still needs to be performed.<br>
 *
 * @param searchFolder The folder under which to search for the shortcuts to update.
 */
record GuiMoveRequest(@NotNull File originalFileOrFolder, @NotNull File destinationFileOrFolder
                    , boolean mustMove, @NotNull File searchFolder) {}
