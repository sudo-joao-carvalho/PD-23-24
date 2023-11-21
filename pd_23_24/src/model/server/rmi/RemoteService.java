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

    public static final String SERVICE_NAME = "TP-PD-2324";

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

    static public void main(String []args) {
        File localDirectory;

        /*
         * Se existirem varias interfaces de rede activas na maquina onde corre esta aplicacao,
         * convem definir de forma explicita o endereco que deve ser incluido na referencia remota do servico
         * RMI criado. Para o efeito, o endereco deve ser atribuido 'a propriedade java.rmi.server.hostname.
         *
         * Pode ser no codigo atraves do metodo System.setProperty():
         *      - System.setProperty("java.rmi.server.hostname", "10.65.129.232"); //O endereco usado e' apenas um exemplo
         *      - System.setProperty("java.rmi.server.hostname", args[3]); //Neste caso, assume-se que o endereco e' passado como quarto argumento na linha de comando
         *
         * Tambem pode ser como opcao passada 'a maquina virtual Java:
         *      - java -Djava.rmi.server.hostname=10.202.128.22 GetRemoteFileClient ... //O endereco usado e' apenas um exemplo
         *      - No Netbeans: Properties -> Run -> VM Options -> -Djava.rmi.server.hostname=10.202.128.22 //O endereco usado e' apenas um exemplo
         */

        /*
         * Trata os argumentos da linha de comando
         */
        if (args.length != 1) {
            System.out.println("Sintaxe: java GetFileUdpServer localRootDirectory");
            return;
        }

        localDirectory = new File(args[0].trim());

        if (!localDirectory.exists()) {
            System.out.println("A directoria " + localDirectory + " nao existe!");
            return;
        }

        if (!localDirectory.isDirectory()) {
            System.out.println("O caminho " + localDirectory + " nao se refere a uma diretoria!");
            return;
        }

        if (!localDirectory.canRead()) {
            System.out.println("Sem permissoes de leitura na diretoria " + localDirectory + "!");
            return;
        }

        /*
         * Lanca o rmiregistry localmente no porto TCP por omissao (1099).
         */
        try {

            try {

                System.out.println("Tentativa de lancamento do registry no porto " +
                        Registry.REGISTRY_PORT + "...");

                LocateRegistry.createRegistry(Registry.REGISTRY_PORT);

                System.out.println("Registry lancado!");

            } catch (RemoteException e) {
                System.out.println("Registry provavelmente ja' em execucao!");
            }

            /*
             * Cria o servico.
             */

            RemoteService service = new RemoteService();

            System.out.println("Servico RemoteService criado e em execucao (" + service.getRef().remoteToString() + "...");

            /*
             * Regista o servico no rmiregistry local para que os clientes possam localiza'-lo, ou seja,
             * obter a sua referencia remota (endereco IP, porto de escuta, etc.).
             */

            Naming.bind("rmi://localhost/" + SERVICE_NAME, service);

            System.out.println("Servico " + SERVICE_NAME + " registado no registry...");

            /*
             * Para terminar um servico RMI do tipo UnicastRemoteObject:
             *
             *  UnicastRemoteObject.unexportObject(fileService, true).
             */
            //UnicastRemoteObject.unexportObject(service, true);

        } catch (RemoteException e) {
            System.out.println("Erro remoto - " + e);
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Erro - " + e);
            System.exit(1);
        }

    }
}
