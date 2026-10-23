/**
 * Created by Davide on 2026-10-24 .
 */
package move_with_shortcuts;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.function.BiConsumer;


// @formatter:off


/**
 * Turns the progress lines written by the PowerShell scripts and by the Java updater loops into phase and percentage
 * updates for the GUI.
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
   * The consumer of the phase and percentage updates.
   */
  private final BiConsumer<String, Integer> progressConsumer;
  
  /**
   * The phase of the current loop.
   */
  private String phase = PHASE_WORKING;
  
  
  /**
   * @param progressConsumer {@link #progressConsumer}.
   */
  ProgressTracker(@NotNull BiConsumer<String, Integer> progressConsumer) {
  
    this.progressConsumer = progressConsumer;
  }
  
  
  /**
   * Parses one output line and reports a phase or percentage update when the line carries one.
   *
   * @param line The output line, or {@code null}.
   */
  void accept(String line) {
  
    if (line == null) {
    
      return;
    }
    final String text = line.strip();
    
    if (text.isEmpty()) {
    
      return;
    }
    if (text.startsWith(READING_PREFIX)) {
    
      setPhase(PHASE_READING);
    }
    else if (text.startsWith(UPDATING_PREFIX)) {
    
      setPhase(PHASE_UPDATING);
    }
    else if (text.startsWith(ANALYZING_PREFIX)) {
    
      reportProgress(PHASE_ANALYZING, text);
    }
    else if (text.startsWith(FINALIZING_PREFIX)) {
    
      reportProgress(PHASE_FINALIZING, text);
    }
    else if (text.startsWith(COMPLETED_PREFIX)) {
    
      this.progressConsumer.accept(this.phase, 100);
    }
    else {
    
      final Integer percentage = parsePercentage(text);
      
      if (percentage != null) {
      
        this.progressConsumer.accept(this.phase, percentage);
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
    
      setPhase(PHASE_SEARCHING);
    }
  }
  
  /**
   * Sets the current phase and reports it without a known percentage.
   *
   * @param newPhase The new phase label.
   */
  private void setPhase(@NotBlank String newPhase) {
  
    this.phase = newPhase;
    
    this.progressConsumer.accept(this.phase, UNKNOWN_PERCENTAGE);
  }
  
  /**
   * Sets the current phase and reports its percentage, when the line contains one.
   *
   * @param newPhase The new phase label.<br>
   *
   * @param line The progress line.
   */
  private void reportProgress(@NotBlank String newPhase, @NotBlank String line) {
  
    this.phase = newPhase;
    
    final Integer percentage = parsePercentage(line);
    
    if (percentage == null) {
    
      this.progressConsumer.accept(this.phase, UNKNOWN_PERCENTAGE);
    }
    else {
    
      this.progressConsumer.accept(this.phase, percentage);
    }
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
        
          final String number = line.substring(openIndex + 1, percentIndex).trim().replace(',', '.');
          
          try {
          
            final double value = Double.parseDouble(number);
            
            result = (int) Math.round(Math.max(0.0, Math.min(100.0, value)));
          }
          catch (NumberFormatException e) {
          
            result = null;
          }
        }
      }
    }
    return result;
  }
}
