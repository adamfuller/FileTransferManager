import java.awt.Container;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

public class UsernameDialog implements Serializable{
    private String selectedUsername, saveLocation;
    private boolean hasSelectedUsername = false;
    private String savename = "usernameDialog.sav";

    public UsernameDialog(){
        this(null);
    }

    /**
     * Create new username dialog with an on complete action to be performed
     * @param onComplete - consumer where input is this username dialog
     */
    public UsernameDialog(Consumer<UsernameDialog> onComplete){
        try{
            this.saveLocation = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            this.saveLocation = this.saveLocation.substring(0, this.saveLocation.lastIndexOf("/")+1);
        } catch (Exception e){}
        this.load(this.saveLocation!=null?this.saveLocation+this.savename:this.savename);
        if (!this.hasSelectedUsername){
            this.show(onComplete);
        } else { // call onComplete as if filled in automatically
            onComplete.accept(this);
        }
        
    }

    public void show(Consumer<UsernameDialog> onComplete){
        JDialog dialog = new JDialog();
        dialog.setTitle("Select A Username");
        dialog.setSize(300, 200);
        // dialog.setResizable(false);

        SpringLayout layout = new SpringLayout();

        JLabel usernameLabel = new JLabel("Username:");
        JTextField usernameInput = new JTextField("                               ");
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("cancel");

        okButton.addActionListener((a)->{
            this.selectedUsername = usernameInput.getText() != null ? usernameInput.getText() : "";
            this.hasSelectedUsername = true;
            if (onComplete!= null){
                onComplete.accept(this);
            }
            this.save();
            dialog.dispose();
        });

        cancelButton.addActionListener((a)->{
            this.selectedUsername = "Unknown User";
            this.hasSelectedUsername = true;
            if (onComplete!= null){
                onComplete.accept(this);
            }
            this.save();
            dialog.dispose();
        });

        Container contentPane = dialog.getContentPane();

        // arrange username label and input
        layout.putConstraint(SpringLayout.NORTH, usernameLabel, 14, SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, usernameInput, 0, SpringLayout.VERTICAL_CENTER, usernameLabel);
        layout.putConstraint(SpringLayout.WEST, usernameInput, 14, SpringLayout.EAST, usernameLabel);
        layout.putConstraint(SpringLayout.WEST, usernameLabel, 14, SpringLayout.WEST, contentPane);

        // arrange the ok and cancel buttons
        layout.putConstraint(SpringLayout.SOUTH, okButton, -14, SpringLayout.SOUTH, contentPane);
        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, okButton, 0, SpringLayout.HORIZONTAL_CENTER, usernameLabel);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, cancelButton, 0, SpringLayout.VERTICAL_CENTER, okButton);
        layout.putConstraint(SpringLayout.EAST, cancelButton, -14, SpringLayout.EAST, contentPane);

        dialog.setLayout(layout);
        dialog.add(usernameLabel);
        dialog.add(usernameInput);
        dialog.add(okButton);
        dialog.add(cancelButton);
        dialog.setVisible(true);
        usernameInput.setText("User_" + Math.round((float) (200 * Math.random())));
    }

    public void show() {
        this.show(null);
    }

    /**
     * Return the username selected by the user
     * @return
     */
    public String getUsername(){
        return this.selectedUsername;
    }

    /**
     * Returns if the user has selected a name
     * @return
     */
    public boolean hasChosenUsername(){
        return this.hasSelectedUsername;
    }

    /**
     * Attempt to save this object
     */
    public void save(){
        try {
            ObjectOutputStream objectOutputStream;
            objectOutputStream = new ObjectOutputStream(new FileOutputStream(this.saveLocation!=null? (this.saveLocation+this.savename):this.savename ));
			objectOutputStream.writeObject(this);
            objectOutputStream.close();
		} catch (Exception ex) {
			ex.printStackTrace();
        }
    }

    public void clone(UsernameDialog otherDialog){
        this.selectedUsername = otherDialog.getUsername();
        this.hasSelectedUsername = otherDialog.hasChosenUsername();
    }

    /**
     * Attempt to load this object from a save file
     * @param fileName
     */
    public void load(String fileName){
        try{
            UsernameDialog loadedDialog;
            ObjectInputStream objectInputStream;
			objectInputStream = new ObjectInputStream(new FileInputStream(fileName)) ;
            loadedDialog = (UsernameDialog) objectInputStream.readObject();

            objectInputStream.close();

            this.clone(loadedDialog);

		} catch (Exception ex) {
            System.out.println("Couldn't be loaded");
			// ex.printStackTrace();
        }
        
    }    

}