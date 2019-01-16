/**
 * Vers:    2.0.0   Added QueryResult compatibility returning methods, added delete, pulls are now pullString
 * Vers:    1.0.0   Initial coding getIP, create, pull, insert, update, getRequest
 */
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.URLEncoder;
import java.util.Map;

public class FileTransferServerClient {
    private final String serverAddress = "http://prattpi.hopto.org/FileTransferServer/";

    /**
     * Obtain the external/public ip address of the network this device is on
     * 
     * @return String representation of the ip address
     */
    private String getPublicIP() {
        return this.getRequest(this.serverAddress + "getIP.php", null);
    }

    /**
     * Obtain the internal ip address of this device
     * 
     * @return String representation of the ip address
     */
    private String getPrivateIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Create a new table for the public ip of the device
     * 
     * @return success if completed anything else if failed
     */
    public String create() {
        Map<String, String> params = new HashMap<>();
        params.put("ex_ip", this.getPublicIP());
        return this.getRequest(this.serverAddress + "create.php", params);
    }

    /**
     * Delete records associated with {@code username}
     * </p>
     * Auto detect the table and the ip
     * 
     * @param username
     * @return success if completed anything else if failed
     */
    public String delete(String username) {
        Map<String, String> params = new HashMap<>();
        params.put("ex_ip", this.getPublicIP());
        params.put("username", username);
        params.put("ip", this.getPrivateIP());

        return this.getRequest(this.serverAddress + "delete.php", params);
    }

    /**
     * Add a new user with name {@code username} and mask {@code mask}
     * </p>
     * Auto detect the table and updates active
     * 
     * @param username - user's username
     * @param mask     - user's mask as int
     * @return success if completed anything else if failed
     */
    public String insert(String username, int mask) {
        return this.insert(username, (byte) mask);
    }

    /**
     * Add a new user with name {@code username} and mask {@code mask}
     * </p>
     * Auto detect the table and updates active
     * 
     * @param username - user's username
     * @param mask     - user's mask as byte
     * @return success if completed anything else if failed
     */
    public String insert(String username, byte mask) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", username);
        parameters.put("mask", String.valueOf(mask));
        parameters.put("ex_ip", this.getPublicIP());
        // parameters.put("table", "open");
        try {
            parameters.put("ip", this.getPrivateIP());
        } catch (Exception e) {
            parameters.put("ip", "unknown");
        }

        LocalTime localTime = LocalTime.now();
        StringBuilder activeBuilder = new StringBuilder();
        activeBuilder.append(localTime.getHour());
        if (localTime.getMinute() < 10) {
            activeBuilder.append("0");
        }
        activeBuilder.append(localTime.getMinute());
        parameters.put("active", activeBuilder.toString());
        return this.getRequest(this.serverAddress + "insert.php", parameters);
    }

    /**
     * Update the mask of an existing user
     * </p>
     * Automatically detects table and updates active
     * 
     * @param username - user's username
     * @param mask     - new mask of the user as int
     * @return - success if it worked anything else if not
     */
    public String update(String username, int mask) {
        return this.update(username, (byte) mask);
    }

    /**
     * Update the mask of an existing user, automatically detects table and updates
     * active
     * 
     * @param username - user's username
     * @param mask     - new mask of the user as byte
     * @return - success if it worked anything else if not
     */
    public String update(String username, byte mask) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", username);
        parameters.put("mask", String.valueOf(mask));
        parameters.put("ex_ip", this.getPublicIP());

        try {
            parameters.put("ip", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            parameters.put("ip", "unknown");
        }

        LocalTime localTime = LocalTime.now();
        StringBuilder activeBuilder = new StringBuilder();
        activeBuilder.append(localTime.getHour());
        if (localTime.getMinute() < 10) {
            activeBuilder.append("0");
        }
        activeBuilder.append(localTime.getMinute());

        parameters.put("active", activeBuilder.toString());

        return this.getRequest(this.serverAddress + "update.php", parameters);
    }

    /**
     * Pull all rows from a specific table
     * 
     * @param table
     * @return
     */
    public String pull(String table) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("table", table);

        return this.getRequest(this.serverAddress + "pull.php", parameters);
    }

    public ArrayList<QueryResult> pull(){
        return this.parsePullResults(this.pullString());
    }

    public ArrayList<QueryResult> pull(Map<String, String> parameters){
        return this.parsePullResults(this.pullString(parameters));
    }

    public ArrayList<QueryResult> pull(String username, String table){
        return this.parsePullResults(this.pullString(username, table));
    }

    /**
     * Pull rows from the table this device is currently in
     * 
     * @return String rep of php json encoded Association Array 
     */
    public String pullString() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("ex_ip", this.getPublicIP());

        return this.getRequest(this.serverAddress + "pull.php", parameters);
    }

    /**
     * Pull rows based on input parameters
     * 
     * @return String rep of php json encoded Association Array
     */
    public String pullString(Map<String, String> parameters) {
        if (!(parameters.containsKey("table") || parameters.containsKey("ex_ip"))) {
            parameters.put("ex_ip", this.getPublicIP());
        }
        return this.getRequest(this.serverAddress + "pull.php", parameters);
    }

    /**
     * Pull for a specific username from a table;
     * 
     * @param username
     * @param table
     * @return String rep of php json encoded Association Array 
     */
    public String pullString(String username, String table) {
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

    /**
     * Returns an array list of QueryResult from a string returned by a pull method
     * @param resultsString - string returned by pull method
     * @return ArrayList containing QueryResults from resultsString
     */
    public ArrayList<QueryResult> parsePullResults(String resultsString) {
        ArrayList<QueryResult> results = new ArrayList<>();
        if (resultsString.contains("[") && resultsString.contains("]")) {
            resultsString = resultsString.substring(resultsString.indexOf("[") + 1, resultsString.indexOf("]"));
        }
        resultsString = resultsString.replace("},{", "}|_#{"); // just use an obscure string to try and ensure it
                                                                   // isn't the username
        String resultsStringArray[] = resultsString.split("\\|_#\\{");
        for (String result : resultsStringArray) {
            QueryResult qr = QueryResult.fromAssocString(result);
            if (qr != null) {results.add(qr);}
        }
        return results;
    }

    public static void main(String args[]) {

        FileTransferServerClient ftsc = new FileTransferServerClient();
        // ftsc.delete("sample_username");
        try {
            ftsc.pullString();
        } catch (Exception e) {
            e.printStackTrace();
            ftsc.create();
        }

        if (ftsc.pullString().contains("sample_username")) {
            ftsc.update("sample_username", 2);
        } else {
            ftsc.insert("sample_username", 8);
        }

        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", "sample_username");
        ftsc.pull(parameters).forEach((result_) -> {
            QueryResult result = (QueryResult) result_;
            System.out.println(result.toString());
        });
        // ftsc.delete("sample_username");
        // ftsc.parsePullResults(ftsc.pull(parameters)).forEach((result_) -> {
        //     QueryResult result = (QueryResult) result_;
        //     System.out.println(result.toString());
        // });

        System.out.println("~~~Speed Test~~~");
        Instant start, end;
        Duration timeElapsed;
        start = Instant.now();
        for (int i = 0; i < 30; i++) {
            ftsc.pull();
        }
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        System.out.println("Average Pull Time: " + timeElapsed.toMillis() / 30 + " ms");

        start = Instant.now();
        for (int i = 0; i < 30; i++) {
            ftsc.update("sample_username", 2);
        }
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        System.out.println("Average Update Time: " + timeElapsed.toMillis() / 30 + " ms");

    }

}