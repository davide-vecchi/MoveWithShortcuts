/**
 * Created by Davide on 2026-10-24 .
 */
package move_with_shortcuts;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.function.Consumer;

import static dutil.number.NumberUtilities.HUNDRED_d;
import static dutil.number.NumberUtilities.I;
import static dutil.number.NumberUtilities.ONE_i;
import static dutil.number.NumberUtilities.ZERO_d;
import static dutil.number.NumberUtilities.ZERO_i;
import static dutil.object.ObjectUtilities.assertNull;
import static dutil.string.TextUtilities.COMMA;
import static dutil.string.TextUtilities.STD_DECIMAL_POINT;


// @formatter:off


/**
 * Turns the progress lines written by the PowerShell scripts and by the Java updater loops into phase, total and
 * percentage updates for the GUI.
 */
final class ProgressTracker {


  /**
   * The phase label used before any operation-specific phase is known.
   */
  static final String PHASE_WORKING = "Working";
  
  /**
   * The phase label for the Java loop that searches for the shortcuts.
   */
  static final String PHASE_SEARCHING = "Searching shortcuts";
  
  /**
   * The phase label for the PowerShell loop that reads the shortcuts.
   */
  static final String PHASE_READING = "Reading shortcuts";
  
  /**
   * The phase label for the Java loop that analyzes the read shortcuts.
   */
  static final String PHASE_ANALYZING = "Analyzing shortcuts";
  
  /**
   * The phase label for the PowerShell loop that updates the shortcuts.
   */
  static final String PHASE_UPDATING = "Updating shortcuts";
  
  /**
   * The phase label for the Java loop that processes the update outcomes.
   */
  static final String PHASE_FINALIZING = "Finalizing";
  
  /**
   * The phase label for the Java loop that parses the rows of the file with the shortcuts read by PowerShell.
   */
  static final String PHASE_PARSING = "Parsing read results";
  
  /**
   * The phase label for the Java loop that shows the reading outcomes.
   */
  static final String PHASE_SUMMARIZING = "Summarizing read results";
  
  /**
   * The percentage value that means "the exact percentage is not known yet".
   */
  static final int UNKNOWN_PERCENTAGE = -1;
  
  /**
   * The prefix of the progress lines written by the Java analyze loop.
   */
  private static final String ANALYZING_PREFIX = PHASE_ANALYZING + ": ";
  
  /**
   * The prefix of the progress lines written by the Java finalize loop.
   */
  private static final String FINALIZING_PREFIX = PHASE_FINALIZING + ": ";
  
  /**
   * The prefix of the progress lines written by the Java read-results parsing loop.
   */
  private static final String PARSING_PREFIX = PHASE_PARSING + ": ";
  
  /**
   * The prefix of the progress lines written by the Java read-results summarizing loop.
   */
  private static final String SUMMARIZING_PREFIX = PHASE_SUMMARIZING + ": ";
  
  /**
   * The prefix of the PowerShell script's reading phase line.
   */
  private static final String READING_PREFIX = "Starting reading targets of ";
  
  /**
   * The prefix of the PowerShell script's updating phase line.
   */
  private static final String UPDATING_PREFIX = "Starting updating the targets and/or work folders of ";
  
  /**
   * The prefix of the PowerShell scripts' completion line.
   */
  private static final String COMPLETED_PREFIX = "Completed. Processed ";
  
  /**
   * The text that precedes the total in the PowerShell reading phase line.
   */
  private static final String READING_TOTAL_PREFIX = "of ";
  
  /**
   * The text that precedes the total in the PowerShell updating phase line.
   */
  private static final String UPDATING_TOTAL_PREFIX = "of the ";
  
  /**
   * The text that precedes the total in the PowerShell completion line.
   */
  private static final String COMPLETED_TOTAL_PREFIX = "Processed ";
  
  /**
   * The text that follows the total in the PowerShell phase and completion lines.
   */
  private static final String TOTAL_SUFFIX = " shortcuts";
  
  /**
   * The consumer of the progress updates.
   */
  private final Consumer<ProgressUpdate> progressConsumer;
  
