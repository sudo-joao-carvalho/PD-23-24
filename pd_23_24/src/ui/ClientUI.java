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
            }else if(outputFromRequestResult.equals("User logged in")){
                System.out.println(outputFromRequestResult);
            }

            /*if(outputFromRequestResult.contains("\nAdmin:1"))
                admin = 1;*/

            /*int startIndex = outputFromRequestResult.lastIndexOf(":") + 2;
            String numberStr = outputFromRequestResult.substring(startIndex);
            int idClient = Integer.parseInt(numberStr);

            client.setClientID(idClient);*/

            /*out = out.replaceAll(" ", "");
            String[] splitted = out.split("\n");
            String[] id = splitted[0].split(":");*/

            //client.setClientID(Integer.parseInt(id[1]));
            //return true;
            //}while(true);

            return true;

    }

    public void verifyLogin(String email, String password){
        ArrayList<String> params= new ArrayList<>();
        params.add(email);
        params.add(password);
        this.client.createDBHelper("SELECT","utilizador" ,params,-1);
    }

<<<<<<< Updated upstream
=======
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

    public void submitEventCode() {
        String eventCode = InputProtection.readString("\tInsert event code: ", true);

        this.client.createDBHelper("INSERT", "presenca", eventCode, this.client.getClientID());

        String outputFromRequestResult = client.waitToReceiveResultRequest();
        System.out.println(outputFromRequestResult);
    }

    private void editEventData() {
        int choice = InputProtection.chooseOption(null, "Update Event Data", "Back to menu");

        if (choice == 1) {
            int eventId = InputProtection.readInt("Event ID: ");

            ArrayList<String> params = new ArrayList<>();


            int choice2 = InputProtection.chooseOption("Edit Menu: ", "Edit Event Name", "Edit Event Location", "Edit Event Date", "Edit Event Beginning Time", "Edit Event End Time", "Back to menu");

            switch (choice2) {
                case 1 -> {
                    String name = InputProtection.readString("New Event Name: ", false);
                    params.add("nome");
                    params.add(name);
                    this.client.createDBHelper("UPDATE", "evento", params, eventId);

                    String outputFromRequestResult = client.waitToReceiveResultRequest();

                    if(outputFromRequestResult.equals("Update done")){
                        System.out.println(outputFromRequestResult  + ": Name");
                    }else if(outputFromRequestResult.equals("Update failed")){
                        System.out.println(outputFromRequestResult);
                    }
                }

                case 2 -> {
                    String location = InputProtection.readString("New Event Location: ", false);
                    params.add("local");
                    params.add(location);
                    this.client.createDBHelper("UPDATE", "evento", params, eventId);

                    String outputFromRequestResult = client.waitToReceiveResultRequest();

                    if(outputFromRequestResult.equals("Update done")){
                        System.out.println(outputFromRequestResult  + ": Location");
                    }else if(outputFromRequestResult.equals("Update failed")){
                        System.out.println(outputFromRequestResult);
                    }
                }

                case 3 -> {
                    String date = InputProtection.readString("New Event Date (use this format dd/mm/yyyy): ", true); // fazer validação de mês não ser maior que 12, dia não ser maior que 31, ano não ser muito pequeno ou muito grande

                    if (!date.contains("/")) {
                        System.out.println("Wrong date format.\n");
                        break;
                    }

                    params.add("data");
                    params.add(date);

                    this.client.createDBHelper("UPDATE", "evento", params, eventId);

                    String outputFromRequestResult = client.waitToReceiveResultRequest();

                    if(outputFromRequestResult.equals("Update done")){
                        System.out.println(outputFromRequestResult  + ": Date");
                    }else if(outputFromRequestResult.equals("Update failed")){
                        System.out.println(outputFromRequestResult);
                    }
                }

                case 4 -> {
                    String beginningHour = InputProtection.readString("New Event Beginning Time (use this format hh:mm): ", true); // fazer validação de mês não ser maior que 12, dia não ser maior que 31, ano não ser muito pequeno ou muito grande

                    if (!beginningHour.contains(":")) {
                        System.out.println("Wrong date format.\n");
                        break;
                    }

                    params.add("horainicio");
                    params.add(beginningHour);

                    this.client.createDBHelper("UPDATE", "evento", params, eventId);

                    String outputFromRequestResult = client.waitToReceiveResultRequest();

                    if(outputFromRequestResult.equals("Update done")){
                        System.out.println(outputFromRequestResult  + ": Hora Inicio");
                    }else if(outputFromRequestResult.equals("Update failed")){
                        System.out.println(outputFromRequestResult);
                    }
                }

                case 5 -> {
                    String endingHour = InputProtection.readString("New Event Ending Time (use this format hh:mm): ", true); // fazer validação de mês não ser maior que 12, dia não ser maior que 31, ano não ser muito pequeno ou muito grande

                    if (!endingHour.contains(":")) {
                        System.out.println("Wrong date format.\n");
                        break;
                    }

                    params.add("horafim");
                    params.add(endingHour);

                    this.client.createDBHelper("UPDATE", "evento", params, eventId);

                    String outputFromRequestResult = client.waitToReceiveResultRequest();

                    if(outputFromRequestResult.equals("Update done")){
                        System.out.println(outputFromRequestResult + ": Hora Fim");
                    }else if(outputFromRequestResult.equals("Update failed")){
                        System.out.println(outputFromRequestResult);
                    }
                }
                case 6 -> {
                    return;
                }

                default -> {
                    System.out.println("Invalid choice.\n");
                }
            }

            params.clear();

        }
    }

    public boolean editProfile(){

        int input = InputProtection.chooseOption("Choose an action:",  "Change Name", "Change Email", "Change Password", "Change NIF" , "Exit");

        switch (input){
            case 1 -> {
                String name = InputProtection.readString("\tNew name: ", false);

                ArrayList<String> updateParams = new ArrayList<>();
                updateParams.add("name");
                updateParams.add(name);
                this.client.createDBHelper("UPDATE", "utilizador", updateParams, this.client.getEmail(), this.client.getClientID());

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
                this.client.createDBHelper("UPDATE", "utilizador", updateParams, this.client.getEmail(), this.client.getClientID());

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
                this.client.createDBHelper("UPDATE", "utilizador", updateParams, this.client.getEmail(), this.client.getClientID());

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
                this.client.createDBHelper("UPDATE", "utilizador", updateParams, this.client.getEmail(), this.client.getClientID());

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

    public void getCSV(){
        this.client.createDBHelper("SELECT", "evento", this.client.getClientID(), true);

        System.out.println(client.waitToReceiveResultRequest());
    }

    public void userMenu(){
        while(true){
            System.out.print("\nMain Menu");

            //TODO adicionar parametros ao menu
            int input = InputProtection.chooseOption("Choose an action:",  "Submit event code", "List All Presences" ,"Edit User Profile", "Get CSV file with presences", "Exit");

            switch (input){
                case 1 -> {
                    submitEventCode();
                }
                case 2 -> {
                    listPresencas();
                }
                case 3 -> {
                    editProfile();
                }
                case 4 -> {
                    getCSV();
                }
                case 5 -> {
                    return;
                }
            }
        }
    }

>>>>>>> Stashed changes
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

        //teste
        /*ResourceManager resourceManager = new ResourceManager();
        Data data = new Data(resourceManager);

        //mock add
        userParams.add("0");
        userParams.add("0");

        data.insertUser(userParams);*/

        //Send information to server -> depois disto o processo continua no server
        this.client.createDBHelper("INSERT", "utilizador", userParams, -1 /*,null*/);

        if (client.waitToReceiveResultRequest().equals("false")) {
            System.out.println("Could not create a new user! Try again!");
            return false;
        }

        System.out.println("New user created! Welcome!");

        return true;
    }

<<<<<<< Updated upstream
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

        /*System.out.println("\nListing all events user is registered in: \n");
=======
    private void checkCreatedEvents(){
        String searchFilter = InputProtection.readString("Search: ", true);

        this.client.createDBHelper("SELECT", "evento", searchFilter);
        System.out.println(client.waitToReceiveResultRequest());
    }

    private void addCodeToEvent(){
        int idEvento = InputProtection.readInt("Event Id: ");
        int codeExpirationTime = InputProtection.readInt("Code expiration time (minutes) : ");
        System.out.println("Randomizing code and inserting it into DataBase...");

        this.client.createDBHelper("UPDATE", "evento", idEvento, codeExpirationTime, -1);
>>>>>>> Stashed changes

        int choiceMenu = InputProtection.chooseOption(null, "List all events user registered in", "Back to menu");

        switch (choiceMenu) {
            case 1 -> {
                int id = InputProtection.readInt("Event ID (-1 for all events): ");

                this.client.createDBHelper("SELECT", "evento", null, id);
                System.out.println(client.waitToReceiveResultRequest());
            }
            case 2 -> {
                return;
            }
            default -> {
                System.out.println("Invalid choice!\n");
            }
        }*/
    }

<<<<<<< Updated upstream
    public void userMenu(){
        while(true){
            System.out.print("\nMain Menu");
=======
    private void checkRegisteredPresences(){
        int eventId = InputProtection.readInt("Insert the Event ID: ");

        this.client.createDBHelper("SELECT", "evento", eventId, -1, true, false);
        System.out.println(client.waitToReceiveResultRequest());
    }

    private void getCSVWithRegisteredPresences(){
        int eventId = InputProtection.readInt("Insert the Event ID: ");

        this.client.createDBHelper("SELECT", "evento", eventId, -1, true, true);
        System.out.println(client.waitToReceiveResultRequest());
    }

    private void listPresencasFromUserEmail(){
        String emailToSearch = InputProtection.readString("Insert the email to search: ", true);
>>>>>>> Stashed changes

            //TODO adicionar parametros ao menu
            int input = InputProtection.chooseOption("Choose an action:",  "List events", "Insert Event", "List All Presences" ,"Edit User Profile", "Exit");

            switch (input){
                case 1 -> {
                    System.out.println("Print1"); // LISTAR EVENT
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

<<<<<<< Updated upstream
    private void deleteEvent() {
=======
    private void getCSVFileFromUser(){
        String emailToSearch = InputProtection.readString("Insert the email to search: ", true);
>>>>>>> Stashed changes

        ArrayList<String> singleParamEmail = new ArrayList<>();
        singleParamEmail.add(emailToSearch);
        this.client.createDBHelper("SELECT", "evento", singleParamEmail, -1, true);

        String outputFromRequestResult = client.waitToReceiveResultRequest();
        System.out.println(outputFromRequestResult);
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

<<<<<<< Updated upstream
            int choice = InputProtection.chooseOption("Choose action: ", "Create Event", "Edit Event Data", "Delete Event", "Check created events", "Generate code for user registration", "Check event attendance", "Get CSV file with event attendance", "Check events that user has attendance in", "Insert attendance", "Logout");
=======
            int choice = InputProtection.chooseOption("Choose action: ", "Create Event", "Edit Event Data", "Delete Event", "Check created events", "Generate code for user registration", "Check registered presences", "Get CSV with registered presences" ,"Check event attendance by user email", "Get CSV with user attendance by user email", "Delete attendance", "Insert attendance", "Logout");
>>>>>>> Stashed changes

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
<<<<<<< Updated upstream
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
=======
                    checkRegisteredPresences();
                }

                case 7 -> {
                    getCSVWithRegisteredPresences();
                }

                case 8 -> {
                    listPresencasFromUserEmail();
                }

                case 9 -> {
                    getCSVFileFromUser();
                }

                case 10 -> {
                    deleteAttendance();
                }

                case 11 -> {
                    insertAttendance();
>>>>>>> Stashed changes
                }
            }
        }
    }

    public void start(){

<<<<<<< Updated upstream
<<<<<<< Updated upstream
        if(!loginRegister()){
=======
        while(true){
            //System.out.println("Could not login");
            if(loginRegister())
=======
        while (true) {
            if (loginRegister()) {
>>>>>>> Stashed changes
                if (!isAdmin) {
                    userMenu();
                } else if (isAdmin) {
                    adminUI();
                    //case 1 -> adminMenu();
                }
            }
        }

       /* if(!loginRegister()){
>>>>>>> Stashed changes
            //System.out.println("Could not login");
            loginRegister();
            return;
        }

        switch (admin){
            case 0 -> userMenu();
            //case 1 -> adminMenu();
        }
    }
}
