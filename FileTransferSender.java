/* 
    Vers:   1.0.1   Added delimiter, switch handshake to array to send extra info
    Vers:   1.0.0   Initial coding send/listen for receiver
*/
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class FileTransferSender {
    private boolean isAsync = false;
    private String selfIP, ipRange;
    private String username = "USER_" + Math.round((float) (200.0 * Math.random()) );
    private String targetIP = "localhost";
    private static int defaultConnections = 2;
    private int numConnections;
    public File chosenFile;
    public String chosenFilePath, chosenFilename;
    private int percentProgress = 0;
    
    // Should be identical between sender and receiver
    private String hostCode = "ADAM";
    private String delimiter = "|";
    private int initPort = 4999;
    private int filePort = 5000;
    private int stringPort = 5001;
    
    /**
     * Create FileTransferSender with default number of available connections
     */
    public FileTransferSender(){
        this(FileTransferSender.defaultConnections);
    }

    /**
     * Create FileTransferSender with numConnections of available connections
     * @param numConnections - number of available connections
     */
    public FileTransferSender(int numConnections){
        this.numConnections = numConnections;
        new GeneralThread((n)->{
            try {
                // System.out.println("About to get own ip");
                
                    this.selfIP = InetAddress.getLocalHost().getHostAddress();
                    this.ipRange = this.selfIP.substring(0, this.selfIP.lastIndexOf(".")+1);

                // System.out.println("Got own ip");
            } catch (Exception e){
                this.selfIP = "localhost";
            }
        }, null);
        if (numConnections == 0){
            return;
        }
        this.listen();
    }

    //#region listen methods
    public boolean listen(int numConnections){
        this.numConnections = numConnections;
        return this.listen();
    }

    public boolean listen(){
        return this.listen(this.isAsync);
    }

    /**
     * Wait for a connection to be made
     * @return true if connection is made false if exception is thrown
     */
    public boolean listen(boolean isAsync){
        if (isAsync){ // use another thread to listen
            new GeneralThread((n)->{
                this.listen(false);
            }, null);
            return false;
        }
        try{
            ServerSocket waitServerSocket = new ServerSocket(initPort, this.numConnections); // wait for socket connection on port 4999
            System.out.println("Waiting for connection...");
            Socket socket = waitServerSocket.accept();
            System.out.println("Connection made.");
            this.targetIP = socket.getInetAddress().getHostAddress();
            // System.out.println("Sender: " + this.targetIP);
            OutputStream os = socket.getOutputStream();
            StringBuilder initHandshake = new StringBuilder();

            initHandshake.append(this.hostCode); // add host code
            initHandshake.append(this.delimiter); // split
            initHandshake.append(this.username); // add username
            initHandshake.append(this.delimiter); // split
            
            os.write(initHandshake.toString().getBytes()); // send handshake
            os.flush();
            os.close();
            socket.close();
            waitServerSocket.close();
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    //#endregion

    public String receiveString(){
        try{
            ServerSocket ssock = new ServerSocket(this.stringPort, 8);
            // new ServerSocket(port, backlog, bindAddr)
            Socket stringSocket = ssock.accept();
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
            ssock.close();
            return stringBuilder.toString();
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }


    /**
     * Send a string over port 5001
     * 
     * @param string
     * @return
     */
    public boolean sendString(String string) {
        try {
            // Initialize Sockets
            // ServerSocket ssock = new ServerSocket(5001);
            ServerSocket ssock = new ServerSocket(this.stringPort, 8);
            // new ServerSocket(port, backlog, bindAddr)
            System.out.println("before accept");
            Socket socket = ssock.accept();
            System.out.println("Accepted");

            // Get socket's output stream
            OutputStream socketOutputStream = socket.getOutputStream();
            System.out.println("Output stream opened");
            socketOutputStream.write(string.getBytes());
            System.out.println("String written");
            socketOutputStream.flush();
            socketOutputStream.close();
            // File transfer done. Close the socket connection!
            socket.close();
            ssock.close();
            System.out.println("String sent");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Send file over port 5000
     * 
     * @param filename
     * @return
     */
    public boolean sendFile(String filename) {
        try {
            // Initialize Sockets
            // ServerSocket ssock = new ServerSocket(5000);
            ServerSocket ssock = new ServerSocket(filePort, 8);
            Socket socket = ssock.accept();

            // Specify the file
            File file = new File(filename);

            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);

            // Get socket's output stream
            OutputStream socketOutputStream = socket.getOutputStream();

            // Read File Contents into contents array
            byte[] contents;
            long fileLength = file.length();
            long current = 0;
            long lastPercent = 0;

            while (current != fileLength) {
                int size = 10000;
                if (fileLength - current >= size)
                    current += size;
                else {
                    size = (int) (fileLength - current);
                    current = fileLength;
                }
                contents = new byte[size];
                bis.read(contents, 0, size);
                socketOutputStream.write(contents);
                if (((current * 100) / fileLength - lastPercent) > 1) {
                    System.out.println("Sending file ... " + (current * 100) / fileLength + "% complete!");
                    this.percentProgress = (int) lastPercent;
                    lastPercent = (current * 100) / fileLength;
                }
            }

            socketOutputStream.flush();
            bis.close();
            // File transfer done. Close the socket connection!
            socket.close();
            ssock.close();
            System.out.println("File sent succesfully!");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public File pickFile(){
        JFileChooser fc = new JFileChooser();
        // fc.setVisible(true);
        fc.showOpenDialog(new JButton());

        this.chosenFile = fc.getSelectedFile();
        if (chosenFile == null){
            return null;
        }
        try{
            this.chosenFilePath = this.chosenFile.getCanonicalPath();
            this.chosenFilename = this.chosenFile.getName();
        }catch (Exception e){
            e.printStackTrace();
        }
        return this.chosenFile;
    }

    /**
     * Send the file chosen by pickFile
     * @return
     */
    public boolean sendChosenFile(){
        if (this.chosenFile == null || this.chosenFilePath == null || this.chosenFilename == null){
            return false;
        }
        boolean stringSent = this.sendString(this.chosenFilename);
        boolean fileSent = this.sendFile(this.chosenFilePath);
        return stringSent && fileSent;
    }

    /**
     * Switch methods to using asynchronous actions
     */
    public void useAsync(){
        this.setAsync(true);
    }

    /**
     * Switch state of objects synchronicity
     * @param shouldUseAsync
     */
    public void setAsync(boolean shouldUseAsync){
        this.isAsync = shouldUseAsync;
    }

    public boolean hasTarget(){
        return !this.targetIP.equals("localhost");
    }

    /**
     * Returns percent of file sent
     * @return
     */
    public int getProgress(){
        return this.percentProgress;
    }

    /**
     * Update the username
     * @param username
     */
    public void setUsername(String username){
        this.username = username;
    }

    /**
     * Get the current username
     * @return
     */
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
        if (args.length >= 1) {
            String filePath = args[0];
            String filename = new File(filePath).getName();
            FileTransferSender fts = new FileTransferSender();
            fts.sendString(filename);
            fts.sendFile(filePath);

        } else {
            
            JFrame frame = new JFrame("File Sender");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(300, 200);
            
            JButton b = new JButton("Pick File to Send");
            frame.add(b);
            frame.setVisible(true);
            

            b.addActionListener((a) -> {
                try {
                    b.setText("Waiting for Client...");
                    
                    // JFileChooser fc = new JFileChooser();
                    // // fc.setVisible(true);
                    // fc.showOpenDialog(b);
                    System.out.println("Button Clicked");
                    FileTransferSender fts = new FileTransferSender(0);
                    System.out.println("Sender made");
                    File f = fts.pickFile();
                    fts.listen(2);
                    
                    fts.sendString(fts.chosenFilename);
                    System.out.println("About to rename button");
                    b.setText("Sending file...");
                    fts.sendFile(fts.chosenFilePath);
                    b.setText("Pick another File");

                } catch (Exception e) {

                }
            });

        }

    }
}