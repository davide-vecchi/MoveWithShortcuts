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
   * Tests that the total is extracted from a progress line.
   */
  @Test
  void parseProgressTotalReadsTotal() {
  
    Assert.assertEquals(ProgressTracker.parseProgressTotal("1 / 4 (25,00%)"), 4);
    
    Assert.assertEquals(ProgressTracker.parseProgressTotal("1 / 1.890 (0,05%)"), 1890);
    
    Assert.assertEquals(ProgressTracker.parseProgressTotal("Analyzing shortcuts: 1 / 908 (1,00%)"), 908);
  }
  
  /**
   * Tests that the last total of a line with several progress entries is used.
   */
  @Test
  void parseProgressTotalUsesLastProgressInLine() {
  
    Assert.assertEquals(ProgressTracker.parseProgressTotal(
                                        "1 / 4 (25,00%)\t\t2 / 1.890 (0,11%)"), 1890);
  }
  
  /**
   * Tests the total extraction from the PowerShell reading and updating phase lines.
   */
  @Test
  void parseHeaderTotalReadsPhaseTotals() {
  
    Assert.assertEquals(ProgressTracker.parseHeaderTotal(
                                        "Starting reading targets of 1.890 shortcuts from file ..."
                                      , "of ", " shortcuts"), 1890);
                                      
    Assert.assertEquals(ProgressTracker.parseHeaderTotal(
                                        "Starting updating the targets and/or work folders of the 12 shortcuts from"
                                      + " file ...", "of the ", " shortcuts"), 12);
                                      
    Assert.assertEquals(ProgressTracker.parseHeaderTotal(
                                        "Completed. Processed 2 shortcuts.", "Processed ", " shortcuts"), 2);
  }
  
  /**
   * Tests the phase, total and percentage updates for a full sequence of lines.
   */
  @Test
  void trackerReportsPhasesAndPercentages() {
  
    final List<String> updates = new ArrayList<>();
    
    final ProgressTracker tracker = new ProgressTracker(
                                        update -> updates.add(update.phase() + "=" + update.total() + "="
                                                            + update.percentage()));
                                                            
    tracker.acceptChars("Retrieving shortcuts to check for needed update, under folder \"C:\\Temp\" ...");
    
    tracker.accept("Starting reading targets of 4 shortcuts...");
    
    tracker.accept("1 / 4 (25,00%)");
    
    tracker.accept("Analyzing shortcuts: 1 / 4 (25,00%)");
    
    tracker.accept("Finalizing: 1 / 2 (50,00%)");
    
    tracker.accept("Completed. Processed 2 shortcuts.");
    
    Assert.assertEquals(updates, List.of("Searching shortcuts=0=-1", "Reading shortcuts=4=-1"
                                        , "Reading shortcuts=4=25", "Analyzing shortcuts=4=25"
                                        , "Finalizing=2=50", "Finalizing=2=100"));
  }
}
