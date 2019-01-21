
/**
 * Vers:    2.0.1   Switched create, insert, update, and delete to return booleans
 * Vers:    2.0.0   Added QueryResult compatibility returning methods, added delete, pulls are now pullString
 * Vers:    1.0.0   Initial coding getIP, create, pull, insert, update, getRequest
 */
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
// import java.net.URLEncoder;
import java.util.Map;

public class FileTransferServerClient {
    private final String serverAddress = "http://prattpi.hopto.org/FileTransferServer/";

    /**
     * Obtain the external/public ip address of the network this device is on
     * 
     * @return String representation of the ip address
     */
    public String getPublicIP() {
        return this.getRequest(this.serverAddress + "getIP.php", new HashMap<>());
    }

    /**
     * Obtain the internal ip address of this device
     * 
     * @return String representation of the ip address
     */
    public String getPrivateIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get an array list of results for currently active local users
     * @return array list of {@code QueryResult} objects
     */
    public ArrayList<QueryResult> getActive(){
        Map<String, String> params = new HashMap<>();
        params.put("ex_ip", this.getPublicIP());
        String res = this.getRequest(this.serverAddress + "getActive.php", params);
        return this.parsePullResults(res);
    }

    /**
     * Quickly set self to active on SQL don't wait for response
     * @param username - username to set active
     * @return true if successful
     */
    public boolean setActive(String username){
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("ip", this.getPrivateIP());
        params.put("ex_ip", this.getPublicIP());
        params.put("active", this.getActiveString());
        return this.getRequest(this.serverAddress + "setActive.php", params).contains("success");
    }

    /**
     * Quickly set self to inactive on SQL DB don't wait for response
     * @param username - username to set active
     */
    public boolean setInactive(String username){
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("ip", this.getPrivateIP());
        params.put("ex_ip", this.getPublicIP());
        params.put("active", this.getActiveString());
        return this.getRequest(this.serverAddress + "setInactive.php", params).contains("success");
    }

    /**
     * Create a new table for the public ip of the device
     * 
     * @return true if the table is created
     */
    public boolean create() {
        Map<String, String> params = new HashMap<>();
        params.put("ex_ip", this.getPublicIP());
        return this.getRequest(this.serverAddress + "create.php", params).contains("success");
    }

    /**
     * Create a new table with the specified name {@code table}
     * 
     * @param table - new table's name
     * @return - true if the table is created
     */
    public boolean create(String table) {
        Map<String, String> params = new HashMap<>();
        params.put("table", table);
        return this.getRequest(this.serverAddress + "create.php", params).contains("success");
    }

    /**
     * Delete records associated with {@code username}
     * </p>
     * Auto detect the table and the ip
     * 
     * @param username
     * @return true if the row(s) are deleted
     */
    public boolean delete(String username) {
        Map<String, String> params = new HashMap<>();
        params.put("ex_ip", this.getPublicIP());
        params.put("username", username);
        params.put("ip", this.getPrivateIP());

        return this.getRequest(this.serverAddress + "delete.php", params).contains("success");
    }

    /**
     * Add a new user with name {@code username} and mask {@code mask}
     * </p>
     * Auto detect the table and updates active
     * 
     * @param username - user's username
     * @param mask     - user's mask as int
     * @return true if completed false if failed
     */
    public boolean insert(String username, int mask) {
        return this.insert(username, (byte) mask);
    }

    /**
     * Add a new user with name {@code username} and mask {@code mask}
     * </p>
     * Auto detect the table and updates active
     * 
     * @param username - user's username
     * @param mask     - user's mask as byte
     * @return true if data is inserted
     */
    public boolean insert(String username, byte mask) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", username);
        parameters.put("mask", String.valueOf(mask));
        parameters.put("ex_ip", this.getPublicIP());
        // parameters.put("table", "open");
        try {
            parameters.put("ip", this.getPrivateIP());
        } catch (Exception e) {
            e.printStackTrace();
            parameters.put("ip", "unknown");
        }

        parameters.put("active", this.getActiveString());

