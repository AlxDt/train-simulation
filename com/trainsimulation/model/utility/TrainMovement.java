package com.trainsimulation.model.utility;

import com.trainsimulation.controller.graphics.GraphicsController;
import com.trainsimulation.controller.screen.MainScreenController;
import com.trainsimulation.model.core.environment.infrastructure.track.Junction;
import com.trainsimulation.model.core.environment.infrastructure.track.Segment;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.Train;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.TrainCarriage;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

// Contains information regarding the train's movement
public class TrainMovement {

    private static final int DEFAULT_HEADWAY_DISTANCE = 300;
    public static final AtomicInteger HEADWAY_DISTANCE = new AtomicInteger(DEFAULT_HEADWAY_DISTANCE);

    // Manages the synchronization between different train movements
    private static final Semaphore MOVEMENT_LOCK = new Semaphore(1, true);

    // Denotes the stopping time of the train at the end of the line (s)
    private final int endWaitingTime;

    // Denotes the train's maximum velocity (km / h)
    private final double maxVelocity;

    // Denotes the train which owns this
    private final Train train;

    // Denotes the linear increment used for the train's acceleration over time
    private final double accelerationIncrement = 0.15;

    // Denotes the waiting time of the train (s)
    private final int waitingTime;

    // Denotes the train's current waited time (s)
    private int waitedTime;

    // Denotes the train's current stopped time at an end (s)
    private int endWaitedTime;

    // Denotes the train's current velocity (km / h)
    private double velocity;

    // Denotes the multiplier for the train's velocity; over time this is used to denote the train's acceleration
    private double accelerationFactor;

