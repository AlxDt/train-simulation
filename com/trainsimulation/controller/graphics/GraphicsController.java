package com.trainsimulation.controller.graphics;

import com.trainsimulation.controller.Controller;
import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.infrastructure.track.Junction;
import com.trainsimulation.model.core.environment.infrastructure.track.Segment;
import com.trainsimulation.model.core.environment.infrastructure.track.Track;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.TrainCarriage;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GraphicsController extends Controller {
    // Send a request to draw on the canvas
    public static void requestDraw(GraphicsContext graphicsContext, TrainSystem trainSystem) {
        Platform.runLater(() -> {
            // Tell the JavaFX thread that we'd like to draw on the canvas
            draw(graphicsContext, trainSystem);
        });
    }

    // Draw all that is needed in the canvas
    private static void draw(GraphicsContext graphicsContext, TrainSystem trainSystem) {
        // Get the height and width of the canvas
        final double CANVAS_WIDTH = graphicsContext.getCanvas().getWidth();
        final double CANVAS_HEIGHT = graphicsContext.getCanvas().getHeight();

        // Clear everything
        graphicsContext.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // TODO: Dynamically compute for the dimensions of the visualization
        // Constants for graphics drawing
        final double initialX = CANVAS_WIDTH * 0.02;
        final double initialY = CANVAS_HEIGHT * 0.5;

        final double STATION_HEIGHT = 15.0;

        // A factor used to scale down the real-world train line dimensions
        final double scaleDownFactor = 0.075;

        // The font to be used
        final String FONT_NAME = "Segoe UI";

        // The font size of the text
        final int FONT_SIZE = 10;

        // The maximum width of the station labels
        final int STATION_LABEL_MAX_WIDTH = 80;

        // The width of lines
        final double LINE_WIDTH = 3.0;

        // The size of the trains
        final double trainGraphicsDiameter = 4.0;

        // Prepare the colors and fonts
        Color color = toColor(trainSystem.getTrainSystemInformation().getColor());
        Font font = new Font(FONT_NAME, FONT_SIZE);

        graphicsContext.setFill(color);
        graphicsContext.setStroke(color);

        graphicsContext.setFont(font);

        // Prepare other graphics settings
        graphicsContext.setLineWidth(LINE_WIDTH);

        // Take note of the direction of drawing
        Track.Direction drawingDirection = Track.Direction.NORTHBOUND;

        // Look for the first station
        Station station = trainSystem.getStations().get(0);

        // Draw the first station
        double x = initialX;
        double y = initialY;

        double yNorthbound = y + y * 0.01;
        double ySouthbound = y - y * 0.01;

        double yDirection = yNorthbound;
        double directionMultiplier = 1.0;

        // Draw the incoming segment to the first station
        // TODO: Don't always assume 0 indices
        double edgeLength = station.getPlatforms().get(Track.Direction.SOUTHBOUND).getPlatformHub().getPlatformSegment()
                .getTo().getOutSegments().get(0).getLength();

        graphicsContext.strokeLine(x, yDirection, x - edgeLength * scaleDownFactor, yDirection);

        // Continue drawing until the train system has been drawn completely
        Segment segment = station.getPlatforms().get(drawingDirection).getPlatformHub().getPlatformSegment();

        // Take note of the trains in a segment
        ConcurrentLinkedDeque<TrainCarriage> trainQueue;

        // Change some variables depending on the direction
        while (true) {
            // If there is a station to be drawn, draw it
            // We really need to draw the station only once, so just draw it when traversing northbound
            if (station != null && drawingDirection == Track.Direction.NORTHBOUND) {
                // Get the length of the station
                double stationLength = station.getPlatforms().get(drawingDirection).getPlatformHub().getPlatformSegment(
                ).getLength();

                // Draw the station proper
                graphicsContext.fillRect(x, y - STATION_HEIGHT * 0.5, stationLength * scaleDownFactor,
                        STATION_HEIGHT);

                // Draw the station label
                graphicsContext.fillText(station.getName(), x, y + STATION_HEIGHT * 1.5, STATION_LABEL_MAX_WIDTH);
            } else {
                // If there isn't, just draw this segment
                graphicsContext.strokeLine(x, yDirection, x + directionMultiplier * segment.getLength()
                        * scaleDownFactor, yDirection);
            }

            // Draw the trains on this segment (carriage by carriage), if any
            trainQueue = segment.getTrainQueue();

            // TODO: Establish train constants (e.g., train color)
            graphicsContext.setFill(Color.PURPLE);

            for (int carriageInSegmentIndex = 0; carriageInSegmentIndex < trainQueue.size(); carriageInSegmentIndex++) {
                if (station != null && carriageInSegmentIndex == 0) {
//                    System.out.println("take note " + station.getName());
                }

//                System.out.println("x: " + trainQueue.get(carriageIndex).getParentTrain()
//                        .getTrainMovement().getSegmentClearances().get(carriageIndex));


                TrainCarriage[] trainQueueAsArray = trainQueue.toArray(new TrainCarriage[0]);

                graphicsContext.fillOval(x + directionMultiplier * (trainQueueAsArray[carriageInSegmentIndex]
                        .getTrainCarriageLocation().getSegmentClearance()) * scaleDownFactor, yDirection - (
                        trainGraphicsDiameter) * 0.5, trainGraphicsDiameter, trainGraphicsDiameter);
                if (station != null && carriageInSegmentIndex == 0) {
//                    System.out.println("end note");
                }
            }

            graphicsContext.setFill(color);

            // Increment or decrement the x value, depending on the drawing direction
            x += directionMultiplier * segment.getLength() * scaleDownFactor;

            // Go to the next segment
            Junction junction = segment.getTo();

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

//        // Remember all station names which have been drawn and traversed
//        Set<String> stationNamesDiscovered = new HashSet<>();
//
//        // Keep drawing and traversing stations until all stations have been exhausted
//        while (!addAndCheckStationCompleteDiscovery(stationNamesDiscovered, station.getName())) {
//            do {
//                // Draw the segment
//                graphicsContext.strokeLine(x, y, x + segment.getLength() * SCALE_DOWN_FACTOR, y);
//
//                // Draw trains belonging to this segment
//                Queue<Train> trains = segment.getTrainQueue();
//
//                for (Train train : trains) {
//                    double metersElapsed = train.getMetersElapsed();
//
//                    // TODO: Establish train constants
//                    graphicsContext.setFill(Color.PURPLE);
//                    graphicsContext.fillOval(x + metersElapsed * SCALE_DOWN_FACTOR, y, 5.0, 5.0);
//                    graphicsContext.setFill(Color.BLUE);
//
//                    System.out.println("\t\t\t" + x);
//                }
//
//                x += segment.getLength() * SCALE_DOWN_FACTOR;
//
//                // Go to the next segment
//                segment = segment.getTo().getOutSegments().get(0);
//            } while (segment.getStation() == null);
//
//            station = segment.getStation();
//
//            // If, in this point, the station has been discovered before, end the loop
//            if (stationNamesDiscovered.contains(station.getName())) {
//                break;
//            }
//
//            // Draw the station
//            stationLength = station.getPlatforms().get(Track.Direction.NORTHBOUND).getPlatformHub()
//                    .getPlatformSegment().getLength();
//
//            graphicsContext.fillRect(x, y - STATION_HEIGHT * 0.5, stationLength * SCALE_DOWN_FACTOR,
//                    STATION_HEIGHT);
//
//            // Draw the station labels
//            graphicsContext.fillText(station.getName(), x, y + STATION_HEIGHT * 2.5, STATION_LABEL_MAX_WIDTH);
//
//            x += stationLength * SCALE_DOWN_FACTOR;
//
//            // Go to the next segment
//            segment = segment.getTo().getOutSegments().get(0);
//        }
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

    // Check if the size of the list of stations discovered has changed or not, after adding a station
    // If it did not change, it means all stations have been discovered
    private static boolean addAndCheckStationCompleteDiscovery(Set<String> stationsDiscovered, String stationName) {
        int sizeBeforeAdding = stationsDiscovered.size();
        stationsDiscovered.add(stationName);
        int sizeAfterAdding = stationsDiscovered.size();

        // Return true if all stations have indeed been discovered
        return sizeBeforeAdding == sizeAfterAdding;
    }
}
