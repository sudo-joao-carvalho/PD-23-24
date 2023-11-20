package model.server.rmi;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteServiceInterface extends Remote {

    void makeBackUpDBChanges(String dbDirectory, String query/*, RemoteServerInterface cliRemoto*/) throws IOException, RemoteException;

    void addBackupServiceObserver(BackupServerRemoteInterface observer) throws java.rmi.RemoteException;

    void removeBackupServiceObserver(BackupServerRemoteInterface observer) throws java.rmi.RemoteException;
}
