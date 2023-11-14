package model.server;

public enum MULTICAST {

    IP("230.44.44.44"),
    PORT("4444");

    private final String value;

    MULTICAST(String value){
        this.value = value;
    }

    public static String getValue(int field){ // 0 -> ip  1 -> port
        MULTICAST[] constants = MULTICAST.values();
        return constants[field].value;
    }

}
