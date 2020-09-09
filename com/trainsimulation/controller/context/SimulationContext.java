package com.trainsimulation.controller.context;

import com.trainsimulation.model.core.environment.TrainSystem;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

// Used to store the basic states of a simulation window to be presented on the screen
public class SimulationContext {
    private final Tab simulationWindow;
    private final TrainSystem trainSystem;
    private final double scaleDownFactor;

    public SimulationContext(Tab simulationWindow, TrainSystem trainSystem, double scaleDownFactor) {
        this.simulationWindow = simulationWindow;
        this.trainSystem = trainSystem;
        this.scaleDownFactor = scaleDownFactor;
    }

    public StackPane getLineViewCanvases() {
        VBox viewContainer = (VBox) ((BorderPane) this.simulationWindow.getContent()).getCenter();

        return (StackPane) viewContainer.getChildren().get(0);
    }

    public HBox getStationViewCanvases() {
        VBox viewContainer = (VBox) ((BorderPane) this.simulationWindow.getContent()).getCenter();

        return (HBox) viewContainer.getChildren().get(2);
    }

    public Tab getSimulationWindow() {
        return simulationWindow;
    }

    public TrainSystem getTrainSystem() {
        return trainSystem;
    }

    public double getScaleDownFactor() {
        return scaleDownFactor;
    }
}
