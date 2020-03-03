package com.trainsimulation.controller.screen;

import com.trainsimulation.controller.Main;
import com.trainsimulation.model.simulator.SimulationTime;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.stage.Stage;

public class SetupScreenController extends ScreenController {
    @FXML
    private Slider startHourSlider;

    @FXML
    private Slider startMinuteSlider;

    @FXML
    private Slider endHourSlider;

    @FXML
    private Slider endMinuteSlider;

    @FXML
    private Button setupButton;

    @FXML
    public void setupAction() {
        Stage stage = (Stage) setupButton.getScene().getWindow();

        // Set the simulation up
        int startHour = (int) startHourSlider.getValue();
        int startMinute = (int) startMinuteSlider.getValue();

        int endHour = (int) endHourSlider.getValue();
        int endMinute = (int) endMinuteSlider.getValue();

        final int defaultSecondValue = 0;

        SimulationTime time = new SimulationTime(startHour, startMinute, defaultSecondValue);
        SimulationTime endTime = new SimulationTime(endHour, endMinute, defaultSecondValue);

        Main.simulator.setup(time, endTime);

        // Signal to the parent controller that the button will be closed from the set up button
        ScreenController.setClosedWithAction(true);

        // Close the window
        stage.close();
    }
}