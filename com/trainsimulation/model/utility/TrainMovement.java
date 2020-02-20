package com.trainsimulation.model.utility;

import com.trainsimulation.controller.graphics.GraphicsController;
import com.trainsimulation.controller.screen.MainScreenController;
import com.trainsimulation.model.core.environment.infrastructure.track.Segment;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.Train;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.TrainCarriage;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

// Contains information regarding the train's movement
public class TrainMovement {
    // Denotes the waiting time of the train (s)
    private final int waitingTime;

    // Denotes the train's maximum velocity (km / h)
    private final double maxVelocity;

    // Denotes the train which owns this
    private final Train train;

    // Denotes the train's current waited time (s)
    private double waitedTime;

    // Denotes the train's current velocity (km / h)
    private int velocity;

    // Denotes the multiplier for the train's velocity; over time this is used to denote the train's acceleration
    private double accelerationFactor;

    public TrainMovement(final int maxVelocity, final int waitingTime, final Train train) {
        this.waitingTime = waitingTime;
        this.maxVelocity = maxVelocity;
        this.accelerationFactor = 0.0;
        this.train = train;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public double getMaxVelocity() {
        return maxVelocity;
    }

    public double getWaitedTime() {
        return waitedTime;
    }

    public void setWaitedTime(double waitedTime) {
        this.waitedTime = waitedTime;
    }

    public int getVelocity() {
        return velocity;
    }

    public void setVelocity(int velocity) {
        this.velocity = velocity;
    }

    public double getAccelerationFactor() {
        return accelerationFactor;
    }

    public void setAccelerationFactor(double accelerationFactor) {
        this.accelerationFactor = accelerationFactor;
    }

    public Train getTrain() {
        return train;
    }

    // Make this train move forward while looking forward to see if there is nothing in its way
    // In the train's movement, take the previous action into account
    public synchronized TrainAction move(TrainAction previousAction) {
        // Before doing anything, check whether moving is even possible in the first place
        // Lookahead distance (in m)
        final int lookaheadDistance = 100;

        TrainAction lookAheadResult = lookahead(lookaheadDistance);

        // If the action is to stop for a station, take into account whether the train was already asked to stop before
        if (lookAheadResult == TrainAction.STOP_FOR_STATION) {
            // Request a draw
            GraphicsController.requestDraw(MainScreenController.getActiveCanvas().getGraphicsContext2D(),
                    MainScreenController.getActiveTrainSystem());

            // Check if the previous action already was to stop for a station
            // If it was, make the train move forward, as it has already waited
            if (previousAction != TrainAction.STOP_FOR_STATION) {
                return TrainAction.STOP_FOR_STATION;
            }
        } else if (lookAheadResult == TrainAction.STOP_FOR_TRAIN) {
            // If the action was to stop because of a train ahead, just draw this train's position and stop
            GraphicsController.requestDraw(MainScreenController.getActiveCanvas().getGraphicsContext2D(),
                    MainScreenController.getActiveTrainSystem());

            return TrainAction.STOP_FOR_TRAIN;
        }

        // If it reaches this point, move the train
        // TODO: Consider acceleration/deceleration
        // Compute the location of each train carriage after moving forward by the specified velocity
        // Move each carriage one by one
        List<TrainCarriage> trainCarriages = this.train.getTrainCarriages();

        for (TrainCarriage trainCarriage : trainCarriages) {
            // Get the current train carriage
            // Then get its location information
            TrainCarriageLocation trainCarriageLocation = trainCarriage.getTrainCarriageLocation();

            // Get the current clearance of this carriage
            double segmentClearance = trainCarriageLocation.getSegmentClearance();

            // Get the current location of this carriage
            Segment currentSegment = trainCarriageLocation.getSegmentLocation();

            // Get the clearing distance (m/s) given the velocity
            double clearedDistance = this.maxVelocity / 3600.0 * 1000.0;

            // Move this carriage; change its clearance
            segmentClearance += clearedDistance;

            // If the clearance is at least 100% of the current segment, move this carriage to the next segment
            if (segmentClearance / currentSegment.getLength() >= 1.0) {
                // Remove this carriage from its current segment
                // By this point, this carriage should be at the front of this segment
                currentSegment.getTrainQueue().remove();

                // Move the new location of this carriage until there is no more excess clearance
                do {
                    // Set the clearance of this carriage relative to the next segment
                    segmentClearance -= currentSegment.getLength();

                    // TODO: Do not assume 0 index
                    currentSegment = currentSegment.getTo().getOutSegments().get(0);
                } while (segmentClearance / currentSegment.getLength() >= 1.0);

                // Add this carriage to the next segment
                currentSegment.getTrainQueue().add(trainCarriage);
            }

            // Set the clearances and locations of this carriage
            trainCarriageLocation.setSegmentClearance(segmentClearance);
            trainCarriageLocation.setSegmentLocation(currentSegment);
        }

        // Request a draw
        GraphicsController.requestDraw(MainScreenController.getActiveCanvas().getGraphicsContext2D(),
                MainScreenController.getActiveTrainSystem());

        return TrainAction.PROCEED;

//        if (lookAheadResult == null) {
//            System.out.println(this.segmentClearances.getFirst() / this.trainCarriageLocations.getFirst()
//                    .getLength());
//
//            if (this.segmentClearances.getFirst() / this.trainCarriageLocations.getFirst()
//                    .getLength() > stationClearPercentage) {
//                // Request a draw
//                GraphicsController.requestDraw(MainScreenController.getActiveCanvas().getGraphicsContext2D(),
//                        MainScreenController.getActiveTrainSystem());
//
//                return null;
//            }
//        } else if (!lookAheadResult) {
//            // Request a draw
//            GraphicsController.requestDraw(MainScreenController.getActiveCanvas().getGraphicsContext2D(),
//                    MainScreenController.getActiveTrainSystem());
//
//            return false;
//        }
//
//        return true;
    }

    // Makes this train wait for the necessary waiting time
    // Return true if the train still needs to wait
    // Else, return false
    public boolean waitAtStation() {
        this.waitedTime += 1;

        if (this.waitedTime == this.waitingTime) {
            this.waitedTime = 0;

            return false;
        } else {
            return true;
        }
    }

    // Look ahead a certain distance away to see if it's safe to continue moving
    private synchronized TrainAction lookahead(final int lookaheadLimit) {
        // Get the front carriage of this train
        TrainCarriage frontCarriage = this.getTrain().getHead();

        // Take note of the next carriage
        TrainCarriage nextCarriage;

        // Get the current segment of this train
        Segment currentSegment = frontCarriage.getTrainCarriageLocation().getSegmentLocation();

        // Get the train queue of the current segment
        ConcurrentLinkedDeque<TrainCarriage> trainQueue = currentSegment.getTrainQueue();

        double separation;
        int nextCarriageIndex;

        // Check if this carriage is at the head of the train queue of this segment (i.e., this carriage is the carriage
        // furthest along this segment; the first to leave this segment)
        // If it isn't, then look for the distance between this carriage and the next carriage in this segment
        if (!(trainQueue.getFirst() == frontCarriage)) {
            // Look for the next train carriage in this segment (there should be one)
            // The next carriage following this carriage is the one directly in front of this carriage in the train
            // queue
            TrainCarriage[] trainQueueAsArray = trainQueue.toArray(new TrainCarriage[0]);

            nextCarriageIndex = Arrays.asList(trainQueueAsArray).indexOf(frontCarriage) - 1;
//            nextCarriageIndex = trainQueue.indexOf(frontCarriage) - 1;

            nextCarriage = trainQueueAsArray[nextCarriageIndex];
//            nextCarriage = trainQueue.get(nextCarriageIndex);

            // Compute the distance between this carriage and that
            separation = computeDistance(frontCarriage, nextCarriage, lookaheadLimit);
        } else {
            // If this carriage is the first in this queue, look for the distance between this carriage and the next
            // carriage in the segment where the lookahead distance falls on
            separation = computeDistance(frontCarriage, null, lookaheadLimit);
//            metersAhead += currentSegment.getLength() - frontCarriage.getTrainCarriageLocation().getSegmentClearance();
//
//            // Look for the segment where the lookahead distance falls on
//            do {
//                // Change the current segment
//                // TODO: Do not assume 0 index
//                currentSegment = currentSegment.getTo().getOutSegments().get(0);
//
//                // Look for the next train carriage in this segment, if any
//                List<TrainCarriage> currentSegmentTrainQueue = currentSegment.getTrainQueue();
//
//                // If there is someone on this segment, compute the distance between this train and that train
//                if (!currentSegmentTrainQueue.isEmpty()) {
//                    nextCarriageIndex = currentSegmentTrainQueue.size() - 1;
//                    nextCarriage = currentSegmentTrainQueue.get(nextCarriageIndex);
//
//                    separation = computeDistance(frontCarriage, nextCarriage);
//
//                    // If the separation between the next train and the current train is less than the lookahead
//                    // distance, halt
//                    // Return false to indicate a need to stop
//                    if (separation <= lookaheadLimit) {
//                        return false;
//                    }
//                }
//
//                // Check if this segment is owned by a station
//                if (currentSegment.getStation() != null) {
//                    return null;
//                }
//
//                // Add the length of the segment to the distance looked forward
//                metersAhead += currentSegment.getLength();
//            } while (metersAhead <= lookaheadLimit);
//
//            // If this point is reached, all is clear
//            return true;
        }

        // If the separation between the next train and the current train is less than the lookahead distance, halt
        // Return false to indicate a need to stop
        if (separation <= lookaheadLimit) {
            return TrainAction.STOP_FOR_TRAIN;
        } else {
            // If there is enough separation, check instead if this segment is owned by a station
            // If it is, proceed until the train would have missed the station if it went any further
            if (currentSegment.getStation() != null) {
                // Get the current train clearance
                double currentClearance = frontCarriage.getTrainCarriageLocation().getSegmentClearance();

                // Get the clearing distance (m/s) given the velocity
                double clearedDistance = this.maxVelocity / 3600.0 * 1000.0;

                // Get the train clearance when the train will have moved one second later
                double nextClearance = currentClearance + clearedDistance;

                // If the would-be clearance would miss the station, it is time to stop
                if (nextClearance > currentSegment.getLength()) {
                    return TrainAction.STOP_FOR_STATION;
                } else {
                    // Otherwise, proceed
                    // TODO: Slow down
                    return TrainAction.PROCEED;
                }
            } else {
                // If it isn't, the train is free to move
                return TrainAction.PROCEED;
            }
        }
    }

    // Compute the distance between two trains (specifically, between the head of this train and the tail of the next
    // train) within the specified lookahead limit
    // If the second train isn't given, the method looks for the distance between the first train and the next train
    // to be found
    // If the distance between the two trains turns out to be more than the lookahead limit, just return a best effort
    // computation to avoid wasting time
    private synchronized double computeDistance(TrainCarriage currentTrainHead, TrainCarriage nextTrainTail,
                                                int lookaheadLimit) {
        double distance;

        // If the two trains are in the same segment, just simply compute the distance between them
        if (nextTrainTail != null) {
            // The two trains must be in the same segment
            assert currentTrainHead.getTrainCarriageLocation().getSegmentLocation() == nextTrainTail
                    .getTrainCarriageLocation().getSegmentLocation() : "The two trains provided are not in the same" +
                    " segment";

            double thisCarriageClearance = currentTrainHead.getTrainCarriageLocation().getSegmentClearance();
            double nextCarriageClearance = nextTrainTail.getTrainCarriageLocation().getSegmentClearance();

            distance = nextCarriageClearance - thisCarriageClearance;
        } else {
            // If the two trains are in different segments, the distance between the two trains is the distance between
            // the preceding train and the end of its segment plus the sum of all segments in between the trains without
            // the second train plus the distance of the second train from the start of the segment

            // Get the distance between the current train and the end of its segment
            Segment currentTrainSegment = currentTrainHead.getTrainCarriageLocation().getSegmentLocation();

            double currentTrainToEndOfSegmentDistance
                    = currentTrainSegment.getLength()
                    - currentTrainHead.getTrainCarriageLocation().getSegmentClearance(
            );

            // Get the total distance of the segments in between this train and the next train not containing either of
            // them
            double emptySegmentDistances = 0.0;

            // Keep track of whether the distance accumulated exceeds the lookahead limit
            boolean exceedsLookahead = false;

            while (true) {
                // Look at the next segment
                // TODO: Do not assume 0 index; maybe implement something in Junction to just select the main line route
                currentTrainSegment = currentTrainSegment.getTo().getOutSegments().get(0);

                // Check if the accumulated distance exceeds the lookahead limit (if it does, no point in this entire
                // calculation, the train may proceed safely)
                if (currentTrainToEndOfSegmentDistance + emptySegmentDistances > lookaheadLimit) {
                    exceedsLookahead = true;

                    break;
                }

                // Check if the next segment contains a train
                // If it does, wrap up with the computation of the accumulated distance of empty segments in between
                if (!currentTrainSegment.getTrainQueue().isEmpty()) {
                    // Take note of the next train
                    nextTrainTail = currentTrainSegment.getTrainQueue().getLast();

                    // Stop looking for more
                    break;
                } else {
                    // If it doesn't, take note of the distance of the empty segment
                    emptySegmentDistances += currentTrainSegment.getLength();
                }
            }

            // If the distance so far was within the lookahead limit, make the complete computation
            if (!exceedsLookahead) {
                // Finally, get the distance between the start of that segment and the next train
                double startOfSegmentToNextTrainDistance = nextTrainTail.getTrainCarriageLocation().getSegmentClearance();

                distance = currentTrainToEndOfSegmentDistance + emptySegmentDistances + startOfSegmentToNextTrainDistance;
            } else {
                // If not, just return the distance exceeding the lookahead limit - it doesn't matter anyway
                distance = currentTrainToEndOfSegmentDistance + emptySegmentDistances;
            }
        }

        // The distance between the next and the current train should be positive
        assert distance >= 0.0 : "Distance between trains are negative";

        return distance;
    }

    public enum TrainAction {
        PROCEED,
        STOP_FOR_TRAIN,
        STOP_FOR_STATION
    }
}
