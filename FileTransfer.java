import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

public class FileTransfer{
    private static boolean isSearching = false;
    private static boolean isListening = false;
    private static boolean receiverConnected = false;
    private static boolean senderConnected = false;


    public static void main(String args[]){
        FileTransferReceiver fileTransferReceiver = new FileTransferReceiver(false); // start receiver without searching
        FileTransferSender fileTransferSender = new FileTransferSender(0); // start sender with 0 listening connections

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
            JButton source = (JButton) a.getSource();
            source.setEnabled(false);
            new Thread(new Runnable(){
                @Override
                public void run() {
                    if (!fileTransferReceiver.hasFoundSender()){
                        prompt.setText("Looking for sender...");
                        fileTransferReceiver.findSender();
                        // wait till it finds a sender
                        while(!fileTransferReceiver.hasFoundSender()){try{Thread.sleep(10);}catch(Exception e){}}
                        prompt.setText("Sender Found");
                    }
                    prompt.setText("Receiving File from " + fileTransferReceiver.getSenderIP() + " ...");

                    // should have a sender by now
                    if (fileTransferReceiver.receiveFile(fileTransferReceiver.receiveString())){
                        prompt.setText("File Received");
                    } else if (fileTransferReceiver.isAsync()) {
                        // File is downloading in background
                        // promptLabel.setText("File Downloading...");

                    } else {
                        prompt.setText("File Failed to Download");
                    }
                    source.setEnabled(true);
                }
            }).start();
        });

        fileSendButton.addActionListener((a)->{
            fileTransferSender.pickFile();
            fileTransferSender.sendChosenFile();
        });

        findSenderButton.addActionListener((a)->{
            prompt.setText("Looking for sender...");
            JButton source = (JButton) a.getSource();
            source.setEnabled(false);
            fileTransferReceiver.findSender();
            source.setEnabled(true);
            prompt.setText("Sender found");
        });

        listenForReceiverButton.addActionListener((a)->{
            prompt.setText("Listening for receiver...");
            JButton source = (JButton) a.getSource();
            source.setEnabled(false);
            fileTransferSender.listen();
            source.setEnabled(true);
            prompt.setText("Receiver found");
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