  /**
   * The phase of the current loop.
   */
  private String phase = PHASE_WORKING;
  
  /**
   * The total number of items in the current phase, or {@code 0} when it is not known.
   */
  private int total;
  
  
  /**
   * @param progressConsumer {@link #progressConsumer}.
   */
  ProgressTracker(@NotNull Consumer<ProgressUpdate> progressConsumer) {
  
    this.progressConsumer = progressConsumer;
  }
  
  
  /**
   * Parses one output line and reports a phase or progress update when the line carries one.
   *
   * @param line The output line, or {@code null}.
   */
  void accept(String line) {
  
    if (line != null) {
    
      final String text = line.strip();
      
      if (! text.isEmpty()) {
      
        acceptText(text);
      }
    }
  }
  
  /**
   * Parses one non-empty stripped output line and reports a phase or progress update when the line carries one.
   *
   * @param text The non-empty stripped output line.
   */
  private void acceptText(@NotNull String text) {
  
    if (text.startsWith(READING_PREFIX)) {
    
      this.total = parseHeaderTotal(text, READING_TOTAL_PREFIX, TOTAL_SUFFIX);
      
      setPhase(PHASE_READING);
    }
    else if (text.startsWith(UPDATING_PREFIX)) {
    
      this.total = parseHeaderTotal(text, UPDATING_TOTAL_PREFIX, TOTAL_SUFFIX);
      
      setPhase(PHASE_UPDATING);
    }
    else if (text.startsWith(ANALYZING_PREFIX)) {
    
      reportProgress(PHASE_ANALYZING, text);
    }
    else if (text.startsWith(FINALIZING_PREFIX)) {
    
      reportProgress(PHASE_FINALIZING, text);
    }
    else if (text.startsWith(PARSING_PREFIX)) {
    
      reportProgress(PHASE_PARSING, text);
    }
    else if (text.startsWith(SUMMARIZING_PREFIX)) {
    
      reportProgress(PHASE_SUMMARIZING, text);
    }
    else if (text.startsWith(COMPLETED_PREFIX)) {
    
      final int completed = parseHeaderTotal(text, COMPLETED_TOTAL_PREFIX, TOTAL_SUFFIX);
      
      if (completed > ZERO_i) {
      
        this.total = completed;
      }
      this.progressConsumer.accept(new ProgressUpdate(this.phase, this.total, 100));
    }
    else {
    
      final Integer percentage = parsePercentage(text);
      
      if (percentage != null) {
      
        final int parsedTotal = parseProgressTotal(text);
        
        if (parsedTotal > ZERO_i) {
        
          this.total = parsedTotal;
        }
        this.progressConsumer.accept(new ProgressUpdate(this.phase, this.total, percentage.intValue()));
      }
    }
  }
  
  /**
   * Parses one user-output text for a phase change. The search loop has no known total, so it only sets the phase.
   *
   * @param chars The user-output text, or {@code null}.
   */
  void acceptChars(String chars) {
  
    if (chars != null && chars.contains("Retrieving shortcuts to check for needed update")) {
    
      this.total = ZERO_i;
      
      setPhase(PHASE_SEARCHING);
    }
  }
  
  /**
   * Checks whether the given line is a raw progress line, which the operation-detail area must not show because the
   * progress bar already displays it.
   *
   * @param line The line to check, or {@code null}.
   *
   * @return Whether the line is a progress line.
   */
  static boolean isProgressLine(String line) {
  
    boolean result = false;
    
    if (line != null) {
    
      final String text = line.strip();
      
      result = text.endsWith("%)")
            || text.startsWith(ANALYZING_PREFIX)    || text.startsWith(FINALIZING_PREFIX)
            || text.startsWith(PARSING_PREFIX)      || text.startsWith(SUMMARIZING_PREFIX);
    }
    return result;
  }
  
  /**
   * Sets the current phase and reports it without a known percentage.
   *
   * @param newPhase The new phase label.
   */
  private void setPhase(@NotBlank String newPhase) {
  
    this.phase = newPhase;
    
    this.progressConsumer.accept(new ProgressUpdate(this.phase, this.total, UNKNOWN_PERCENTAGE));
  }
  
