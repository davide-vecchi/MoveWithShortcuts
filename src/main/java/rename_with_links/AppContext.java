/**
 * Created by OpenCode on 2026-05-08 .
 */
package rename_with_links;


import dlog.log.Log;
import duser_input_output.AUserInputOutput;
import dutil.exception.exceptions.InvalidValueInternalErrorException;
import dutil.string.TextUtilities;
import jakarta.validation.constraints.NotNull;

import static dfile.file.FileUtilities.getCanonicalPathAsDescr;
import static dutil.number.NumberUtilities.MINUS1_i;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.object.ObjectUtilities.assertNonNull;
import static dutil.string.TextUtilities.NL;
import static dutil.string.TextUtilities.removeEnd;
import static rename_with_links.RenameWithLinksMain.MAX_VERBOSITY;


// @formatter:off


/** TODO Class duplicated in several places. UNIFY INTO A GENERIC LIBRARY.<br><br>
 *
 * Class that represents the context in which an application runs, {@link #screenLog with} {@link #userLog its} {@link #devLog
 * logger}s and {@link #userIO user input / output}.<br><br>It can be used to share global objects that must be
 * accessible from anywhere in the application, like the loggers and the screen / keyboard user I/O.<br>
 * It is {@link AutoCloseable} so it should be used with {@code try-with-resources}, in which case it must be kept in
 * mind that its {@link #close()} method will also close its {@link #userIO user input / output}, {@link #screenLog},
 * {@link #userLog} and {@link #devLog}.<br>
 *
 * When a class has an instance of this class as a field, consider excluding the field from that class' {@link Object#toString()
 * toString()} and {@link Object#equals(Object) equals()}.
 */
public class AppContext implements AutoCloseable {
  
  
  /**
   * The object to use to communicate with the user. To be set by clients.
   */
  public @NotNull AUserInputOutput userIO;
  
  /**
   * The log containing only the text that is shown {@link #outUser(String) on the screen}. To be set by clients. Its {@link Log#logBare
   * logBare} will be always {@code true}.
   */
  public @NotNull Log screenLog;
  
  /**
   * The log containing info for the user, not only for the developer. To be set by clients.
   */
  public @NotNull Log userLog;
  
  /**
   * The log containing only info for the developer, not for the user. To be set by clients.
   */
  public @NotNull Log devLog;
  
  /**
   * The verbosity level :<ul>
   *   <li>0 = Only error messages.</li>
   *   <li>1 = Also messages about startup, logs location, updated shortcuts.</li>
   *   <li>2 = Everything else (e.g. show every processed shortcut).</li>
   * </ul>
   */
  int currentVerbosity = MINUS1_i; // : Not set to a valid value yet.
  
  
  /**
   * Constructor.
   *
   * @param userIO    {@link #userIO}.<br>
   *
   * @param screenLog {@link #screenLog}. Its {@link Log#logBare logBare} will be set to {@code true}.<br>
   *
   * @param userLog   {@link #userLog}.<br>
   *
   * @param devLog    {@link #devLog}.
   */
  private AppContext(@NotNull AUserInputOutput userIO, @NotNull Log screenLog, @NotNull Log userLog, @NotNull Log devLog) {
    
    this.userIO =    assertNonNull(userIO);
    
    this.screenLog = assertNonNull(screenLog);
    
    this.screenLog.logBare = true;
    
    this.userLog =   assertNonNull(userLog);
    
    this.devLog =    assertNonNull(devLog);
  }
  
  
  /**
   * Factory method.
   *
   * @param userIO    {@link #userIO}.<br>
   * @param screenLog {@link #screenLog}.<br>
   * @param userLog   {@link #userLog}.<br>
   * @param devLog    {@link #devLog}.
   *
   * @return A new {@link AppContext} instantiated with the given values.
   */
  public static AppContext newAppContext(@NotNull AUserInputOutput userIO, @NotNull Log screenLog, @NotNull Log userLog
                                                                         , @NotNull Log devLog) {
    return new AppContext(userIO, screenLog, userLog, devLog);
  }
  
  /**
   * {@link AUserInputOutput#outLine(String) Outputs} the given {@code string}, followed by {@link TextUtilities#NL new
   * line}, to {@link #userIO}, {@link #screenLog}, {@link #userLog} and {@link #devLog}.
   *
   * @param verbosity The verbosity level for the given {@code string} (see {@link #currentVerbosity}).<br>
   *
   * @param string The string to output.
   *
   * @return The given {@code string}.
   */
  public String outUser(int verbosity, String string) {
    
    return outUser_Chars(verbosity, string + NL);
  }
  
  /**
   * {@link AUserInputOutput#errChars(String) Outputs} the given {@code string} as error, to {@link #userIO}, {@link
   * #screenLog}, {@link #userLog} and {@link #devLog}, followed by {@link TextUtilities#NL new line}.
   *
   * @param verbosity The verbosity level for the given {@code string} (see {@link #currentVerbosity}).<br>
   *
   * @param string The string to output.
   *
   * @return The given {@code string}.
   */
  public String errUser(int verbosity, String string) {
    
    return errUser_Chars(verbosity,string + NL);
  }
  
