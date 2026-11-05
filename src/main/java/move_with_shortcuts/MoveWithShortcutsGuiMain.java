/**
 * Created by Davide on 2026-10-23 .
 */
package move_with_shortcuts;


import application.AAppContext;
import dfile.shortcut.ShortcutFileNamePolicy;
import dlog.log.Log;
import dutil.value_holder.ObjectAndDescr;
import jakarta.validation.constraints.NotNull;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.CodeSource;

import static dlog.log.Log.writeLogsHeaders;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.object.ObjectUtilities.assertNull;
import static dutil.string.TextUtilities.NL2;
import static dutil.system.OSUtilities.setSystemEncodingUTF8;
import static move_with_shortcuts.MoveWithShortcutsMain.APP_DESCR;
import static move_with_shortcuts.MoveWithShortcutsMain.APP_NAME;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;


// @formatter:off


/**
 * The startup class of the MWS GUI, launched by the Windows Explorer context-menu integration or standalone.
 */
public final class MoveWithShortcutsGuiMain {


  /**
   * Private constructor to prevent instantiation.
   */
  private MoveWithShortcutsGuiMain() {}
  
  
  /**
   * Entry point of the MWS GUI.
   *
   * @param args The command line args : the first one, when present, is the path of the selected file or folder;
   *             no args start the GUI standalone.
   */
  public static void main(String[] args) {
  
    setSystemEncodingUTF8();
    
    final String originalPath = args.length > ZERO_i ? args[ZERO_i] : null;
    
    start(originalPath, null, null, null
        , null, ShortcutFileNamePolicy.EXACT, null
        , null, null);
  }
  
  /**
   * Starts the GUI with the given initial values and overrides.
   *
   * @param originalPath The original path to prefill, or {@code null}.<br>
   *
   * @param destinationPath The destination path to prefill, or {@code null}.<br>
   *
   * @param searchPath The search folder to prefill, or {@code null}.<br>
   *
   * @param updaterKindOverride The updater to use, or {@code null} to use the configuration/default.<br>
   *
   * @param shortcutFileNamePolicy The shortcut-file name policy to use.<br>
   *
   * @param createContextMenuOverride Whether to create or update the context-menu integration, or {@code null} to use
   *        the configuration.<br>
   *
   * @param verbosityOverride The verbosity to use, or {@code null} to use the GUI default.<br>
   *
   * @param warningMessage A message to show in the operation-detail area, or {@code null}.
   */
  static void start(String                 originalPath,           String               destinationPath
                  , String                 searchPath,             String               defaultSearchPathArg
                  , ShortcutsUpdaterKind   updaterKindOverride,    ShortcutFileNamePolicy shortcutFileNamePolicy
                  , Boolean                createContextMenuOverride, Number            verbosityOverride
                  , String                 warningMessage) {// NODIR 1305
                  
    try {
    
      final File logFolder = resolveGuiLogFolder(originalPath);
      
      final Log screenLog = new Log(APP_DESCR + " - screen log"
                                        , MoveWithShortcutsLogs.fileName(logFolder, APP_NAME + "_screen-log.LOG")
                                 , true);
                                  
      final Log userLog =   new Log(APP_DESCR + " - user log"
                                        , MoveWithShortcutsLogs.fileName(logFolder, APP_NAME + "_user-log.LOG")
                                 , true);
                                  
      final Log devLog =    new Log(APP_DESCR + " - developer log"
                                        , MoveWithShortcutsLogs.fileName(logFolder, APP_NAME + "_dev-log.LOG")
                                 , true);
                                  
      final GuiProgressReporter progressReporter = GuiProgressReporter.newInstance();
      
      final GuiTextReporter textReporter = GuiTextReporter.newInstance();
      
      final AAppContext appContext = AppContext.newAppContext(
                                         GuiUserInputOutput.newInstance(progressReporter, textReporter)
                                               , screenLog, userLog, devLog);
      if (verbosityOverride != null) {
      
        appContext.currentVerbosity = verbosityOverride.intValue();
      }
      else {
      
        appContext.currentVerbosity = MoveWithShortcutsMain.DEFAULT_VERBOSITY;
      }
      writeLogsHeaders(screenLog, userLog, devLog, APP_NAME, APP_DESCR);
      
      setSystemLookAndFeel();
      
      installOrUpdateContextMenu(appContext, createContextMenuOverride);
      
      final ShortcutsUpdaterKind updaterKind = updaterKindOverride != null ?
                                               updaterKindOverride
                                             : MoveWithShortcutsMain.selectShortcutsUpdaterForGui(appContext);
                                              
      final String configuredDefaultSearchPath = isNotBlank(defaultSearchPathArg)
                                               ? defaultSearchPathArg
                                               : ShortcutsUpdaterConfig.readConfiguredDefaultSearchPath(appContext);
                                               
      final String defaultSearchPathSource = isNotBlank(defaultSearchPathArg)
                                          ? "command-line option -" + MoveWithShortcutsMain.DEFAULT_SEARCH_OPTION_NAME
                                          : "configuration parameter " + MoveWithShortcutsParams.DEFAULT_SEARCH_PATH_PARAM_NAME;
                                          
      final SearchPathDefaults.Resolution defaultSearchPath =
                                        SearchPathDefaults.resolveDefaultSearchPath(configuredDefaultSearchPath, defaultSearchPathSource);
                                        
      final String searchPrefill = isNotBlank(searchPath) ? searchPath : defaultSearchPath.path();
      
      final String combinedWarning = combineWarnings(warningMessage, defaultSearchPath.warning());
      
      SwingUtilities.invokeLater(() -> MoveWithShortcutsGui.show(appContext,       updaterKind
                                                                      , originalPath,     destinationPath
                                                                      , searchPrefill,    shortcutFileNamePolicy
                                                                      , progressReporter, textReporter
                                                                      , combinedWarning));
    }
    catch (Exception e) {
    
      showErrorDialog(e);
      
      System.exit(ONE_i);
    }
  }
  
