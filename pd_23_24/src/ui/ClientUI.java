package ui;

import model.client.Client;
import model.data.Data;
import resources.ResourceManager;
import ui.util.InputProtection;

import java.sql.SQLException;
import java.util.ArrayList;

public class ClientUI {

    private final Client client;
    private boolean isAdmin = false;
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

        /*if (client.getIsAdmin()) {
            userParams.add("0"); //autenticado
            userParams.add("0"); //admin

            this.client.insertEvento(this.client.dbHelper, userParams);
        }*/

        verifyLogin(email, password);

        String outputFromRequestResult = client.waitToReceiveResultRequest();

        if(outputFromRequestResult.equals("User doesnt exist")){
            System.out.println(outputFromRequestResult);
            return false;
        }else if(outputFromRequestResult.contains("User logged in")){
            StringBuilder idS = new StringBuilder();

            for(int i = 0; outputFromRequestResult.charAt(i) != 'U'; i++){
                idS.append(outputFromRequestResult.charAt(i));
            }

            int id = Integer.parseInt(idS.toString());

            this.client.setClientID(id);

            System.out.println(outputFromRequestResult);
        }

        return true;

    }

    public void verifyLogin(String email, String password){
        ArrayList<String> params= new ArrayList<>();
        params.add(email);
        params.add(password);
        this.client.createDBHelper("SELECT","utilizador" ,params,-1);
    }

    //ADMIN
    public boolean addEvent() {
        //TODO verificaçao de inputs

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

        if(client.waitToReceiveResultRequest().equals("event not created")){
            System.out.println("Could not create a new event");
            return false;
        }

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

        //Send information to server -> depois disto o processo continua no server
        this.client.createDBHelper("INSERT", "utilizador", userParams, -1 /*,null*/);

        String outputFromRequestResult = client.waitToReceiveResultRequest();

        StringBuilder idS = new StringBuilder();
        for(int i = 0; outputFromRequestResult.charAt(i) != 't'; i++){
            idS.append(outputFromRequestResult.charAt(i));
        }
        int id = Integer.parseInt(idS.toString());
        this.client.setClientID(id);
        if (outputFromRequestResult.equals("false")) {
            System.out.println("Could not create a new user! Try again!");
            return false;
        }
        System.out.println("New user created! Welcome!");
        return true;
    }

    public boolean editProfile(){

        int input = InputProtection.chooseOption("Choose an action:",  "Change Name", "Change Email", "Change Password", "Change NIF" , "Exit");

        switch (input){
            case 1 -> {
                String name = InputProtection.readString("\tNew name: ", false);

                ArrayList<String> updateParams = new ArrayList<>();
                updateParams.add("name");
                updateParams.add(name);
                this.client.createDBHelper("UPDATE", "utilizador", updateParams, this.client.getEmail());

                String outputFromRequestResult = client.waitToReceiveResultRequest();

                if(outputFromRequestResult.equals("Update done")){
                    //System.out.println("cheguei");
                    System.out.println(outputFromRequestResult);
                }else if(outputFromRequestResult.equals("Update failed")){
                    System.out.println(outputFromRequestResult);
                    return false;
                }

                return true;
            }
            case 2 -> {
                String email = InputProtection.readString("\tNew email: ", true);

                ArrayList<String> updateParams = new ArrayList<>();
                updateParams.add("email");
                updateParams.add(email);
                this.client.createDBHelper("UPDATE", "utilizador", updateParams, this.client.getEmail());

                String outputFromRequestResult = client.waitToReceiveResultRequest();

                if(outputFromRequestResult.equals("Update done")){
                    System.out.println(outputFromRequestResult);
                }else if(outputFromRequestResult.equals("Update failed")){
                    System.out.println(outputFromRequestResult);
                    return false;
                }

                return true;
            }
            case 3 -> {
                String password = InputProtection.readString("\tNew password: ", true);

                ArrayList<String> updateParams = new ArrayList<>();
                updateParams.add("password");
                updateParams.add(password);
                this.client.createDBHelper("UPDATE", "utilizador", updateParams, this.client.getEmail());

                String outputFromRequestResult = client.waitToReceiveResultRequest();

                if(outputFromRequestResult.equals("Update done")){
                    System.out.println(outputFromRequestResult);

                }else if(outputFromRequestResult.equals("Update failed")){
                    System.out.println(outputFromRequestResult);
                    return false;
                }

                return true;
            }
            case 4 -> {
                int nif = InputProtection.readInt("\tNew NIF: ");

                ArrayList<String> updateParams = new ArrayList<>();
                updateParams.add("nif");
                updateParams.add(Integer.toString(nif));
                this.client.createDBHelper("UPDATE", "utilizador", updateParams, this.client.getEmail());

                String outputFromRequestResult = client.waitToReceiveResultRequest();

                if(outputFromRequestResult.equals("Update done")){
                    System.out.println(outputFromRequestResult);
                }else if(outputFromRequestResult.equals("Update failed")){
                    System.out.println(outputFromRequestResult);
                    return false;
                }
            }
        }

        return false;
        //this.client.createDBHelper("UPDATE", ut);
    }

    public void listPresencas() {

        System.out.println("\nListing all events user is registered in: \n");
        System.out.println("\nListing: \n");

        int choiceMenu = InputProtection.chooseOption(null, "List all events user registered in", "Back to menu");

        switch (choiceMenu) {
            case 1 -> {
                int idEvento = InputProtection.readInt("Event ID (-1 for all events): ");
                this.client.createDBHelper("SELECT", "evento", idEvento, this.client.getClientID());

                System.out.println(client.waitToReceiveResultRequest());
            }
            case 2 -> {
                return;
            }
            default -> {
                System.out.println("Invalid choice!\n");
            }
        }
    }

    public void userMenu(){
        while(true){
            System.out.print("\nMain Menu");

            //TODO adicionar parametros ao menu
            int input = InputProtection.chooseOption("Choose an action:",  "List events", "Insert Event", "List All Presences" ,"Edit User Profile", "Exit");

            switch (input){
                case 1 -> {
                    System.out.println("XOTA"); // LISTAR EVENT
                }
                case 2 -> {
                    addEvent(); // ADD EVENT
                }
                case 3 -> {
                    listPresencas();
                }
                case 4 -> {
                    editProfile();
                }
                case 5 -> {
                    return;
                }
            }
        }
    }

    //public void start(){

        /*while(true){
            //System.out.println("Could not login");
            if(loginRegister())
                switch (admin){
                    case 0 -> userMenu();
                    //case 1 -> adminMenu();
                }
        }*/

       /* if(!loginRegister()){
            //System.out.println("Could not login");
            loginRegister();
            return;
        }

        switch (admin){
            case 0 -> userMenu();
            //case 1 -> adminMenu();
        }*/
    //}

    private void deleteEvent() {

    }

    public void adminUI() {
        System.out.println("\nWelcome back admin.\n");

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            System.out.println("\n------------ ADMIN MENU ------------");

            int choice = InputProtection.chooseOption("Choose action: ", "Create Event", "Edit Event Data", "Delete Event", "Check created events", "Generate code for user registration", "Check event attendance", "Get CSV file with event attendance", "Check events that user has attendance in", "Insert attendance", "Logout");

            switch(choice) {
                case 1 -> {
                    addEvent();
                }

                case 2 -> {
                    //editEventData();
                }

                case 3 -> {
                    deleteEvent();
                }

                case 4 -> {
                    // checkCreatedEvents();
                }

                case 5 -> {
                    // addCodeToEvent();
                }

                case 6 -> {
                    //listPresencasFromUserEmail();
                }

                case 7 -> {
                    //getCSVFileFromUser();
                }

                case 8 -> {
                    ///deleteAttendance();
                }

                case 9 -> {
                    //insertAttendance();
                }
            }
        }
    }

    public void start() {

        while (true) {
            //System.out.println("Could not login");
            if (loginRegister())
                if (!isAdmin) {
                    userMenu();
                } else if (isAdmin) {
                    adminUI();
                    //case 1 -> adminMenu();
                }
        }
    }
}
