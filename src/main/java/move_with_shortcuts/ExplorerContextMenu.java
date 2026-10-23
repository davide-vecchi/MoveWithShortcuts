/**
 * Created by Davide on 2026-10-23 .
 */
package move_with_shortcuts;


import application.AAppContext;
import dutil.system.OSUtilities;
import dutil.system.OSUtilities.RunCommandOutcome;
import dutil.value_holder.ObjectAndDescr;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static dfile.file.FileUtilities.getCanonicalPath;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.dq;
import static dutil.system.OSUtilities.assertWindowsOS;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static move_with_shortcuts.MoveWithShortcutsMain.APP_NAME;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;


// @formatter:off


/**
 * Creates or updates the per-user Windows Explorer context-menu integration for MWS.
 *
 * <p>The integration is a static registry verb, so it appears both for files and for folders. Windows 11 shows it
 * under "Show more options".</p>
 */
final class ExplorerContextMenu {


  /**
   * The registry key of the context-menu verb for files.
   */
  static final String FILES_VERB_KEY = "HKCU\\Software\\Classes\\*\\shell\\" + APP_NAME;
  
  /**
   * The registry key of the context-menu verb for folders.
   */
  static final String FOLDERS_VERB_KEY = "HKCU\\Software\\Classes\\Directory\\shell\\" + APP_NAME;
  
  /**
   * The label shown in the Explorer context menu.
   */
  static final String MENU_LABEL = "Move/rename with MWS";
  
  /**
   * The name of the registry value that limits the selection to one item.
   */
  static final String MULTI_SELECT_MODEL = "MultiSelectModel";
  
  /**
   * The value of {@link #MULTI_SELECT_MODEL}.
   */
  static final String MULTI_SELECT_MODEL_VALUE = "Single";
  
  /**
   * The fully qualified name of the GUI main class that the registry verb must launch.
   */
  static final String GUI_MAIN_CLASS = MoveWithShortcutsGuiMain.class.getName();
  
  /**
   * The message shown when the integration was created or updated.
   */
  static final String INSTALLED_MESSAGE = "The Windows Explorer integration for MWS has been installed/updated. If the"
                                        + " new context-menu entry does not appear, refresh or restart Windows Explorer.";
                                        
  /**
   * The name of the registry subkey that holds the command to execute.
   */
  private static final String COMMAND_SUBKEY = "command";
  
  /**
   * The command used to query and update the Windows registry.
   */
  private static final String REG_COMMAND = "reg";
  
  /**
   * The timeout, in milliseconds, for each registry command.
   */
  private static final long REG_TIMEOUT_MS = 30_000L;
  
  
  /**
   * Private constructor to prevent instantiation.
   */
  private ExplorerContextMenu() {}
  
  
  /**
   * Creates or updates the context-menu integration for files and folders.
   *
   * @param appContext The application context to use for logging and warnings.
   *
   * @return {@link Boolean#TRUE TRUE} in {@link ObjectAndDescr#object object} when the integration was created or
   *         updated, {@link Boolean#FALSE FALSE} when it was already up to date ; a description when a registry
   *         command failed.
   */
  static ObjectAndDescr<Boolean> installOrUpdate(@NotNull AAppContext appContext) {
  
    assertWindowsOS();
    
    final String command = buildCommand(javawPath(), currentClasspath());
    
    boolean changed = false;
    
    String error = null;
    
    for (final String key : List.of(FILES_VERB_KEY, FOLDERS_VERB_KEY)) {
    
      final ObjectAndDescr<Boolean> outcome = ensureKey(appContext, key, command);
      
      if (outcome.description != null) {
      
        error = outcome.description;
      }
      else if (TRUE.equals(outcome.object)) {
      
        changed = true;
      }
    }
    final ObjectAndDescr<Boolean> result;
    
    if (error != null) {
    
      result = new ObjectAndDescr<>(null, error);
    }
    else {
    
      result = new ObjectAndDescr<>(changed ? TRUE : FALSE, null);
    }
    return result;
  }
  
  /**
   * Builds the command line that Explorer must execute when the verb is selected.
   *
   * @param javawPath The path of the {@code javaw.exe} executable.<br>
   *
   * @param classpath The classpath that contains the MWS classes.
   *
   * @return The command line, with the dropped item's path represented by {@code "%1"}.
   */
  static String buildCommand(@NotBlank String javawPath, @NotBlank String classpath) {
  
    return dq(javawPath) + SPACE + "-cp" + SPACE + dq(classpath) + SPACE + GUI_MAIN_CLASS + SPACE + dq("%1");
  }
  
