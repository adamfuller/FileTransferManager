

public class test{
    public static void main(String args[]){
        FileTransferReceiver ftr = new FileTransferReceiver();
        ftr.useAsync(true);
        ftr.findSender();
        try{
            Thread.sleep(300);
        } catch(Exception e){
            
        }
        if (ftr.isLookingForSender()){
            System.out.println("File Transfer Receiver is looking for sender");
        } else {
            System.out.println("File Transfer Receiver is NOT looking for sender");
        }
        try{
            Thread.sleep(28000);
        } catch (Exception e){

        }
        if (ftr.isLookingForSender()){
            System.out.println("File Transfer Receiver is looking for sender");
        } else {
            System.out.println("File Transfer Receiver is NOT looking for sender");
        }
        
    }
}