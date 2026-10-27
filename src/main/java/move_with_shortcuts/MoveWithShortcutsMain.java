/**
 * Created by Davide on 2026-05-08 .
 */
package move_with_shortcuts;


import application.AAppContext;
import dcommandlinearg.CommandLineArgException;
import dcommandlinearg.DCommandLineSpec;
import dcommandlinearg.OptionSpec;
import dcommandlinearg.ParsedArgs;
import dfile.shortcut.IShortcutsUpdater;
import dfile.shortcut.ShortcutFileNamePolicy;
import dlog.log.Log;
import duser_input_output.impl.consoleUserIO.ColorConsoleUserIO;
import dutil.exception.UserRequestedTermination;
import dutil.io.ConditionallyCloseablePrintStream;
import dutil.system.OSUtilities;
import dutil.value_holder.ObjectAndDescr;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Date;

import static application.AAppContext.MAX_VERBOSITY;
import static dfile.file.FileUtilities.getCurrentFolder;
import static dlog.log.Log.writeLogsHeaders;
import static duser_input_output.AUserInputOutput.calcCancelCharsPrompt;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.list.text.TextListUtilities.listToString;
import static dutil.number.NumberUtilities.I;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.object.ObjectUtilities.B;
import static dutil.string.TextUtilities.CHARSET_UTF_8;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.S;
import static dutil.string.TextUtilities.dq;
import static dutil.system.OSUtilities.WIN_SHORTCUT_EXTENSION;
import static dutil.system.OSUtilities.setSystemEncodingUTF8;
import static java.util.Arrays.asList;
import static move_with_shortcuts.MoveWithShortcuts.CANCEL_CHARS;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;


// @formatter:off


/**
 * The startup class of the {@code MoveWithShortcuts} application.
 */
public final class MoveWithShortcutsMain {


  /**
   * Private constructor to prevent instantiation.
   */
  private MoveWithShortcutsMain() {}
  
  /**
   * The name of this program. Short name, no description (see #APP_DESCR).
   */
  public static final String APP_NAME = "MoveWithShortcuts";
  
  /**
   * The description of this program. Description, not a Short name (see #APP_NAME).
   */
  public static final String APP_DESCR = APP_NAME + " - Renames / moves a file or folder and updates all " + WIN_SHORTCUT_EXTENSION + " shortcuts accordingly.";
  
  /**
   * The DEFAULT value for the {@link AAppContext#currentVerbosity verbosity} level if not specified.
   */
  public static final int DEFAULT_VERBOSITY = ONE_i;
  
  /**
   * The name of the option that specifies the original file or folder path.
   */
  static final String FROM_OPTION_NAME = "from";
  
  /**
   * The name of the option that specifies the destination path.
   */
  static final String TO_OPTION_NAME = "to";
  
  /**
   * The name of the option that specifies the folder to scan for shortcuts.
   */
  static final String SEARCH_OPTION_NAME = "search";
  
  /**
   * The name of the option that starts the console mode.
   */
  static final String CONSOLE_OPTION_NAME = "console";
  
  /**
   * The name of the option that starts the GUI mode.
   */
  static final String GUI_OPTION_NAME = "gui";
  
  /**
   * The name of the option that controls the Explorer context-menu integration.
   */
  static final String CREATE_CONTEXT_MENU_OPTION_NAME = "create-context-menu";
  
  /**
   * The name of the option that sets the verbosity level.
   */
  static final String VERBOSITY_OPTION_NAME = "verbosity";
  
  /**
   * The name of the option that selects the shortcut-file name policy.
   */
  static final String SHORTCUT_FILE_NAME_OPTION_NAME = "shortcut-file-name";
  
