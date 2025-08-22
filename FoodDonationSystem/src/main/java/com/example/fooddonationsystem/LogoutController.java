package com.example.fooddonationsystem;

import javafx.event.ActionEvent;

/**
 * Controller for handling user logout.
 * <p>
 * Responsibility:
 * <ul>
 *   <li>Switch the current scene back to the login view.</li>
 * </ul>
 * <em>Usage:</em> Wire {@link #userLogOut(ActionEvent)} to a Logout button's
 * {@code onAction} in the FXML.
 */
public class LogoutController {

    /**
     * FXML action handler that navigates back to the login screen.
     *
     * @param event action event from the Logout button.
     * @throws Exception if the login FXML cannot be loaded.
     */
    public void userLogOut(ActionEvent event) throws Exception {
        FoodDonationSystem m = new FoodDonationSystem();
        m.changeScene("login.fxml");
    }
}
