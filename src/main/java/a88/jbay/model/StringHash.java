package a88.jbay.model;

public class StringHash {
    public static String hash(String s) {
        if (s!=null) return Integer.toString(s.hashCode());
        return "";
    }
}
