package com.example.fooddonationsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX application entry point for the Food Donation System.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Bootstraps the JavaFX runtime and shows the initial login view.</li>
 *   <li>Provides a simple scene-switching helper via {@link #changeScene(String)}.</li>
 *   <li>Initializes the database connection once at startup (see {@link #main(String[])}).</li>
 * </ul>
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>The primary {@link Stage} is kept in a static field for convenient scene switching.
 *       If you prefer stricter encapsulation, consider passing the stage or using a scene
 *       manager instead of a static reference.</li>
 *   <li>FXML resources are resolved relative to this class's package
 *       (e.g., {@code /com/example/oopproject/login.fxml}).</li>
 * </ul>
 */
public class FoodDonationSystem extends Application {

    /** Primary application window used for scene switching. */
    private static Stage stage;

    /**
     * Called by the JavaFX runtime to start the UI.
     *
     * @param primaryStage the primary stage provided by the JavaFX runtime.
     * @throws IOException if the initial FXML (login view) cannot be loaded.
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        stage = primaryStage;
        primaryStage.setResizable(false);

        Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
        Scene scene = new Scene(root, 600, 400);

        primaryStage.setTitle("Food Donation System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Replaces the current scene root with the given FXML view.
     * <p>
     * The FXML path is resolved relative to this class's package. For example,
     * passing {@code "dashboard.fxml"} will attempt to load
     * {@code /com/example/oopproject/dashboard.fxml} from the classpath.
     *
     * @param fxml the relative FXML resource name (e.g., {@code "dashboard.fxml"}).
     * @throws IOException if the FXML cannot be found or loaded.
     */
    public void changeScene(String fxml) throws IOException {
        Parent pane = FXMLLoader.load(getClass().getResource(fxml));
        stage.getScene().setRoot(pane);
    }

    /**
     * JVM entry point.
     * <p>
     * Initializes the database connection (via {@link dbManagement}) and then launches
     * the JavaFX application lifecycle.
     *
     * @param args standard command-line arguments (unused).
     */
    public static void main(String[] args) {
        dbManagement dbConnect = new dbManagement();
        launch();
    }
}
