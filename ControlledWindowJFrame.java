import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.Timer;


public class ControlledWindowJFrame extends JFrame implements WindowListener, WindowFocusListener, WindowStateListener{
    private static final long serialVersionUID = 120494853; // why not?
    private Consumer<Object> onClose;

    public ControlledWindowJFrame(String name){
        super(name);
        super.addWindowListener(this);
        super.addWindowFocusListener(this);
        super.addWindowStateListener(this);
    }

    /**
     * Set a consumer to be run on close
     * @param onClose Consumer<null> called on window close
     */
    public void setOnClose(Consumer<Object> onClose){
        this.onClose = onClose;
    }

    public void windowClosing(WindowEvent e) {
        JFrame f = (JFrame) e.getSource();
        f.setVisible(false);
        if (this.onClose!=null){
            this.onClose.accept(null);
        }
    }

    public void windowClosed(WindowEvent e) {
        
    }

    public void windowOpened(WindowEvent e) {

    }

    public void windowIconified(WindowEvent e) {

    }

    public void windowDeiconified(WindowEvent e) {
        
    }

    public void windowActivated(WindowEvent e) {
        
    }

    public void windowDeactivated(WindowEvent e) {
        
    }

    public void windowGainedFocus(WindowEvent e) {
        
    }

    public void windowLostFocus(WindowEvent e) {
        
    }

    public void windowStateChanged(WindowEvent e) {

    }

    
}