package model.server.hb;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalTime;

public class HeartBeat implements Serializable {

    @Serial
    private static final long serialVersionUID = 1000L;

    private String msg;
    private int port;
    private String ip;
    private boolean available;
    private int dbVersion;
    private int nConnections;
    private String dbDirectory;
    private LocalTime time;
    private String allQueries;

    public HeartBeat(int port, boolean available/*, int dbVersion, int nConnections*/, String dbDirectory) { // tudo o que vai no hb
        this.port = port;
        this.available = available;
        /*this.dbVersion = dbVersion;
        this.nConnections = nConnections;*/
        this.dbDirectory = dbDirectory;
        this.msg = "Estou bibo";
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getDbVersion() {
        return dbVersion;
    }

    public void setDbVersion(int dbVersion) {
        this.dbVersion = dbVersion;
    }

    public int getnConnections() {
        return nConnections;
    }

    public void setnConnections(int nConnections) {
        this.nConnections = nConnections;
    }

    public String getDbDirectory() {
        return dbDirectory;
    }

    public void setDbDirectory(String dbDirectory) {
        this.dbDirectory = dbDirectory;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public String getAllQueries() {
        return allQueries;
    }

    public void setAllQueries(String allQueries) {
        this.allQueries = allQueries;
    }

    @Override
    public String toString() {
        return "HeartBeat{" +
                "msg='" + msg + '\'' +
                ", port=" + port +
                ", ip='" + ip + '\'' +
                ", available=" + available +
                ", dbVersion=" + dbVersion +
                ", nConnections=" + nConnections +
                ", dbDirectory='" + dbDirectory + '\'' +
                ", time=" + time +
                ", allQueries='" + allQueries + '\'' +
                '}';
    }

    @Override
    public int hashCode(){
        return port;
    }
}
