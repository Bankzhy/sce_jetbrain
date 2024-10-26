package net.codeoasis.sce_jetbrain;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class UserInfoDialog extends DialogWrapper {
    private JTextField userIdField;
    private JTextField usernameField;
    private JButton loginButton;
    private JButton logoutButton;

    public UserInfoDialog() {
        super(true); // use current window as parent
        setTitle("Login to the Code Oasis");
        init();  // Initializes the dialog
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2));

        panel.add(new JLabel("User Id:"));
        String userId = LoginManager.getUserId();
        if (userId == null) {
            userId = "Not data";
        }
        panel.add(new JLabel(userId));

        String username = LoginManager.getUsername();
        if (username == null) {
            username = "Not data";
        }
        panel.add(new JLabel("User Name:"));
        panel.add(new JLabel(username));
        return panel;
    }

    private void onLogin(ActionEvent e) {
        // Close the dialog
        close(DialogWrapper.CLOSE_EXIT_CODE);

        LoginDialog loginDialog = new LoginDialog();
        if (loginDialog.showAndGet()) {  // If the user clicked "OK"
            String username = loginDialog.getUsername();
            String password = loginDialog.getPassword();
            OasisActivator.doOKAction(username, password);
        } else {
//            showNotification("Login canceled!");
            return;  // Exit without sending the snippet
        }
    }

    private void onLogout(ActionEvent e) {
        // Close the dialog
        close(DialogWrapper.CLOSE_EXIT_CODE);
        LoginManager.logout();
    }


    @Override
    protected JComponent createSouthPanel() {
        JPanel southPanel = new JPanel();

        JButton loginButton = new JButton("Login");
        JButton logoutButton = new JButton("Logout");

        loginButton.addActionListener(this::onLogin);
        logoutButton.addActionListener(this::onLogout);

        // Add buttons to the south panel
        southPanel.add(loginButton);
        southPanel.add(logoutButton);

        return southPanel;
    }
}
