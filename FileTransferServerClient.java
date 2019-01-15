/**
 * 
 * Vers:   1.0.0   Initial coding getIP, create, pull, insert, update, getRequest
 */
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.net.URLEncoder;
import java.util.Map;

public class FileTransferServerClient {
    private final String serverAddress = "http://prattpi.hopto.org/FileTransferServer/";

    private String getIP() {
        return this.getRequest(this.serverAddress + "getIP.php", null);
    }

    public String create(){
        Map<String, String> params = new HashMap<>();
        params.put("ex_ip", this.getIP());
        return this.getRequest(this.serverAddress + "create.php", params);
    }

    public String insert(String username, byte mask){
        
        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", username);
        parameters.put("mask", String.valueOf(mask) );
        parameters.put("ex_ip", this.getIP());
        try{
            parameters.put("ip", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e){
            parameters.put("ip", "unknown");
        }

        LocalTime localTime = LocalTime.now();
        StringBuilder activeBuilder = new StringBuilder();
        activeBuilder.append(localTime.getHour());
        activeBuilder.append(localTime.getMinute());
    
        parameters.put("active", activeBuilder.toString());

        return this.getRequest(this.serverAddress + "insert.php", parameters);
    }

    public String update(String username, byte mask){
        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", username);
        parameters.put("mask", String.valueOf(mask) );
        parameters.put("ex_ip", this.getIP());

        try{
            parameters.put("ip", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e){
            parameters.put("ip", "unknown");
        }

        LocalTime localTime = LocalTime.now();
        StringBuilder activeBuilder = new StringBuilder();
        activeBuilder.append(localTime.getHour());
        activeBuilder.append(localTime.getMinute());
    
        parameters.put("active", activeBuilder.toString());

        return this.getRequest(this.serverAddress + "update.php", parameters);
    }

    /**
     * Pull all from a specific table
     * @param table
     * @return
     */
    public String pull(String table){
        Map<String, String> parameters = new HashMap<>();
        parameters.put("table", table);

        return this.getRequest(this.serverAddress + "pull.php", parameters);
    }

    /**
     * Pull for a specific username from a table;
     * @param username
     * @param table
     * @return
     */
    public String pull(String username, String table){
        Map<String, String> parameters = new HashMap<>();
        parameters.put("table", table);
        parameters.put("username", username);

        return this.getRequest(this.serverAddress + "pull.php", parameters);
    }

    public String getRequest(String urlString, Map<String, String> parameters) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            con.setDoOutput(true);

            DataOutputStream outputStream = new DataOutputStream(con.getOutputStream());

            if (parameters != null) {
                outputStream.writeBytes(this.getParamsString(parameters));
                // System.out.println(this.getParamsString(parameters));
            }
            outputStream.flush();
            outputStream.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();

            return content.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getParamsString(Map<String, String> params) {
        StringBuilder result = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {

                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                result.append("&");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String resultString = result.toString();
        return resultString.length() > 0 ? resultString.substring(0, resultString.length() - 1) : resultString;
    }

    public static void main(String args[]){
        FileTransferServerClient ftsc = new FileTransferServerClient();
        System.out.println(ftsc.pull("sample_username", "open"));
    }

}