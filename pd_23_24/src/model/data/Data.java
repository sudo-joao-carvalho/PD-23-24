package model.data;

import resources.ResourceManager;

import java.util.ArrayList;

public class Data {

    private ResourceManager resourceManager;

    public Data(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public boolean connectToDB(String DBDirectory, int port){
        return this.resourceManager.connectToDB(DBDirectory, port);
    }

    /*public int getVersion() {
        this.resourceManager.getVersion();
    }*/

    public boolean insertUser(ArrayList<String> parameters){
        return this.resourceManager.insertUser(parameters);
    }

    public int insertEvent(ArrayList<String> params) { return this.resourceManager.insertEvent(params); }

    public boolean verifyLogin(ArrayList<String> params){ return this.resourceManager.verifyLogin(params); }
}
