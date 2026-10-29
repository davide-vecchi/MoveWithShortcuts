/**
 * Created by Davide on 2026-10-22 .
 */
package move_with_shortcuts;


import application.AAppContext;
import dfile.shortcut.IShortcutsUpdater;
import dfile.shortcut.WinShortcutsUpdater_PSScriptsMulti;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.Arrays;
import java.util.function.Function;

import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.string.TextUtilities.assertNonBlank;
import static dutil.string.TextUtilities.dq;


/**
 * The shortcuts updater implementations that MWS can select.
 */
enum ShortcutsUpdaterKind {


  /**
   * The PowerShell-based updater that processes all shortcuts in one process.
   */
  PS_SCRIPTS_MULTI(    "ps-scripts-multi"
    , "PowerShell, one process for all shortcuts (WScript.Shell COM)."
    , ac -> WinShortcutsUpdater_PSScriptsMulti.newInstance(null, ac));
  // The JNA-based implementation will be added by TASK-28.
  
  @Getter
  private final String id;
  
  /**
   * The description shown by the GUI combo box and by the command-line selection prompt.
   */
  @Getter
  private final String description;
  
  private final Function<AAppContext, IShortcutsUpdater> factory;
  
  
  /**
   * @param id          {@link #id}.<br>
   *
   * @param description {@link #description}.<br>
   *
   * @param factory     The factory that creates an updater of this kind.
   */
  ShortcutsUpdaterKind(@NotBlank String id,                                       @NotBlank String description
                     , @NotNull  Function<AAppContext, IShortcutsUpdater> factory) {
                     
    this.id =          assertNonBlank(id);
    
    this.description = assertNonBlank(description);
    
    this.factory =     assertNonNull(   factory);
  }
  
  
  /**
   * Returns the kind used when no choice is given.
   *
   * @return The default kind.
   */
  static ShortcutsUpdaterKind defaultKind() {
  
    return PS_SCRIPTS_MULTI;
  }
  
  /**
   * Returns the IDs of all the available kinds, in declaration order.
   *
   * @return The IDs.
   */
  static String[] ids() {
  
    return Arrays.stream(values()).map(ShortcutsUpdaterKind::getId).toArray(String[]::new);
  }
  
  /**
   * Returns the kind with the given ID.
   *
   * @param id The ID, matched case-insensitively.
   *
   * @return The kind.
   *
   * @throws IllegalArgumentException If the ID does not name an available kind.
   */
  static ShortcutsUpdaterKind parse(@NotBlank String id) {
  
    final String trimmedId = id.trim();
    
    return Arrays.stream(values())
                 .filter(kind -> kind.id.equalsIgnoreCase(trimmedId))
                 .findFirst()
                 .orElseThrow(() -> new IllegalArgumentException(
                                                     "Unknown shortcuts updater implementation " + dq(id)
                                                   + " ; expected one of " + String.join(", ", ids()) + "."));
  }
  
  /**
   * Creates a new updater of this kind.
   *
   * @param appContext The application context to use.
   *
   * @return The new updater.
   */
  IShortcutsUpdater newUpdater(@NotNull AAppContext appContext) {
  
    return this.factory.apply(appContext);
  }
  
}
