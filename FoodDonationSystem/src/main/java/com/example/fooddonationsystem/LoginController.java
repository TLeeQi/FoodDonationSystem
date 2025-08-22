package com.example.fooddonationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

/**
 * Controller for the login screen.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Read username/password from the UI.</li>
 *   <li>Validate against hard-coded credentials ({@code donor}/{@code 1234}).</li>
 *   <li>Show inline error/success messages.</li>
 *   <li>On success, switch to the main scene.</li>
 * </ul>
 * <em>Note:</em> This demo uses hard-coded credentials for simplicity. In a real app,
 * authenticate against a user store (DB/LDAP/etc.) and hash passwords.
 */
public class LoginController {

    /** @FXML Label injected from FXML. Displays validation errors or success text. */
    @FXML
    private Label wrongLogIn;

    /** @FXML TextField injected from FXML. Username input field. */
    @FXML
    private TextField username;

    /** @FXML PasswordField injected from FXML. Password input field (masked). */
    @FXML
    private PasswordField password;

    /**
     * FXML event handler for the "Log In" button.
     * Delegates to {@link #checkLogin()} to validate and route accordingly.
     *
     * @param event action event from the login button.
     * @throws Exception propagated from underlying calls (e.g., scene loading).
     */
    public void userLogIn(ActionEvent event) throws Exception{
        checkLogin();
    }

    /**
     * Validates the entered credentials and navigates to the main screen if valid.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Success when {@code username == "donor"} and {@code password == "1234"}.</li>
     *   <li>Otherwise sets a helpful message in {@link #wrongLogIn}.</li>
     *   <li>On success, calls {@link FoodDonationSystem#changeScene(String)} with {@code "main.fxml"}.</li>
     * </ul>
     *
     * @throws IOException if the next FXML cannot be loaded.
     */
    private void checkLogin() throws IOException {
        FoodDonationSystem m = new FoodDonationSystem();

        if (username.getText().toString().equals("donor") && password.getText().toString().equals("1234")){
            wrongLogIn.setText("Success!");
            m.changeScene("main.fxml");
        }
        else if (username.getText().isEmpty()){
            wrongLogIn.setText("Please enter your username (e.g., Donor)!");
        }
        else if (password.getText().isEmpty()){
            wrongLogIn.setText("Please enter your password!");
        }
        else{
            wrongLogIn.setText("Wrong username or password!");
        }
    }
}
