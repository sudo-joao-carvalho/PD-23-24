package model.server;

import model.server.hb.HeartBeat;
import model.server.rmi.BackupServerRemoteInterface;
import model.server.rmi.RemoteServiceInterface;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicReference;

class MulticastHandler extends Thread { //thread to receive the hearbeat with the info (HEARTBEAT RECEIVER)
    public static final int MAX_SIZE = 1000;
    private MulticastSocket mcastSocket;
    private boolean isRunning;

    private RemoteServiceInterface remoteService;

    private String dbDirectory;

    private HeartBeat hb;

    public MulticastHandler(MulticastSocket mcastSocket) {
        this.mcastSocket = mcastSocket;
        this.isRunning = true;
    }

    public void terminate() {
        isRunning = false;
    }

    public void setParams(RemoteServiceInterface remoteService, String dbDirectory) {
        this.remoteService = remoteService;
        this.dbDirectory = dbDirectory;
    }

    public HeartBeat getHeartBeat(){return hb;}

    @Override
    public void run(){
        Object obj;
        HeartBeat hb;
        DatagramPacket pkt;

        if (mcastSocket == null || !isRunning) {
            return;
        }

        try{
            while (isRunning) {

                pkt = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                mcastSocket.receive(pkt);

                try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(pkt.getData(), 0, pkt.getLength()))) {
                    obj = in.readObject();

                    if (obj instanceof HeartBeat) {

                        hb = (HeartBeat) obj;
                        this.hb = hb;

                        System.out.println();
                        System.out.print("(" + pkt.getAddress().getHostAddress() + ":" + pkt.getPort() + "\t" + LocalTime.now() + ") ");
                        System.out.println(hb.getMsg());

                        if (remoteService != null) {
                            if (hb.getIsUpdateDB()) {
                                remoteService.makeBackUpDBChanges(dbDirectory, hb.getQuery());
                            }

                            if(hb.getDbVersion() + 1 != remoteService.getCurrentDBVersion(dbDirectory)) {
                                throw new Exception("Backup DB and Server DB have different versions");
                            }
                        }
                    }

                } catch (ClassNotFoundException e) {
                    System.out.println();
                    System.out.println("Mensagem recebida de tipo inesperado! " + e);
                    terminate();
                    return;
                } catch (IOException e) {
                    System.out.println();
                    System.out.println("Impossibilidade de aceder ao conteudo da mensagem recebida! " + e);
                    terminate();
                    return;
                } catch (Exception e) {
                    System.out.println();
                    System.out.println("Excepcao: " + e);
                    terminate();
                    //System.exit(0);
                }
            }

        }catch (Exception e){
            // lidar com esta exceção tal como todas as outras no projeto
        }
    }
}

public class BackupServer extends UnicastRemoteObject implements BackupServerRemoteInterface { // perguntar ao prof para ver se é mm assim

    private MulticastSocket mcastSocket = null;
    private NetworkInterface networkInterface;
    private InetAddress groupIp;
    private SocketAddress socketAddr;
    MulticastHandler mHandler; //thread

    private static final String ID_FILE_PATH = "src/resources/files/backup_server_id.txt"; // ficheiro de texto serve apenas para guardar e puxar os ids dos sv backup

    private static int idS;