  /**
   * Sets the current phase, updates the total and reports the percentage, when the line contains one.
   *
   * @param newPhase The new phase label.<br>
   *
   * @param line The progress line.
   */
  private void reportProgress(@NotBlank String newPhase, @NotBlank String line) {
  
    this.phase = newPhase;
    
    final int parsedTotal = parseProgressTotal(line);
    
    if (parsedTotal > ZERO_i) {
    
      this.total = parsedTotal;
    }
    final Integer percentage = parsePercentage(line);
    
    this.progressConsumer.accept(new ProgressUpdate(this.phase, this.total
                                          , percentage == null ? UNKNOWN_PERCENTAGE : percentage.intValue()));
  }
  
  /**
   * Extracts the percentage from a progress line, like {@code "1 / 4 (25,00%)"}.
   *
   * @param line The line to parse, or {@code null}.
   *
   * @return The percentage, clamped to 0-100, or {@code null} when the line does not contain one.
   */
  static Integer parsePercentage(String line) {
  
    Integer result = null;
    
    if (line != null) {
    
      final int percentIndex = line.lastIndexOf('%');
      
      if (percentIndex > 0) {
      
        final int openIndex = line.lastIndexOf('(', percentIndex);
        
        if (openIndex >= 0) {
        
          final String number = line.substring(openIndex + ONE_i, percentIndex)
                                    .trim().replace(COMMA, STD_DECIMAL_POINT);
          try {
          
            final double value = Double.parseDouble(number);
            
            result = I(Math.toIntExact(Math.round(Math.clamp(value, ZERO_d, HUNDRED_d))));
          }
          catch (NumberFormatException e) {
          
            assertNull(result);
          }
        }
      }
    }
    return result;
  }
  
  /**
   * Extracts the total from a progress line, like {@code "1 / 4 (25,00%)"}.
   *
   * @param line The line to parse, or {@code null}.
   *
   * @return The total, or {@code 0} when it cannot be parsed.
   */
  static int parseProgressTotal(String line) {
  
    int result = ZERO_i;
    
    if (line != null) {
    
      final int percentIndex = line.lastIndexOf('%');
      
      if (percentIndex > 0) {
      
        final int openIndex = line.lastIndexOf('(', percentIndex);
        
        if (openIndex > 0) {
        
          final int slashIndex = line.lastIndexOf('/', openIndex);
          
          if (slashIndex > 0) {
          
            result = parseCount(line.substring(slashIndex + 1, openIndex).trim());
          }
        }
      }
    }
    return result;
  }
  
  /**
   * Extracts the total from one of the PowerShell phase or completion lines.
   *
   * @param line The line to parse, or {@code null}.<br>
   *
   * @param prefix The text that precedes the total.<br>
   *
   * @param suffix The text that follows the total.
   *
   * @return The total, or {@code 0} when it cannot be parsed.
   */
  static int parseHeaderTotal(String line, @NotBlank String prefix, @NotBlank String suffix) {
  
    int result = ZERO_i;
    
    if (line != null) {
    
      final int prefixIndex = line.indexOf(prefix);
      
      if (prefixIndex >= 0) {
      
        final int start = prefixIndex + prefix.length();
        
        final int end = line.indexOf(suffix, start);
        
        if (end > start) {
        
          result = parseCount(line.substring(start, end));
        }
      }
    }
    return result;
  }
  
  /**
   * Parses an integer that may contain a grouping separator.
   *
   * @param text The text to parse.
   *
   * @return The parsed integer, or {@code 0} when it cannot be parsed.
   */
  private static int parseCount(@NotBlank String text) {
  
    int result = ZERO_i;
    
    try {
    
      final String digits = text.replace(".", "").replace(",", "").replace(" ", "")
                                .replace("\u00A0", "").replace("\u202F", "");
                                
      result = Integer.parseInt(digits);
    }
    catch (NumberFormatException e) {
    
      result = ZERO_i;
    }
    return result;
  }
}
