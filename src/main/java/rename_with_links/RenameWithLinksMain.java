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
import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.util.Date;

import static dfile.file.FileUtilities.getCanonicalPath;
import static dlog.log.Log.writeLogsHeaders;
import static dutil.exception.ExceptionUtilities.getFullDescriptionWithRootCause;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.TWO_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.string.TextUtilities.CHARSET_UTF_8;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.NL2;
import static dutil.string.TextUtilities.NL2T;
import static dutil.string.TextUtilities.dq;
import static dutil.system.OSUtilities.WIN_SHORTCUT_EXTENSION;
import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;


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
   * Entry point of the RenameWithLinks program.
   *
   * @param args The command line args (currently unused).
   */
  public static void main(String[] args) throws Exception {

    if (args.length == ZERO_i || args.length == 3) {

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
        
          writeLogsHeaders(ac.screenLog, ac.userLog, ac.devLog, APP_NAME, APP_DESCR);
          
          showStartupMessages(ac);
          
          final RenameWithLinks app = RenameWithLinks.newInstance(ac);
          
          // Create the desired type of updater instance :
          
          final IShortcutTargetUpdater shortcutTargetUpdater = WinShortcutUpdater_PS_COM_WScript_Shell01.newInstance(
                                                                              false, ac.devLog);
          
          // Perform the renaming operation using the chosen updater :
          
          if (args.length == ZERO_i) {
  
            app.run(shortcutTargetUpdater);
          }
          else {
            
            app.run(shortcutTargetUpdater, args[ZERO_i], args[ONE_i], args[TWO_i]);
          }
        }
        catch (UserRequestedTermination t) {
        
          ac.warnUser(NL + (t.getMessage() != null ? t.getMessage() : "Terminated on user request."));
        }
        catch (IOException e) {
        
          ac.errUser(NL2 + "Terminated due to an error : " + e.getClass().getSimpleName() + " :" + NL2T + e.getLocalizedMessage().trim() + NL2);
          
          ac.outUserLog(getFullDescriptionWithRootCause(e));
        }
        finally {
        
          ac.showLogInfo();
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
   * @param ac The application context.
   */
  private static void showStartupMessages(@NotNull AppContext ac) {
  
    ac.outUser();
    
    ac.outUser("Starting " + dq(APP_DESCR) + " on " + new Date() + NL);
    
    ac.outUser("Screen log: " + getCanonicalPath(ac.screenLog.logFile));
    
    ac.outUser("  User log: " + getCanonicalPath(ac.userLog.logFile));
    
    ac.outUser("   Dev log: " + getCanonicalPath(ac.devLog.logFile));
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
