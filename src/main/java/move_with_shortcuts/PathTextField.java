/**
 * Created by Davide on 2026-10-23 .
 */
package move_with_shortcuts;


import jakarta.validation.constraints.NotNull;

import javax.swing.JTextField;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.List;

import static dfile.file.FileUtilities.getCanonicalPath;


// @formatter:off


/**
 * A text field that accepts files and folders dropped from Windows Explorer, setting the dropped item's full path as
 * its text. It keeps the standard text-field transfer handler, so the clipboard actions and the context menu work as
 * usual.
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
   * Private constructor. Installs the drop target.
   */
  private PathTextField() {
  
    super();
    
    new DropTarget(this, DnDConstants.ACTION_COPY, new PathDropTargetListener(), true);
  }
  
  
  /**
   * Drop target listener that accepts the first file or folder of a dropped file list.
   */
  private final class PathDropTargetListener extends DropTargetAdapter {
  
  
    /**
     * Accepts or rejects the drag when it enters this field.
     *
     * @param dtde The drag event.
     */
    @Override
    public void dragEnter(@NotNull DropTargetDragEvent dtde) {
    
      acceptOrReject(dtde);
    }
    
    /**
     * Accepts or rejects the drag while it is over this field.
     *
     * @param dtde The drag event.
     */
    @Override
    public void dragOver(@NotNull DropTargetDragEvent dtde) {
    
      acceptOrReject(dtde);
    }
    
    /**
     * Sets this field's text to the full path of the first dropped file or folder.
     *
     * @param dtde The drop event.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void drop(@NotNull DropTargetDropEvent dtde) {
    
      if (! dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
      
        dtde.rejectDrop();
      }
      else {
      
        dtde.acceptDrop(DnDConstants.ACTION_COPY);
        
        boolean success = false;
        
        try {
        
          final Transferable transferable = dtde.getTransferable();
          
          final List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
          
          if (! files.isEmpty()) {
          
            final PathTextField target = PathTextField.this;
            
            target.setText(getCanonicalPath(files.getFirst()));
            
            success = true;
          }
        }
        catch (UnsupportedFlavorException | IOException e) {
        
          success = false;
        }
        finally {
        
          dtde.dropComplete(success);
        }
      }
    }
    
    /**
     * Accepts the drag as a copy when it carries a file list, and rejects it otherwise.
     *
     * @param dtde The drag event.
     */
    private void acceptOrReject(@NotNull DropTargetDragEvent dtde) {
    
      if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
      
        dtde.acceptDrag(DnDConstants.ACTION_COPY);
      }
      else {
      
        dtde.rejectDrag();
      }
    }
  }
}
