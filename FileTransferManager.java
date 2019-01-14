import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SpringLayout;

public class FileTransferManager{
    private FileTransferReceiver fileTransferReceiver; // start receiver without searching
    private FileTransferSender fileTransferSender; // start sender with 0 listening connections
    private boolean isSearchingForSender = false;
    private boolean isListeningForReceiver = false;
    private boolean receiverConnected = false;
    private boolean senderConnected = false;
    private boolean isAsync = false;
    private Consumer<ActionEvent> onYes, onNo; // consumers for Yes and No buttons
    private JButton yesButton, noButton;

    public FileTransferManager(boolean listen, int numConnections, boolean isAsync){
        this.isAsync = isAsync;
        this.fileTransferReceiver = new FileTransferReceiver(listen);
        this.fileTransferReceiver.useAsync(isAsync);
        this.fileTransferSender = new FileTransferSender(numConnections);
        this.fileTransferSender.setAsync(isAsync);
    }

    public FileTransferManager(boolean listen, int numConnections){
        this(listen, numConnections, false);
    }

    public void setYesAndNoButtons(JButton yesButton, JButton noButton){
        this.yesButton = yesButton;
        this.noButton = noButton;
    }

    public void activateYesAndNo(){
        if ( yesButton != null ){ 
            yesButton.setVisible(true);
            yesButton.setEnabled(true);
        }
        if (noButton != null){
            noButton.setVisible(true);
            noButton.setEnabled(true);
        }
    }

    public void disableYesAndNo(){
        if ( yesButton != null ){ 
            yesButton.setVisible(false);
            yesButton.setEnabled(false);
            onYes = null;
        }
        if (noButton != null){
            noButton.setVisible(false);
            noButton.setEnabled(false);
            onNo = null;
        }
    }

    public void receiveFile(){
        this.receiveFile(null, null);
    }

    /**
     * Actual file retreival portion of receiveFile
     * @param prompt - prompt for the user
     * @param source - button that activated the receiveFile method
     */
    public void getTheFile(JLabel prompt, JButton source){
        if (prompt != null) { prompt.setText("Receiving File from " + fileTransferReceiver.getSenderUsername() + "..."); };
        // should have a sender by now
        String filename = fileTransferReceiver.receiveString();
        if (fileTransferReceiver.receiveFile(filename)){
            if (prompt != null) { prompt.setText("File Received"); };
        } else if (fileTransferReceiver.isAsync()) {
            // File is downloading in background
            // promptLabel.setText("File Downloading...");

        } else {
            if (prompt != null) { prompt.setText("File Failed to Download"); };
            System.out.println(filename + " failed to download");
            
        }
        if (source != null ){ source.setEnabled(true); };
    }

    /**
     * Method to start the file retrieving process
     * @param prompt - prompt to provide user with info
     * @param source - button that activated this method
     */
    public void receiveFile(JLabel prompt, JButton source){
        if (source != null ){ source.setEnabled(false); };
        new Thread(new Runnable(){
            @Override
            public void run() {
                if (!fileTransferReceiver.hasFoundSender()){
                    if (prompt != null) { prompt.setText("Looking for sender..."); };
                    fileTransferReceiver.findSender();
                    // wait till it finds a sender
                    while(!fileTransferReceiver.hasFoundSender()){try{Thread.sleep(10);}catch(Exception e){}}
                    if (prompt != null) { prompt.setText("Sender Found"); };
                }
                
                if (yesButton == null || noButton == null){
                    getTheFile(prompt, source);
                    return;
                }

                if (prompt != null) { prompt.setText("Receive a file from " + fileTransferReceiver.getSenderUsername() + "?"); };
                
                

                onYes = (n)->{
                    disableYesAndNo();
                    newThread((n_)->{
                        if (fileTransferReceiver.acceptFile()){
                            try{Thread.sleep(500);}catch(Exception e){}
                            getTheFile(prompt, source);
                        };
                    });
                };

                onNo = (n)->{ // let the sender know the file isn't wanted
                    disableYesAndNo();
                    newThread((n_)->{
                        fileTransferReceiver.rejectFile();
                    });
                    if (source != null ){ source.setEnabled(true); };
                };

                activateYesAndNo();

            }

                
        }).start();
    }

    public void sendFile(){
        this.sendFile(null, null);
    }

