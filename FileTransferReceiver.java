/* 
    Vers:   1.0.1   Added delimiter switch handshake to array
    Vers:   1.0.0   Initial coding receive/find sender
*/
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.Timer;

public class FileTransferReceiver {
    private String targetIP = "localhost";
    private String ipRange, selfIP, senderUsername, username;
    private boolean usingAsync = false;
    private boolean isLookingForSender = false;

    // Should be identical between sender/receiver
    private int initPort = 4999;
    private int filePort = 5000;
    private int stringPort = 5001;
    private String hostCode = "ADAM";
    private String delimiter = "\\|"; // regex delimiter

    /* Ports
    init: 4999
    Strings: 5001
    File: 5000
    */

    public FileTransferReceiver(boolean shouldFindSender){
        try{
            this.selfIP = InetAddress.getLocalHost().getHostAddress();
            this.ipRange = this.selfIP.substring(0, this.selfIP.lastIndexOf(".")+1);
    
            if (shouldFindSender){
                if (!this.findSender()){
                    System.out.println("No Sender Found");
                } else{
                    System.out.println("Sender found at " + targetIP);
                }
            } else {
                // don't search for sender yet (same as without params)
            }
        } catch (Exception e){
            this.selfIP = "localhost";
        }
    }

    public FileTransferReceiver(){
        this(false);
    }

    /**
     * Always false if async 
     * @return true if sender is found false if not found or using async findSender
     */
    public boolean findSender(){
        return this.findSender(this.usingAsync);
    }

