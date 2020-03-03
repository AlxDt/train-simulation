package com.trainsimulation.controller.graphics;

import com.trainsimulation.controller.Controller;
import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.infrastructure.track.Junction;
import com.trainsimulation.model.core.environment.infrastructure.track.Segment;
import com.trainsimulation.model.core.environment.infrastructure.track.Track;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
import com.trainsimulation.model.utility.TrainQueue;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.concurrent.atomic.AtomicBoolean;

public class GraphicsController extends Controller {
    // Denotes whether signals should be shown
    public static final AtomicBoolean SHOW_SIGNALS = new AtomicBoolean(false);

    // Send a request to draw on the canvas
    public static void requestDraw(StackPane canvases, TrainSystem trainSystem, boolean background) {
        Platform.runLater(() -> {
            // Tell the JavaFX thread that we'd like to draw on the canvas
            draw(canvases, trainSystem, background);
        });
    }

    // Draw all that is needed in the canvas
    private static void draw(StackPane canvases, TrainSystem trainSystem, boolean background) {
        // Get the canvases and their graphics contexts
        final Canvas backgroundCanvas = (Canvas) canvases.getChildren().get(0);
        final Canvas foregroundCanvas = (Canvas) canvases.getChildren().get(1);

        final GraphicsContext backgroundGraphicsContext = backgroundCanvas.getGraphicsContext2D();
        final GraphicsContext foregroundGraphicsContext = foregroundCanvas.getGraphicsContext2D();

        // Get the height and width of the canvases
        final double CANVAS_WIDTH = backgroundCanvas.getWidth();
        final double CANVAS_HEIGHT = backgroundCanvas.getHeight();

        // Clear everything in the foreground canvas, if all dynamic elements are to be drawn
        if (!background) {
            foregroundGraphicsContext.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        }

        // TODO: Dynamically compute for the dimensions of the visualization
        // Constants for graphics drawing
        final double initialX = CANVAS_WIDTH * 0.02;
        final double initialY = CANVAS_HEIGHT * 0.5;

        // A factor used to scale down the real-world train line dimensions
        final double scaleDownFactor = 0.07;

        // The font to be used
        final String FONT_NAME = "Segoe UI";

        // The font size of the text
        final int FONT_SIZE = 10;

        // The maximum width of the station labels
        final int STATION_LABEL_MAX_WIDTH = 80;

        // The width of lines
        final double LINE_WIDTH = 3.0;

        // The size of the trains
        final double trainGraphicsDiameter = LINE_WIDTH * 1.1;

        // The size of the signals
        final double signalGraphicsDiameter = LINE_WIDTH;

        // The height of the station
        final double STATION_HEIGHT = LINE_WIDTH * 4.0;

        // Prepare the colors and fonts
        Color color = toColor(trainSystem.getTrainSystemInformation().getColor());
        Font font = new Font(FONT_NAME, FONT_SIZE);

        Color trainColor = Color.SILVER;

        backgroundGraphicsContext.setFill(color);
        backgroundGraphicsContext.setStroke(color);

        backgroundGraphicsContext.setFont(font);

        // Prepare other graphics settings
        backgroundGraphicsContext.setLineWidth(LINE_WIDTH);

        // Take note of the direction of drawing
        Track.Direction drawingDirection = Track.Direction.NORTHBOUND;

        // Look for the first station
        Station station = trainSystem.getStations().get(0);

        double x = initialX;
        double y = initialY;

        double yNorthbound = y + y * 0.01;
        double ySouthbound = y - y * 0.01;

        double yDirection = yNorthbound;
        double directionMultiplier = 1.0;

        if (background) {
            // Draw the incoming segment to the first station
            // TODO: Don't always assume 0 indices
            double edgeLength = station.getPlatforms().get(Track.Direction.SOUTHBOUND).getPlatformHub()
                    .getPlatformSegment().getTo().getOutSegments().get(0).getLength();

            backgroundGraphicsContext.strokeLine(x, yDirection, x - edgeLength * scaleDownFactor, yDirection);
        }

        // Continue drawing until the train system has been drawn completely
        Segment segment = station.getPlatforms().get(drawingDirection).getPlatformHub().getPlatformSegment();

        // Take note of the trains in a segment
        TrainQueue trainQueue;

        // Change some variables depending on the direction
        while (true) {
            if (background) {
                // If there is a station to be drawn, draw it
                // We really need to draw the station only once, so just draw it when traversing northbound
                if (station != null && drawingDirection == Track.Direction.NORTHBOUND) {
                    // Get the length of the station
                    double stationLength = station.getPlatforms().get(drawingDirection).getPlatformHub()
                            .getPlatformSegment().getLength();

                    // Draw the station proper
                    backgroundGraphicsContext.fillRect(x, y - STATION_HEIGHT * 0.5, stationLength
                            * scaleDownFactor, STATION_HEIGHT);

                    // Draw the station label
                    backgroundGraphicsContext.fillText(station.getName(), x, y + STATION_HEIGHT * 1.5,
                            STATION_LABEL_MAX_WIDTH);
                } else {
                    // If there isn't, just draw this segment
                    backgroundGraphicsContext.strokeLine(x, yDirection, x + directionMultiplier
                            * segment.getLength() * scaleDownFactor, yDirection);
                }
            } else {
                synchronized (segment.getTrainQueue()) {
                    // Draw the trains on this segment (carriage by carriage), if any
                    trainQueue = segment.getTrainQueue();

                    // TODO: Establish train constants (e.g., train color)
                    foregroundGraphicsContext.setFill(trainColor);

                    for (int carriageInSegmentIndex = 0; carriageInSegmentIndex < trainQueue.getTrainQueueSize();
                         carriageInSegmentIndex++) {
                        foregroundGraphicsContext.fillRect(x + directionMultiplier * (trainQueue.getTrainCarriage(
                                carriageInSegmentIndex).getTrainCarriageLocation().getSegmentClearance())
                                        * scaleDownFactor, yDirection - (trainGraphicsDiameter) * 0.5,
                                trainQueue.getTrainCarriage(carriageInSegmentIndex).getLength() * scaleDownFactor,
                                trainGraphicsDiameter);
                    }
                }

                foregroundGraphicsContext.setFill(color);
            }

            // Increment or decrement the x value, depending on the drawing direction
            x += directionMultiplier * segment.getLength() * scaleDownFactor;

            // Go to the next segment
            Junction junction = segment.getTo();

            // Draw the signal of this segment
            if (GraphicsController.SHOW_SIGNALS.get()) {
                foregroundGraphicsContext.setFill(junction.getSignal().availablePermits() == 0 ? Color.RED : Color.GREEN);

                foregroundGraphicsContext.fillRect(x, yDirection - (signalGraphicsDiameter) * 0.5,
                        signalGraphicsDiameter, signalGraphicsDiameter);

                foregroundGraphicsContext.setFill(color);
            }

            // Check if the junction is where the lines ends
            // If it is, then change directions
            if (junction.isEnd()) {
                // If the drawing direction was going northbound, it is now time to go south
                if (drawingDirection == Track.Direction.NORTHBOUND) {
                    drawingDirection = Track.Direction.SOUTHBOUND;

                    yDirection = ySouthbound;
                    directionMultiplier = -1.0;
                } else {
                    // If the direction was going southbound, we have now drawn the whole train system
                    break;
                }
            }

            // Go to the next segment
            segment = junction.getOutSegments().get(0);

            // See whether the next segment contains a station
            station = segment.getStation();
        }
    }

    // Convert string colors to paint objects usable by the graphics library
    private static Color toColor(String color) {
        switch (color) {
            case "green":
                return Color.GREEN;
            case "blue":
                return Color.BLUE;
            case "yellow":
                return Color.YELLOW;
            default:
                return null;
        }
    }
}