    public void sendFile(JLabel prompt, JButton button){
        button.setEnabled(false);
        fileTransferSender.pickFile();
        if (!fileTransferSender.hasTarget()){
            this.listen(prompt, button, true);
        }

        newThread((n)->{
            while (!fileTransferSender.hasTarget()){
                try{Thread.sleep(10);}catch(Exception e){}
            }
            if (fileTransferSender.receiveString().equals("YES")){
                System.out.println("Receiver accepted file");
                if (prompt != null) { prompt.setText("Sending File..."); };
                System.out.println(String.valueOf(fileTransferSender.sendChosenFile()) + " - Send chosen file result");
                if (prompt != null) { prompt.setText("File Sent"); };
            } else {
                if (prompt != null) { prompt.setText("File Rejected :("); };
                System.out.println("Sender Rejected");
            }
            button.setEnabled(true);
        });
        // newThread((n)->{
        //     while(!button.isEnabled()){
        //         prompt.setText("Sending... " + fileTransferSender.getProgress() + "%");
        //         try{ Thread.sleep(10); } catch (Exception e){ }
        //     }
        // });
        
    }

    public void findSender(){
        this.findSender(null, null);
    }

    public void findSender(JLabel prompt, JButton button){
        this.findSender(prompt, button, this.isAsync);
    }

    public void findSender(JLabel prompt, JButton button, boolean useAsync){
        if (useAsync){
            newThread((n)->{
                this.findSender(prompt, button, false);
            });
            return;
        }
        this.isSearchingForSender = true;
        if (prompt != null) { prompt.setText("Looking for sender..."); };
        if (button != null) { button.setEnabled(false); };
        fileTransferReceiver.findSender();
        if (button != null) { button.setEnabled(true); };
        prompt.setText("Sender found");
        if (prompt != null) { prompt.setText("Sender found"); };
        this.isSearchingForSender = false;
    }

    public void listen(){
        this.listen(null, null);
    }

    public void listen(JLabel prompt, JButton source){
        this.listen(prompt, source, this.isAsync);
    }

    public void listen(JLabel prompt, JButton source, boolean useAsync){
        if (useAsync){
            newThread((n)->{
                this.listen(prompt, source, false);
            });
            return;
        }
        this.isListeningForReceiver = true;
        if (prompt != null ) { prompt.setText("Listening for receiver..."); };
        if (source != null ) { source.setEnabled(false); };
        fileTransferSender.listen();
        if (source != null ) { source.setEnabled(true); };
        if (prompt != null ) { prompt.setText("Receiver found"); };
        this.isListeningForReceiver=false;
    }

    private void newThread(Consumer<Object> c){
        this.newThread(c, null);
    }

    private void newThread(Consumer<Object> c, Object object){
        new Thread(new Runnable(){
            @Override
            public void run() {
                c.accept(object);
            }
        }).start();
    }

    /**
     * Set the methods to be syncrhonous as well as receiver and sender methods
     * @param isAsync
     */
    public void setAsync(boolean isAsync){
        this.isAsync = isAsync;
        this.fileTransferReceiver.setAsync(isAsync);
        this.fileTransferSender.setAsync(isAsync);
    }

    /**
     * Get Consumer for the yes button
     * @return
     */
    public Consumer<ActionEvent> getOnYes(){
        return this.onYes;
    }

    /**
     * Get Consumer for the no button
     * @return
     */
    public Consumer<ActionEvent> getOnNo(){
        return this.onNo;
    }

    /**
     * Update the username
     * @param username - new username
     */
    public void setUsername(String username){
        this.fileTransferSender.setUsername(username);
        this.fileTransferReceiver.setUsername(username);
    }

    /**
     * Get username if they match between sending and receiving
     * @return
     */
    public String getUsername(){
        String senderUser = fileTransferSender.getUsername();
        String receiverUser = fileTransferReceiver.getUsername();


        if (senderUser.equals(receiverUser)){
            return senderUser;
        } else {
            return "Receiving: " + receiverUser + " Sending: " + senderUser;
        }
    }

