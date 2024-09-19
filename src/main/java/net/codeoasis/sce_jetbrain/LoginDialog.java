package net.codeoasis.sce_jetbrain;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.*;

public class LoginDialog extends DialogWrapper {

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginDialog() {
        super(true); // use current window as parent
        setTitle("Login to the Code Oasis");
        init();  // Initializes the dialog
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2));

        panel.add(new JLabel("Please login to the Code Oasis."));
        panel.add(new JLabel(""));
        panel.add(new JLabel("Username:"));
        usernameField = new JTextField(20);
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField(20);
        panel.add(passwordField);

        return panel;
    }

    @Override
    protected JComponent createSouthPanel() {
        JComponent buttonsPanel = super.createSouthPanel();
        JButton loginButton = getButton(getOKAction());  // Get the "OK" button
        loginButton.setText("Login");  // Change the button text to "Login"
        return buttonsPanel;
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }
}
