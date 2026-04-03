package a88.jbay.model;

public class UniqueID {
    private static long curUID = 0;
    private static long curAID = 0;
    private static long curIID = 0;
    private static long curBID = 0;

    public static String genUID() {
        curUID++;
        return "U" + Long.toString(curUID);
    }

    public static String genAID() {
        curAID++;
        return "A" + Long.toString(curAID);
    }

    public static String genIID() {
        curIID++;
        return "I" + Long.toString(curIID);
    }
    public static String genBID() {
        curBID++;
        return "B" + Long.toString(curBID);
    }
}