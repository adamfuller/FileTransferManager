import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.Timer;

public class FileTransferManager {
    private FileTransferReceiver fileTransferReceiver; // start receiver without searching
    private FileTransferSender fileTransferSender; // start sender with 0 listening connections
    private FileTransferServerClient fileTransferServerClient;
    private boolean isAsync = false;
    private Consumer<ActionEvent> onYes, onNo; // consumers for Yes and No buttons
    private JButton yesButton, noButton;
    private JLabel prompt;

    public FileTransferManager(boolean isAsync) {
        this.isAsync = isAsync;
        this.fileTransferServerClient = new FileTransferServerClient();

        this.fileTransferReceiver = new FileTransferReceiver();
        this.fileTransferReceiver.setManager(this);
        this.fileTransferReceiver.useAsync(isAsync);

        this.fileTransferSender = new FileTransferSender();
        this.fileTransferSender.setAsync(isAsync);

        this.fileTransferReceiver.startListening();// start listening for send requests
    }

    public void setYesAndNoButtons(JButton yesButton, JButton noButton) {
        this.yesButton = yesButton;
        this.noButton = noButton;
    }

    public void enableYesAndNo() {
        if (yesButton != null) {
            yesButton.setVisible(true);
            yesButton.setEnabled(true);
        }
        if (noButton != null) {
            noButton.setVisible(true);
            noButton.setEnabled(true);
        }
    }

    public void disableYesAndNo() {
        if (yesButton != null) {
            yesButton.setVisible(false);
            yesButton.setEnabled(false);
            onYes = null;
        }
        if (noButton != null) {
            noButton.setVisible(false);
            noButton.setEnabled(false);
            onNo = null;
        }
    }

    public void setPrompt(JLabel prompt){
        this.prompt = prompt;
    }

    // #region FileTransferServerClient methods

    /**
     * Updates the user if found, if not inserts the user
     * </p>
     * If the table the user is going to be on does not exist the table is created
     * and the user is added
     * 
     * @return true if the user is update/added false if this fails or is
     *         asynchronous
     */
    public boolean setupServerConnection() {
        return this.setupServerConnection(this.isAsync);
    }

    /**
     * Updates the user if found, if not inserts the user
     * </p>
     * If the table the user is going to be on does not exist the table is created
     * and the user is added
     * 
     * @param isAsync - {@true true} to run this function in another thread
     * @return true if the user is update/added false if this fails or is
     *         asynchronous
     */
    public boolean setupServerConnection(boolean iSAsync) {
        if (isAsync) {
            this.newThread((n) -> {
                this.setupServerConnection(false);
            });
            return false;
        }
        Map<String, String> parameters = new HashMap<>();
        parameters.put("ex_ip", this.fileTransferServerClient.getPublicIP());
        parameters.put("ip", this.fileTransferServerClient.getPrivateIP());
        parameters.put("username", this.getUsername());

        // active and mask not necessary since it will be added by insert
        ArrayList<QueryResult> results = this.fileTransferServerClient.pullByUsername(this.getUsername());
        if (results.size() == 0) {
            // System.out.println("User doesn't already exist");
            if (fileTransferServerClient.update(parameters)) {
                // System.out.println("update succeeded");
                return true;
            } else {
                if (fileTransferServerClient.insert(parameters)) {
                    // System.out.println("insert succeeded");
                    return true;
                } else {
                    if (fileTransferServerClient.create(
                            parameters.get("table") != null ? parameters.get("table") : parameters.get("ex_ip"))) {
                        // System.out.println("create succeeded");
                        if (fileTransferServerClient.insert(parameters)) {
                            // System.out.println("second insert succeeded");
                            return true;
                        }
                    }
                }
            }
        } else { // user exists so update it
            // System.out.println("User already existed");
            parameters.put("mask", String.valueOf(results.get(0).mask)); // copy mask over from first
            if (fileTransferServerClient.update(parameters)) {
                // System.out.println("update succeeded");
                return true;
            } else {
            } // update failed for some reason
            // System.out.println("Array was not empty");
        }
        return false;
    }

