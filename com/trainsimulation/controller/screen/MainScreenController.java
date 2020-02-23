package com.trainsimulation.controller.screen;

import com.trainsimulation.controller.Main;
import com.trainsimulation.controller.graphics.GraphicsController;
import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.Train;
import com.trainsimulation.model.db.DatabaseQueries;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class MainScreenController extends ScreenController {
    // Used to manage how the add train button is activated with respect to when the trains have left the depot and
    // entered a station
    public static final Semaphore ARM_ADD_TRAIN_BUTTON = new Semaphore(0);

    // Used to store the current canvas object
    private static final List<Canvas> CANVASES = new ArrayList<>();

    // Used to store the current train system object
    private static final List<TrainSystem> TRAIN_SYSTEMS = new ArrayList<>();

    // Used to store the index of the active element
    private static int activeIndex = 0;

    @FXML
    private Button setupButton;

    @FXML
    private Button startButton;

    @FXML
    private Button playPauseButton;

    @FXML
    private Button addTrainButton;

    public static List<Canvas> getCanvases() {
        return CANVASES;
    }

    public static List<TrainSystem> getTrainSystems() {
        return TRAIN_SYSTEMS;
    }

    // Get the active canvas
    public static Canvas getActiveCanvas() {
        return MainScreenController.getCanvases().get(MainScreenController.activeIndex);
    }

    // Get the active train system
    public static TrainSystem getActiveTrainSystem() {
        return MainScreenController.getTrainSystems().get(MainScreenController.activeIndex);
    }

    @FXML
    public void setupAction() throws IOException {
        SetupScreenController setupController = new SetupScreenController();

        // Set the buttons up, if the dialog was closed due to the set up button
        if (setupController.showWindow(
                "/com/trainsimulation/view/SetupInterface.fxml",
                "Set the simulation up",
                true)) {
            // Reset the close button check
            ScreenController.setClosedWithAction(false);

            // Get the train systems
            List<TrainSystem> trainSystems = Main.SIMULATOR.getTrainSystems();

            // Load the trains belonging to each train system
            loadTrains(trainSystems);

            // For each train line, create a tab for it
            TabPane tabPane = createTabs(setupButton.getScene(), trainSystems);

            // Draw the train systems onto each graphics canvas
            drawPerTab(tabPane, trainSystems);

            // Position the buttons
            positionButtonsSetup();
        }
    }

    @FXML
    public void startAction() {
        positionButtonsStart();

        // TODO: Start the simulation
        // TODO: How would we know if the simulation has ended
    }

    @FXML
    public void playPauseAction() {

    }

    @FXML
    public void addTrainAction() {
        // Get the list of trains
        List<Train> inactiveTrains = getActiveTrainSystem().getInactiveTrains();
        List<Train> activeTrains = getActiveTrainSystem().getActiveTrains();

        // Check if it is possible to spawn a new train by checking whether there are inactive trains left in the active
        // train system
        if (inactiveTrains.size() > 0) {
            // Temporarily disable this button to prevent race conditions concerning the situations when a train has
            // been added to a segment in the the system without the previous one not having left it yet
            addTrainButton.setDisable(true);

            // Get a train from that list of inactive trains and activate it
            Train train = inactiveTrains.remove(0);
            train.activate(activeTrains);

            // Run a quick thread to monitor the rearming of the add train button
            // This code is in a separate thread to avoid choking the JavaFX UI thread
            new Thread(() -> {
                try {
                    // Wait until the train enters a station from the depot for the first time
                    MainScreenController.ARM_ADD_TRAIN_BUTTON.acquire();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                // Eventually enable this button once the train has entered a station from the depot
                addTrainButton.setDisable(false);
            }).start();
        } else {
            // TODO: Display a dialog saying it is not possible
            System.out.println("There are no more trains for this train system.");
        }
    }

    private void positionButtonsSetup() {
        // Enable and reset the necessary buttons
        startButton.setDisable(false);

        playPauseButton.setDisable(true);
        playPauseButton.setText("Play");

        addTrainButton.setDisable(true);
    }

    private void positionButtonsStart() {
        // Enable and disable the necessary buttons
        playPauseButton.setDisable(false);
        startButton.setDisable(true);

        addTrainButton.setDisable(false);
    }

    private void positionButtonsPlayPause() {
        // Enable the necessary buttons
        if (playPauseButton.getText().equals("Play")) {
            playPauseButton.setText("Pause");

            addTrainButton.setDisable(false);
        } else {
            playPauseButton.setText("Play");

            addTrainButton.setDisable(true);
        }
    }

    // Load the trains to the inactive train memories of each train system
    private void loadTrains(List<TrainSystem> trainSystems) {
        for (TrainSystem trainSystem : trainSystems) {
            trainSystem.getInactiveTrains().addAll(DatabaseQueries.getTrains(Main.SIMULATOR.getDatabaseInterface(),
                    trainSystem)
            );
        }
    }

    // For each train system, create a tab for it
    private TabPane createTabs(Scene scene, List<TrainSystem> trainSystems) {
        BorderPane borderPane = (BorderPane) scene.getRoot();
        TabPane tabPane = (TabPane) ((BorderPane) borderPane.getCenter()).getCenter();

        // Clear the tab pane first
        tabPane.getTabs().clear();

        // Clear the active elements array too
        MainScreenController.getCanvases().clear();
        MainScreenController.getTrainSystems().clear();

        for (TrainSystem trainSystem : trainSystems) {
            // Create the tab, then add it to the tab pane
            // Add a canvas to each tab as well
            // This is where the visualizations will be drawn
            Tab tab = new Tab(trainSystem.getTrainSystemInformation().getName());

            Canvas canvas = new Canvas(tabPane.getBoundsInParent().getWidth(), tabPane.getBoundsInParent().getHeight());
            tab.setContent(canvas);

            tabPane.getTabs().add(tab);

            // Update the active elements
            MainScreenController.getCanvases().add(canvas);
            MainScreenController.getTrainSystems().add(trainSystem);

            updateActiveElements(tabPane.getSelectionModel().getSelectedIndex());
        }

        // Tell the tab pane that whenever the tab is changed, change the active canvas too, as well as the active train
        // system
        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    int index = tabPane.getSelectionModel().getSelectedIndex();

                    updateActiveElements(index);
                }
        );

        return tabPane;
    }

    // Draw the train infrastructure into the canvas
    private void drawPerTab(TabPane tabPane, List<TrainSystem> trainSystems) {
        // The number of tabs should be the same as the number of train systems
        assert tabPane.getTabs().size() == trainSystems.size() : "The number of tabs are not equal to the number of " +
                "train systems";

        // For each tab (which contains a canvas), draw its train system
        for (int trainsystemindex = 0; trainsystemindex < tabPane.getTabs().size(); trainsystemindex++) {
            Canvas canvas = getActiveCanvas();

            // Draw each train system onto its respective tab
            GraphicsController.requestDraw(canvas.getGraphicsContext2D(), trainSystems.get(trainsystemindex));
        }
    }

    // Update the active canvas and train systems
    private void updateActiveElements(int index) {
        MainScreenController.activeIndex = index;
    }
}
