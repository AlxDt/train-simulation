package com.trainsimulation.controller.screen;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXTabPane;
import com.trainsimulation.controller.Main;
import com.trainsimulation.controller.context.SimulationContext;
import com.trainsimulation.controller.graphics.GraphicsController;
import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.property.PassengerServiceProperty;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.property.TrainProperty;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.Train;
import com.trainsimulation.model.db.DatabaseQueries;
import com.trainsimulation.model.simulator.SimulationTime;
import com.trainsimulation.model.simulator.Simulator;
import com.trainsimulation.model.utility.TrainMovement;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainScreenController extends ScreenController {
    // Used to manage how the add train button is activated with respect to when the trains have left the depot and
    // entered a station
    public static final Semaphore ARM_ADD_TRAIN_BUTTON = new Semaphore(0);

    // Denotes whether the button activatable (regardless of whether the inactive trains list is empty or not)
    private static final AtomicBoolean armAddTrainActivateable = new AtomicBoolean(true);

    // Used to store all the simulation states to be presented on the screen
    private static final List<SimulationContext> SIMULATION_CONTEXTS = new ArrayList<>();

    // Used to store the index of the active element
    private static int activeIndex = 0;

    @FXML
    private JFXButton setupButton;

    @FXML
    private JFXButton startButton;

    @FXML
    private JFXButton playPauseButton;

    @FXML
    private JFXButton addTrainButton;

    @FXML
    private JFXSlider simulationSpeedSlider;

    @FXML
    private JFXButton editTrainButton;

    @FXML
    private JFXButton markTrainButton;

    @FXML
    private JFXButton clearMarkButton;

    @FXML
    private JFXSlider headwaySlider;

    @FXML
    private JFXCheckBox signalCheckBox;

    @FXML
    private Button previousStationButton;

    @FXML
    private Text currentStationText;

    @FXML
    private Button nextStationButton;

    @FXML
    private Label simulationSpeedLabel;

    @FXML
    private Text trainSystemText;

    @FXML
    private Text trainsDeployedText;

    @FXML
    private Label headwayLabel;

    @FXML
    private Text runningTimeText;

    @FXML
    private Text elapsedTimeText;

    @FXML
    private TableView<TrainProperty> activeTrainsTable;

    @FXML
    private TableColumn<TrainProperty, String> trainNumberColumn;

    @FXML
    private TableColumn<TrainProperty, String> trainStatusColumn;

    @FXML
    private TableColumn<TrainProperty, String> trainVelocityColumn;

//    @FXML
//    private TableColumn<Train.TrainProperty, String> trainPassengersColumn;

    // Get the active simulation context
    public static SimulationContext getActiveSimulationContext() {
        return MainScreenController.SIMULATION_CONTEXTS.get(MainScreenController.activeIndex);
    }

    @FXML
    public void initialize() {
        // Set label references
        simulationSpeedLabel.setLabelFor(simulationSpeedSlider);
        headwayLabel.setLabelFor(headwaySlider);

        // Set slider listeners
        simulationSpeedSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            SimulationTime.SLEEP_TIME_MILLISECONDS.set((int) (1.0 / newValue.intValue() * 1000));
        }));

        headwaySlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
            TrainMovement.HEADWAY_DISTANCE.set(newValue.intValue());
        }));

        signalCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            GraphicsController.SHOW_SIGNALS.set(newValue);
        });

        // Prepare the table columns
        trainNumberColumn.setCellValueFactory(trainNumber -> trainNumber.getValue().trainNumberProperty());
        trainStatusColumn.setCellValueFactory(trainStatus -> trainStatus.getValue().statusProperty());
        trainVelocityColumn.setCellValueFactory(trainVelocity -> trainVelocity.getValue().velocityProperty());

        // Prepare the table row bindings
        editTrainButton.disableProperty().bind(Bindings.isEmpty(activeTrainsTable.getSelectionModel().getSelectedItems()
        ));

        markTrainButton.disableProperty().bind(Bindings.isEmpty(activeTrainsTable.getSelectionModel().getSelectedItems()
        ));

        clearMarkButton.disableProperty().bind(Bindings.isEmpty(activeTrainsTable.getSelectionModel()
                .getSelectedItems()));
    }

    // Requests an update to the simulation time on the screen
    public void requestUpdateSimulationTime(SimulationTime simulationTime) {
        Platform.runLater(() -> {
            updateSimulationTime(simulationTime);
        });
    }

    // Updates the simulation time on the screen
    private void updateSimulationTime(SimulationTime simulationTime) {
        LocalTime currentTime = simulationTime.getTime();
        long elapsedTime = simulationTime.getStartTime().until(currentTime, ChronoUnit.SECONDS);

        String timeString;

        timeString = String.format("%02d", currentTime.getHour()) + ":"
                + String.format("%02d", currentTime.getMinute()) + ":"
                + String.format("%02d", currentTime.getSecond());

        runningTimeText.setText(timeString);

        timeString = "(" + elapsedTime + " s)";

        elapsedTimeText.setText(timeString);
    }

    @FXML
    public void setupAction() throws IOException {
        FXMLLoader loader = ScreenController.getLoader(getClass(),
                "/com/trainsimulation/view/SetupInterface.fxml");
        Parent root = loader.load();

        SetupScreenController setupController = loader.getController();

        setupController.showWindow(
                root,
                "Set the simulation up",
                true);

        // Set the buttons up, if the dialog was closed due to the set up button
        if (setupController.isClosedWithAction()) {
            // Get the train systems
            List<TrainSystem> trainSystems = Main.simulator.getTrainSystems();

            // Load the trains belonging to each train system
            loadTrains(trainSystems);

            // For each train line, create a tab for it
            JFXTabPane tabPane = createTabs(setupButton.getScene(), trainSystems);

            // Draw the train systems onto each graphics canvas
            drawPerTab(tabPane, trainSystems);

            // Position the buttons
            requestPositionButtonsSetup();

            // Update the UI initially
            requestUpdateUI(getActiveSimulationContext().getTrainSystem(), false);
        }
    }

    @FXML
    public void startAction() {
        requestPositionButtonsStart();

        // Start the simulation time
        Main.simulator.start();
    }

    @FXML
    public void playPauseAction() {
        requestPositionButtonsPlayPause();
    }

    @FXML
    public void addTrainAction() throws IOException {
        FXMLLoader loader = ScreenController.getLoader(
                getClass(),
                "/com/trainsimulation/view/InsertTrainInterface.fxml");
        Parent root = loader.load();

        InsertTrainScreenController insertTrainScreenController = loader.getController();

        // Set the window input first
        insertTrainScreenController.getWindowInput().put(InsertTrainScreenController.INPUT_KEYS[0],
                getActiveSimulationContext().getTrainSystem().getInactiveTrains());

        insertTrainScreenController.getWindowInput().put(InsertTrainScreenController.INPUT_KEYS[1],
                getActiveSimulationContext().getTrainSystem().getStations());

        // Insert input values
        insertTrainScreenController.setElements();

        insertTrainScreenController.showWindow(
                root,
                "Insert an " + MainScreenController.getActiveSimulationContext().getTrainSystem()
                        .getTrainSystemInformation().getName() + " train",
                true);

        // Set the buttons up, if the dialog was closed due to the set up button
        if (insertTrainScreenController.isClosedWithAction()) {
            // Retrieve the train to be activated
            Train selectedTrain
                    = (Train) insertTrainScreenController.getWindowOutput().get(InsertTrainScreenController.OUTPUT_KEY);

            // Get the list of trains
            List<Train> inactiveTrains = MainScreenController.getActiveSimulationContext().getTrainSystem()
                    .getInactiveTrains();

            List<Train> activeTrains = MainScreenController.getActiveSimulationContext().getTrainSystem()
                    .getActiveTrains();

            // Check if it is possible to spawn a new train by checking whether there are inactive trains left in the
            // active train system
            if (inactiveTrains.size() > 0) {
                // Temporarily disable this button to prevent race conditions concerning the situations when a train has
                // been added to a segment in the system without the previous one having left it yet
                addTrainButton.setDisable(true);

                // From this point on, the add train button may not be activated in any way
                MainScreenController.armAddTrainActivateable.set(false);

                // Get a train from that list of inactive trains and deploy it
                selectedTrain.deploy(activeTrains, inactiveTrains);

                // Reset the choices, as one train has already been chosen
                insertTrainScreenController.updateTrainChoices();

                // Run a quick thread to monitor the rearming of the add train button
                // This code is in a separate thread to avoid choking the JavaFX UI thread
                new Thread(() -> {
                    try {
                        // Wait until the train enters a station from the depot for the first time
                        MainScreenController.ARM_ADD_TRAIN_BUTTON.acquire();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }

                    // Eventually enable this button once the train has entered a station from the depot if there still
                    // are inactive trains left
                    if (inactiveTrains.size() > 0) {
                        addTrainButton.setDisable(false);
                    }

                    // The add train button may now be reactivated again
                    armAddTrainActivateable.set(true);
                }).start();

                // Update the UI
                requestUpdateUI(getActiveSimulationContext().getTrainSystem(), false);
            }
        }
    }

    @FXML
    public void editTrainAction() throws IOException {
        FXMLLoader loader = ScreenController.getLoader(
                getClass(),
                "/com/trainsimulation/view/EditTrainInterface.fxml");
        Parent root = loader.load();

        EditTrainScreenController editTrainScreenController = loader.getController();

        // Get the train selected
        Train trainSelected = (Train) activeTrainsTable.getSelectionModel().getSelectedItem().getOwner();

        // Only render the content if the train is active
        if (trainSelected.getTrainMovement().isActive()) {
            // Set the window input
            editTrainScreenController.getWindowInput().put(EditTrainScreenController.INPUT_KEYS[0], trainSelected);
            editTrainScreenController.getWindowInput().put(EditTrainScreenController.INPUT_KEYS[1],
                    getActiveSimulationContext().getTrainSystem().getStations());

            // Insert input values
            editTrainScreenController.setElements();
        }

        editTrainScreenController.showWindow(
                root,
                "Edit train #" + trainSelected.getIdentifier(),
                true);

        // Set the buttons up, if the dialog was closed due to the set up button
        if (editTrainScreenController.isClosedWithAction()) {
            // Get the edited train
            Train editedTrain
                    = (Train) editTrainScreenController.getWindowOutput().get(EditTrainScreenController.OUTPUT_KEY);

            // If the train was deactivated, prepare rearming the add train button
            if (!editedTrain.getTrainMovement().isActive()) {
                // Run a quick thread to monitor the rearming of the add train button
                // This code is in a separate thread to avoid choking the JavaFX UI thread
                new Thread(() -> {
                    try {
                        // Wait until the train enters a station from the depot for the first time
                        MainScreenController.ARM_ADD_TRAIN_BUTTON.acquire();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }

                    // Eventually enable this button
                    addTrainButton.setDisable(false);
                }).start();
            }
        }
    }

    @FXML
    public void markAction() {
        // Mark the selected train
        GraphicsController.markedTrain = (Train) activeTrainsTable.getSelectionModel().getSelectedItem()
                .getOwner();
    }

    @FXML
    public void clearMarkAction() {
        // Remove all markings
        GraphicsController.markedTrain = null;
    }

    private void requestPositionButtonsSetup() {
        Platform.runLater(() -> {
            // Enable and reset the necessary buttons
            simulationSpeedLabel.setDisable(true);
            simulationSpeedSlider.setDisable(true);

            headwayLabel.setDisable(true);
            headwaySlider.setDisable(true);

            signalCheckBox.setDisable(true);

            startButton.setDisable(false);

            playPauseButton.setDisable(true);
            playPauseButton.setText("Pause");

            addTrainButton.setDisable(true);

            activeTrainsTable.setDisable(true);
        });
    }

    private void requestPositionButtonsStart() {
        Platform.runLater(() -> {
            // Enable and disable the necessary buttons
            simulationSpeedLabel.setDisable(false);
            simulationSpeedSlider.setDisable(false);

            headwayLabel.setDisable(false);
            headwaySlider.setDisable(false);

            signalCheckBox.setDisable(false);

            playPauseButton.setDisable(false);
            startButton.setDisable(true);

            addTrainButton.setDisable(false);

            activeTrainsTable.setDisable(false);
        });
    }

    private void requestPositionButtonsPlayPause() {
        Platform.runLater(() -> {
            // Enable the necessary buttons
            if (playPauseButton.getText().equals("Play")) {
                simulationSpeedLabel.setDisable(false);
                simulationSpeedSlider.setDisable(false);

                headwayLabel.setDisable(false);
                headwaySlider.setDisable(false);

                signalCheckBox.setDisable(false);

                playPauseButton.setText("Pause");
                startButton.setDisable(true);

                addTrainButton.setDisable(false);

                activeTrainsTable.setDisable(false);
            } else {
                simulationSpeedLabel.setDisable(true);
                simulationSpeedSlider.setDisable(true);

                headwayLabel.setDisable(true);
                headwaySlider.setDisable(true);

                signalCheckBox.setDisable(true);

                playPauseButton.setText("Play");
                startButton.setDisable(true);

                addTrainButton.setDisable(true);

                activeTrainsTable.setDisable(true);
            }
        });
    }

    // Disables all necessary buttons
    public void requestDisableButtons() {
        Platform.runLater(() -> {
            // Enable and reset the necessary buttons
            simulationSpeedLabel.setDisable(true);
            simulationSpeedSlider.setDisable(true);

            headwayLabel.setDisable(true);
            headwaySlider.setDisable(true);

            signalCheckBox.setDisable(true);

            startButton.setDisable(true);

            playPauseButton.setDisable(true);
            playPauseButton.setText("Done");

            addTrainButton.setDisable(true);

            activeTrainsTable.setDisable(true);
        });
    }

    // Called when changing tabs, this requests for an update to the UI details
    public void requestUpdateUI(final TrainSystem activeTrainSystem, boolean checkButton) {
        Platform.runLater(() -> {
            // Update the train system information on the UI
            updateTrainSystem(activeTrainSystem);
            updateActiveTrainCount(activeTrainSystem);
            updateActiveTrainsTable(activeTrainSystem);

            // Check whether the add train button should be disabled
            if (checkButton) {
                addTrainButton.setDisable(
                        !Simulator.getStarted().get() || (
                                activeTrainSystem.getInactiveTrains().isEmpty()
                                        || !MainScreenController.armAddTrainActivateable.get()
                        )
                );
            }
        });
    }

    // Update the train system displayed in the UI
    private void updateTrainSystem(final TrainSystem activeTrainSystem) {
        trainSystemText.setText(activeTrainSystem.getTrainSystemInformation().getName());
    }

    // Update the active train count of the active train system on the UI
    private void updateActiveTrainCount(final TrainSystem activeTrainSystem) {
        int activeTrainCount = activeTrainSystem.getActiveTrains().size();
        int totalTrainCount = activeTrainSystem.getActiveTrains().size()
                + activeTrainSystem.getInactiveTrains().size();

        trainsDeployedText.setText(activeTrainCount + " / " + totalTrainCount + " trains deployed");
    }

    // Update the active trains table on the UI
    public void updateActiveTrainsTable(final TrainSystem activeTrainSystem) {
        Platform.runLater(() -> {
            // Prepare the train properties (the table data)
            List<Train> activeTrainList = activeTrainSystem.getActiveTrains();
            List<TrainProperty> activeTrainPropertyList = new ArrayList<>();

            for (Train train : activeTrainList) {
                activeTrainPropertyList.add(train.getTrainProperty());
            }

            // Prepare the table in the UI (the table itself)
            activeTrainsTable.setItems(PassengerServiceProperty.toObservablePropertyList(activeTrainPropertyList));
        });
    }

    // Load the trains to the inactive train memories of each train system
    private void loadTrains(List<TrainSystem> trainSystems) {
        for (TrainSystem trainSystem : trainSystems) {
            trainSystem.getInactiveTrains().addAll(DatabaseQueries.getTrains(Main.simulator.getDatabaseInterface(),
                    trainSystem)
            );
        }
    }

    // For each train system, create a tab for it
    private JFXTabPane createTabs(Scene scene, List<TrainSystem> trainSystems) {
        // TODO: Automate the generation of these constants
        // Set the scale down constants of the train system
        final double[] scaleDownConstants = {0.046, 0.0679, 0.0579};

        BorderPane borderPane = (BorderPane) scene.getRoot();
        JFXTabPane tabPane = (JFXTabPane) ((BorderPane) borderPane.getCenter()).getCenter();

        // Clear the tab pane first
        tabPane.getTabs().clear();

        // Clear the active contexts too
        MainScreenController.SIMULATION_CONTEXTS.clear();

        for (int trainSystemIndex = 0; trainSystemIndex < trainSystems.size(); trainSystemIndex++) {
            TrainSystem trainSystem = trainSystems.get(trainSystemIndex);

            // Create the tab, then add it to the tab pane
            // Then add the border panes to each tab
            // Add two canvases to each pane as well
            // These are where the visualizations will be drawn
            // Each pane will get a sidebar for control as well
            Tab tab = new Tab(trainSystem.getTrainSystemInformation().getName());

            // Add a border pane
            BorderPane tabBorderPane = new BorderPane();

            // Create the line and station views
            StackPane lineViewStackPane = createLineView(tabPane);
            VBox stationViewVBox = createStationView(tabPane);

            // Add a separator in between
            Separator separator = new Separator(Orientation.HORIZONTAL);

            // Then create a container to handle both stack panes
            VBox viewContainer = new VBox(lineViewStackPane, separator, stationViewVBox);
            viewContainer.setPadding(new Insets(20.0));

            // Then add the container to the center of the border pane
            tabBorderPane.setCenter(viewContainer);

            // Finally, add the border pane to the tab
            tab.setContent(tabBorderPane);
            tabPane.getTabs().add(tab);

            // Update the active elements
            MainScreenController.SIMULATION_CONTEXTS.add(new SimulationContext(tab, trainSystem,
                    scaleDownConstants[trainSystemIndex]));

            updateActiveContext(tabPane.getSelectionModel().getSelectedIndex());
        }

        // Tell the tab pane that whenever the tab is changed, change the active canvas too, as well as the active train
        // system
        // Then update the UI accordingly with respect to the current train system
        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    int index = tabPane.getSelectionModel().getSelectedIndex();

                    updateActiveContext(index);

                    // Update the UI accordingly
                    requestUpdateUI(getActiveSimulationContext().getTrainSystem(), true);

                    // Redraw the graphics
                    GraphicsController.requestDraw(MainScreenController.getActiveSimulationContext().getLineViewCanvases(),
                            MainScreenController.getActiveSimulationContext().getTrainSystem(),
                            MainScreenController.getActiveSimulationContext().getScaleDownFactor(), false);
                }
        );

        return tabPane;
    }

    private VBox createStationView(JFXTabPane tabPane) {
        // Create a button bar for the station view controls
        JFXButton previousStationButton = new JFXButton("<");

        previousStationButton.setButtonType(JFXButton.ButtonType.RAISED);
        previousStationButton.setDisable(true);
        previousStationButton.setRipplerFill(Color.WHITE);
        previousStationButton.setStyle("-fx-background-color: #85756e;");
        previousStationButton.setTextFill(Color.WHITE);
        previousStationButton.setFont(new Font("System Bold", 12.0));

        Text currentStationText = new Text("No train systems loaded");

        currentStationText.setStrokeType(StrokeType.OUTSIDE);
        currentStationText.setStrokeWidth(0.0);
        currentStationText.setFont(new Font("System Bold", 12.0));

        JFXButton nextStationButton = new JFXButton(">");

        nextStationButton.setButtonType(JFXButton.ButtonType.RAISED);
        nextStationButton.setDisable(true);
        nextStationButton.setRipplerFill(Color.WHITE);
        nextStationButton.setStyle("-fx-background-color: #85756e;");
        nextStationButton.setTextFill(Color.WHITE);
        nextStationButton.setFont(new Font("System Bold", 12.0));

        HBox stationViewControls = new HBox(previousStationButton, currentStationText, nextStationButton);

        stationViewControls.setAlignment(Pos.CENTER);
        stationViewControls.setPrefHeight(50.0);
        stationViewControls.setSpacing(20.0);

        // Create canvases for the station view
        Canvas stationViewBackgroundCanvas
                = new Canvas(tabPane.getBoundsInParent().getWidth(),
                tabPane.getBoundsInParent().getHeight() * 0.75);

        Canvas stationViewForegroundCanvas
                = new Canvas(tabPane.getBoundsInParent().getWidth(),
                tabPane.getBoundsInParent().getHeight() * 0.75);

        // Create a stack pane to handle the canvases
        StackPane stationViewCanvases = new StackPane(
                stationViewBackgroundCanvas,
                stationViewForegroundCanvas
        );

        // Then create a vbox that handles the station controls along with the canvases
        return new VBox(stationViewControls, stationViewCanvases);
    }

    private StackPane createLineView(JFXTabPane tabPane) {
        // Create canvases for the line view
        Canvas lineViewBackgroundCanvas
                = new Canvas(tabPane.getBoundsInParent().getWidth(),
                tabPane.getBoundsInParent().getHeight() * 0.25);
        Canvas lineViewForegroundCanvas
                = new Canvas(tabPane.getBoundsInParent().getWidth(),
                tabPane.getBoundsInParent().getHeight() * 0.25);

        // Create a stack pane to handle the canvases
        return new StackPane(lineViewBackgroundCanvas, lineViewForegroundCanvas);
    }

    // Draw the train infrastructure into the canvas
    private void drawPerTab(JFXTabPane tabPane, List<TrainSystem> trainSystems) {
        // The number of tabs should be the same as the number of train systems
        assert tabPane.getTabs().size() == trainSystems.size() : "The number of tabs are not equal to the number of " +
                "train systems";

        // For each tab (which contains a canvas), draw its train system
        StackPane canvases;
        double scaleDownFactor;

        for (int trainSystemIndex = 0; trainSystemIndex < tabPane.getTabs().size(); trainSystemIndex++) {
            canvases = MainScreenController.SIMULATION_CONTEXTS.get(trainSystemIndex).getLineViewCanvases();
            scaleDownFactor = MainScreenController.SIMULATION_CONTEXTS.get(trainSystemIndex).getScaleDownFactor();

            // Draw each train system onto its respective tab
            GraphicsController.requestDraw(canvases, trainSystems.get(trainSystemIndex), scaleDownFactor,
                    true);
        }
    }

    // Update the active canvas and train systems
    private void updateActiveContext(int index) {
        MainScreenController.activeIndex = index;
    }
}
