package model.server.rmi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class RemoteService extends UnicastRemoteObject implements RemoteServiceInterface {

    public static final String SERVICE_NAME = "tp-pd";
    List<BackupServerRemoteInterface> observers;

    protected RemoteService() throws RemoteException {
    }

    @Override
    public void addBackupServiceObserver(BackupServerRemoteInterface observer) throws RemoteException{
        synchronized (observer){
            if(!observers.contains(observer)){
                observers.add(observer);
                System.out.println("Mais um observador");
            }
        }
    }

    @Override
    public void removeBackupServiceObserver(BackupServerRemoteInterface observer) throws RemoteException {
        synchronized (observer){
            if(observers.remove(observer)){
                System.out.println("Menos um observador");
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
                    System.out.println("- um observador (observador inacessivel)");
                }
            }

            observers.removeAll(observersToRemove);
        }

        /*synchronized (observers){
            observers.removeAll(observersToRemove);
        }*/
    }

    //fazer aqui as operacoes da base de dados
    public void makeBackUpDBChanges(String dbDirectory, RemoteServerInterface cliRemoto) throws IOException {
//        byte [] fileChunk = new byte[MAX_CHUNCK_SIZE];
//        int nbytes;
//
//        fileName = fileName.trim();
//        System.out.println("Recebido pedido para: " + fileName);
//
//        try(FileInputStream requestedFileInputStream = getRequestedFileInputStream(fileName)){
//
//            /*
//             * Obtem os bytes do ficheiro por blocos de bytes ("file chunks").
//             */
//            while((nbytes = requestedFileInputStream.read(fileChunk))!=-1){
//
//                /*
//                 * Escreve o bloco actual no cliente, invocando o metodo writeFileChunk da
//                 * sua interface remota.
//                 */
//
//                cliRemoto.writeFileChunk(fileChunk, nbytes); //cliRemoto é a interface do server
//
//            }
//
//            System.out.println("Ficheiro " + new File(localDirectory+File.separator+fileName).getCanonicalPath() +
//                    " transferido para o cliente com sucesso.");
//            notifyObservers("Ficheiro " + new File(localDirectory+File.separator+fileName).getCanonicalPath() +
//                    " transferido para o cliente com sucesso.");
//            System.out.println();
//
//            return;
//
//        }catch(FileNotFoundException e){   //Subclasse de IOException
//            System.out.println("Ocorreu a excecao {" + e + "} ao tentar abrir o ficheiro!");
//            throw new FileNotFoundException(fileName);
//        }catch(IOException e){
//            System.out.println("Ocorreu a excecao de E/S: \n\t" + e);
//            throw new IOException(fileName, e.getCause());
//        }

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
            RemoteService fileService = new RemoteService();

            System.out.println("Servico GetRemoteFile criado e em execucao (" + fileService.getRef().remoteToString() + "...");

            /*
             * Regista o servico no rmiregistry local para que os clientes possam localiza'-lo, ou seja,
             * obter a sua referencia remota (endereco IP, porto de escuta, etc.).
             */

            Naming.bind("rmi://localhost/" + SERVICE_NAME, fileService);

            System.out.println("Servico " + SERVICE_NAME + " registado no registry...");

            /*
             * Para terminar um servico RMI do tipo UnicastRemoteObject:
             *
             *  UnicastRemoteObject.unexportObject(fileService, true).
             */
            UnicastRemoteObject.unexportObject(fileService, true);

        } catch (RemoteException e) {
            System.out.println("Erro remoto - " + e);
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Erro - " + e);
            System.exit(1);
        }

    }
}
