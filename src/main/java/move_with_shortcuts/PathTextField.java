/**
 * Created by Davide on 2026-10-23 .
 */
package move_with_shortcuts;


import jakarta.validation.constraints.NotNull;

import javax.swing.JTextField;
import javax.swing.TransferHandler;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.List;

import static dfile.file.FileUtilities.getCanonicalPath;


// @formatter:off


/**
 * A text field that accepts files and folders dropped from Windows Explorer, setting the dropped item's full path as
 * its text.
 */
final class PathTextField extends JTextField {


  @Serial
  private static final long serialVersionUID = 1L;
  
  
  /**
   * Factory method.
   *
   * @return The new text field.
   */
  static PathTextField newInstance() {
  
    return new PathTextField();
  }
  
  /**
   * Private constructor. Installs the drop handler.
   */
  private PathTextField() {
  
    super();
    
    setTransferHandler(new PathTransferHandler());
  }
  
  
  /**
   * Transfer handler that accepts the first file or folder of a dropped file list.
   */
  private final class PathTransferHandler extends TransferHandler {
  
  
    @Serial
    private static final long serialVersionUID = 1L;
    
    
    /**
     * Tells whether the transfer can be imported as a dropped file or folder.
     *
     * @param support The transfer support.
     *
     * @return Whether the transfer can be imported.
     */
    @Override
    public boolean canImport(@NotNull TransferSupport support) {
    
      return support.isDrop() && support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }
    
    /**
     * Sets this field's text to the full path of the first dropped file or folder.
     *
     * @param support The transfer support.
     *
     * @return Whether the transfer was imported.
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean importData(@NotNull TransferSupport support) {
    
      boolean result = false;
      
      if (canImport(support)) {
      
        try {
        
          final List<File> files = (List<File>) support.getTransferable()
                                                      .getTransferData(DataFlavor.javaFileListFlavor);
                                                      
          if (! files.isEmpty()) {
          
            final PathTextField target = PathTextField.this;
            
            target.setText(getCanonicalPath(files.get(0)));
            
            result = true;
          }
        }
        catch (UnsupportedFlavorException | IOException e) {
        
          // Do nothing, result stays false.
        }
      }
      return result;
    }
  }
}
