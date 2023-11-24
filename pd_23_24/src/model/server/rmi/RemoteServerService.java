package model.server.rmi;

import model.server.rmi.RemoteServerInterface;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteServerService extends UnicastRemoteObject implements RemoteServerInterface {

    FileOutputStream fout = null;

    public RemoteServerService() throws java.rmi.RemoteException{

    }

    @Override
    public void writeDBFileChunk(byte[] fileChunk, int nbytes) throws IOException {
        if(fout == null){
            System.out.println("Nao existe qualquer ficheiro aberto para escrita");
            throw new IOException("<CLI> Nao existe qualquer ficheiro aberto para escrita");
        }

        try{
            fout.write(fileChunk, 0, nbytes);
        }catch (IOException e){
            System.out.println("Excepcao ao escrever no ficheiro " + e);
            throw new IOException("<CLI> Excepcao ao escrever no ficheiro " , e.getCause());
        }
    }
}
