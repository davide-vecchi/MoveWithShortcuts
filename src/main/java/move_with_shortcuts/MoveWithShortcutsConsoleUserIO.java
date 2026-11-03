/**
 * Created by Davide on 2026-11-03 .
 */
package move_with_shortcuts;


import duser_input_output.impl.consoleUserIO.ColorConsoleUserIO;
import dutil.io.ConditionallyCloseablePrintStream;
import jakarta.validation.constraints.NotNull;
import org.fusesource.jansi.Ansi;

import java.io.InputStream;

import static dutil.string.TextUtilities.NL2;


// @formatter:off


/**
 * Console user I/O for {@link MoveWithShortcuts}, which precedes every warning and error with a blank line.
 */
final class MoveWithShortcutsConsoleUserIO extends ColorConsoleUserIO {


  /**
   * Factory method.
   *
   * @param inStream    {@link #inStream}.<br>
   *
   * @param out         {@link #out}.<br>
   *
   * @param err         {@link #err}.<br>
   *
   * @param fgOutColor  {@link #fgOutColor}. May be {@code null}.<br>
   *
   * @param bgOutColor  {@link #bgOutColor}. May be {@code null}.<br>
   *
   * @param fgErrColor  {@link #fgErrColor}. May be {@code null}.<br>
   *
   * @param bgErrColor  {@link #bgErrColor}. May be {@code null}.<br>
   *
   * @param fgWarnColor {@link #fgWarnColor}. May be {@code null}.<br>
   *
   * @param bgWarnColor {@link #bgWarnColor}. May be {@code null}.
   *
   * @return The new instance.
   */
  static MoveWithShortcutsConsoleUserIO newInstance(@NotNull InputStream inStream
                                                  , @NotNull ConditionallyCloseablePrintStream out
                                                  , @NotNull ConditionallyCloseablePrintStream err
                                  , Ansi.Color fgOutColor, Ansi.Color bgOutColor,  Ansi.Color fgErrColor
                                  , Ansi.Color bgErrColor, Ansi.Color fgWarnColor, Ansi.Color bgWarnColor) {
                                  
    return new MoveWithShortcutsConsoleUserIO(inStream,   out,        err
                                            , fgOutColor, bgOutColor, fgErrColor, bgErrColor, fgWarnColor
                                            , bgWarnColor);
  }
  
  /**
   * Private constructor.
   *
   * @param inStream    {@link #inStream}.<br>
   *
   * @param out         {@link #out}.<br>
   *
   * @param err         {@link #err}.<br>
   *
   * @param fgOutColor  {@link #fgOutColor}. May be {@code null}.<br>
   *
   * @param bgOutColor  {@link #bgOutColor}. May be {@code null}.<br>
   *
   * @param fgErrColor  {@link #fgErrColor}. May be {@code null}.<br>
   *
   * @param bgErrColor  {@link #bgErrColor}. May be {@code null}.<br>
   *
   * @param fgWarnColor {@link #fgWarnColor}. May be {@code null}.<br>
   *
   * @param bgWarnColor {@link #bgWarnColor}. May be {@code null}.
   */
  private MoveWithShortcutsConsoleUserIO(@NotNull InputStream inStream
                                       , @NotNull ConditionallyCloseablePrintStream out
                                       , @NotNull ConditionallyCloseablePrintStream err
                       , Ansi.Color fgOutColor, Ansi.Color bgOutColor,  Ansi.Color fgErrColor
                       , Ansi.Color bgErrColor, Ansi.Color fgWarnColor, Ansi.Color bgWarnColor) {
                       
    super(inStream,   out,        err, fgOutColor, bgOutColor, fgErrColor, bgErrColor, fgWarnColor, bgWarnColor);
  }
  
  /**
   * Prints the given warning text, preceded by a blank line.
   *
   * @param chars The warning text.
   *
   * @return The text that was printed.
   */
  @Override
  public String warnChars(String chars) {
  
    return super.warnChars(NL2 + chars);
  }
  
  /**
   * Prints the given error text, preceded by a blank line.
   *
   * @param chars The error text.
   *
   * @return The text that was printed.
   */
  @Override
  public String errChars(String chars) {
  
    return super.errChars(NL2 + chars);
  }
}
