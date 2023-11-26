package model.server.hb;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalTime;

public class HeartBeat implements Serializable {

    @Serial
    private static final long serialVersionUID = 1000L;

    private String msg;
    private int registryListeningPort;
    private String ip;
    private boolean available;
    private int dbVersion;
    private int nConnections;
    private String dbDirectory;
    private LocalTime time;
    private String query;

    private String RMIServiceName;

    private boolean updateDB;

    public HeartBeat(int registryListeningPort, boolean available, int dbVersion, String dbDirectory, String serviceName) { // tudo o que vai no hb
        this.registryListeningPort = registryListeningPort;
        this.available = available;
        this.dbVersion = dbVersion;
        this.dbDirectory = dbDirectory;
        this.msg = "Estou bibo";
        this.RMIServiceName = serviceName;
        this.updateDB = false;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getRegistryListeningPort() {
        return registryListeningPort;
    }

    public void setRegistryListeningPort(int registryListeningPort) {
        this.registryListeningPort = registryListeningPort;
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

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getRMIServiceName() {
        return RMIServiceName;
    }

    public void setRMIServiceName(String RMIServiceName) {
        this.RMIServiceName = RMIServiceName;
    }

    public boolean getIsUpdateDB() {
        return updateDB;
    }

    public void setUpdateDB(boolean updateDB) {
        this.updateDB = updateDB;
    }

    @Override
    public String toString() {
        return "HeartBeat{" +
                "msg='" + msg + '\'' +
                ", registryListeningPort=" + registryListeningPort +
                ", ip='" + ip + '\'' +
                ", available=" + available +
                ", dbVersion=" + dbVersion +
                ", nConnections=" + nConnections +
                ", dbDirectory='" + dbDirectory + '\'' +
                ", time=" + time +
                ", query='" + query + '\'' +
                ", rmiServiceName='" + RMIServiceName + '\'' +
                '}';
    }

    @Override
    public int hashCode(){
        return registryListeningPort;
    }
}
