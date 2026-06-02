/**
 * Created by OpenCode on 2026-05-08 .
 */
package rename_with_links;


import dfile.shortcut.IShortcutTargetUpdater;
import dfile.shortcut.WinShortcutUpdater_PS_COM_WScript_Shell01;
import dlog.log.Log;
import duser_input_output.impl.consoleUserIO.ColorConsoleUserIO;
import dutil.exception.UserRequestedTermination;
import dutil.io.ConditionallyCloseablePrintStream;
import dutil.system.OSUtilities;
import jakarta.validation.constraints.NotNull;

import java.util.Date;

import static dfile.file.FileUtilities.getCanonicalPath;
import static dfile.file.FileUtilities.getCurrentFolder;
import static dlog.log.Log.writeLogsHeaders;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.list.text.TextListUtilities.listToString;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.TWO_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.string.TextUtilities.CHARSET_UTF_8;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.S;
import static dutil.string.TextUtilities.dq;
import static dutil.system.OSUtilities.WIN_SHORTCUT_EXTENSION;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static rename_with_links.RenameWithLinks.resolveVerbosity;


// @formatter:off


/**
 * The startup class of the {@code RenameWithLinks} application.
 */
@SuppressWarnings("PublicConstructor")
public class RenameWithLinksMain {


  /**
   * The name of this program. Short name, no description (see #APP_DESCR).
   */
  public static final String APP_NAME = "Rename With Links";
  
  /**
   * The description of this program. Description, not a Short name (see #APP_NAME).
   */
  public static final String APP_DESCR = APP_NAME + " - Renames / moves a file or folder and updates all " + WIN_SHORTCUT_EXTENSION + " shortcuts that point to it.";
  
  /**
   * The maximum allowed value for the {@link #verbosity} level.
   */
  static final int MAX_VERBOSITY = TWO_i;
  
  
  /**
   * Entry point of the RenameWithLinks program.
   *
   * @param originalArgs The command line args.
   */
  public static void main(String[] originalArgs) throws Exception {
    
    // If 3 args were given, the 4th (the verbosity) must be the default (the max), so add it as if it had been given :
    
    String[] args = originalArgs;
    
    if (args.length == 3) {
      
      args = new String[] {args[ZERO_i], args[ONE_i], args[TWO_i], S(MAX_VERBOSITY)};
    }
    // Start the execution :
    
    if (args.length == ZERO_i || args.length == 4) {

      try (
        
        final Log screenLog = new Log(APP_DESCR + " - screen log",    APP_NAME + "_screen-log.LOG", true);
  
        final Log userLog =   new Log(APP_DESCR + " - user log",      APP_NAME + "_user-log.LOG",   true);
  
        final Log devLog =    new Log(APP_DESCR + " - developer log", APP_NAME + "_dev-log.LOG",    true);
        
        final ConditionallyCloseablePrintStream out = new ConditionallyCloseablePrintStream(System.out,   true
                                                                                          , CHARSET_UTF_8, false);
        
        final ConditionallyCloseablePrintStream err = new ConditionallyCloseablePrintStream(
                                                                               System.err,   true
                                                                                           , CHARSET_UTF_8, false);
  
        final AppContext ac = AppContext.newAppContext(
                         ColorConsoleUserIO.newInstance1(System.in,         out,                 err
                                                             , CYAN,   BLACK,   RED
                                                             , BLACK, YELLOW, BLACK)
                                                                        , screenLog,          userLog,            devLog))
      {
  
        try {
          
          ac.currentVerbosity = resolveVerbosity(args.length >= 4 ? args[3] : null
                                               , MAX_VERBOSITY, ONE_i);
          
          writeLogsHeaders(ac.screenLog, ac.userLog, ac.devLog, APP_NAME, APP_DESCR);
          
          // Create the desired type of updater instance :
          
          final IShortcutTargetUpdater shortcutTargetUpdater = WinShortcutUpdater_PS_COM_WScript_Shell01.newInstance(
                                                                              false, ac.devLog);
          showStartupMessages(ac, shortcutTargetUpdater, args);
          
          final RenameWithLinks app = RenameWithLinks.newInstance(ac);
          
          // Perform the renaming operation using the chosen updater :
          
          if (args.length == ZERO_i) {
            
            app.run(shortcutTargetUpdater);
          }
          else {
            
            app.run(shortcutTargetUpdater, args[ZERO_i], args[ONE_i]
                                           , args[TWO_i],       args[3]);
          }
        }
        catch (UserRequestedTermination t) {
        
          ac.warnUser(ZERO_i, NL + (t.getMessage() != null ? t.getMessage() : "Terminated on user request."));
        }
        catch (Exception e) {
        
          ac.errUser(ZERO_i, NL2 + "Terminated due to an error : " + e.getClass().getSimpleName() + " :" + NL2T + e.getLocalizedMessage().trim() + NL2);
          
          ac.outUserLog(getFullDescriptionWithRootCause(e));
        }
        finally {
        
          ac.showLogInfo(ONE_i);
        }
      }
    }
    else {
      
      showUsage();
    }
  }
  
  /**
   * Shows the startup messages.
   *
   * @param ac                    The application context.<br>
   *
   * @param shortcutTargetUpdater The {@link IShortcutTargetUpdater} instance that this execution will use.<br>
   *
   * @param args                  The command line arguments with which this execution has been started.
   */
  private static void showStartupMessages(@NotNull AppContext ac, @NotNull IShortcutTargetUpdater shortcutTargetUpdater
                                        , @NotNull String[] args) {
  
    ac.outUser(ONE_i, NL +"Starting " + dq(APP_DESCR) + " on " + new Date() + NL);
    
    if (args.length > ZERO_i) {
      
      ac.outUser(ONE_i, NL + listToString(asList(args), "Program arguments", EMPTY, EMPTY, NL));
    }
    ac.outUser(ONE_i, NL + "Current folder : " + dq(getCurrentFolder()) + ".");
    
    ac.outUser(ONE_i, NL + "Screen log: " + getCanonicalPath(ac.screenLog.logFile));
    
    ac.outUser(ONE_i,      "  User log: " + getCanonicalPath(ac.userLog.logFile));
    
    ac.outUser(ONE_i,      "   Dev log: " + getCanonicalPath(ac.devLog.logFile));
    
    ac.outUser(ONE_i, NL + OSUtilities.getDescription());
    
    ac.outUser(ONE_i, NL + "Using " +   IShortcutTargetUpdater.class.getSimpleName()
                                           + dq(shortcutTargetUpdater.getClass().getSimpleName()) + NL2);
  }

  private static void showUsage() {

    System.out.println();

    System.out.println(APP_DESCR);

    System.out.println();

    System.out.println("Usage: RenameWithLinksMain [<originalPath> <destinationPath> <searchPath>]");

    System.out.println();

    System.out.println("  When no arguments are provided, the program prompts interactively.");

    System.out.println("  When 3 arguments are provided, they are used as the paths (no prompts).");

    System.out.println("  Any other number of arguments prints this message.");
  }
  
}
