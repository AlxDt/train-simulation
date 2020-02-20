package com.trainsimulation.controller.screen;

import com.trainsimulation.controller.Controller;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public abstract class ScreenController extends Controller {
    // Used to pass values around windows, mostly to determine how a window was closed
    private static boolean closedWithAction = false;

    public ScreenController() {
    }

    public static boolean isClosedWithAction() {
        return closedWithAction;
    }

    public static void setClosedWithAction(boolean closedWithAction) {
        ScreenController.closedWithAction = closedWithAction;
    }

    public boolean showWindow(String interfaceLocation, String title, boolean isDialog) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(interfaceLocation));
        Scene scene = new Scene(root);
        Stage stage = new Stage();

        stage.setTitle(title);
        stage.setResizable(false);
        stage.setScene(scene);

        if (isDialog) {
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } else {
            stage.show();
        }

        return ScreenController.isClosedWithAction();
    }
}