  /**
   * The name of the option that selects the shortcuts updater implementation.
   */
  static final String SHORTCUTS_UPDATER_OPTION_NAME = "shortcuts-updater";
  
  
  /**
   * Entry point of the MoveWithShortcuts program.
   *
   * @param originalArgs The command line args, as named options; see {@link #getUsageText()} for the full list.
   */
  public static void main(String[] originalArgs) throws Exception {
  
    setSystemEncodingUTF8();
    
    final DCommandLineSpec argsSpec = newArgsSpec();
    
    boolean consoleFlag = false;
    
    boolean guiFlag = false;
    
    for (final String arg : originalArgs) {
    
      if (isOption(arg, CONSOLE_OPTION_NAME)) {
      
        consoleFlag = true;
      }
      else if (isOption(arg, GUI_OPTION_NAME)) {
      
        guiFlag = true;
      }
    }
    if (consoleFlag && guiFlag) {
    
      System.out.println(NL + "The options -" + CONSOLE_OPTION_NAME + " and -" + GUI_OPTION_NAME
                              + " cannot be used together.");
                              
      printUsage(argsSpec);
      
      System.exit(ONE_i);
    }
    final boolean configConsole = consoleFlag || guiFlag ? false : ShortcutsUpdaterConfig.readConsole();
    
    final boolean consoleMode = isConsoleRequested(consoleFlag, guiFlag, configConsole);
    
    if (consoleMode) {
    
      final ParsedArgs args = originalArgs.length == ZERO_i ? null : parseArgs(argsSpec, originalArgs);
      
      runConsole(args, originalArgs);
    }
    else {
    
      startGui(argsSpec, originalArgs);
    }
  }
  
  /**
   * Starts the GUI, or falls back to the console in a headless environment.
   *
   * @param argsSpec The command-line specification.<br>
   *
   * @param originalArgs The command line args.
   *
   * @throws Exception If the console fallback fails.
   */
  private static void startGui(@NotNull DCommandLineSpec argsSpec, @NotNull String[] originalArgs)
                                                                                       throws Exception {
                                                                                       
    ParsedArgs args = null;
    
    String warning = null;
    
    try {
    
      args = argsSpec.parsePartial(originalArgs);
      
      if (args.isHelpRequested()) {
      
        printUsage(argsSpec);
        
        System.exit(ZERO_i);
        
        return;
      }
    }
    catch (CommandLineArgException e) {
    
      final String detail = e.getLocalizedMessage();
      
      warning = "Some command-line values were invalid and were not prefilled"
              + (isBlank(detail) ? "." : ": " + detail);
    }
    if (GraphicsEnvironment.isHeadless()) {
    
      System.out.println(NL + "Headless environment: the GUI cannot be started, running in console mode.");
      
      final ParsedArgs consoleArgs;
      
      if (originalArgs.length == ZERO_i) {
      
        consoleArgs = null;
      }
      else {
      
        consoleArgs = args != null ? args : parseArgs(argsSpec, originalArgs);
      }
      runConsole(consoleArgs, originalArgs);
    }
    else {
    
      final String originalPath =    args != null && args.wasGiven(FROM_OPTION_NAME)
                                                   ? args.getFile(FROM_OPTION_NAME).getPath()    : null;
                                                   
      final String destinationPath = args != null && args.wasGiven( TO_OPTION_NAME)
                                                   ? args.getString(TO_OPTION_NAME)              : null;
                                                   
      final String searchPath =      args != null && args.wasGiven(SEARCH_OPTION_NAME)
                                                   ? args.getFile( SEARCH_OPTION_NAME).getPath() : null;
                                                   
      final ShortcutsUpdaterKind updaterKind = args != null &&               args.wasGiven(SHORTCUTS_UPDATER_OPTION_NAME)
                                               ? ShortcutsUpdaterKind.parse(
                                                                        args.getString(SHORTCUTS_UPDATER_OPTION_NAME))
                                               : null;
                                               
      final ShortcutFileNamePolicy policy = args != null &&                args.wasGiven( SHORTCUT_FILE_NAME_OPTION_NAME)
                                            ? ShortcutFileNamePolicy.parse(
                                                                    args.getString(SHORTCUT_FILE_NAME_OPTION_NAME))
                                            : ShortcutFileNamePolicy.NEVER;
                                            
      final Boolean createContextMenu =     args != null &&   args.wasGiven( CREATE_CONTEXT_MENU_OPTION_NAME)
                                            ? B(args.getString(CREATE_CONTEXT_MENU_OPTION_NAME)) : null;
                                            
      final Integer verbosity =             args != null &&   args.wasGiven(VERBOSITY_OPTION_NAME)
                                            ? I(     args.getInt(  VERBOSITY_OPTION_NAME)) : null;
                                            
      MoveWithShortcutsGuiMain.start(originalPath, destinationPath, searchPath, updaterKind, policy, createContextMenu
                                   , verbosity,    warning);
    }
  }
  
