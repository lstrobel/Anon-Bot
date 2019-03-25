package model;

public class AnonymousModel {
    
    private static AnonymousModel ourInstance = new AnonymousModel();
    
    public static AnonymousModel getInstance() {
        return ourInstance;
    }
    
    private AnonymousModel() {
    }
    
    
}
