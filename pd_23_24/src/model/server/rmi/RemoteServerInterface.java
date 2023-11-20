package model.server.rmi;

import java.rmi.Remote;

public interface RemoteServerInterface extends Remote {

    void makeDBChanges(String query) throws java.io.IOException;
}