  /**
   * Runs the console flow.
   *
   * @param args The parsed arguments, or {@code null} in zero-arg mode.<br>
   *
   * @param originalArgs The command line args.
   *
   * @throws Exception If the console flow fails.
   */
  private static void runConsole(ParsedArgs args, @NotNull String[] originalArgs) throws Exception {
  
    // In the help / bad-args case parseArgs already printed the usage and set the exit code, and never returns :
    
    if (args == null && originalArgs.length > ZERO_i) {
    
      return;
    }
    // Start the execution :
    
    final File logFolder = MoveWithShortcutsLogs.createInstanceLogFolder(
                                      new File(getCurrentFolder(), MoveWithShortcutsLogs.LOG_FOLDER_NAME)
                                    , originalPathForLogs(args));
                                    
    try (
    
      final Log screenLog = new Log(APP_DESCR + " - screen log"
                                  , MoveWithShortcutsLogs.fileName(logFolder, APP_NAME + "_screen-log.LOG"), true);
                                  
      final Log userLog =   new Log(APP_DESCR + " - user log"
                                  , MoveWithShortcutsLogs.fileName(logFolder, APP_NAME + "_user-log.LOG"), true);
                                  
      final Log devLog =    new Log(APP_DESCR + " - developer log"
                                  , MoveWithShortcutsLogs.fileName(logFolder, APP_NAME + "_dev-log.LOG"), true);
                                  
      final ConditionallyCloseablePrintStream out = new ConditionallyCloseablePrintStream(
                                                                                          System.out,   true
                                                                                        , CHARSET_UTF_8, false);
                                                                                        
      final ConditionallyCloseablePrintStream err = new ConditionallyCloseablePrintStream(
                                                                             System.err,   true
                                                                                        , CHARSET_UTF_8, false);
      final AAppContext ac = AppContext.newAppContext(
                       ColorConsoleUserIO.newInstance1(System.in,         out,                 err
                                                           , CYAN,   BLACK,   RED
                                                           , BLACK, YELLOW, BLACK)
                                                                      , screenLog,          userLog,            devLog))
      {
      
        final MoveWithShortcuts app = MoveWithShortcuts.newInstance(ac);
        
        try {
        
          ac.currentVerbosity = args != null ? args.getInt(VERBOSITY_OPTION_NAME) : DEFAULT_VERBOSITY;
          
          final ShortcutFileNamePolicy shortcutFileNamePolicy = args != null ? ShortcutFileNamePolicy.parse(
                                                                          args.getString(
                                                                                          SHORTCUT_FILE_NAME_OPTION_NAME))
                                                                             : ShortcutFileNamePolicy.NEVER;
                                                                             
          writeLogsHeaders(ac.screenLog, ac.userLog, ac.devLog, APP_NAME, APP_DESCR);
          
          installOrUpdateContextMenuIfEnabled(args, ac);
          
          
          // -------------------------------------------------------
          // Create the selected type of shortcuts updater instance :
          
          final ShortcutsUpdaterKind shortcutsUpdaterKind = selectShortcutsUpdater(args, ac);
          
          final IShortcutsUpdater shortcutsTargetUpdater = shortcutsUpdaterKind.newUpdater(ac);
          
          // -------------------------------------------------------
          
          
          showStartupMessages(ac, shortcutsTargetUpdater, originalArgs, shortcutFileNamePolicy);
          
          // Perform the renaming operation using the chosen updater :
          
          if (args == null) {
          
            app.run(shortcutsTargetUpdater, shortcutFileNamePolicy);
          }
          else {
          
            app.run(shortcutsTargetUpdater, args, shortcutFileNamePolicy);
          }
        }
        catch (UserRequestedTermination t) {
        
          ac.warnUser(ZERO_i, NL + (t.getMessage() != null ? t.getMessage() : "Terminated on user request."));
        }
        catch (Exception e) {
        
          ac.errUser(ZERO_i, NL2 + "Terminated due to an error : " + e.getClass().getSimpleName() + " :" + NL2T + e.getLocalizedMessage() + NL2);
          
          ac.outUserLog(getFullDescriptionWithRootCause(e));
        }
        finally {
        
          ac.outUser(NL + "Total shortcuts to process :     " + app.numTotalShortcuts     + " .");
          
          ac.outUser(NL + "Total shortcuts needing update : " + app.numProcessedShortcuts + " .");
          
          ac.outUser(NL + "Total shortcuts updated :        " + app.numUpdatedShortcuts   + " .");
          
          ac.outUser(NL + "Total shortcut files renamed :   " + app.numRenamedShortcuts + " .");
          
          ac.outUser(NL + "Total shortcut-file rename collisions : " + app.numRenameCollisions + " .");
          
          ac.outUser(NL + "Total shortcut-file renames skipped : " + app.numRenameSkippedShortcuts + " .");
          
          ac.outUser(NL + "Total shortcuts skipped :        " + app.numSkippedShortcuts   + " .");
          
          ac.showLogInfo(ONE_i);
        }
      }
  }
  
