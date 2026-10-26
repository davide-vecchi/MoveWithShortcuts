/**
 * Created by Davide on 2026-10-26 .
 */
package move_with_shortcuts;


import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static dutil.string.TextUtilities.NL;


// @formatter:off


/**
 * Tests for {@link GuiUserInputOutput}.
 */
public class GuiUserInputOutputTest {


  /**
   * Tests that normal, warning and error texts are forwarded with their kind.
   */
  @Test
  void forwardsKinds() {
  
    final List<String> texts = new ArrayList<>();
    
    final GuiUserInputOutput userIO = newUserIO(texts);
    
    userIO.outLine("Normal line");
    
    userIO.warnChars("Warning text");
    
    userIO.errChars("Error text");
    
    Assert.assertEquals(texts, List.of("NORMAL=Normal line", "WARNING=Warning text", "ERROR=Error text"));
  }
  
  /**
   * Tests that the raw progress lines are not forwarded to the operation-detail area.
   */
  @Test
  void filtersProgressLines() {
  
    final List<String> texts = new ArrayList<>();
    
    final GuiUserInputOutput userIO = newUserIO(texts);
    
    userIO.outLine("  1 / 4 (25,00%)");
    
    userIO.outLine("Analyzing shortcuts: 1 / 4 (25,00%)");
    
    userIO.outLine("Finalizing: 1 / 2 (50,00%)");
    
    Assert.assertTrue(texts.isEmpty());
  }
  
  /**
   * Tests that a text split across several calls is forwarded as one line.
   */
  @Test
  void buffersPartialLines() {
  
    final List<String> texts = new ArrayList<>();
    
    final GuiUserInputOutput userIO = newUserIO(texts);
    
    userIO.outChars("Hel");
    
    userIO.outChars("lo");
    
    userIO.outLine(" world");
    
    Assert.assertEquals(texts, List.of("NORMAL=Hello world"));
  }
  
  /**
   * Tests that complete lines are forwarded immediately while the incomplete tail is kept.
   */
  @Test
  void forwardsCompleteLinesAndKeepsTail() {
  
    final List<String> texts = new ArrayList<>();
    
    final GuiUserInputOutput userIO = newUserIO(texts);
    
    userIO.outChars("one" + NL + "two");
    
    Assert.assertEquals(texts, List.of("NORMAL=one"));
    
    userIO.outLine();
    
    Assert.assertEquals(texts, List.of("NORMAL=one", "NORMAL=two"));
  }
  
  /**
   * Tests that a warning flushes the pending normal text first.
   */
  @Test
  void warningFlushesPendingText() {
  
    final List<String> texts = new ArrayList<>();
    
    final GuiUserInputOutput userIO = newUserIO(texts);
    
    userIO.outChars("partial");
    
    userIO.warnChars("careful");
    
    Assert.assertEquals(texts, List.of("NORMAL=partial", "WARNING=careful"));
  }
  
  /**
   * Creates a user I/O that records the forwarded texts.
   *
   * @param texts The list that receives the recorded texts.
   *
   * @return The user I/O.
   */
  private static GuiUserInputOutput newUserIO(List<String> texts) {
  
    final GuiTextReporter textReporter = GuiTextReporter.newInstance();
    
    textReporter.setListener(update -> texts.add(update.kind() + "=" + update.text()));
    
    return GuiUserInputOutput.newInstance(GuiProgressReporter.newInstance(), textReporter);
  }
}
