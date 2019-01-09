import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

public class FileTransferManager{
    private FileTransferReceiver fileTransferReceiver; // start receiver without searching
    private FileTransferSender fileTransferSender; // start sender with 0 listening connections
    private boolean isSearchingForSender = false;
    private boolean isListeningForReceiver = false;
    private boolean receiverConnected = false;
    private boolean senderConnected = false;
    private boolean isAsync = false;

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

    public void receiveFile(){
        this.receiveFile(null, null);
    }

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
                if (prompt != null) { prompt.setText("Receiving File from " + fileTransferReceiver.getSenderIP() + " ..."); };

                // should have a sender by now
                if (fileTransferReceiver.receiveFile(fileTransferReceiver.receiveString())){
                    if (prompt != null) { prompt.setText("File Received"); };
                } else if (fileTransferReceiver.isAsync()) {
                    // File is downloading in background
                    // promptLabel.setText("File Downloading...");

                } else {
                    if (prompt != null) { prompt.setText("File Failed to Download"); };
                }
                if (source != null ){ source.setEnabled(true); };
            }
        }).start();
    }

    public void sendFile(){
        fileTransferSender.pickFile();
        fileTransferSender.sendChosenFile();
    }

    public void findSender(){
        this.findSender(null, null);
    }

    public void findSender(JLabel prompt, JButton button){
        this.isSearchingForSender = true;
        this.newThread((n)->{
            if (prompt != null) { prompt.setText("Looking for sender..."); };
            if (button != null) { button.setEnabled(false); };
            fileTransferReceiver.findSender();
            if (button != null) { button.setEnabled(true); };
            prompt.setText("Sender found");
            if (prompt != null) { prompt.setText("Sender found"); };
            this.isSearchingForSender = false;
        });
    }

    public void listen(){
        this.listen(null, null);
    }

    public void listen(JLabel prompt, JButton source){
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

    public static void main(String args[]){
        FileTransferManager fileTransferManager = new FileTransferManager(false, 0, true);

        JFrame frame = new JFrame("File Transfer Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setResizable(false);
        // frame.setLayout(new GridLayout(4,4));
        // frame.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        SpringLayout layout = new SpringLayout();
        
        
        JLabel prompt = new JLabel("Welcome to the File Transfer Manager");
        // promptLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton fileReceiveButton = new JButton("Receive File");
        JButton fileSendButton = new JButton("Send File");
        JButton findSenderButton = new JButton("Find Sender");
        JButton listenForReceiverButton = new JButton("Wait for Receiver");

        fileReceiveButton.addActionListener((a)->{
            fileTransferManager.receiveFile(prompt, (JButton) a.getSource());
        });

        fileSendButton.addActionListener((a)->{
            fileTransferManager.sendFile();
        });

        findSenderButton.addActionListener((a)->{
            fileTransferManager.findSender(prompt, (JButton) a.getSource());
        });

        listenForReceiverButton.addActionListener((a)->{
            fileTransferManager.listen(prompt, (JButton) a.getSource());
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


        frame.setLayout(layout);

        frame.add(fileReceiveButton);
        frame.add(fileSendButton);
        frame.add(findSenderButton);
        frame.add(listenForReceiverButton);

        frame.setVisible(true);
    }
}