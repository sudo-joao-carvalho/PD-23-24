package model.server.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BackupServerRemoteInterface extends Remote {
    void notify(String description) throws RemoteException;
}