  /**
   * Builds the arguments of a {@code reg add} command.
   *
   * @param key The registry key.<br>
   *
   * @param valueName The name of the value, or {@code null} for the key's default value.<br>
   *
   * @param value The value to write.
   *
   * @return The arguments, without the program name.
   */
  static List<String> buildRegAddArgs(@NotBlank String key, String valueName, @NotBlank String value) {
  
    final List<String> result = new ArrayList<>();
    
    result.add("add");
    
    result.add(key);
    
    addValueNameArgs(result, valueName);
    
    result.add("/d");
    
    result.add(value);
    
    result.add("/f");
    return result;
  }
  
  /**
   * Builds the arguments of a {@code reg query} command.
   *
   * @param key The registry key.<br>
   *
   * @param valueName The name of the value, or {@code null} for the key's default value.
   *
   * @return The arguments, without the program name.
   */
  static List<String> buildRegQueryArgs(@NotBlank String key, String valueName) {
  
    final List<String> result = new ArrayList<>();
    
    result.add("query");
    
    result.add(key);
    
    addValueNameArgs(result, valueName);
    return result;
  }
  
  /**
   * Returns the path of the {@code javaw.exe} executable of the current JVM, falling back to a bare {@code javaw.exe}.
   *
   * @return The executable path.
   */
  static String javawPath() {
  
    final String javaHome = System.getProperty("java.home");
    
    String result = "javaw.exe";
    
    if (javaHome != null && ! javaHome.isBlank()) {
    
      final File candidate = new File(new File(javaHome, "bin"), "javaw.exe");
      
      if (candidate.isFile()) {
      
        result = candidate.getPath();
      }
    }
    return result;
  }
  
  /**
   * Returns the classpath of the current JVM, preferring the canonical path of the JAR that contains this class.
   *
   * @return The classpath.
   */
  static String currentClasspath() {
  
    String result = null;
    
    final CodeSource codeSource = ExplorerContextMenu.class.getProtectionDomain().getCodeSource();
    
    if (codeSource != null && codeSource.getLocation() != null) {
    
      try {
      
        final File location = new File(codeSource.getLocation().toURI());
        
        if (location.isFile()) {
        
          result = getCanonicalPath(location);
        }
      }
      catch (URISyntaxException ignored) {
      
        result = null;
      }
    }
    if (result == null) {
    
      final String classPath = System.getProperty("java.class.path");
      
      result = classPath == null ? EMPTY : classPath;
    }
    return result;
  }
  
  /**
   * Ensures that one verb key has the expected label, multi-select model and command.
   *
   * @param appContext The application context to use for logging and warnings.<br>
   *
   * @param key The verb key.<br>
   *
   * @param command The command line to store.
   *
   * @return {@link Boolean#TRUE TRUE} in {@link ObjectAndDescr#object object} when the key was written, {@link
   *         Boolean#FALSE FALSE} when it was already up to date ; a description when a registry command failed.
   */
  private static ObjectAndDescr<Boolean> ensureKey(@NotNull AAppContext appContext, @NotBlank String key
                                                 , @NotBlank String command) {
                                                 
    final boolean upToDate = isValueUpToDate(appContext, key, null, MENU_LABEL)
                          && isValueUpToDate(appContext, key, MULTI_SELECT_MODEL, MULTI_SELECT_MODEL_VALUE)
                          && isValueUpToDate(appContext, key + "\\" + COMMAND_SUBKEY, null, command);
                          
    ObjectAndDescr<Boolean> result = new ObjectAndDescr<>(FALSE, null);
    
    if (! upToDate) {
    
      result = addKeyValues(appContext, key, command);
    }
    return result;
  }
  
