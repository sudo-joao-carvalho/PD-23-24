package ui;

import model.client.Client;
import model.data.Data;
import resources.ResourceManager;
import ui.util.InputProtection;

import java.sql.SQLException;
import java.util.ArrayList;

public class ClientUI {

    private final Client client;
    private int admin = 0;
    public ClientUI(Client client){
        this.client = client;
    }

    public boolean loginRegister(){
        int option = InputProtection.chooseOption("Choose a menu: " , "Login","Register","Exit");

        switch (option){
            case 1 -> {
                return login();
            }
            case 2 ->{
                return register();
            }
            default -> {
                return false;
            }
        }
    }

    public boolean login(){

        String email = InputProtection.readString("\tEmail: ", true);
        String password = InputProtection.readString("\tPassword: ", true);


        ArrayList<String> userParams = new ArrayList<>();
        userParams.add(email);
        userParams.add(password);

        if (client.getIsAdmin()) {
            userParams.add("0"); //autenticado
            userParams.add("0"); //admin

            this.client.insertEvento(this.client.dbHelper, userParams);
        }

        //do{
            email = InputProtection.readString("\tEmail: ", true);

            password = InputProtection.readString("\tPassword: ", true);

            //verifyLogin(email, password); //TODO fazer funcao para verficar na BD se ja existe o user

            //TODO fazer
            /*String out = client.waitToReceiveResultRequest();


            if(out.equals("User doesnt exist!")){
                System.out.println(out);
                return false;
            }
            if(out.contains("\nAdmin:1"))
                admin = 1;

            out = out.replaceAll(" ", "");
            String[] splitted = out.split("\n");
            String[] id = splitted[0].split(":");

            client.setClientID(Integer.parseInt(id[1]));
            return true;*/
        //}while(true);

        return true;
    }

    public boolean addEvent() {
        String local = InputProtection.readString("\tLocal: ", true);
        String nome = InputProtection.readString("\tNome: ", false);
        String dia = InputProtection.readString("\tDia: ", true);
        String mes = InputProtection.readString("\tMês: ", true);
        String ano = InputProtection.readString("\tAno: ", true);
        String horaInicio = InputProtection.readString("Hora início (apenas hora): ", true);
        String minutoInicio = InputProtection.readString("Minuto da hora início (apenas minutos): ", true);
        String horaFim = InputProtection.readString("Hora fim (apenas hora): ", true);
        String minutoFim = InputProtection.readString("Minuto da hora fim (apenas minutos): ", true);

        ArrayList<String> eventParams = new ArrayList<>();
        //eventParams.add(null);
        eventParams.add(local);
        eventParams.add(nome);
        eventParams.add(dia + '/' + mes + '/' + ano); // para transformar em xx/yy/zz
        eventParams.add(horaInicio + ':' + minutoInicio);
        eventParams.add(horaFim + ':' + minutoFim);

        this.client.createDBHelper("INSERT", "evento", eventParams, -1);

        System.out.println("New event created!\n");

        return true;
    }

    public boolean register() {

        String nome = InputProtection.readString("\tNome: ", false);
        String email = InputProtection.readString("\tEmail: ", true);
        int nif = InputProtection.readInt("\tNIF: ");
        String password = InputProtection.readString("\tPassword: ", true);


        ArrayList<String> userParams = new ArrayList<>();
        userParams.add(nome);
        userParams.add(email);
        userParams.add(Integer.toString(nif)); //nao esquecer que depois a ler tem que se fazer parse int
        userParams.add(password);
        userParams.add("0"); //autenticado
        userParams.add("0"); //admin

        //teste
        /*ResourceManager resourceManager = new ResourceManager();
        Data data = new Data(resourceManager);

        //mock add
        userParams.add("0");
        userParams.add("0");

        data.insertUser(userParams);*/

        //Send information to server -> depois disto o processo continua no server
        this.client.createDBHelper("INSERT","utilizador" , userParams, -1 /*,null*/);

        if(client.waitToReceiveResultRequest().equals("false")){
            System.out.println("Could not create a new user! Try again!");
            return false;
        }

        System.out.println("New user created! Welcome!");

        return true;
    }

    public void userMenu(){
        while(true){
            System.out.print("\nMain Menu");

            //TODO adicionar parametros ao menu
            int input = InputProtection.chooseOption("Choose an action:",  "List events", "Insert Event", "Exit");

            switch (input){
                case 1 -> {
                    System.out.println("XOTA"); // LISTAR EVENT
                }
                case 2 -> {
                    addEvent(); // ADD EVENT
                }
                case 5 -> {
                    return;
                }
            }
        }
    }

    public void start(){

        if(!loginRegister()){
            System.out.println("Could not login");
            loginRegister();
            return;
        }

        switch (admin){
            case 0 -> userMenu();
            //case 1 -> adminMenu();
        }
    }
}
