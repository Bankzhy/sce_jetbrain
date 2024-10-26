package net.codeoasis.sce_jetbrain;

import com.google.gson.Gson;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class LoginAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        // Show login window
//        LoginDialog loginDialog = new LoginDialog();
//        if (loginDialog.showAndGet()) {  // If the user clicked "OK"
//            String username = loginDialog.getUsername();
//            String password = loginDialog.getPassword();
//            OasisActivator.doOKAction(username, password);
//        } else {
//            showNotification("Login canceled!");
//            return;  // Exit without sending the snippet
//        }

        UserInfoDialog userInfoDialog = new UserInfoDialog();
        if (userInfoDialog.showAndGet()) {
            LoginDialog loginDialog = new LoginDialog();
            if (loginDialog.showAndGet()) {  // If the user clicked "OK"
                String username = loginDialog.getUsername();
                String password = loginDialog.getPassword();
                OasisActivator.doOKAction(username, password);
            } else {
//                showNotification("Login canceled!");
                return;  // Exit without sending the snippet
            }
        } else {

        }
    }
}
