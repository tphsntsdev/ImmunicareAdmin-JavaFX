package com.immunicare.immunicare_admin.session;
import java.util.HashMap;
import java.util.Map;


public class JavaSession {
    private static JavaSession instance;
    private Map<String, Object> sessionData;
    private JavaSession() {
        sessionData = new HashMap<>();
    }
    public static synchronized JavaSession getInstance() {
        if (instance == null) {
            instance = new JavaSession();
        }
        return instance;
    }
    public void startSession(String firstName, String lastName, String imageURL, String title, String licensenumber) {
        sessionData.put("firstName", firstName);
        sessionData.put("lastName", lastName);
        sessionData.put("imageURL", imageURL);
        sessionData.put("title", title);
        sessionData.put("license_number", licensenumber);

    }

    public void endSession() {
        sessionData.clear();
    }

    public Object getSessionVariable(String key) {
        return sessionData.get(key);
    }

    public String getfirstName() {
        return (String) getSessionVariable("firstName");
    }

    public String getLastName() {
        return (String) getSessionVariable("lastName");
    }

    public String getimageURL() {
        return (String) getSessionVariable("imageURL");
    }
    public String getTitle() {
        return (String) getSessionVariable("title");
    }

    public String getLicense_Number() {
        return (String) getSessionVariable("license_number");
    }
}
