package ui;

import model.ModelManager;
import ui.util.InputProtection;

import java.io.IOException;
import java.sql.SQLException;

public class ServerUI {
    private final ModelManager modelManagerData;

    public ServerUI(ModelManager modelManagerData) {
        this.modelManagerData = modelManagerData;
    }

    /*private void listAllAvailableServers() {
        System.out.println(this.modelManagerData.listAllAvailableServers());
    }*/

    public void start() {

        //System.out.println("Server running");

        //while (true) {

            /*try{
                Thread.sleep(500);
            }catch (InterruptedException ignored){
            }*/

            //int input = InputProtection.chooseOption("Choose an action:", "List available servers", "Exit");

            //switch (input) {
                /*case 1 -> listInformation();
                case 1 -> listInformation();
                case 2 -> insertData();
                case 3 -> deleteData();
                case 4 -> updateData();*/
                /*case 1 -> listAllAvailableServers();
                case 2 -> {
                    try{
                        this.modelManagerData.closeServer();
                    }catch (SQLException | IOException | InterruptedException ignored){}
                    return;
                }*/
            //}
        //}
    }
}