package com.trainsimulation.controller.context;

import com.trainsimulation.model.core.environment.TrainSystem;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;

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

    public StackPane getCanvases() {
        return (StackPane) this.simulationWindow.getContent();
    }

    public void setCanvases(StackPane canvases) {
        ((StackPane) this.simulationWindow.getContent()).getChildren().add(canvases);
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
