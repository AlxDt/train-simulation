package com.trainsimulation.controller.context;

import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

// Used to store the basic states of a simulation window to be presented on the screen
public class SimulationContext {
    private final Tab simulationWindow;
    private final TrainSystem trainSystem;
    private final double scaleDownFactor;
    public int stationIndex;
    private Station currentStation;
    private Button previousStationButton;
    private Text currentStationText;
    private Button nextStationButton;

    public SimulationContext(Tab simulationWindow, TrainSystem trainSystem, double scaleDownFactor) {
        this.simulationWindow = simulationWindow;
        this.trainSystem = trainSystem;
        this.scaleDownFactor = scaleDownFactor;
        this.stationIndex = 0;

        this.currentStation = this.trainSystem.getStations().get(this.stationIndex);
    }

    public TrainSystem getTrainSystem() {
        return trainSystem;
    }

    public double getScaleDownFactor() {
        return scaleDownFactor;
    }

    public StackPane getLineViewCanvases() {
        VBox viewContainer = (VBox) ((BorderPane) this.simulationWindow.getContent()).getCenter();

        return (StackPane) viewContainer.getChildren().get(0);
    }

    public StackPane getStationViewCanvases() {
        VBox viewContainer = (VBox) ((BorderPane) this.simulationWindow.getContent()).getCenter();

        return (StackPane) ((VBox) viewContainer.getChildren().get(2)).getChildren().get(1);
    }

    public Station getCurrentStation() {
        return this.currentStation;
    }

    public void setPreviousStationButton(Button previousStationButton) {
        this.previousStationButton = previousStationButton;
    }

    public Text getCurrentStationText() {
        return currentStationText;
    }

    public void setCurrentStationText(Text currentStationText) {
        this.currentStationText = currentStationText;

        currentStationText.setText(this.currentStation.getName());
    }

    public void setNextStationButton(Button nextStationButton) {
        this.nextStationButton = nextStationButton;
    }

    public void configureStationButtonsDisabled() {
        // Current station is the first station
        if (this.stationIndex == 0) {
            this.previousStationButton.setDisable(true);
            this.nextStationButton.setDisable(false);
        } else if (this.stationIndex == this.trainSystem.getStations().size() - 1) {
            // Current station is the last station
            this.previousStationButton.setDisable(false);
            this.nextStationButton.setDisable(true);
        } else {
            // Current station is somewhere between the first and last stations
            this.previousStationButton.setDisable(false);
            this.nextStationButton.setDisable(false);
        }
    }

    public boolean moveToPreviousStation() {
        if (this.stationIndex == 0) {
            return false;
        } else {
            this.stationIndex--;
            this.currentStation = this.trainSystem.getStations().get(this.stationIndex);

            // Change labels
            this.currentStationText.setText(this.currentStation.getName());
            this.configureStationButtonsDisabled();

            return true;
        }
    }

    public boolean moveToNextStation() {
        if (this.stationIndex == this.trainSystem.getStations().size() - 1) {
            return false;
        } else {
            this.stationIndex++;
            this.currentStation = this.trainSystem.getStations().get(this.stationIndex);

            // Change labels
            this.currentStationText.setText(this.currentStation.getName());
            this.configureStationButtonsDisabled();

            return true;
        }
    }
}