        String requestResults = this.getRequest(this.serverAddress + "insert.php", parameters);
        if (requestResults.contains("doesn't exist")){ // this table doesn't exist yet
            // maybe throw an exception here?
        }
        return requestResults != null ? requestResults.contains("success") : false;
    }

    /**
     * Add a new user with name {@code username} and mask {@code mask}
     * </p>
     * Auto detect the table and updates active
     * 
     * @param parameters - parameters to inser
     * @return true if data is inserted
     */
    public boolean insert(Map<String, String> parameters) {
        if (!parameters.containsKey("username")) {
            return false;
        }
        if (!parameters.containsKey("table") && !parameters.containsKey("ex_ip")) {
            parameters.put("ex_ip", this.getPublicIP());
        }
        if (!parameters.containsKey("ip")) {
            // parameters.put("table", "open");
            try {
                parameters.put("ip", this.getPrivateIP());
            } catch (Exception e) {
                e.printStackTrace();
                parameters.put("ip", "unknown");
            }
        }
        if (!parameters.containsKey("mask")) {
            parameters.put("mask", "0");
        }
        if (!parameters.containsKey("active")) {
            parameters.put("active", this.getActiveString());
        }
        String requestResults = this.getRequest(this.serverAddress + "insert.php", parameters);
        if (requestResults.contains("doesn't exist")){ // this table doesn't exist yet
            // maybe throw an exception here?
        }
        return requestResults != null ? requestResults.contains("success") : false;
    }

    /**
     * Update the mask of an existing user
     * 
     * @param parameters - new user parameters
     * @return  true if data is updated
     */
    public boolean update(Map<String, String> parameters){
        if (!parameters.containsKey("username")){
            return false;
        }
        if (!parameters.containsKey("mask")){
            parameters.put("mask", "0");
        }
        if (! (parameters.containsKey("ex_ip") || parameters.containsKey("table"))){
            parameters.put("ex_ip", this.getPublicIP());
        }
        if (!parameters.containsKey("ip")){
            try {
                parameters.put("ip", InetAddress.getLocalHost().getHostAddress());
            } catch (Exception e) {
                e.printStackTrace();
                parameters.put("ip", "unknown");
            }
        }
        if (!parameters.containsKey("active")){
            parameters.put("active", this.getActiveString());
        }

        String requestResults = this.getRequest(this.serverAddress + "update.php", parameters);
        if (requestResults.contains("doesn't exist")){ // this table doesn't exist yet
            // maybe throw an exception here?
        }
        return (requestResults != null ? requestResults.contains("success") : false);
    }

    /**
     * Update the mask of an existing user
     * </p>
     * Automatically detects table and updates active
     * 
     * @param username - user's username
     * @param mask     - new mask of the user as int
     * @return - true if data is updated
     */
    public boolean update(String username, int mask) {
        return this.update(username, (byte) mask);
    }

    /**
     * Update the mask of an existing user, automatically detects table and updates
     * active
     * 
     * @param username - user's username
     * @param mask     - new mask of the user as byte
     * @return - true if data is updated
     */
    public boolean update(String username, byte mask) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", username);
        parameters.put("mask", String.valueOf(mask));
        parameters.put("ex_ip", this.getPublicIP());

        try {
            parameters.put("ip", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            e.printStackTrace();
            parameters.put("ip", "unknown");
        }
        parameters.put("active", this.getActiveString());

        String requestResults = this.getRequest(this.serverAddress + "update.php", parameters);
        if (requestResults.contains("doesn't exist")){ // this table doesn't exist yet
            // maybe throw an exception here?
        }
        return (requestResults != null ? requestResults.contains("success") : false);
    }

    // #region pull region

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

    /**
     * Pull rows from the table this device is currently in
     * </p>
     * Automatically detects the table (based on external IP)
     * 
     * @return QueryResults parsed from json response
     */
    public ArrayList<QueryResult> pull() {
        return this.parsePullResults(this.pullString());
    }

    /**
     * Pull rows from the table this device is currently in
     * </p>
     * Automatically detects the table (based on external IP)
     * 
     * @param parameters - input parameters for pull request
     * @return QueryResults parsed from json response
     */
    public ArrayList<QueryResult> pull(Map<String, String> parameters) {
        return this.parsePullResults(this.pullString(parameters));
    }

    /**
     * Pull rows that contain a specified username
     * </p>
     * Automatically detects the table (based on external IP)
     * 
     * @param username - username to search for
     * @return
     */
    public ArrayList<QueryResult> pullByUsername(String username) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", username);

        return this.parsePullResults(this.pullString(parameters));
    }

    /**
     * Pull rows from specified table with a specified username
     * 
     * @param username - username to search for
     * @param table    - specific table to search
     * @return
     */
    public ArrayList<QueryResult> pull(String username, String table) {
        return this.parsePullResults(this.pullString(username, table));
    }

    /**
     * Pull rows from the table this device is currently in
     * </p>
     * Automatically detects the table
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
     * </p>
     * Automatically detects the table
     * 
     * @param parameters - input parameters to request
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

    // #endregion pull region


    /**
     * Returns the String returned by a GET request to {@code urlString} with
     * parameters {@code parameters}
     * 
     * @param urlString  - url to send request
     * @param parameters - parameters for request
     * @return String anything returned for request null if failed
     */
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
            // System.out.println(content.toString());
            return content.toString();

        } catch (Exception e) {
            System.out.println(this.getParamsString(parameters));
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Make a String of parameters to be put at the end of a url
     * </p>
     * Resulting Format: ?key1=value1&key2=value2
     * 
     * @param params map of values to be represented
     * @return String with the values arranged for a url
     */
    private String getParamsString(Map<String, String> params) {
        StringBuilder result = new StringBuilder();
        // System.out.println("Number of params: " + params.size());
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                // System.out.println("KEY: " + entry.getKey() + " VALUE: " + entry.getValue());
                if (entry.getKey() != null && entry.getValue() != null){
                    result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                    result.append("=");
                    result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                    result.append("&");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String resultString = result.toString();
        return resultString.length() > 0 ? resultString.substring(0, resultString.length() - 1) : resultString;
    }

    /**
     * Returns an array list of QueryResult from a string returned by a pull method
     * 
     * @param resultsString - string returned by pull method
     * @return ArrayList containing QueryResults from resultsString
     */
    public ArrayList<QueryResult> parsePullResults(String resultsString) {
        ArrayList<QueryResult> results = new ArrayList<>();
        if (resultsString == null){
            return results;
        }
        if (resultsString.contains("[") && resultsString.contains("]")) {
            resultsString = resultsString.substring(resultsString.indexOf("[") + 1, resultsString.indexOf("]"));
        }
        resultsString = resultsString.replace("},{", "}|_#{"); // just use an obscure string to try and ensure it
                                                               // isn't the username
        String resultsStringArray[] = resultsString.split("\\|_#\\{");
        for (String result : resultsStringArray) {
            QueryResult qr = QueryResult.fromAssocString(result);
            if (qr != null) {
                results.add(qr);
            }
        }
        return results;
    }

    private String getActiveString(){
        LocalTime localTime = LocalTime.now();
        StringBuilder activeBuilder = new StringBuilder();
        activeBuilder.append(localTime.getHour());
        if (localTime.getMinute() < 10) {
            activeBuilder.append("0");
        }
        activeBuilder.append(localTime.getMinute());
        return activeBuilder.toString();
    }

}