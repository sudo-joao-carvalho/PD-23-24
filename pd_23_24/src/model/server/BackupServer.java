package model.server;

import model.server.hb.HeartBeat;
import model.server.rmi.BackupServerRemoteInterface;
import model.server.rmi.RemoteServiceInterface;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

class MulticastHandler extends Thread { //thread to receive the hearbeat with the info (HEARTBEAT RECEIVER)
    public static final int MAX_SIZE = 1000;
    private MulticastSocket mcastSocket;
    private boolean isRunning;

    public MulticastHandler(MulticastSocket mcastSocket) {
        this.mcastSocket = mcastSocket;
        this.isRunning = true;
    }

    public void terminate() {
        isRunning = false;
    }

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

                        //System.out.println("Recebi heartbeat: " + hb.getMsg())

                        System.out.println();
                        System.out.print("(" + pkt.getAddress().getHostAddress() + ":" + pkt.getPort() + ") ");
                        System.out.println(hb.getMsg());

                        /*if (msg.getMsg().toUpperCase().contains(LIST.toUpperCase())) {

                            try (ByteArrayOutputStream buff = new ByteArrayOutputStream();
                                 ObjectOutputStream out = new ObjectOutputStream(buff)) {

                                out.writeObject(username);

                                pkt.setData(buff.toByteArray());
                                pkt.setLength(buff.size());
                            }

                            s.send(pkt);
                            continue;
                        }

                        System.out.println();
                        System.out.print("(" + pkt.getAddress().getHostAddress() + ":" + pkt.getPort() + ") ");
                        System.out.println(msg.getNickname() + ": " + msg.getMsg() + " (" + msg.getClass() + ")");*/

                    } /*else if (obj instanceof String) {

                        System.out.println((String) obj + " (" + obj.getClass() + ")");
                    }*/

                } catch (ClassNotFoundException e) {
                    System.out.println();
                    System.out.println("Mensagem recebida de tipo inesperado! " + e);
                } catch (IOException e) {
                    System.out.println();
                    System.out.println("Impossibilidade de aceder ao conteudo da mensagem recebida! " + e);
                } catch (Exception e) {
                    System.out.println();
                    System.out.println("Excepcao: " + e);
                }
            }

        }catch (Exception e){
            // lidar com esta exceção tal como todas as outras no projeto
        }

        if (!isRunning){
            return;
        }
    }
}

public class BackupServer extends UnicastRemoteObject implements BackupServerRemoteInterface { // perguntar ao prof para ver se é mm assim

    private MulticastSocket mcastSocket = null;
    private NetworkInterface networkInterface;
    private InetAddress groupIp;
    private SocketAddress socketAddr;
    MulticastHandler mHandler; //thread

    public BackupServer() throws IOException {
        this.mcastSocket = new MulticastSocket(Integer.parseInt(MULTICAST.getValue(1)));
        this.groupIp = InetAddress.getByName(MULTICAST.getValue(0));
        this.socketAddr = new InetSocketAddress(groupIp, Integer.parseInt( MULTICAST.getValue(1)));
        this.networkInterface = NetworkInterface.getByIndex(0);
        this.mcastSocket.joinGroup(socketAddr, networkInterface); // creates group for object comms
        this.mHandler = new MulticastHandler(mcastSocket);
        mHandler.start();
    }

    @Override
    public void notify(String description) throws RemoteException { //fazer as operacoes da db
        System.out.println("-> " + description);
        System.out.println();
    }

    public static void main(String[] args) throws IOException {
        /*if (args.length != 1) {
            System.out.println("Número inválido de argumentos recebido. Sintaxe: java -dbDirectory-\n");
            return;
        }

        File localDir = new File(args[0].trim());

        if (!localDir.exists()) {
            System.out.println("\nCan't write in non existent directory.\n");
            return;
        }

        if (!localDir.isDirectory()) {
            System.out.println("\nDesired path isn't directory.\n");
            return;
        }

        if (localDir.list().length != 0) {
            System.out.println("Directory already contains files. Shutting down.\n");
            return;
        }

        if (!localDir.canWrite()) {
            System.out.println("\nNo WRITE permissions to directory!\n");
            return;
        }

        BackupServer backupServer = new BackupServer();

        while (true){
        }*/

        try {
            if (args.length != 1) {
                System.out.println("Número inválido de argumentos recebido. Sintaxe: java -dbDirectory-\n");
                return;
            }

            //args seguintes sao mandados pelo heartbeat
            /*String objectUrl = "rmi://" + args[0] + "/servidor-ficheiros-pd";

            RemoteServiceInterface getRemoteFileService = (RemoteServiceInterface) Naming.lookup(objectUrl);

            System.setProperty("java-rmi.server.hostname", args[1]);*/

            BackupServer BackupServer = new BackupServer();

            System.out.println("Serviço BackupServer criado e em execução...\n");

            /*getRemoteFileService.addBackupServiceObserver(BackupServer);

            System.out.println("A espera para terminar...\n");

            System.out.println();

            System.in.read();

            getRemoteFileService.removeBackupServiceObserver(BackupServer);

            // terminar o serviço
            UnicastRemoteObject.unexportObject(BackupServer, true);*/

        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } /*catch (NotBoundException e) {
            throw new RuntimeException(e);
        } */catch (IOException e) {


        }
    }
}
