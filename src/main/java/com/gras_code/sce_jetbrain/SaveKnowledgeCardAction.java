package com.gras_code.sce_jetbrain;

import com.google.gson.Gson;
import com.intellij.lang.Language;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class SaveKnowledgeCardAction extends AnAction {

    private void showNotification(String content) {
        Notification notification = new Notification(
                "Snippet Save Notification",  // Notification Group ID (can be used to categorize notifications)
                "GRAS",               // Title of the notification
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

    private void sendKnowledgeCardToServer(String snippet, AnActionEvent e) {

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
            postData.put("text", snippet);
            postData.put("url", "");
//            postData.put("snippet_language", formatLanguage(currentLanguage));

            Gson gson = new Gson();
            String json = gson.toJson(postData);

            // Build the HTTP POST request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://www.gras-code.com/backend/api/knowledge_card_from_text_task/"))  // Replace with your server's URL
                    .header("Content-Type", "application/json")  // Set Content-Type header
                    .header("Authorization", "Bearer " + LoginManager.getAccess())
                    .POST(HttpRequest.BodyPublishers.ofString(json))       // Attach the request body
                    .build();

            HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                Map<String, Object> result = gson.fromJson(response.body().toString(), Map.class);
                System.out.println(result);
                showNotification("Knowledge Card Saved !");
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
                sendKnowledgeCardToServer(selectedText, e);
            } else {
                showNotification("No text selected!");
            }
        }
    }
}