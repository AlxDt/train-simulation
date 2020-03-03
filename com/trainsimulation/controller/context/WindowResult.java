package com.trainsimulation.controller.context;

import com.trainsimulation.controller.screen.ScreenController;

// A context object for noting the result of opening/closing a window
public class WindowResult {
    // Denotes whether the window has been closed with action
    private boolean windowClosedWithAction;

    // Denotes the controller object of the successfully opened window
    private ScreenController screenController;

    public WindowResult(boolean windowClosedWithAction, ScreenController screenController) {
        this.windowClosedWithAction = windowClosedWithAction;
        this.screenController = screenController;
    }

    public boolean isWindowClosedWithAction() {
        return windowClosedWithAction;
    }

    public void setWindowClosedWithAction(boolean windowClosedWithAction) {
        this.windowClosedWithAction = windowClosedWithAction;
    }

    public ScreenController getScreenController() {
        return screenController;
    }

    public void setScreenController(ScreenController screenController) {
        this.screenController = screenController;
    }
}
