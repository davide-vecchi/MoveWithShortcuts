/**
 * Created by Davide on 2026-10-23 .
 */
package move_with_shortcuts;


import application.AAppContext;
import dfile.shortcut.IShortcutsUpdater;
import dlog.log.Log;
import duser_input_output.impl.NeutralUserInputOutput;
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
import static dutil.system.OSUtilities.setSystemEncodingUTF8;
import static move_with_shortcuts.MoveWithShortcutsMain.APP_DESCR;
import static move_with_shortcuts.MoveWithShortcutsMain.APP_NAME;


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
    
    try {
    
      final File logFolder = resolveGuiLogFolder();
      
      final Log screenLog = new Log(APP_DESCR + " - screen log"
                                  , logFileName(logFolder, APP_NAME + "_screen-log.LOG"), true);
                                  
      final Log userLog =   new Log(APP_DESCR + " - user log"
                                  , logFileName(logFolder, APP_NAME + "_user-log.LOG"), true);
                                  
      final Log devLog =    new Log(APP_DESCR + " - developer log"
                                  , logFileName(logFolder, APP_NAME + "_dev-log.LOG"), true);
                                  
      final AAppContext appContext = AppContext.newAppContext(NeutralUserInputOutput.newInstance()
                                                                    , screenLog, userLog, devLog);
                                                                    
      writeLogsHeaders(screenLog, userLog, devLog, APP_NAME, APP_DESCR);
      
      setSystemLookAndFeel();
      
      installOrUpdateContextMenu(appContext);
      
      final IShortcutsUpdater updater = MoveWithShortcutsMain.selectShortcutsUpdaterForGui(appContext)
                                                             .newUpdater(appContext);
                                                             
      SwingUtilities.invokeLater(() -> MoveWithShortcutsGui.show(appContext, updater, originalPath));
    }
    catch (Exception e) {
    
      showErrorDialog(e);
      
      System.exit(ONE_i);
    }
  }
  
  /**
   * Returns the folder where the GUI logs must be written : a {@code LOG} folder next to the JAR that contains this
   * class, falling back to a per-user folder when that folder cannot be created.
   *
   * @return The log folder, or {@code null} when no folder can be created.
   */
  private static File resolveGuiLogFolder() {
  
    File result = null;
    
    final File appFolder = applicationFolder();
    
    if (appFolder != null) {
    
      final File appLogFolder = new File(appFolder, "LOG");
      
      if (ensureFolder(appLogFolder)) {
      
        result = appLogFolder;
      }
    }
    if (result == null) {
    
      final String userHome = System.getProperty("user.home");
      
      if (userHome != null) {
      
        final File userLogFolder = new File(new File(userHome, "MoveWithShortcuts"), "LOG");
        
        if (ensureFolder(userLogFolder)) {
        
          result = userLogFolder;
        }
      }
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
      
        result = null;
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
   * Returns the path to use for one GUI log file.
   *
   * @param logFolder The log folder, or {@code null} to use a bare file name.<br>
   *
   * @param fileName The log file name.
   *
   * @return The log file path.
   */
  private static String logFileName(File logFolder, @NotNull String fileName) {
  
    return logFolder == null ? fileName : new File(logFolder, fileName).getPath();
  }
  
  /**
   * Creates or updates the Windows Explorer context-menu integration when the configuration enables it, showing a
   * message only when it was created or updated, or an error when it failed.
   *
   * @param appContext The application context to use for the configuration and the messages.
   */
  private static void installOrUpdateContextMenu(@NotNull AAppContext appContext) {
  
    if (ShortcutsUpdaterConfig.readCreateContextMenu(appContext)) {
    
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