  /**
   * Writes the expected label, multi-select model and command of one verb key.
   *
   * @param appContext The application context to use for logging and warnings.<br>
   *
   * @param key The verb key.<br>
   *
   * @param command The command line to store.
   *
   * @return {@link Boolean#TRUE TRUE} in {@link ObjectAndDescr#object object} when all values were written ; a
   *         description when a registry command failed.
   */
  private static ObjectAndDescr<Boolean> addKeyValues(@NotNull AAppContext appContext, @NotBlank String key
                                                    , @NotBlank String command) {
                                                    
    String error = addValue(appContext, key, null, MENU_LABEL);
    
    if (error == null) {
    
      error = addValue(appContext, key, MULTI_SELECT_MODEL, MULTI_SELECT_MODEL_VALUE);
    }
    if (error == null) {
    
      error = addValue(appContext, key + "\\" + COMMAND_SUBKEY, null, command);
    }
    final ObjectAndDescr<Boolean> result;
    
    if (error == null) {
    
      result = new ObjectAndDescr<>(TRUE, null);
    }
    else {
    
      result = new ObjectAndDescr<>(null, error);
    }
    return result;
  }
  
  /**
   * Writes one registry value.
   *
   * @param appContext The application context to use for logging and warnings.<br>
   *
   * @param key The registry key.<br>
   *
   * @param valueName The name of the value, or {@code null} for the key's default value.<br>
   *
   * @param value The value to write.
   *
   * @return {@code null} on success, or a human-readable error description.
   */
  private static String addValue(@NotNull AAppContext appContext, @NotBlank String key, String valueName
                               , @NotBlank String value) {
                               
    final RunCommandOutcome outcome = runReg(appContext, buildRegAddArgs(key, valueName, value));
    
    String result = null;
    
    if (outcome == null) {
    
      result = "Could not run the Windows registry command.";
    }
    else if (outcome.exitValue() != ZERO_i) {
    
      result = "The Windows registry command failed with exit value " + outcome.exitValue() + " :" + NL
             + outcome.processOutput();
    }
    return result;
  }
  
  /**
   * Tells whether one registry value is already equal to the expected one.
   *
   * @param appContext The application context to use for logging and warnings.<br>
   *
   * @param key The registry key.<br>
   *
   * @param valueName The name of the value, or {@code null} for the key's default value.<br>
   *
   * @param expected The expected value.
   *
   * @return Whether the value is already up to date.
   */
  private static boolean isValueUpToDate(@NotNull AAppContext appContext, @NotBlank String key, String valueName
                                       , @NotBlank String expected) {
                                       
    final RunCommandOutcome outcome = runReg(appContext, buildRegQueryArgs(key, valueName));
    
    final boolean result = outcome != null && outcome.exitValue() == ZERO_i
                        && outcome.processOutput() != null
                        && outcome.processOutput().contains(expected);
    return result;
  }
  
  /**
   * Runs a {@code reg} command, capturing its output.
   *
   * @param appContext The application context to use for logging and warnings.<br>
   *
   * @param args The arguments, without the program name.
   *
   * @return The outcome, or {@code null} if the command could not be run.
   */
  private static RunCommandOutcome runReg(@NotNull AAppContext appContext, @NotNull List<String> args) {
  
    RunCommandOutcome result;
    
    try {
    
      result = OSUtilities.runCommand(null, REG_COMMAND, REG_TIMEOUT_MS, appContext.devLog
                                    , Function.identity(), null, null, null, args.toArray(String[]::new));
    }
    catch (InterruptedException e) {
    
      Thread.currentThread().interrupt();
      
      result = logRegistryFailure(appContext, e);
    }
    catch (IOException | TimeoutException e) {
    
      result = logRegistryFailure(appContext, e);
    }
    return result;
  }
  
  /**
   * Logs a registry command failure and returns {@code null}.
   *
   * @param appContext The application context to use for logging and warnings.<br>
   *
   * @param e The failure.
   *
   * @return Always {@code null}.
   */
  private static RunCommandOutcome logRegistryFailure(@NotNull AAppContext appContext
                                                     , @NotNull Exception e) {
                                                     
    appContext.warnUser(NL + "Could not run the Windows registry command : " + e.getLocalizedMessage());
    
    return null;
  }
  
  /**
   * Adds either the {@code /ve} flag or the {@code /v <name>} arguments to the given command arguments.
   *
   * @param args The command arguments to add to.<br>
   *
   * @param valueName The name of the value, or {@code null} for the key's default value.
   */
  private static void addValueNameArgs(@NotNull List<String> args, String valueName) {
  
    if (valueName == null) {
    
      args.add("/ve");
    }
    else {
    
      args.add("/v");
      
      args.add(valueName);
    }
  }
}
