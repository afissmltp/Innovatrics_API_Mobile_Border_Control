package com.dynamsoft.documentscanner.model;

public class SessionData {
    private static SessionData instance;

    public String expirationStatus;
    public String mrzStatus;
    public String printCopyStatus;
    public String textConsistencyStatus;
    public String ocrConfidenceStatus;
    public String screenshotStatus;
    public String ageComparison;
    public String genderComparison;

    private SessionData() { }

    public static SessionData getInstance() {
        if (instance == null) {
            instance = new SessionData();
        }
        return instance;
    }

    public static void clear() {
        instance = null;
    }
}