    static { // lê-se do ficheiro de texto e puxa-se para dentro da variável do id para associar a cada backup sv
        try (BufferedReader reader = new BufferedReader(new FileReader(ID_FILE_PATH))) {
            String idStr = reader.readLine();
            if (idStr != null && !idStr.isEmpty()) {
                idS = Integer.parseInt(idStr);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private int id;

    public BackupServer() throws IOException {
        this.id = ++idS;

        // Atualiza o arquivo com o novo ID
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ID_FILE_PATH))) { // escreve-se para o ficheiro o novo Id
            writer.write(String.valueOf(idS));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //System.setProperty("java.net.preferIPv4Stack", "true");
        this.mcastSocket = new MulticastSocket(Integer.parseInt(MULTICAST.getValue(1)));
        this.groupIp = InetAddress.getByName(MULTICAST.getValue(0));
        this.socketAddr = new InetSocketAddress(groupIp, Integer.parseInt( MULTICAST.getValue(1)));
        this.networkInterface = NetworkInterface.getByIndex(0);
        this.mcastSocket.joinGroup(socketAddr, networkInterface); // creates group for object comms
        this.mHandler = new MulticastHandler(mcastSocket);
        mHandler.start();
    }

    @Override
    public void notify(String description) throws RemoteException {
        System.out.println("-> " + description);
        System.out.println();
    }

    public static void main(String[] args) throws IOException {

        try {
            if (args.length != 2) {
                System.out.println("Número inválido de argumentos recebido. Sintaxe: java -dbDirectory- -dbFileName-\n");
                return;
            }

            BackupServer backupServer = new BackupServer();

            System.out.println("Serviço BackupServer criado e em execução...\n");

            //args seguintes sao mandados pelo heartbeat
            //String objectUrl = "rmi://localhost/TP-PD-2324";

            while(backupServer.getMHandler().getHeartBeat() == null){ //este while serve para enquanto nao receber o primeiro hearbeat nao dar erro

            }

            RemoteServiceInterface getRemoteService = (RemoteServiceInterface) Naming.lookup(backupServer.getMHandler().getHeartBeat().getRMIServiceName());

            byte[] databaseCopy = getRemoteService.getDatabaseCopy();

            String dbDirectory = null;

            String result = backupServer.saveDatabaseCopyLocally(databaseCopy, args[0], args[1]);
            
            if (result.equals("Error")) {
                return;
            }

            dbDirectory = result;

            getRemoteService.addBackupServiceObserver(backupServer);

            backupServer.getMHandler().setParams(getRemoteService, dbDirectory);


            //System.out.println("A espera para terminar...\n");

            //System.out.println();

            //System.in.read();

            //getRemoteFileService.removeBackupServiceObserver(backupServer);

            // terminar o serviço
            //UnicastRemoteObject.unexportObject(backupServer, true);

        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
        }
    }

    public String saveDatabaseCopyLocally(byte[] databaseCopy, String directory, String filename) {

        File fileDirectory = new File(directory + id + "/"); // se o caminho introduzido por parâmetro de linha de comandos não existir, vai ter de se criar
        // depois disso verificamos se existe ou não algum ficheiro dentro dela

        if(!fileDirectory.exists()) {
            boolean created = fileDirectory.mkdir();
            if (!created) {
                System.out.println("Não foi possível criar o diretório de backup.");
                return "Error";
            }
        }

        if (fileDirectory.list() != null && fileDirectory.list().length != 0) {
            System.out.println("\nDirectory already contains files. Shutting down...\n");
            return "Error";
        }

        //File backupFile = new File(directory + "backupDirectory" + id + "/" + filename + "-" + id + ".db");

        // Criar o caminho completo para o diretório de backup
        /*String backupDirectoryPath = directory + id + "/";

        // Criar o objeto File para representar o diretório de backup
        File backupDirectory = new File(backupDirectoryPath);

        if (!backupDirectory.exists()) {
            boolean created = backupDirectory.mkdir();
            if (!created) {
                System.out.println("Não foi possível criar o diretório de backup.");
                return false;
            }
        }*/

        // Criar o caminho completo para o arquivo de backup
        String backupFilePath = fileDirectory + "/" + filename + "-" + id + ".db";

        try (FileOutputStream fos = new FileOutputStream(backupFilePath)) {
            fos.write(databaseCopy);
            System.out.println("Cópia da base de dados recebida e guardada.\n");

            return backupFilePath;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Error";
    }

    public MulticastHandler getMHandler() {
        return mHandler;
    }
}