    public void promptToAccept(){
        if (yesButton == null || noButton == null){
            fileTransferReceiver.acceptFile();
            return;
        }

        if (prompt != null) { prompt.setText("Receive a file from " + fileTransferReceiver.getSenderUsername() + "?"); };

        onYes = (n)->{
            disableYesAndNo();
            newThread((n_)->{
                if (fileTransferReceiver.acceptFile()){
                };
            });
        };

        onNo = (n)->{ // let the sender know the file isn't wanted
            disableYesAndNo();
            newThread((n_)->{
                fileTransferReceiver.rejectFile();
            });
        };

        this.enableYesAndNo();
    }

    public void receivingFile(){
        if (this.prompt == null) return;
        this.prompt.setText("Receiving File from " + fileTransferReceiver.getSenderUsername() + "...");
    }

    public void fileReceived(){
        if (this.prompt == null) return;
        prompt.setText("File Received");
    }

    public void fileFailedToDownload(){
        if (this.prompt == null) return;
        this.prompt.setText("File Failed to Download");
    }

    /**
     * Get a map of usernames and IPs
     * 
     * @return Map with username keys and IP address values
     */
    public Map<String, String> getLocalClients() {
        Map<String, String> clients = new HashMap<>();
        ArrayList<QueryResult> results = this.fileTransferServerClient.pull();
        results.forEach((result) -> {
            clients.put(result.username, result.ip);

        });
        return clients;
    }

    // #endregion FileTransferServerClient methods


    public void sendFile(JLabel prompt, JButton button, String ip) {
        button.setEnabled(false);
        fileTransferSender.pickFile();

        fileTransferSender.setTarget(ip); // set the senders target

        newThread((n) -> {
            if (fileTransferSender.receiveString().equals("YES")) {
                System.out.println("Receiver accepted file");
                if (prompt != null) {
                    prompt.setText("Sending File...");
                }
                ;
                System.out.println(String.valueOf(fileTransferSender.sendChosenFile()) + " - Send chosen file result");
                if (prompt != null) {
                    prompt.setText("File Sent");
                }
                ;
            } else {
                if (prompt != null) {
                    prompt.setText("File Rejected :(");
                }
                ;
                System.out.println("Sender Rejected");
            }
            button.setEnabled(true);
        });
    }

    private void newThread(Consumer<Object> c) {
        this.newThread(c, null);
    }

