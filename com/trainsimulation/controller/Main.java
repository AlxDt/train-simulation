package com.trainsimulation.controller;

import com.trainsimulation.controller.context.WindowResult;
import com.trainsimulation.controller.screen.MainScreenController;
import com.trainsimulation.model.simulator.Simulator;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    // Stores the simulator object in charge of all the simulation processes
    public static Simulator simulator = null;

    // Keep a reference to the main controller
    public static MainScreenController mainScreenController;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Set the simulator up
            initializeSimulator();

            // Set the interface up
            // The reason we set the simulator up before the interface is because connecting to the database takes some
            // time so it's better to bring the interface up after the database connection has been fulfilled (otherwise
            // there will be a few seconds of unresponsiveness from the interface until the database connection is
            // fulfilled
            MainScreenController mainController = new MainScreenController();
            WindowResult windowResult
                    = mainController.showWindow("/com/trainsimulation/view/MainInterface.fxml",
                    "Train simulation",
                    false);

            Main.mainScreenController = (MainScreenController) windowResult.getScreenController();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    // Initializes the simulator
    private void initializeSimulator() throws Throwable {
        simulator = new Simulator();
    }
}
