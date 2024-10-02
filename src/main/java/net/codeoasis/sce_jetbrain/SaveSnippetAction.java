package net.codeoasis.sce_jetbrain;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
    import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

import java.io.IOException;
import java.util.Locale;

public class SaveSnippetAction extends AnAction {

    private void showNotification(String content) {
        Notification notification = new Notification(
                "Snippet Save Notification",  // Notification Group ID (can be used to categorize notifications)
                "Code Oasis",               // Title of the notification
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

    String formatLanguage(Language language) {
        String lang = language.getDisplayName();
        lang = lang.toLowerCase();
        return lang;
    }

    private void sendSnippetToServer(String snippet, AnActionEvent e) {

        // Check if the user is already logged in (by checking locally stored data)
        if (!LoginManager.isLoggedIn()) {
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

        try {
            Project project = e.getProject();
            Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
            String projectName = project.getName();

            // Create an HttpClient instance
            HttpClient client = HttpClient.newHttpClient();

            // Prepare the data (URL encoding the snippet to be safely transmitted)
            Language currentLanguage = LanguageUtil.getCurrentLanguage(project, e);

            JSONObject json = new JSONObject();
            json.put("snippet_id", -1);
            json.put("snippet_name", "auto");
            json.put("snippet_path", "root");
            json.put("keyword", "root,jetbrain,"+projectName);
            json.put("snippet", snippet);
            json.put("snippet_language", formatLanguage(currentLanguage));

            // Build the HTTP POST request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://www.codeoasis.net:8005/api/snippet/"))  // Replace with your server's URL
                    .header("Content-Type", "application/json")  // Set Content-Type header
                    .header("Authorization", "Bearer " + LoginManager.getAccess())
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))       // Attach the request body
                    .build();

            HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                JSONParser jsonParser = new JSONParser();
                JSONObject jsonObject = (JSONObject) jsonParser.parse(response.body().toString());
                showNotification("Code Snippet Save Successfully!");
            } else {
                showNotification("Save failed! The error code:" + response.statusCode());
            }

        } catch (Exception xe) {
            xe.printStackTrace();
            showNotification("An error occurred: " + xe.getMessage());
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);

        if (editor != null) {
            SelectionModel selectionModel = editor.getSelectionModel();
            String selectedText = selectionModel.getSelectedText();

            if (selectedText != null) {
                // Send the selectedText to the server (implement this next)
                sendSnippetToServer(selectedText, e);
            } else {
                showNotification("No text selected!");
            }
        }
    }
}