    private void newThread(Consumer<Object> c, Object object) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                c.accept(object);
            }
        }).start();
    }

    /**
     * Set the methods to be syncrhonous as well as receiver and sender methods
     * 
     * @param isAsync
     */
    public void setAsync(boolean isAsync) {
        this.isAsync = isAsync;
        this.fileTransferReceiver.setAsync(isAsync);
        this.fileTransferSender.setAsync(isAsync);
    }

    /**
     * Get Consumer for the yes button
     * 
     * @return
     */
    public Consumer<ActionEvent> getOnYes() {
        return this.onYes;
    }

    /**
     * Get Consumer for the no button
     * 
     * @return
     */
    public Consumer<ActionEvent> getOnNo() {
        return this.onNo;
    }

    /**
     * Update the username
     * 
     * @param username - new username
     */
    public void setUsername(String username) {
        this.fileTransferSender.setUsername(username);
        this.fileTransferReceiver.setUsername(username);
    }

    /**
     * Get username if they match between sending and receiving
     * 
     * @return
     */
    public String getUsername() {
        String senderUser = fileTransferSender.getUsername();
        String receiverUser = fileTransferReceiver.getUsername();

        if (senderUser.equals(receiverUser)) {
            return senderUser;
        } else {
            return "Receiving: " + receiverUser + " Sending: " + senderUser;
        }
    }

    public static void main(String args[]) {
        FileTransferManager fileTransferManager = new FileTransferManager(false);
        JLabel prompt = new JLabel("Welcome to the File Transfer Manager");
        JComboBox<String> clientsBox = new JComboBox<>();
        Map<String, String> clients = fileTransferManager.getLocalClients();
        JButton fileSendButton = new JButton("Send File");
        JButton yesButton = new JButton("Yes");
        JButton noButton = new JButton("No");
        JFrame frame = new ControlledWindowJFrame("File Transfer Manager");
        SpringLayout layout = new SpringLayout();

        clients.keySet().forEach((key) -> {
            // System.out.println(key);
            clientsBox.addItem(key);
        });

        final Timer serverUpdateTimer = new Timer(61000, (n) -> { // start timer to update server in background
            // System.out.println("Timer called");
            fileTransferManager.setupServerConnection();
        });

        final Timer clientUpdateTimer = new Timer(30000, (n)->{
            Map<String, String> newClients = fileTransferManager.getLocalClients();
            clientsBox.removeAllItems();
            newClients.keySet().forEach((key) -> {
                clientsBox.addItem(key);
            });
        });

        UsernameDialog usernameDialog = new UsernameDialog((self) -> {
            UsernameDialog dialog = (UsernameDialog) self;
            fileTransferManager.setUsername(dialog.getUsername());

            fileTransferManager.setupServerConnection(); // upload to server
            if (!serverUpdateTimer.isRunning()) {
                serverUpdateTimer.start();
            }
            if (!clientUpdateTimer.isRunning()){
                clientUpdateTimer.start();
            }

            if (prompt != null) {
                prompt.setText("Welcome to the File Transfer Manager, " + dialog.getUsername());
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setResizable(false);

        // promptLabel.setHorizontalAlignment(SwingConstants.CENTER);

        fileTransferManager.setYesAndNoButtons(yesButton, noButton);

        fileSendButton.addActionListener((a) -> {
            fileTransferManager.sendFile(prompt, (JButton) a.getSource(), clients.get(clientsBox.getSelectedItem()));
        });

        yesButton.addActionListener((a) -> {
            if (fileTransferManager.getOnYes() != null) {
                fileTransferManager.getOnYes().accept((ActionEvent) a);
            }
        });

        noButton.addActionListener((a) -> {
            if (fileTransferManager.getOnNo() != null) {
                fileTransferManager.getOnNo().accept((ActionEvent) a);
            }
        });

        frame.add(prompt);

        Container contentPane = frame.getContentPane();
        // set the prompt
        layout.putConstraint(SpringLayout.NORTH, prompt, 8, SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, prompt, 0, SpringLayout.HORIZONTAL_CENTER, contentPane);

        // set the file receive button
        layout.putConstraint(SpringLayout.NORTH, clientsBox, 14, SpringLayout.SOUTH, prompt);
        layout.putConstraint(SpringLayout.WEST, clientsBox, 8, SpringLayout.WEST, contentPane);

        // set the file send button
        layout.putConstraint(SpringLayout.NORTH, fileSendButton, 14, SpringLayout.SOUTH, prompt);
        layout.putConstraint(SpringLayout.EAST, fileSendButton, 8, SpringLayout.EAST, contentPane);

        // set the yes button
        layout.putConstraint(SpringLayout.NORTH, yesButton, 14, SpringLayout.SOUTH, clientsBox);
        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, yesButton, 0, SpringLayout.HORIZONTAL_CENTER, clientsBox);

        // set the no button
        layout.putConstraint(SpringLayout.NORTH, noButton, 14, SpringLayout.SOUTH, fileSendButton);
        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, noButton, 0, SpringLayout.HORIZONTAL_CENTER,
                fileSendButton);

        frame.setLayout(layout);

        frame.add(clientsBox);
        frame.add(fileSendButton);
        frame.add(yesButton);
        frame.add(noButton);

        // hide yes and no buttons until needed
        yesButton.setVisible(false);
        noButton.setVisible(false);
        while (!usernameDialog.hasChosenUsername()) {
            try {
                Thread.sleep(10);
            } catch (Exception e) {
            }
        }
        frame.setVisible(true);
    }

}