    public static void main(String args[]){
        FileTransferManager fileTransferManager = new FileTransferManager(false, 0, false);
        JLabel prompt = new JLabel("Welcome to the File Transfer Manager");

        UsernameDialog usernameDialog = new UsernameDialog((self)->{
            UsernameDialog dialog = (UsernameDialog) self;
            fileTransferManager.setUsername(dialog.getUsername());
            if (prompt != null){
                prompt.setText("Welcome to the File Transfer Manager, " + dialog.getUsername());
            }
        });

        JFrame frame = new JFrame("File Transfer Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setResizable(false);
        // frame.setLayout(new GridLayout(4,4));
        // frame.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        SpringLayout layout = new SpringLayout();
        
        // promptLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton fileReceiveButton = new JButton("Receive File");
        JButton fileSendButton = new JButton("Send File");
        JButton findSenderButton = new JButton("Find Sender");
        JButton listenForReceiverButton = new JButton("Wait for Receiver");
        JButton yesButton = new JButton("Yes");
        JButton noButton = new JButton("No");

        fileTransferManager.setYesAndNoButtons(yesButton, noButton);

        fileReceiveButton.addActionListener((a)->{
            fileTransferManager.receiveFile(prompt, (JButton) a.getSource());
        });

        fileSendButton.addActionListener((a)->{
            fileTransferManager.sendFile(prompt, (JButton) a.getSource());
        });

        findSenderButton.addActionListener((a)->{
            fileTransferManager.findSender(prompt, (JButton) a.getSource());
        });

        listenForReceiverButton.addActionListener((a)->{
            fileTransferManager.listen(prompt, (JButton) a.getSource());
        });

        yesButton.addActionListener((a) -> { 
            if (fileTransferManager.getOnYes() != null){
                fileTransferManager.getOnYes().accept((ActionEvent) a);
            }
         });
        
        noButton.addActionListener((a) -> { 
            if (fileTransferManager.getOnNo() != null){
                fileTransferManager.getOnNo().accept((ActionEvent) a);
            }
        });

        frame.add(prompt);

        Container contentPane = frame.getContentPane();
        // set the prompt
        layout.putConstraint( SpringLayout.NORTH, prompt, 8, SpringLayout.NORTH,   contentPane);
        layout.putConstraint( SpringLayout.HORIZONTAL_CENTER, prompt, 0, SpringLayout.HORIZONTAL_CENTER, contentPane);

        // set the file receive button
        layout.putConstraint(SpringLayout.NORTH, fileReceiveButton, 14, SpringLayout.SOUTH, prompt);
        layout.putConstraint(SpringLayout.WEST, fileReceiveButton, 8, SpringLayout.WEST, contentPane);

        // set the file send button
        layout.putConstraint(SpringLayout.NORTH, fileSendButton, 14, SpringLayout.SOUTH, prompt);
        // layout.putConstraint(SpringLayout.EAST, fileSendButton, 8, SpringLayout.EAST, contentPane);

        // set the find sender button
        layout.putConstraint(SpringLayout.NORTH, findSenderButton, 14, SpringLayout.SOUTH, fileReceiveButton);
        // layout.putConstraint(SpringLayout.WEST, findSenderButton, 14, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, findSenderButton, 0, SpringLayout.HORIZONTAL_CENTER, fileReceiveButton);

        // set the listenForReceiver button
        layout.putConstraint(SpringLayout.NORTH, listenForReceiverButton, 14, SpringLayout.SOUTH, fileSendButton);
        layout.putConstraint(SpringLayout.EAST, listenForReceiverButton, -14, SpringLayout.EAST, contentPane);
        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, fileSendButton, 0, SpringLayout.HORIZONTAL_CENTER, listenForReceiverButton);

        // set the yes button
        layout.putConstraint(SpringLayout.NORTH, yesButton, 14, SpringLayout.SOUTH, findSenderButton);
        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, yesButton, 0, SpringLayout.HORIZONTAL_CENTER, findSenderButton);

        // set the no button
        layout.putConstraint(SpringLayout.NORTH, noButton, 14, SpringLayout.SOUTH, listenForReceiverButton);
        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, noButton, 0, SpringLayout.HORIZONTAL_CENTER, listenForReceiverButton);

        frame.setLayout(layout);

        frame.add(fileReceiveButton);
        frame.add(fileSendButton);
        frame.add(findSenderButton);
        frame.add(listenForReceiverButton);
        frame.add(yesButton);
        frame.add(noButton);
        
        // hide yes and no buttons until needed
        yesButton.setVisible(false);
        noButton.setVisible(false);
        while (!usernameDialog.hasChosenUsername()){
            try{
                Thread.sleep(10);
            } catch(Exception e){}
        }
        frame.setVisible(true);
    }
}