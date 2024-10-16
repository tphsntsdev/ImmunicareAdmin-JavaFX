package com.immunicare.immunicare_admin.view;

import java.util.ResourceBundle;

public enum FXMLView {
    LOGIN {
        @Override
		public String getTitle() {
            return getStringFromResourceBundle("login.title");
        }

        @Override
		public String getFxmlFile() {
            return "/fxml/login.fxml";
        }
    },
    ADMIN {
        @Override
		public String getTitle() {
            return getStringFromResourceBundle("admin.title");
        }

        @Override
		public String getFxmlFile() {
            return "/fxml/admin.fxml";
        }
    },
    FORGOTPASSWORD {
        @Override
        public String getTitle() {
            return getStringFromResourceBundle("forgotpassword.title");
        }

        @Override
        public String getFxmlFile() {
            return "/fxml/forgotpassword.fxml";
        }
    },
    UPDATEAPPOINTMENT {
        @Override
        public String getTitle() {
            return getStringFromResourceBundle("updateappointment.title");
        }

        @Override
        public String getFxmlFile() {
            return "/fxml/updateappointment.fxml";
        }
    },
    GENERATEREPORT {
        @Override
        public String getTitle() {
            return getStringFromResourceBundle("generatereport.title");
        }

        @Override
        public String getFxmlFile() {
            return "/fxml/generatereport.fxml";
        }
    },
    CLIENT {
        @Override
        public String getTitle() {
            return getStringFromResourceBundle("client.title");
        }

        @Override
        public String getFxmlFile() {
            return "/fxml/client.fxml";
        }
    };

    
    public abstract String getTitle();
    public abstract String getFxmlFile();
    String getStringFromResourceBundle(String key){
        return ResourceBundle.getBundle("Bundle").getString(key);
    }
}