  /**
   * Like {@link #outUser(String)} but the written text is <b>not</b> followed by {@link TextUtilities#NL new line}.
   *
   * @param verbosity The verbosity level for the given {@code string} (see {@link #currentVerbosity}).<br>
   *
   * @param string The string to output.
   *
   * @return The given {@code string}.
   */
  public String outUser_Chars(int verbosity, String string) {
    
    if (assertValidVerbosity(verbosity) <= this.currentVerbosity) {
    
      this.userIO.outChars(string);
    }
    outScreenLog(string);
    
    return string;
  }
  
  /**
   * Like {@link #errUser(String)} but the written text is <b>not</b> followed by {@link TextUtilities#NL new line}.
   *
   * @param verbosity The verbosity level for the given {@code string} (see {@link #currentVerbosity}).<br>
   *
   * @param string The string to output.
   *
   * @return The given {@code string}.
   */
  public String errUser_Chars(int verbosity, String string) {
    
    if (assertValidVerbosity(verbosity) <= this.currentVerbosity) {
    
      this.userIO.errChars(string);
    }
    outScreenLog(string);
    
    outUserLog(  string);
    
    return string;
  }
  
  /**
   * {@link AUserInputOutput#errChars(String) Outputs} the given {@code string} to {@link #userIO} (as a warning), {@link
   * #screenLog}, {@link #userLog} and {@link #devLog}, followed by {@link TextUtilities#NL new line}.
   *
   * @param verbosity The verbosity level for the given {@code string} (see {@link #currentVerbosity}).<br>
   *
   * @param string The string to output.
   *
   * @return The given {@code string}.
   */
  public String warnUser(int verbosity, String string) {
    
    return warnUser_Chars(verbosity, string + NL);
  }
  
  /**
   * Like {@link #warnUser(String)} but the written text is <b>not</b> followed by {@link TextUtilities#NL new line}.
   *
   * @param verbosity The verbosity level for the given {@code string} (see {@link #currentVerbosity}).<br>
   *
   * @param string The string to output.
   *
   * @return The given {@code string}.
   */
  public String warnUser_Chars(int verbosity, String string) {
    
    if (assertValidVerbosity(verbosity) <= this.currentVerbosity) {
      
      this.userIO.warnChars(string);
    }
    outScreenLog(string);
    
    outUserLog(  string);
    
    return string;
  }
  
  /**
   * Outputs the given {@code string} to {@link #screenLog}, {@link #userLog} and {@link #devLog}.
   *
   * @param string The string to output. If it ends with a {@link TextUtilities#NL new line}, that will be stripped in
   *               the {@link #screenLog}.
   *
   * @return The given string.
   */
  public String outScreenLog(String string) {
    
    outUserLog(string);
    
    this.screenLog.log(removeEnd(string, NL));
    
    return string;
  }
  
  /**
   * {@link AUserInputOutput#outLine(String) Outputs} the given {@code string} to {@link #userLog} and {@link #devLog}.
   *
   * @param string The string to output.
   *
   * @return The given string.
   */
  public String outUserLog(String string) {
    
    outDevLog(string);
    
    this.userLog.log(string);
    
    return string;
  }
  
  /**
   * {@link AUserInputOutput#outLine(String) Outputs} the given {@code string} to {@link #devLog}.
   *
   * @param string The string to output.
   *
   * @return The outputted string.
   */
  public String outDevLog(String string) {
    
    this.devLog.log(string);
    
    return string;
  }
  
  /**
   * @param verbosity The verbosity level for the log info {@code string}. (see {@link #currentVerbosity}).
   *
   * {@link AppContext#outUser() Outputs} to the user info on the location of the log files.
   */
  public void showLogInfo(int verbosity) {
    
    outUser(verbosity, NL + "Screen log: " + getCanonicalPathAsDescr(this.screenLog.logFile) + ".");
    
    outUser(verbosity,      "  User log: " + getCanonicalPathAsDescr(this.userLog  .logFile) + ".");
    
    outUser(verbosity,      "   Dev log: " + getCanonicalPathAsDescr(this.devLog   .logFile) + ".");
  }
  
  /**
   * Closes {@link #userIO} and all the logs.
   *
   * @throws Exception
   */
  @Override
  public void close() throws Exception {
    
    if (this.userIO != null) {
      
      this.userIO.close();
    }
    if (this.screenLog != null) {
      
      this.screenLog.close();
    }
    if (this.userLog != null) {
      
      this.userLog.close();
    }
    if (this.devLog != null) {
      
      this.devLog.close();
    }
  }
  
  /**
   * @param verbosity The verbosity level to validate.
   *
   * @return The given {@code verbosity} if it and {@link #currentVerbosity} are both valid.
   *
   * @throws InvalidValueInternalErrorException If either the given {@code verbosity} or {@link #currentVerbosity} are
   *                                            not valid (negative or > {@link RenameWithLinks#MAX_VERBOSITY
   *                                            MAX_VERBOSITY}).
   */
  int assertValidVerbosity(int verbosity) {
    
    if (verbosity < ZERO_i || verbosity > MAX_VERBOSITY) {
      
      throw new InvalidValueInternalErrorException("The given verbosity level " + verbosity + " is not valid, must be between 0 and " + MAX_VERBOSITY + " .");
    }
    if (this.currentVerbosity < ZERO_i || this.currentVerbosity > MAX_VERBOSITY) {
      
      throw new InvalidValueInternalErrorException("The currently set verbosity level " + this.currentVerbosity + " is not valid, must be between 0 and " + MAX_VERBOSITY + " .");
    }
    return verbosity;
  }
  
}