  /**
   * Tells whether the given argument is the given option, with one or two leading hyphens.
   *
   * @param arg The argument.<br>
   *
   * @param optionName The option name, without the hyphens.
   *
   * @return Whether it is the option.
   */
  private static boolean isOption(@NotNull String arg, @NotBlank String optionName) {
  
    return arg.equalsIgnoreCase("-" + optionName) || arg.equalsIgnoreCase("--" + optionName);
  }
  
  /**
   * Tells whether console mode must be used.
   *
   * @param consoleFlag Whether the {@link #CONSOLE_OPTION_NAME "start as console" arg} was given.<br>
   *
   * @param guiFlag Whether the {@link #GUI_OPTION_NAME "start as GUI" arg} was given.<br>
   *
   * @param configConsole Whether the configuration selects console mode.
   *
   * @return Whether console mode must be used.
   */
  static boolean isConsoleRequested(boolean consoleFlag, boolean guiFlag, boolean configConsole) {
  
    return consoleFlag || (! guiFlag && configConsole);
  }
  
  /**
   * Returns the command-line usage text.
   *
   * @return The usage text.
   */
  static String getUsageText() {
  
    return formatUsageText(newArgsSpec().getUsageText());
  }
  
  /**
   * Formats the usage text with one empty line before each option description.
   *
   * @param usage The raw usage text.
   *
   * @return The formatted usage text.
   */
  static String formatUsageText(@NotNull String usage) {
  
    final StringBuilder result = new StringBuilder();
    
    boolean previousBlank = false;
    
    for (final String line : usage.lines().toList()) {
    
      final boolean optionLine = line.startsWith("  -");
      
      if (optionLine && ! previousBlank) {
      
        result.append(NL);
      }
      result.append(line).append(NL);
      
      previousBlank = line.isBlank();
    }
    return result.toString();
  }
  
  /**
   * Prints the formatted usage text.
   *
   * @param argsSpec The command-line specification.
   */
  private static void printUsage(@NotNull DCommandLineSpec argsSpec) {
  
    System.out.println(formatUsageText(argsSpec.getUsageText()));
  }
  
