/**
 * Created by Davide on 2026-10-24 .
 */
package move_with_shortcuts;


import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;


// @formatter:off


/**
 * Tests for {@link ProgressTracker}.
 */
public class ProgressTrackerTest {


  /**
   * Tests that a percentage with a comma decimal separator is parsed.
   */
  @Test
  void parsePercentageHandlesCommaDecimal() {
  
    Assert.assertEquals(ProgressTracker.parsePercentage("1 / 4 (25,00%)"), Integer.valueOf(25));
  }
  
  /**
   * Tests that a percentage with a dot decimal separator is parsed.
   */
  @Test
  void parsePercentageHandlesDotDecimal() {
  
    Assert.assertEquals(ProgressTracker.parsePercentage("1 / 4 (25.00%)"), Integer.valueOf(25));
  }
  
  /**
   * Tests that the last progress entry of a line is used.
   */
  @Test
  void parsePercentageUsesLastProgressInLine() {
  
    Assert.assertEquals(ProgressTracker.parsePercentage("1 / 4 (25,00%)\t\t2 / 4 (50,00%)")
                      , Integer.valueOf(50));
  }
  
  /**
   * Tests that out-of-range percentages are clamped.
   */
  @Test
  void parsePercentageClampsToRange() {
  
    Assert.assertEquals(ProgressTracker.parsePercentage("1 / 1 (150,00%)"), Integer.valueOf(100));
    
    Assert.assertEquals(ProgressTracker.parsePercentage("1 / 1 (-5,00%)"), Integer.valueOf(0));
  }
  
  /**
   * Tests that non-progress lines do not produce a percentage.
   */
  @Test
  void parsePercentageReturnsNullForNonProgressLines() {
  
    Assert.assertNull(ProgressTracker.parsePercentage("Starting processing..."));
    
    Assert.assertNull(ProgressTracker.parsePercentage(null));
  }
  
  /**
   * Tests the phase and percentage updates for a full sequence of lines.
   */
  @Test
  void trackerReportsPhasesAndPercentages() {
  
    final List<String> updates = new ArrayList<>();
    
    final ProgressTracker tracker = new ProgressTracker(
                                        (phase, percentage) -> updates.add(phase + "=" + percentage));
                                        
    tracker.acceptChars("Retrieving shortcuts to check for needed update, under folder \"C:\\Temp\" ...");
    
    tracker.accept("Starting reading targets of 4 shortcuts...");
    
    tracker.accept("1 / 4 (25,00%)");
    
    tracker.accept("Analyzing shortcuts: 1 / 4 (25,00%)");
    
    tracker.accept("Finalizing: 1 / 2 (50,00%)");
    
    tracker.accept("Completed. Processed 2 shortcuts.");
    
    Assert.assertEquals(updates, List.of("Searching shortcuts=-1", "Reading shortcuts=-1"
                                        , "Reading shortcuts=25", "Analyzing shortcuts=25"
                                        , "Finalizing=50", "Finalizing=100"));
  }
}
