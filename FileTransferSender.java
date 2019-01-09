/* Version 2
*/
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class FileTransferSender {
    private boolean isAsync = false;
    private String selfIP, ipRange;
    private String targetIP = "localhost";
    private String hostCode = "ADAM";
    private int initPort = 4999;
    private int filePort = 5000;
    private int stringPort = 5001;
    private static int defaultConnections = 2;
    private int numConnections;
    public File chosenFile;
    public String chosenFilePath, chosenFilename;

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
            os.write(this.hostCode.getBytes());
            os.flush();
            os.close();
            socket.close();
            waitServerSocket.close();
            return true;
        }catch (Exception e){
            return false;
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
            ServerSocket ssock = new ServerSocket(this.stringPort, 2);
            // new ServerSocket(port, backlog, bindAddr)
            Socket socket = ssock.accept();

            // Get socket's output stream
            OutputStream socketOutputStream = socket.getOutputStream();
            socketOutputStream.write(string.getBytes());

            socketOutputStream.flush();
            // File transfer done. Close the socket connection!
            socket.close();
            ssock.close();
            System.out.println("String sent");
            return true;
        } catch (Exception e) {
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
            ServerSocket ssock = new ServerSocket(filePort, 2);
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
            return false;
        }
    }

    public File pickFile(){
        JFileChooser fc = new JFileChooser();
        // fc.setVisible(true);
        fc.showOpenDialog(new JButton());

        this.chosenFile = fc.getSelectedFile();
        try{
            this.chosenFilePath = this.chosenFile.getCanonicalPath();
        }catch (Exception e){
            e.printStackTrace();
        }
        this.chosenFilename = this.chosenFile.getName();
        return this.chosenFile;
    }

    /**
     * Send the file chosen by pickFile
     * @return
     */
    public boolean sendChosenFile(){
        if (this.chosenFile == null){
            return false;
        }
        return this.sendString(this.chosenFilename) && this.sendFile(this.chosenFilePath);
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

    private class GeneralThread implements Runnable {
        Consumer consumer;
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