  /**
   * Builds the specification of this program's named command-line arguments.
   *
   * @return the command-line specification
   */
  private static DCommandLineSpec newArgsSpec() {
  
    return DCommandLineSpec.newSpec(APP_NAME, APP_DESCR)
                           .addOption(OptionSpec.option(FROM_OPTION_NAME
                                                       , "The path of the file or folder to rename / move, or the path it"
                                                       + " had before if the operation was already done elsewhere, in"
                                                       + " which case no rename / move will be done and only the shortcuts"
                                                       + " adjustment will be done.")
                                               .fileOrFolderPath())
                           .addOption(OptionSpec.option(TO_OPTION_NAME
                                                       , "The new name or path for the file or folder, or the path it has"
                                                       + " now if the operation was already done elsewhere, in which case no"
                                                       + " rename / move will be done and only the shortcuts adjustment will"
                                                       + " be done. Exactly one of -" + FROM_OPTION_NAME + " and -"
                                                       + TO_OPTION_NAME + " must exist, the other must"
                                                       + " not."))
                           .addOption(OptionSpec.option(SEARCH_OPTION_NAME
                                                       , "The folder to scan for " + WIN_SHORTCUT_EXTENSION + " shortcuts to update.")
                                               .existingFolder())
                           .addOption(OptionSpec.option(SHORTCUT_FILE_NAME_OPTION_NAME
                                                       , "The shortcut file name policy : " + ShortcutFileNamePolicy.getDescription()
                                                       + " (default " + ShortcutFileNamePolicy.NEVER.descr + "). "
                                                       + ShortcutFileNamePolicy.EXACT.descr + " / " + ShortcutFileNamePolicy.DESCENDANT.descr + " rename a shortcut file"
                                                       + " named after the old leaf, preserving the Windows default"
                                                       + " \" - Shortcut\" part.")
                                               .allowedValues("never", "exact", "descendant")
                                               .defaultValue("never"))
                           .addOption(OptionSpec.option(SHORTCUTS_UPDATER_OPTION_NAME
                                                       , "The shortcuts updater implementation to use. Default: "
                                                       + ShortcutsUpdaterKind.defaultKind().getId() + ".")
                                               .allowedValues(ShortcutsUpdaterKind.ids()))
                           .addOption(OptionSpec.option(CREATE_CONTEXT_MENU_OPTION_NAME
                                                       , "Whether to create or update the Windows Explorer"
                                                       + " right-click context-menu integration at startup (default true).")
                                               .allowedValues("true", "false")
                                               .defaultValue("true"))
                           .addOption(OptionSpec.option(CONSOLE_OPTION_NAME
                                                       , "Starts in console mode instead of the default GUI"
                                                       + " mode. In console mode the operation options are"
                                                       + " required unless prompted. Cannot be combined with -"
                                                       + GUI_OPTION_NAME + ".").flag())
                           .addOption(OptionSpec.option(GUI_OPTION_NAME
                                                       , "Starts in GUI mode, overriding Console=true in the"
                                                       + " configuration file. In GUI mode the operation"
                                                       + " options are optional prefills. Cannot be combined"
                                                       + " with -" + CONSOLE_OPTION_NAME + ".").flag())
                           .addOption(OptionSpec.option(VERBOSITY_OPTION_NAME
                                                       , "The verbosity level (0-" + MAX_VERBOSITY + ")")
                                               .intRange(ZERO_i, MAX_VERBOSITY).defaultValue(S(DEFAULT_VERBOSITY)));
                                               
  }
  
  /**
   * Parses the given (non-empty) command-line arguments against this program's specification ; on a help request or
   * an argument error, prints the usage text, sets the exit code (0 for help, 1 for an error) and returns {@code
   * null} — after {@code System.exit}, so nothing follows in practice.
   *
   * @param argsSpec      The command-line specification, see {@link #newArgsSpec()}.<br>
   *
   * @param originalArgs  The command line args, not empty.
   *
   * @return The parsed args, or {@code null} after printing the usage text.
   */
  private static ParsedArgs parseArgs(@NotNull DCommandLineSpec argsSpec, @NotNull String[] originalArgs) {
  
    ParsedArgs args;
    
    try {
    
      args = argsSpec.parsePartial(originalArgs);
      
      if (args.isHelpRequested()) {
      
        printUsage(argsSpec);
        System.exit(ZERO_i);
        args = null;
      }
    }
    catch (CommandLineArgException e) {
    
      System.out.println(e.getMessage());
      printUsage(argsSpec);
      System.exit(ONE_i);
      args = null;
    }
    return args;
    
  }
  
