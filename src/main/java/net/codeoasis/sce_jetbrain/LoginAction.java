package net.codeoasis.sce_jetbrain;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoginAction extends AnAction {

    private void showNotification(String content) {
        System.out.println(content);
        Notification notification = new Notification(
                "Snippet Save Notification",  // Notification Group ID (can be used to categorize notifications)
                "Code Oasis Login",               // Title of the notification
                content,                      // Content of the notification
                NotificationType.INFORMATION  // Type of notification (e.g., INFORMATION, WARNING, ERROR)
        );
        Notifications.Bus.notify(notification);
    }

    protected void doOKAction(String username, String password) {

        // Call the login method from LoginManager
        try {
            // Create an HttpClient instance
            HttpClient client = HttpClient.newHttpClient();

            // Prepare the data (URL encoding the snippet to be safely transmitted)
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("password", password);

            // Build the HTTP POST request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://www.codeoasis.net:8005/clogin/"))  // Replace with your server's URL
                    .header("Content-Type", "application/json")  // Set Content-Type header
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))       // Attach the request body
                    .build();

            // Send the request asynchronously
            HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONParser jsonParser = new JSONParser();
                JSONObject jsonObject = (JSONObject) jsonParser.parse(response.body().toString());
                LoginManager.saveUserDataLocally(
                        jsonObject.getAsString("username"),
                        jsonObject.getAsString("access"),
                        jsonObject.getAsString("userId"),
                        jsonObject.getAsString("refresh")
                );
                showNotification("Login successfully!");
            } else {
                showNotification("login failed! The error code:" + response.statusCode());
            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        // Show login window
        LoginDialog loginDialog = new LoginDialog();
        if (loginDialog.showAndGet()) {  // If the user clicked "OK"
            String username = loginDialog.getUsername();
            String password = loginDialog.getPassword();
            doOKAction(username, password);
        } else {
            showNotification("Login canceled!");
            return;  // Exit without sending the snippet
        }
    }
}
