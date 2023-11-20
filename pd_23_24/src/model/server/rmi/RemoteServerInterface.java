package model.server.rmi;

import java.rmi.Remote;

public interface RemoteServerInterface extends Remote {

    void writeDBFileChunk(byte[] fileChunk, int nbytes) throws java.io.IOException;
}
