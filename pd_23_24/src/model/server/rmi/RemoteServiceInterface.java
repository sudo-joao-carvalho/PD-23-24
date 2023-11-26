package model.server.rmi;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;

public interface RemoteServiceInterface extends Remote {

    void makeBackUpDBChanges(String dbDirectory, String query/*, RemoteServerInterface cliRemoto*/) throws IOException, RemoteException;

    int getCurrentDBVersion(String dbDirectory) throws IOException, SQLException;

    void addBackupServiceObserver(BackupServerRemoteInterface observer) throws java.rmi.RemoteException;

    void removeBackupServiceObserver(BackupServerRemoteInterface observer) throws java.rmi.RemoteException;

    byte[] getDatabaseCopy() throws RemoteException;
}
