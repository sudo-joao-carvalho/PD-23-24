package model.server.rmi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RemoteService extends UnicastRemoteObject implements RemoteServiceInterface {

    List<BackupServerRemoteInterface> observers;

    public RemoteService() throws RemoteException {
        observers = new ArrayList<>();
    }

    @Override
    public void addBackupServiceObserver(BackupServerRemoteInterface observer) throws RemoteException{
        synchronized (observer){
            if(!observers.contains(observer)){
                System.out.println("Adicionei um backup");
                observers.add(observer);
                System.out.println("Mais um BackupServer");
            }
        }
    }

    @Override
    public void removeBackupServiceObserver(BackupServerRemoteInterface observer) throws RemoteException {
        synchronized (observer){
            if(observers.remove(observer)){
                System.out.println("Menos um BackupServer");
            }
        }
    }

    protected /*synchronized*/ void notifyObservers(String msg){ //esta funcao se nao conseguir notificar um observer remove-o da lista

        List<BackupServerRemoteInterface> observersToRemove = new ArrayList<>();

        synchronized (observers) {

            for(BackupServerRemoteInterface observer : observers){

                try{
                    observer.notify(msg);
                }catch (RemoteException e){
                    observersToRemove.add(observer);
                    System.out.println("- um BackupServer (BackupServer inacessivel)");
                }
            }

            observers.removeAll(observersToRemove);
        }

        /*synchronized (observers){
            observers.removeAll(observersToRemove);
        }*/
    }

    //fazer aqui as operacoes da base de dados
    public synchronized void makeBackUpDBChanges(String dbDirectory, String query/*, RemoteServerInterface cliRemoto*/) throws IOException {
        //System.out.println(query);

        Connection conn = null;

        //System.out.println(dbDirectory);

        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbDirectory);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        try {
            Statement statement = conn.createStatement();
            int numRowsAffected = statement.executeUpdate(query);

            if(numRowsAffected != 0){
                notifyObservers("Base de dados de backup alterada");
            }else{
                notifyObservers("Nao foi possivel alterar a base de dados");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create statement!\n");
        }

    }

    @Override
    public synchronized byte[] getDatabaseCopy() throws RemoteException {
        try {
            // Lógica para obter a cópia da base de dados em bytes
            File dbFile = new File("src/resources/db/PD-2023-24-TP.db"); // Substitua pelo caminho correto
            byte[] databaseCopy = Files.readAllBytes(dbFile.toPath());
            notifyObservers("Database copied");
            return databaseCopy;
        } catch (IOException e) {
            throw new RemoteException("Erro ao obter cópia da base de dados", e);
        }
    }
}
