package model.server.rmi;

import model.server.rmi.RemoteServerInterface;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteServerService extends UnicastRemoteObject implements RemoteServerInterface {

    ObjectOutputStream oos = null;

    protected RemoteServerService() throws RemoteException {
    }

    public synchronized void setOos(ObjectOutputStream oos) {
        this.oos = oos;
    }

    @Override
    public void makeDBChanges(String query) throws IOException {
        if(oos == null){
            System.out.println("Nao existe qualquer tipo de operacao a ser feita");
            throw new IOException("<CLI> Nao existe qualquer tipo de operacao a ser feita");
        }

        try{
            oos.write(query.getBytes(), 0 , query.length());
        }catch (IOException e){
            System.out.println("Excepcao ao fazer operacao na base de dados " + e);
            throw new IOException("<CLI> Excepcao ao fazer operacao na base de dados " , e.getCause());
        }
    }
}