  /**
   * Shows the startup messages.
   *
   * @param ac                     The application context.<br>
   *
   * @param shortcutsTargetUpdater The {@link IShortcutsUpdater} instance that this execution will use.<br>
   *
   * @param args                   The command line arguments with which this execution has been started.<br>
   *
   * @param shortcutFileNamePolicy The {@link ShortcutFileNamePolicy} selected for this execution.
   */
  private static void showStartupMessages(@NotNull AAppContext ac,  @NotNull IShortcutsUpdater shortcutsTargetUpdater
                                        , @NotNull String[]    args
                                        , @NotNull ShortcutFileNamePolicy shortcutFileNamePolicy) {
                                        
    ac.outUser(ONE_i, NL +"Starting " + dq(APP_DESCR) + " on " + new Date());
    
    if (args.length > ZERO_i) {
    
      ac.outUser(ONE_i, NL + listToString(asList(args), "Program arguments", EMPTY, EMPTY, NL2));
    }
    ac.outUser(ONE_i, NL + "Current folder : " + dq(getCurrentFolder()) + ".");
    
    ac.showLogInfo(ONE_i);
    
    ac.outUser(ONE_i, NL + OSUtilities.getDescription());
    
    ac.outUser(ONE_i, NL + "Using "    + IShortcutsUpdater.class.getSimpleName() + " implementation "
                                           + dq(shortcutsTargetUpdater.getDescription()) + ".");
                                           
    ac.outUser(ONE_i, NL + "Current verbosity (0-" + MAX_VERBOSITY + ") : " + ac.currentVerbosity + " .");
    
    ac.outUser(ONE_i, NL + "Shortcut file name policy : " + shortcutFileNamePolicy + " .");
    
    ac.outUser(ONE_i, NL);
  }
  
  /**
   * Returns the original path to use in the per-instance log folder name, when it is given on the command line.
   *
   * @param args The parsed arguments, or {@code null} in zero-arg mode.
   *
   * @return The original path, or {@code null} when it is not known.
   */
  private static String originalPathForLogs(ParsedArgs args) {
  
    return args != null && args.wasGiven(FROM_OPTION_NAME) ? args.getFile(FROM_OPTION_NAME).getPath() : null;
  }
  
  /**
   * Creates or updates the Windows Explorer context-menu integration when it is enabled, and tells the user when it
   * was created or updated.
   *
   * @param args The parsed arguments, or {@code null} in zero-arg mode.<br>
   *
   * @param appContext The application context to use for the configuration and the messages.
   */
  private static void installOrUpdateContextMenuIfEnabled(ParsedArgs args, @NotNull AAppContext appContext) {
  
    final boolean enabled = args != null && args.wasGiven(CREATE_CONTEXT_MENU_OPTION_NAME)
                            ? Boolean.parseBoolean(args.getString(CREATE_CONTEXT_MENU_OPTION_NAME))
                            : ShortcutsUpdaterConfig.readCreateContextMenu(appContext);
                            
    if (enabled) {
    
      final ObjectAndDescr<Boolean> outcome = ExplorerContextMenu.installOrUpdate(appContext);
      
      if (outcome.description != null) {
      
        appContext.warnUser(NL + outcome.description);
      }
      else if (outcome.object != null && outcome.object.booleanValue()) {
      
        appContext.outUser(ONE_i, NL + ExplorerContextMenu.INSTALLED_MESSAGE);
      }
    }
  }
  