    /**
     * Finds the ip of the sender and stores it
     * @return true if a sender is found false if no sender found
     */
    private boolean findSender(final boolean isAsync){
        if (isAsync){ // use another thread to find the sender
            new GeneralThread((n)->{
                System.out.println("Starting new Thread");
                this.isLookingForSender = true;
                this.findSender(false);
                this.isLookingForSender = false;
            }, null);
            return false;
        }
        boolean senderFound = false;
        for (int i = 0; i<256; i++){
            this.targetIP = this.ipRange+i;
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(InetAddress.getByName(this.targetIP), this.initPort), 100);

                // System.out.println("Receiver: " + this.targetIP);
                InputStream iS = socket.getInputStream();
                Scanner scanner = new Scanner(iS);
                Scanner delimitedScanner = scanner.useDelimiter("\\A");

                StringBuilder stringBuilder = new StringBuilder();

                while (delimitedScanner.hasNext()){
                    stringBuilder.append(delimitedScanner.next());
                }

                socket.close();
                delimitedScanner.close();
                scanner.close();
                iS.close();

                String initHandshake[] = stringBuilder.toString().split(this.delimiter); // split handshake by delimiter

                if (initHandshake != null && initHandshake[0].contains(this.hostCode)){
                    senderFound = true;
                    this.senderUsername = initHandshake.length >=2 ? initHandshake[1] : "Unknown User";
                    break;
                } else {
                    System.out.println("Handshake of length " + initHandshake.length);
                    System.out.println("Failed handshake with ");
                    for (String s: initHandshake){
                        System.out.print(s);
                    }
                }

            }catch(Exception e){
                // System.out.println("Exception thrown");
                try{
                    if (!socket.isClosed()){
                        socket.close();
                    }
                } catch (Exception e2){
                }
                
                continue;
            }
        }
        if (!senderFound){
            this.targetIP = "localhost";
        }
        return senderFound;
    }

    /**
     * Gets string over port 5001
     * @return received string or null if failed or asynchronous
     */
    public String receiveString(){
        return this.receiveString(this.usingAsync);
    }

    
    private String receiveString(final boolean isAsync){
        if (isAsync){ // use another thread to receive the string
            new GeneralThread((n)->{
                this.receiveString(false);
            }, null);
            return null;
        }
        try{
            Socket stringSocket = new Socket();
            stringSocket.connect(new InetSocketAddress(InetAddress.getByName(this.targetIP), this.stringPort), 1000);
            InputStream stringInputStream = stringSocket.getInputStream();
            Scanner scanner = new Scanner(stringInputStream);
            Scanner delimitedScanner = scanner.useDelimiter("\\A");

            StringBuilder stringBuilder = new StringBuilder();

            while (delimitedScanner.hasNext()){
                stringBuilder.append(delimitedScanner.next());
            }
            stringSocket.close();
            delimitedScanner.close();
            scanner.close();
            stringInputStream.close();
            return stringBuilder.toString();
        } catch (Exception e){
            return null;
        }   
    }

    /**
     * Reject the incoming file
     * @return
     */
    public boolean rejectFile(){
        try{
            Socket stringSocket = new Socket();
            stringSocket.connect(new InetSocketAddress(InetAddress.getByName(this.targetIP), this.stringPort), 500);
            // Get socket's output stream
            OutputStream socketOutputStream = stringSocket.getOutputStream();
            socketOutputStream.write("NO".getBytes());

            socketOutputStream.flush();
            socketOutputStream.close();
            stringSocket.close();
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        
    }

    /**
     * Reject the incoming file
     * @return
     */
    public boolean acceptFile(){
        try{
            Socket stringSocket = new Socket();
            InetSocketAddress targetAddress = new InetSocketAddress(InetAddress.getByName(this.targetIP), this.stringPort);
            stringSocket.connect(targetAddress, 500);
            // Get socket's output stream
            OutputStream socketOutputStream = stringSocket.getOutputStream();
            socketOutputStream.write("YES".getBytes());
            socketOutputStream.flush();
            socketOutputStream.close();
            stringSocket.close();
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        
    }


    /**
     * Downloads file over port 5000 and saves it
     * @param filename - filename to download as
     * @return true if file successfully downloaded false if failed or asynchronous
     */
    public boolean receiveFile(String filename){
        return this.receiveFile(filename, this.usingAsync);
    }
    
    /**
     * Download the file from the targetIP
     * @param filename - filename of incoming file
     * @param isAsync - should the file be downloaded in the background
     * @return
     */
    private boolean receiveFile(final String filename, boolean isAsync){
        if (isAsync){ // use another thread to find the sender
            new GeneralThread((n)->{
                this.receiveFile(filename, false);
            }, null);
            return false;
        }
        try{
            Socket socket = new Socket();
            InetSocketAddress targetAddress = new InetSocketAddress(InetAddress.getByName(this.targetIP), this.filePort);
            socket.connect(targetAddress, 500);
            byte[] contents = new byte[10000];

            FileOutputStream fos = new FileOutputStream(filename);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            InputStream is = socket.getInputStream();
    
            // Number of bytes read in one read() call
            int bytesRead = 0;
    
            while ((bytesRead = is.read(contents)) != -1){
                bos.write(contents, 0, bytesRead);
            }
    
            bos.flush();
            socket.close();
            bos.close();
            is.close();
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        
    }

    /**
     * Returns if the receiver has found a sender
     * @return true if target is not self
     */
    public boolean hasFoundSender(){
        return !this.targetIP.equals("localhost");
    }

    /**
     * Get the String representation of the Sender's IP address
     * @return IP address of the connected sender
     */
    public String getSenderIP(){
        return this.targetIP;
    }

    /**
     * Return sender's username or "Unknown User" if none is sent
     * @return Username of the sender
     */
    public String getSenderUsername(){
        return this.senderUsername;
    }

    /**
     * 
     * @return
     */
    public boolean isLookingForSender(){
        return this.isLookingForSender;
    }

    /**
     * Switches functions to being Asynchronous
     * @param shouldUseAsync
     */
    public void useAsync(boolean shouldUseAsync){
        this.usingAsync = shouldUseAsync;
    }

    /**
     * Switches synchronicity
     * @param shouldUseAsync true for asynchronous false for synchronous
     */
    public void setAsync(boolean shouldUseAsync){
        this.usingAsync = shouldUseAsync;
    }

    /**
     * Returns if this file receiver is functioning asynchronously
     * @return true is asynchronous
     */
    public boolean isAsync(){
        return this.usingAsync;
    }

    /**
     * Update the username
     * @param username - new username
     */
    public void setUsername(String username){
        this.username = username;
    }

    public String getUsername(){
        return this.username;
    }

    private class GeneralThread implements Runnable {
        Consumer<Object> consumer;
        Object object;
        Thread thread;
    
        public GeneralThread(Consumer<Object> consumer, Object object) {
            Runnable r = this;
            this.consumer = consumer;
            this.object = object;
            this.thread = new Thread(r);
            this.thread.start();
        }
    
        /**
         * Execute the consumer
         */
        @Override
        public void run() {
            this.consumer.accept(object);
            this.thread.interrupt();
        }
    
        public void stop() { this.thread.interrupt(); }
    
    }

    public static void main(String[] args) throws Exception {

        /* Command line version
        String filename = FileTransferReceiver.receiveString();
        FileTransferReceiver.receiveFile(filename);

        System.out.println("File saved successfully!");
        */

        JFrame frame = new JFrame("File Receiver");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        
        JButton b = new JButton("Receive File");
        final FileTransferReceiver fileTransferReceiver = new FileTransferReceiver();
        

        ActionListener al = new ActionListener(){
            
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton button = (JButton) (e.getSource());
                
                try {
                    // FileTransferReceiver.changeButtonText(button, "Clicked");
                    button.setText("Clicked");
                    new Thread(new Runnable(){
                        @Override
                        public void run(){
                            FileTransferReceiver ftr = new FileTransferReceiver(true);
                            String filename = ftr.receiveString();
                            ftr.receiveFile(filename);
                        }
                    }).start();
                    System.out.println("Clicked");
                    
                    // if (!ftr.foundSender()){
                    //     button.setText("No Sender Available");
                    //     return;
                    // } else {
                    //     button.setText("Downloading file...");
                    // }
                    
                    button.setText("File Received");
                    Timer t = new Timer(2000, (n)->{
                        button.setText("Receive File");
                    });
                    t.setRepeats(false);
                    t.start();
                } catch (Exception ex) {
                    System.out.println("Failed to get file");
                }
                // button.setText("Receive File");
            }
        };

        b.addActionListener(al);
        frame.add(b);
        // frame.pack(); // sets to smaller size
        frame.setVisible(true);
    }
}