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
                //return register();
                    try{
                        register();
                    }catch(SQLException e){
                        e.printStackTrace();
                    }

                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public boolean login(){

        String email, password;
        System.out.println();
        System.out.println("LOGIN");

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

    public boolean register() throws SQLException {

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
        this.client.createDBHelper("INSERT","user" , userParams, -1 /*,null*/);

        /*if(client.waitToReceiveResultRequest().equals("false")){
            System.out.println("Could not create a new user! Try again!");
            return false;
        }*/

        System.out.println("New user created! Welcome!");
        return true;

    }

    public void userMenu(){
        while(true){
            System.out.print("\nMain Menu");

            //TODO adicionar parametros ao menu
            int input = InputProtection.chooseOption("Choose an action:",  "List events", "Exit");

            switch (input){
                /*case 1 -> listEvents();
                case 5 -> {
                    client.closeClient();
                    return;
                }*/
            }
        }
    }

    public void start(){

        if(!loginRegister()){
            System.out.println("Could not login");
            return;
        }

        switch (admin){
            case 0 -> userMenu();
            //case 1 -> adminMenu();
        }
    }
}
