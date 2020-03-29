package com.trainsimulation.controller.screen;

import com.trainsimulation.controller.Main;
import com.trainsimulation.model.simulator.SimulationTime;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

public class SetupScreenController extends ScreenController {
    @FXML
    private Label startHourLabel;

    @FXML
    private Label startMinuteLabel;

    @FXML
    private Label endHourLabel;

    @FXML
    private Label endMinuteLabel;

    @FXML
    private Spinner<Integer> startHourSpinner;

    @FXML
    private Spinner<Integer> startMinuteSpinner;

    @FXML
    private Spinner<Integer> endHourSpinner;

    @FXML
    private Spinner<Integer> endMinuteSpinner;

    @FXML
    private Button setupButton;

    @FXML
    public void initialize() {
        // Set label references
        startHourLabel.setLabelFor(startHourSpinner);
        startMinuteLabel.setLabelFor(startMinuteSpinner);

        endHourLabel.setLabelFor(endHourSpinner);
        endMinuteLabel.setLabelFor(endMinuteSpinner);

        // Set spinners
        final int hourLimit = 23;
        final int minuteLimit = 59;

        final int startHourDefault = 4;
        final int endHourDefault = 22;

        final int startMinuteDefault = 30;
        final int endMinuteDefault = 30;

        SpinnerValueFactory<Integer> startHourSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, hourLimit, startHourDefault
        );

        SpinnerValueFactory<Integer> endHourSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, hourLimit, endHourDefault
        );

        SpinnerValueFactory<Integer> startMinuteSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, minuteLimit, startMinuteDefault
        );

        SpinnerValueFactory<Integer> endMinuteSpinnerFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, minuteLimit, endMinuteDefault
        );

        startHourSpinner.setValueFactory(startHourSpinnerFactory);
        endHourSpinner.setValueFactory(endHourSpinnerFactory);

        startMinuteSpinner.setValueFactory(startMinuteSpinnerFactory);
        endMinuteSpinner.setValueFactory(endMinuteSpinnerFactory);
    }

    @FXML
    public void setupAction() {
        Stage stage = (Stage) setupButton.getScene().getWindow();

        // Set the simulation up
        int startHour = startHourSpinner.getValue();
        int startMinute = startMinuteSpinner.getValue();

        int endHour = endHourSpinner.getValue();
        int endMinute = endMinuteSpinner.getValue();

        final int defaultSecondValue = 0;

        SimulationTime time = new SimulationTime(startHour, startMinute, defaultSecondValue);
        SimulationTime endTime = new SimulationTime(endHour, endMinute, defaultSecondValue);

        Main.simulator.setup(time, endTime);

        // Signal that the button is closed from the set up button
        this.setClosedWithAction(true);

        // Close the window
        stage.close();
    }
}