  /**
   * Combines two optional warning messages.
   *
   * @param first The first warning, or {@code null}.<br>
   *
   * @param second The second warning, or {@code null}.
   *
   * @return The combined warning, or {@code null}.
   */
  private static String combineWarnings(String first, String second) {
  
    final String result;
    
    if (isBlank(first)) {
    
      result = second;
    }
    else if (isBlank(second)) {
    
      result = first;
    }
    else {
    
      result = first + NL2 + second;
    }
    return result;
  }
  
  /**
   * Returns the per-instance folder where the GUI logs must be written : a {@code LOG} folder next to the JAR that
   * contains this class, falling back to a per-user folder when that folder cannot be created.
   *
   * @param originalPath The original path used in the folder name, or {@code null} when it is not known.
   *
   * @return The log folder, or {@code null} when no folder can be created.
   */
  private static File resolveGuiLogFolder(String originalPath) {
  
    File result = null;
    
    final File appFolder = applicationFolder();
    
    if (appFolder != null) {
    
      result = createLogFolder(new File(appFolder, MoveWithShortcutsLogs.LOG_FOLDER_NAME)
                                             , originalPath);
    }
    if (result == null) {
    
      final String userHome = System.getProperty("user.home");
      
      if (userHome != null) {
      
        result = createLogFolder(new File(new File(userHome, "MoveWithShortcuts")
                                                         , MoveWithShortcutsLogs.LOG_FOLDER_NAME)
                                               , originalPath);
      }
    }
    return result;
  }
  
  /**
   * Creates the per-instance log folder under the given parent log folder, when the parent is writable.
   *
   * @param logParentFolder The parent log folder.<br>
   *
   * @param originalPath The original path used in the folder name, or {@code null} when it is not known.
   *
   * @return The new folder, or {@code null}.
   */
  private static File createLogFolder(@NotNull File logParentFolder, String originalPath) {
  
    File result = null;
    
    if (ensureFolder(logParentFolder)) {
    
      result = MoveWithShortcutsLogs.createInstanceLogFolder(logParentFolder, originalPath);
    }
    return result;
  }
  
  /**
   * Returns the folder that contains the running JAR, or the class folder when running from an IDE.
   *
   * @return The application folder, or {@code null} when it cannot be determined.
   */
  private static File applicationFolder() {
  
    File result = null;
    
    final CodeSource codeSource = MoveWithShortcutsGuiMain.class.getProtectionDomain().getCodeSource();
    
    if (codeSource != null && codeSource.getLocation() != null) {
    
      try {
      
        final File location = new File(codeSource.getLocation().toURI());
        
        result = location.isFile() ? location.getParentFile() : location;
      }
      catch (URISyntaxException e) {
        
        assertNull(result);
      }
    }
    return result;
  }
  
  /**
   * Creates the given folder if needed and tells whether it is a writable folder.
   *
   * @param folder The folder to ensure.
   *
   * @return Whether the folder exists and is writable.
   */
  private static boolean ensureFolder(@NotNull File folder) {
  
    boolean result;
    
    try {
    
      Files.createDirectories(folder.toPath());
      
      result = folder.isDirectory() && folder.canWrite();
    }
    catch (IOException e) {
    
      result = false;
    }
    return result;
  }
  
  /**
   * Creates or updates the Windows Explorer context-menu integration when the configuration enables it, showing a
   * message only when it was created or updated, or an error when it failed.
   *
   * @param appContext The application context to use for the configuration and the messages.<br>
   *
   * @param createContextMenuOverride Whether to create or update the integration, or {@code null} to use the
   *        configuration.
   */
  private static void installOrUpdateContextMenu(@NotNull AAppContext appContext
                                               , Boolean createContextMenuOverride) {
                                               
    final boolean enabled = createContextMenuOverride != null ? createContextMenuOverride.booleanValue()
                                                              : ShortcutsUpdaterConfig.readCreateContextMenu(appContext);
                                                              
    if (enabled) {
    
      final ObjectAndDescr<Boolean> outcome = ExplorerContextMenu.installOrUpdate(appContext);
      
      if (outcome.description != null) {
      
        JOptionPane.showMessageDialog(null, outcome.description, APP_NAME
                                       , JOptionPane.WARNING_MESSAGE);
      }
      else if (outcome.object != null && outcome.object.booleanValue()) {
      
        JOptionPane.showMessageDialog(null, ExplorerContextMenu.INSTALLED_MESSAGE, APP_NAME
                                       , JOptionPane.INFORMATION_MESSAGE);
      }
    }
  }
  
  /**
   * Sets the system look and feel, keeping the default one when it is not available.
   */
  private static void setSystemLookAndFeel() {
  
    try {
    
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
    
      // Keep the default look and feel.
    }
  }
  
  /**
   * Shows an error dialog.
   *
   * @param e The error.
   */
  private static void showErrorDialog(@NotNull Throwable e) {
  
    JOptionPane.showMessageDialog(null, getFullDescriptionWithRootCause(e), APP_NAME
                                   , JOptionPane.ERROR_MESSAGE);
  }
  
}
