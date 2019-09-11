package com.openbravo.basic;

import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.JDialog;

public class JJDialog extends JDialog {
	
	/** Creates new  */
    public JJDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        //Parche segundo monitor
        if (!windowFitsOnScreen(this)) centerWindowToScreen(this);
    }
    /** Creates new  */
    public JJDialog(java.awt.Dialog parent, boolean modal) {
        super(parent, modal);
        //Parche segundo monitor
        if (!windowFitsOnScreen(this)) centerWindowToScreen(this);
    }
    
    //Parche segundo monitor
    public static boolean windowFitsOnScreen(Window w) {
        return w.getGraphicsConfiguration().getBounds().contains(w.getBounds());
    }

    //Parche segundo monitor
    public static void centerWindowToScreen(Window w) {
        Rectangle screen = w.getGraphicsConfiguration().getBounds();
        w.setLocation(
            screen.x + (screen.width - w.getWidth()) / 2,
            screen.y + (screen.height - w.getHeight()) / 2
        );
    }

}