    public TrainMovement(final double maxVelocity, final int waitingTime, final Train train) {
        // The end waiting time is expressed as a factor of the station waiting time
        final int endWaitingTimeFactor = 6;

        this.waitingTime = /*waitingTime*/waitingTime + (new Random().nextInt(2) * 2 - 1)
                * new Random().nextInt((int) (0.5 * waitingTime));

        this.endWaitingTime = waitingTime * endWaitingTimeFactor;
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

    public int getWaitedTime() {
        return waitedTime;
    }

    public void setWaitedTime(int waitedTime) {
        this.waitedTime = waitedTime;
    }

    public int getEndWaitingTime() {
        return endWaitingTime;
    }

    public int getEndWaitedTime() {
        return endWaitedTime;
    }

    public void setEndWaitedTime(int endWaitedTime) {
        this.endWaitedTime = endWaitedTime;
    }

    public double getVelocity() {
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
    public TrainAction move(TrainAction previousAction) throws InterruptedException {
        // Only one train may move at a time to avoid race conditions
        TrainMovement.MOVEMENT_LOCK.acquire();

        // Before doing anything, check whether moving is even possible in the first place
        // Lookahead distance (in m)
        TrainAction actionTaken = decideMovementAction(HEADWAY_DISTANCE.get());

        // Denotes whether the train is generally accelerating or decelerating
        boolean increasing = true;

        // Take note of the appropriate action
        if (actionTaken == TrainAction.SLOW_DOWN) {
            // Note that the train should be decelerating
            // That is, the acceleration should be decreasing
            increasing = false;
        } else if (actionTaken == TrainAction.HEADWAY_STOP) {
            // If the action was to stop because of a train ahead, just draw this train's position and stop
            GraphicsController.requestDraw(MainScreenController.getActiveSimulationContext().getCanvases(),
                    MainScreenController.getActiveSimulationContext().getTrainSystem(), false);

            // Signal to all trains waiting to process their movement that they may now proceed to do so
            TrainMovement.MOVEMENT_LOCK.release();

            return TrainAction.HEADWAY_STOP;
        } else if (actionTaken == TrainAction.END_STOP) {
            // Check if the previous action already was to stop for an end
            // If it was, make the train move forward, as it has already waited
            if (previousAction != TrainAction.END_STOP) {
                // Request a draw
                GraphicsController.requestDraw(MainScreenController.getActiveSimulationContext().getCanvases(),
                        MainScreenController.getActiveSimulationContext().getTrainSystem(), false);

                // Signal to all trains waiting to process their movement that they may now proceed to do so
                TrainMovement.MOVEMENT_LOCK.release();

                return TrainAction.END_STOP;
            }
        } else if (actionTaken == TrainAction.STATION_STOP) {
            // Check if the previous action already was to stop for a station
            // If it was, make the train move forward, as it has already waited
            if (previousAction != TrainAction.STATION_STOP) {
                // Request a draw
                GraphicsController.requestDraw(MainScreenController.getActiveSimulationContext().getCanvases(),
                        MainScreenController.getActiveSimulationContext().getTrainSystem(), false);

                // Signal to all trains waiting to process their movement that they may now proceed to do so
                TrainMovement.MOVEMENT_LOCK.release();

                return TrainAction.STATION_STOP;
            }
        } else if (actionTaken == TrainAction.SIGNAL_STOP) {
            // If the action was to stop because of a train ahead, just draw this train's position and stop
            GraphicsController.requestDraw(MainScreenController.getActiveSimulationContext().getCanvases(),
                    MainScreenController.getActiveSimulationContext().getTrainSystem(), false);

            // Signal to all trains waiting to process their movement that they may now proceed to do so
            TrainMovement.MOVEMENT_LOCK.release();

            return TrainAction.SIGNAL_STOP;
        }

        // If it reaches this point, move the train
        // Compute the location of each train carriage after moving forward by the specified velocity
        // Move each carriage one by one
        synchronized (this.train.getTrainCarriages()) {
            List<TrainCarriage> trainCarriages = this.train.getTrainCarriages();

            // Compute for the updated velocity
            this.updateVelocity(increasing);

            for (TrainCarriage trainCarriage : trainCarriages) {
                // Get the current train carriage
                // Then get its location information
                TrainCarriageLocation trainCarriageLocation = trainCarriage.getTrainCarriageLocation();

                // Get the current clearance of this carriage
                double segmentClearance = trainCarriageLocation.getSegmentClearance();

                // Get the current location of this carriage
                Segment currentSegment = trainCarriageLocation.getSegmentLocation();

                // Get the clearing distance (m/s) given the velocity
//                double clearedDistance = this.maxVelocity / 3600.0 * 1000.0;
                double clearedDistance = this.velocity / 3600.0 * 1000.0;

                // Move this carriage; change its clearance
                segmentClearance += clearedDistance;

                // If the clearance is at least 100% of the current segment, move this carriage to the next segment
                if (segmentClearance / currentSegment.getLength() >= 1.0) {
                    Segment previousSegment = currentSegment;

                    // Move the new location of this carriage until there is no more excess clearance
                    do {
                        // Set the clearance of this carriage relative to the next segment
                        segmentClearance -= currentSegment.getLength();

                        // TODO: Do not assume 0 index
                        currentSegment = currentSegment.getTo().getOutSegments().get(0);
                    } while (segmentClearance / currentSegment.getLength() >= 1.0);

                    // If this carriage is the head of the train, try to enter the next segment
                    // This will only happen if the next segment is clear
                    Semaphore nextSegmentSignal = currentSegment.getFrom().getSignal();

                    if (trainCarriage == trainCarriage.getParentTrain().getHead()) {
                        // Before trying to move, surrender the movement lock first to allow others to move in case this
                        // carriage has to wait
                        TrainMovement.MOVEMENT_LOCK.release();

                        // Try moving to the next segment
                        nextSegmentSignal.acquire();

                        // Once this carriage can now move, reacquire the movement lock again
                        TrainMovement.MOVEMENT_LOCK.acquire();
                    }

                    // Remove this carriage from its former segment
                    // By this point, this carriage should be at the front of this segment
                    previousSegment.getTrainQueue().removeFirstTrainCarriage();

                    // Add this carriage to the next segment
                    currentSegment.getTrainQueue().insertTrainCarriage(trainCarriage);

                    // If this carriage is the tail of the train, set the signal at the start of the previous segment to
                    // allow other trains to enter it
                    Semaphore previousSegmentSignal = previousSegment.getFrom().getSignal();

                    if (trainCarriage == trainCarriage.getParentTrain().getTail()) {
                        previousSegmentSignal.release();
                    }
                }

                // Set the clearances and locations of this carriage
                trainCarriageLocation.setSegmentClearance(segmentClearance);
                trainCarriageLocation.setSegmentLocation(currentSegment);
            }
        }

        // Request a draw
        GraphicsController.requestDraw(MainScreenController.getActiveSimulationContext().getCanvases(),
                MainScreenController.getActiveSimulationContext().getTrainSystem(), false);

        // Signal to all trains waiting to process their movement that they may now proceed to do so
        TrainMovement.MOVEMENT_LOCK.release();

        return TrainAction.PROCEED;
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

    // Makes this train wait for the necessary end waiting time
    // Return true if the train still needs to wait
    // Else, return false
    public boolean waitAtEnd() {
        this.endWaitedTime += 1;

        if (this.endWaitedTime == this.endWaitingTime) {
            this.endWaitedTime = 0;

            return false;
        } else {
            return true;
        }
    }

    // Decide what the train should to at the current time
    private TrainAction decideMovementAction(final int lookaheadLimit) {
        // Get the front carriage of this train
        TrainCarriage frontCarriage = this.getTrain().getHead();

        // Take note of the next carriage
        TrainCarriage nextCarriage;

        // Get the current segment of this train
        Segment currentSegment = frontCarriage.getTrainCarriageLocation().getSegmentLocation();

        // Denotes the number of seconds the train looks ahead for stations, signals, or ends
        // This number is based on the acceleration increment
        // TODO: Review
        final int secondsAhead = (int) (1.0 / accelerationIncrement);

        // Get the current train clearance
        double currentClearance = frontCarriage.getTrainCarriageLocation().getSegmentClearance();

        // Get the clearing distance (m/s) given the velocity
        double clearedDistance = this.maxVelocity / 3600.0 * 1000.0;

        // Get the train clearance when the train will have moved one second later
        double nextClearance = currentClearance + clearedDistance;

        // Get the train clearance when the train will have moved a few seconds later
        double futureClearance = currentClearance + clearedDistance * secondsAhead;

        // Get the junction after this segment
        Junction nextJunction = currentSegment.getTo();

        // Check whether this train is currently in a station
        // If it is, proceed until the train would have missed the station if it went any further
        if (currentSegment.getStation() != null) {
            // If the would-be clearance would miss the station, it is time to stop
            if (nextClearance > currentSegment.getLength()) {
                return TrainAction.STATION_STOP;
            } else if (futureClearance > currentSegment.getLength()) {
                // If the would-be clearance would miss the station a few seconds from now, it is time to slow down
                return TrainAction.SLOW_DOWN;
            } else {
                // Otherwise, proceed
                return TrainAction.PROCEED;
            }
        } else if (nextJunction.isEnd()) {
            // If the junction at the end of the current segment is the end of the line, check how far this train
            // has gone for this segment
            // If the would-be clearance would miss the junction, it is time to stop
            if (nextClearance > currentSegment.getLength()) {
                return TrainAction.END_STOP;
            } else if (futureClearance > currentSegment.getLength()) {
                // If the would-be clearance would miss the station a few seconds from now, it is time to slow down
                return TrainAction.SLOW_DOWN;
            } else {
                // Otherwise, proceed
                return TrainAction.PROCEED;
            }
        } else {
            // If the train does not need to stop because of a station or an end, measure the distance of this train and
            // the next train, if any
            // Get the train queue of the current segment
            TrainQueue trainQueue = currentSegment.getTrainQueue();

            double separation;
            int nextCarriageIndex;

            // Check if this carriage is at the head of the train queue of this segment (i.e., this carriage is the
            // carriage furthest along this segment; the first to leave this segment)
            // If it isn't, then look for the distance between this carriage and the next carriage in this segment
            if (!(trainQueue.getFirstTrainCarriage() == frontCarriage)) {
                // Look for the next train carriage in this segment (there should be one)
                // The next carriage following this carriage is the one directly in front of this carriage in the train
                // queue
                nextCarriageIndex = trainQueue.getIndexOfTrainCarriage(frontCarriage) - 1;
                nextCarriage = trainQueue.getTrainCarriage(nextCarriageIndex);

                // Compute the distance between this carriage and that
                separation = computeDistance(frontCarriage, nextCarriage, lookaheadLimit);
            } else {
                // If this carriage is the first in this queue, look for the distance between this carriage and the next
                // carriage in the segment where the lookahead distance falls on
                separation = computeDistance(frontCarriage, null, lookaheadLimit);
            }

            // If the separation between the next train and the current train is less than the lookahead distance, halt
            // Return a signal to indicate a need to slow down
            if (separation <= lookaheadLimit) {
                // Denotes the percentage of the lookahead distance within which the train has to slow down
                final double separationSlowDownPercentage = 0.75;

                // If the separation is at least the specified percentage from the train in front, slow down
                if (separation >= separationSlowDownPercentage * lookaheadLimit) {
                    return TrainAction.SLOW_DOWN;
                } else {
                    // If it is dangerously close to the next train, stop immediately
                    return TrainAction.HEADWAY_STOP;
                }
            }

            // Finally, check whether it is safe to proceed (using signals)
            if (nextJunction.getSignal().availablePermits() == 0) {
                // If the signal at the next junction is a stop signal, proceed until the train would have missed the
                // signal
                if (nextClearance > currentSegment.getLength()) {
                    return TrainAction.SIGNAL_STOP;
                } else if (futureClearance > currentSegment.getLength()) {
                    // If the would-be clearance would miss the signal a few seconds from now, it is time to slow down
                    return TrainAction.SLOW_DOWN;
                }
            }

            // Otherwise, the train is free to move
            return TrainAction.PROCEED;
        }
    }

    // Compute the distance between two trains (specifically, between the head of this train and the tail of the next
    // train) within the specified lookahead limit
    // If the second train isn't given, the method looks for the distance between the first train and the next train
    // to be found
    // If the distance between the two trains turns out to be more than the lookahead limit, just return a best effort
    // computation to avoid wasting time
    private double computeDistance(TrainCarriage currentTrainHead, TrainCarriage nextTrainTail,
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
                if (!currentTrainSegment.getTrainQueue().isTrainQueueEmpty()) {
                    // Take note of the next train
                    nextTrainTail = currentTrainSegment.getTrainQueue().getLastTrainCarriage();

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
                double startOfSegmentToNextTrainDistance = nextTrainTail.getTrainCarriageLocation()
                        .getSegmentClearance();

                distance = currentTrainToEndOfSegmentDistance + emptySegmentDistances
                        + startOfSegmentToNextTrainDistance;
            } else {
                // If not, just return the distance exceeding the lookahead limit - it doesn't matter anyway
                distance = currentTrainToEndOfSegmentDistance + emptySegmentDistances;
            }
        }

        // The distance between the next and the current train should be positive
        assert distance >= 0.0 : "Distance between trains are negative";

        return distance;
    }

    // Set this train to a segment
    public void setTrainAtSegment(final Segment segment, final double carriageGap) throws InterruptedException {
        // Set in such a way that the train is just about to clear this segment
        final double headClearance = segment.getLength();
        double carriageOffset = 0.0;

        // Wait until this segment is free of trains previous here
        segment.getFrom().getSignal().acquire();

        // Compute for the location of each train carriage
        for (TrainCarriage trainCarriage : this.train.getTrainCarriages()) {
            // Add this carriage to this segment
            segment.getTrainQueue().insertTrainCarriage(trainCarriage);

            // Set the clearance of this segment
            trainCarriage.getTrainCarriageLocation().setSegmentClearance(headClearance - carriageOffset);

            // Set the location of this carriage to this segment
            trainCarriage.getTrainCarriageLocation().setSegmentLocation(segment);

            // Increment the offset
            carriageOffset += trainCarriage.getLength() + carriageGap;
        }
    }

    // Computes the velocity of the train (km / h)
    public double updateVelocity(boolean increasing) {
        this.velocity = updateAccelerationFactor(increasing) * this.maxVelocity;

        return this.velocity;
    }

    // Gets the next acceleration factor for the train
    private double updateAccelerationFactor(boolean increasing) {
        final double maximumAccelerationFactor = 1.0;
        final double minimumAccelerationFactor = accelerationIncrement;

        if (increasing) {
            this.accelerationFactor += accelerationIncrement;

            // Set an acceleration cap to avoid over-accelerating
            if (this.accelerationFactor >= maximumAccelerationFactor) {
                this.accelerationFactor = maximumAccelerationFactor;
            }
        } else {
            this.accelerationFactor -= accelerationIncrement;

            // Set a deceleration cap to avoid under-accelerating (going backwards or stopping)
            if (this.accelerationFactor <= minimumAccelerationFactor) {
                this.accelerationFactor = minimumAccelerationFactor;
            }
        }

        return this.accelerationFactor;
    }

    // Represents the possible actions for the train
    public enum TrainAction {
        PROCEED, // Tell the train to move
        SLOW_DOWN, // Tell the train to slow down
        HEADWAY_STOP, // Tell the train to stop because of a train in front of it (for headway maintenance)
        END_STOP, // Tell the train to stop because it is at the end of the line
        STATION_STOP, // Tell the train to stop because the train is in a station,
        SIGNAL_STOP // Tell the train to stop because a signal says so
    }
}