  /**
   * Selects the shortcuts updater implementation : command line, then configuration file, then an interactive prompt
   * (zero-arg mode only), then the default.
   *
   * @param args The parsed arguments, or {@code null} in zero-arg mode.<br>
   *
   * @param appContext The application context to use for configuration and warnings.
   *
   * @return The selected kind.
   */
  static ShortcutsUpdaterKind selectShortcutsUpdater(ParsedArgs args, @NotNull AAppContext appContext) throws UserRequestedTermination {
  
    ShortcutsUpdaterKind result;
    
    if (args != null && args.wasGiven(SHORTCUTS_UPDATER_OPTION_NAME)) {
    
      result = ShortcutsUpdaterKind.parse(args.getString(SHORTCUTS_UPDATER_OPTION_NAME));
    }
    else {
    
      result = configuredUpdaterOrNull(appContext);
      
      if (result == null) {
      
        result = args == null && ShortcutsUpdaterKind.ids().length > ONE_i ? askShortcutsUpdater(appContext)
                                                                           : ShortcutsUpdaterKind.defaultKind();
      }
    }
    return result;
  }
  
  /**
   * Selects the shortcuts updater implementation for the GUI : the configured one, or the default. It never prompts,
   * because the GUI has no console.
   *
   * @param appContext The application context to use for the configuration and warnings.
   *
   * @return The selected kind.
   */
  static ShortcutsUpdaterKind selectShortcutsUpdaterForGui(@NotNull AAppContext appContext) {
  
    ShortcutsUpdaterKind result = configuredUpdaterOrNull(appContext);
    
    if (result == null) {
    
      result = ShortcutsUpdaterKind.defaultKind();
    }
    return result;
  }
  
  /**
   * Returns the shortcuts updater implementation selected in the configuration file, or {@code null} when it is
   * missing or invalid.
   *
   * @param appContext The application context to use for the configuration and warnings.
   *
   * @return The configured kind, or {@code null}.
   */
  private static ShortcutsUpdaterKind configuredUpdaterOrNull(@NotNull AAppContext appContext) {
  
    final String configuredId = ShortcutsUpdaterConfig.readConfiguredUpdaterId(appContext);
    
    ShortcutsUpdaterKind result = null;
    
    if (configuredId != null && ! configuredId.isBlank()) {
    
      try {
      
        result = ShortcutsUpdaterKind.parse(configuredId);
      }
      catch (IllegalArgumentException e) {
      
        appContext.warnUser(NL + e.getLocalizedMessage() + NL2T + "Using the default shortcuts updater.");
      }
    }
    return result;
  }
  
  /**
   * Asks the user which shortcuts updater implementation to use.
   *
   * @param appContext The application context to use for the prompt and warnings.
   *
   * @return The selected kind.
   *
   * @throws UserRequestedTermination If the user cancels.
   */
  private static ShortcutsUpdaterKind askShortcutsUpdater(@NotNull AAppContext appContext)
                                                          throws UserRequestedTermination {
                                                          
    final StringBuilder prompt = new StringBuilder("Choose the shortcuts updater implementation, or type ")
                                          .append(calcCancelCharsPrompt(CANCEL_CHARS))
                                          .append(" :");
                                          
    for (final ShortcutsUpdaterKind kind : ShortcutsUpdaterKind.values()) {
    
      prompt.append(NL2T).append(kind.getId()).append(" : ").append(kind.getDescription(appContext));
    }
    prompt.append(NL2T).append("Press Enter to use ").append(ShortcutsUpdaterKind.defaultKind().getId()).append(".");
    
    ShortcutsUpdaterKind result = null;
    
    while (result == null) {
    
      final String answer = appContext.userIO.in(prompt.toString(), EMPTY, CANCEL_CHARS);
      
      if (answer == null) {
      
        throw new UserRequestedTermination();
      }
      if (answer.isBlank()) {
      
        result = ShortcutsUpdaterKind.defaultKind();
      }
      else {
      
        try {
        
          result = ShortcutsUpdaterKind.parse(answer);
        }
        catch (IllegalArgumentException e) {
        
          appContext.warnUser(NL + e.getLocalizedMessage());
        }
      }
    }
    return result;
  }
  
}
