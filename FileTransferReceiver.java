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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.function.Consumer;

public class FileTransferReceiver {
    private FileTransferManager manager;
    private String targetIP = "localhost";
    private String senderUsername, username;
    private boolean usingAsync = false;
    private ServerSocket initServerSocket;
    private ServerSocket stringServerSocket;
    private ServerSocket fileServerSocket;
    private boolean hasAccepted = false;
    private boolean hasRejected = false;

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

    public FileTransferReceiver(){
        
    }

    public void setManager(FileTransferManager manager){
        this.manager = manager;
    }

    //#region receiving region

    private String receiveString(Socket stringSocket, final boolean isAsync){
        if (isAsync){ // use another thread to receive the string
            new GeneralThread((n)->{
                this.receiveString(stringSocket, false);
            }, null);
            return null;
        }
        try{

            InputStream stringInputStream = stringSocket.getInputStream();
            Scanner scanner = new Scanner(stringInputStream);
            Scanner delimitedScanner = scanner.useDelimiter("\\A");

            StringBuilder stringBuilder = new StringBuilder();

            while (delimitedScanner.hasNext()){
                stringBuilder.append(delimitedScanner.next());
            }
            delimitedScanner.close();
            scanner.close();
            stringInputStream.close();
            return stringBuilder.toString();

        } catch (Exception e){
            return null;
        }   
    }
    
    /**
     * Download the file from the targetIP
     * @param filename - filename of incoming file
     * @param isAsync - should the file be downloaded in the background
     * @return
     */
    private boolean receiveFile(final String filename, Socket socket, boolean isAsync){
        if (isAsync){ // use another thread to find the sender
            new GeneralThread((n)->{
                this.receiveFile(filename, socket, false);
            }, null);
            return false;
        }
        try{
            
            byte[] contents = new byte[10000];
            String home = System.getProperty("user.home");
            FileOutputStream fos = new FileOutputStream(home + "/Downloads/"+filename);
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

    public boolean receiveHandshake(Socket initSocket){
        this.targetIP = initSocket.getInetAddress().getHostAddress();
        String initString = this.receiveString(initSocket, false);
        String initHandshake[] = initString.split(this.delimiter); // split handshake by delimiter

        if (initHandshake != null && initHandshake[0].contains(this.hostCode)){
            this.senderUsername = initHandshake.length >=2 ? initHandshake[1] : "Unknown User";
        } else {
            System.out.println("Failed handshake with ");
            for (String s: initHandshake){
                System.out.print(s);
            }
        }

        if (this.manager!=null){
            this.manager.promptToAccept();
        }

        while (!this.hasAccepted || !this.hasRejected){
            try{Thread.sleep(10);}catch (Exception e){}
        }
        if (this.hasRejected){
            return false;
        }
        this.hasAccepted = false;
        this.hasRejected = false;
        return true;
    }

    //#endregion receiving region

    /**
     * Start listening for a handshake the a filename then a file
     */
    public void startListening(){ // this will get a lot of exceptions thrown
        new GeneralThread((NULL)->{
            try{
                if (this.initServerSocket == null){
                    this.initServerSocket = new ServerSocket(this.initPort, 8);
                } else {
                    return;
                }
                if (this.stringServerSocket == null){
                    this.stringServerSocket = new ServerSocket(this.stringPort, 8);
                } else {
                    return;
                }
                if (this.fileServerSocket == null) {
                    this.fileServerSocket = new ServerSocket(this.filePort, 8);
                } else {
                    return;
                }

            } catch (Exception e){
                e.printStackTrace();
            }
            while(true){
                try{
                    // handshake phase
                    Socket initSocket = this.initServerSocket.accept();
                    if (!this.receiveHandshake(initSocket)){
                        continue; // if handshake fails or file rejected restart
                    }

                    // filename getting phase
                    Socket stringSocket = this.stringServerSocket.accept();
                    String filename = this.receiveString(stringSocket, false);
                    stringSocket.close();

                    if (filename == null){
                        continue;
                    }

                    // File getting phase
                    Socket fileSocket = this.fileServerSocket.accept();
                    if (this.manager != null){
                        this.manager.receivingFile();
                    }

                    if (this.receiveFile(filename, fileSocket, false)){
                        if (this.manager != null){
                            this.manager.fileReceived();
                        }
                    } else {
                        if (this.manager != null){
                            this.manager.fileFailedToDownload();
                        }
                    }
                    fileSocket.close();
                    
                } catch (Exception e){
                    // e.printStackTrace();
                    break;
                }
            }
        }, null);
    }

    public boolean stopListening(){
        try{
            if (this.initServerSocket != null){
                this.initServerSocket.close();
            }
            if (this.stringServerSocket != null){
                this.stringServerSocket.close();
            }
            if (this.fileServerSocket != null){
                this.fileServerSocket.close();
            }
            return true;
        } catch (Exception e){
            return false;
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
            this.hasRejected = true;
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
        this.hasAccepted = true;
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
    
}