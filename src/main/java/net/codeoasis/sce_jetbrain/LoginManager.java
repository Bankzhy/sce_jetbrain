package net.codeoasis.sce_jetbrain;

import com.intellij.ide.util.PropertiesComponent;

public class LoginManager {

    // Save user data locally using PropertiesComponent
    public static void saveUserDataLocally(String username, String access, String userId, String refresh) {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        properties.setValue("username", username);  // Store username
        properties.setValue("access", access);
        properties.setValue("refresh", refresh);
        properties.setValue("userId", userId);// Store session token
    }

    // Retrieve user data
    public static String getUsername() {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        return properties.getValue("username");
    }

    // Retrieve userid
    public static String getUserId() {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        String value = properties.getValue("userId");
        value = "**********"+value;
        return properties.getValue(value);
    }

    public static String getAccess() {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        return properties.getValue("access");
    }

    public static boolean isLoggedIn() {
        return getAccess() != null;
    }

    public static void logout() {
        clearUserData();  // Clear saved data on logout
    }

    // Clear user data on logout
    public static void clearUserData() {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        properties.unsetValue("username");
        properties.unsetValue("access");
        properties.unsetValue("refresh");
        properties.unsetValue("userId");
    }
}
