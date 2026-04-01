package a88.jbay.model;

public class UniqueID {
    private static long curID = 0;
    public static long genID() {
        curID++;
        return curID;
    }
}