public class QueryResult {
    public String username, ip;
    public int active;
    public byte mask;

    public QueryResult(String username, String ip, int active, byte mask) {
        this.username = username;
        this.ip = ip;
        this.active = active;
        this.mask = mask;
    }

    @Override
    public String toString() {
        return "username:" + this.username + ", ip:" + this.ip + ", active:" + this.active + ", mask:" + this.mask;
    }

    /**
     * Returns null if queryString is empty
     * @param queryString - String rep of php json encoded Association Array 
     * @return QueryResult object of {@code queryString}
     */
    public static QueryResult fromAssocString(String queryString) {
        // {"username":"sample_username","ip":"172.24.5.68","active":"1031","mask":"8"}
        String associations[] = queryString.replace("{", "").replace("}", "").split(",");
        String username = null;
        String ip = null;
        int active = 0;
        byte mask = 0;

        for (String association : associations) {
            if (association.contains("username")) {
                username = association.substring(association.indexOf(":")+1, association.length()).replace("\"", "");
            } else if (association.contains("ip")) {
                ip = association.substring(association.indexOf(":")+1, association.length()).replace("\"", "");
            } else if (association.contains("active")) {
                active = Integer.parseInt(
                        association.substring(association.indexOf(":")+1, association.length()).replace("\"", ""));
            } else if (association.contains("mask")) {
                mask = Byte.parseByte(
                        association.substring(association.indexOf(":")+1, association.length()).replace("\"", ""));
            }
        }

        if (username == null && ip == null && active == 0){
            return null;
        }

        return new QueryResult(username, ip, active, mask);

    }

}