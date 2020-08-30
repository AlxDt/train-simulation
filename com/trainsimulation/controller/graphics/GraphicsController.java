package com.trainsimulation.controller.graphics;

import com.trainsimulation.controller.Controller;
import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.infrastructure.track.Junction;
import com.trainsimulation.model.core.environment.infrastructure.track.Segment;
import com.trainsimulation.model.core.environment.infrastructure.track.Track;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Platform;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.Train;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.TrainCarriage;
import com.trainsimulation.model.utility.TrainQueue;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class GraphicsController extends Controller {
    // Denotes whether signals should be shown
    public static final AtomicBoolean SHOW_SIGNALS = new AtomicBoolean(false);

    // Denotes the train to mark, if any
    public static Train markedTrain = null;

    // Send a request to draw on the canvas
    public static void requestDraw(StackPane canvases, TrainSystem trainSystem, double scaleDownFactor,
                                   boolean background) {
        javafx.application.Platform.runLater(() -> {
            // Tell the JavaFX thread that we'd like to draw on the canvas
            draw(canvases, trainSystem, scaleDownFactor, background);
        });
    }

    // Draw all that is needed in the canvas
    private static void draw(StackPane canvases, TrainSystem trainSystem, double scaleDownFactor, boolean background) {
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

        // The font to be used
        final String FONT_NAME = "Segoe UI";

        // The font size of the text
        final double FONT_SIZE = 7.5;

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

        // The radius of the marking circle
        final double MARKING_RADIUS = 50;

        // Prepare the colors and fonts
        Color color = toColor(trainSystem.getTrainSystemInformation().getColor());
        Font font = new Font(FONT_NAME, FONT_SIZE);

        Color trainColor = Color.rgb(170, 170, 170);

        backgroundGraphicsContext.setFill(color);
        backgroundGraphicsContext.setStroke(color);

        backgroundGraphicsContext.setFont(font);

        foregroundGraphicsContext.setStroke(Color.BLACK);

        // Prepare other graphics settings
        backgroundGraphicsContext.setLineWidth(LINE_WIDTH);

        // Take note of the direction of drawing
        Track.Direction drawingDirection = Track.Direction.NORTHBOUND;

        double x = initialX;
        double y = initialY;

        double yNorthbound = y + y * 0.01;
        double ySouthbound = y - y * 0.01;

        double yDirection = yNorthbound;
        double directionMultiplier = 1.0;

        // Continue drawing until the train system has been drawn completely
        // Start from the segment from the first station
        // Look for the first station
        final Station firstStation = trainSystem.getStations().get(0);
        final Track.Direction oppositeDirection = Track.opposite(drawingDirection);
        final Map<Track.Direction, Platform> platforms = firstStation.getPlatforms();
        final Junction stationOutJunction = platforms.get(oppositeDirection).getPlatformHub().getPlatformSegment()
                .getTo();
        final Junction end = stationOutJunction.getOutSegment(oppositeDirection).getTo();

        Segment segment = end.getOutSegment(drawingDirection);

        // Then take note of the station at the current segment
        Station station = segment.getStation();

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
                    backgroundGraphicsContext.setFill(Color.BLACK);
                    backgroundGraphicsContext.fillText(station.getName(), x, y + STATION_HEIGHT * 1.5,
                            STATION_LABEL_MAX_WIDTH);
                    backgroundGraphicsContext.setFill(color);
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

                    TrainCarriage trainCarriage;

                    for (int carriageInSegmentIndex = 0; carriageInSegmentIndex < trainQueue.getTrainQueueSize();
                         carriageInSegmentIndex++) {
                        trainCarriage = trainQueue.getTrainCarriage(carriageInSegmentIndex);

                        foregroundGraphicsContext.fillRect(
                                x + directionMultiplier
                                        * (trainCarriage.getTrainCarriageLocation().getSegmentClearance() -
                                        (drawingDirection == Track.Direction.NORTHBOUND ?
                                                trainCarriage.getLength() : 0))
                                        * scaleDownFactor,
                                yDirection - (trainGraphicsDiameter) * 0.5,
                                trainCarriage.getLength() * scaleDownFactor,
                                trainGraphicsDiameter
                        );

                        // If the train composed of this train carriage is supposed to be marked, draw a large circle to
                        // mark it
                        if (trainCarriage.getParentTrain() == GraphicsController.markedTrain) {
                            // Make sure only the head carriage is marked (no need for multiple marks on one train)
                            if (trainCarriage.getParentTrain().getHead() == trainCarriage) {
                                foregroundGraphicsContext.strokeOval((x + directionMultiplier * trainCarriage
                                                .getTrainCarriageLocation().getSegmentClearance() * scaleDownFactor)
                                                - MARKING_RADIUS * 0.5,
                                        (yDirection - (trainGraphicsDiameter) * 0.5) - MARKING_RADIUS * 0.5,
                                        MARKING_RADIUS,
                                        MARKING_RADIUS);
                            }
                        }
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

            // Check if the junction is where the lines end
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
            segment = junction.getOutSegment(drawingDirection);

            // See whether the next segment contains a station
            station = segment.getStation();
        }
    }

    // Convert string colors to paint objects usable by the graphics library
    private static Color toColor(String color) {
        switch (color) {
            case "green":
                return Color.rgb(1, 132, 75);
            case "blue":
                return Color.rgb(50, 74, 156);
            case "yellow":
                return Color.rgb(255, 204, 0);
            default:
                return null;
        }
    }
}
