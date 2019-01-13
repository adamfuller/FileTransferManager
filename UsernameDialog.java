import java.awt.Container;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

public class UsernameDialog {
    private String selectedUsername;
    private boolean hasSelectedUsername = false;
    private Consumer<UsernameDialog> onComplete;

    public UsernameDialog(){
        this(null);
    }

    /**
     * Create new username dialog with an on complete action to be performed
     * @param onComplete - consumer where input is this username dialog
     */
    public UsernameDialog(Consumer<UsernameDialog> onComplete){
        if (!this.hasSelectedUsername){
            this.show();
        }
        this.onComplete = onComplete;
    }

    public void show() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Select A Username");
        dialog.setSize(300, 200);
        dialog.setResizable(false);

        SpringLayout layout = new SpringLayout();

        JLabel usernameLabel = new JLabel("Username:");
        JTextField usernameInput = new JTextField("                               ");
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("cancel");

        okButton.addActionListener((a)->{
            this.selectedUsername = usernameInput.getText() != null ? usernameInput.getText() : "";
            this.hasSelectedUsername = true;
            if (this.onComplete!= null){
                this.onComplete.accept(this);
            }
            dialog.dispose();
        });

        cancelButton.addActionListener((a)->{
            this.selectedUsername = "Unknown User";
            this.hasSelectedUsername = true;
            if (this.onComplete!= null){
                this.onComplete.accept(this);
            }
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
     * Save this class
     */
    public void save(){
        
    }

}