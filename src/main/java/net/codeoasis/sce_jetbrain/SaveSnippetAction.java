package net.codeoasis.sce_jetbrain;

import com.google.gson.Gson;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
                OasisActivator.doOKAction(username, password);
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
            Map<String, Object> postData = new HashMap<>();
            postData.put("snippet_id", -1);
            postData.put("snippet_name", "auto");
            postData.put("snippet_path", "root");
            postData.put("keyword", "root,jetbrain,"+projectName);
            postData.put("snippet", snippet);
            postData.put("add_time", OasisActivator.getCurrentFormatTime());
            postData.put("snippet_language", formatLanguage(currentLanguage));

            Gson gson = new Gson();
            String json = gson.toJson(postData);

            // Build the HTTP POST request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://www.codeoasis.net:8005/api/snippet/"))  // Replace with your server's URL
                    .header("Content-Type", "application/json")  // Set Content-Type header
                    .header("Authorization", "Bearer " + LoginManager.getAccess())
                    .POST(HttpRequest.BodyPublishers.ofString(json))       // Attach the request body
                    .build();

            HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                Map<String, Object> result = gson.fromJson(response.body().toString(), Map.class);
                showNotification("Code Snippet Save Successfully!");
            } else if(response.statusCode() == 401) {
                OasisActivator.showUnLoginNotification();
            }else {
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