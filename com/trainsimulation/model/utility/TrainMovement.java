package com.trainsimulation.model.utility;

import com.trainsimulation.controller.graphics.GraphicsController;
import com.trainsimulation.controller.screen.MainScreenController;
import com.trainsimulation.model.core.environment.infrastructure.track.Junction;
import com.trainsimulation.model.core.environment.infrastructure.track.Segment;
import com.trainsimulation.model.core.environment.trainservice.maintenance.Depot;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.Train;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.TrainCarriage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

// Contains information regarding the train's movement
public class TrainMovement {
    // Prepare the headway distances (the distances to be maintained between each train)
    private static final int DEFAULT_HEADWAY_DISTANCE = 300;
    public static final AtomicInteger HEADWAY_DISTANCE = new AtomicInteger(DEFAULT_HEADWAY_DISTANCE);

    // Special value for when the track is a dead end
    private static final double TRACK_ENDS = -1.0;

    // Manages the synchronization between different train movements
    private static final Semaphore MOVEMENT_LOCK = new Semaphore(1, true);

    // Denotes the stopping time of the train at the end of the line (s)
    private final int endWaitingTime;

    // Denotes the train's maximum velocity (km/h)
    private final double maxVelocity;

    // Denotes the train which owns this
    private final Train train;

    // Denotes the waiting time of the train (s)
    private final int waitingTime;

    // Denotes the deceleration speed of the train (m/s^2)
    private final double deceleration;

    // Denotes the stations which this train should stop at
    private final List<Station> stationStops;

    // Denotes the train's current waited time (s)
    private int waitedTime;

    // Denotes the train's current stopped time at an end (s)
    private int endWaitedTime;

    // Denotes the train's current velocity (km/h)
    private double velocity;

    // Denotes the train's last visited station
    private Station previousStoppedStation;

    // Denotes the train's last passed station
    private Station previousPassedStation;

    // Denotes whether the train has recently waited at the end
    private boolean waitedAtEnd;

    // Denotes whether this train has already made its stop to disembark passengers when it was removed
    private boolean disembarkedWhenRemoved;

    // Denotes whether the train is active or not (if it isn't, it should be going back to the depot)
    private volatile boolean isActive;

    public TrainMovement(final double maxVelocity, final double deceleration, final Train train) {
        // TODO: Make editable
        final int waitingTime = 30;
        final int endWaitingTime = 300;

        this.deceleration = deceleration;

        // TODO: Remove artificial stochasticity
        this.waitingTime = /*waitingTime*/waitingTime + (new Random().nextInt(2) * 2 - 1)
                * new Random().nextInt((int) (0.5 * waitingTime));

        this.endWaitingTime = endWaitingTime;
        this.maxVelocity = maxVelocity;

        this.previousStoppedStation = null;
        this.previousPassedStation = null;

        this.waitedAtEnd = false;
        this.disembarkedWhenRemoved = false;

        this.stationStops = new ArrayList<>();

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

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public Train getTrain() {
        return train;
    }

    public Station getPreviousStoppedStation() {
        return previousStoppedStation;
    }

    public void setPreviousStoppedStation(Station previousStoppedStation) {
        this.previousStoppedStation = previousStoppedStation;
    }

    public boolean isWaitedAtEnd() {
        return waitedAtEnd;
    }

    public void setWaitedAtEnd(boolean waitedAtEnd) {
        this.waitedAtEnd = waitedAtEnd;
    }

    public double getDeceleration() {
        return deceleration;
    }

    public List<Station> getStationStops() {
        return stationStops;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isDisembarkedWhenRemoved() {
        return disembarkedWhenRemoved;
    }

    public void setDisembarkedWhenRemoved(boolean disembarkedWhenRemoved) {
        this.disembarkedWhenRemoved = disembarkedWhenRemoved;
    }

    // Make this train move forward while looking forward to see if there is nothing in its way
    public TrainAction move() throws InterruptedException {
        // Only one train may move at a time to avoid race conditions
        TrainMovement.MOVEMENT_LOCK.acquire();

        // Check whether moving is even possible in the first place
        // Lookahead distance (in m)
        TrainAction actionTaken = decideAction(HEADWAY_DISTANCE.get());

        // Take note of the appropriate action
        if (actionTaken == TrainAction.HEADWAY_STOP) {
            // Stop the train
            this.velocity = 0.0;

            // If the action was to stop because of a train ahead, just draw this train's position and stop
            GraphicsController.requestDraw(MainScreenController.getActiveSimulationContext().getCanvases(),
                    MainScreenController.getActiveSimulationContext().getTrainSystem(),
                    MainScreenController.getActiveSimulationContext().getScaleDownFactor(), false);

            // Signal to all trains waiting to process their movement that they may now proceed to do so
            TrainMovement.MOVEMENT_LOCK.release();

            return TrainAction.HEADWAY_STOP;
        } else if (actionTaken == TrainAction.END_STOP) {
            // Check if the train has already stopped for the end, in which case, it doesn't need to stop anymore
            if (!this.waitedAtEnd) {
                // Stop the train
                this.velocity = 0.0;

                // The train has now begun stopping in the end
                this.waitedAtEnd = true;

                // Request a draw
                GraphicsController.requestDraw(MainScreenController.getActiveSimulationContext().getCanvases(),
                        MainScreenController.getActiveSimulationContext().getTrainSystem(),
                        MainScreenController.getActiveSimulationContext().getScaleDownFactor(), false);

                // Signal to all trains waiting to process their movement that they may now proceed to do so
                TrainMovement.MOVEMENT_LOCK.release();

                return TrainAction.END_STOP;
            }
        } else if (actionTaken == TrainAction.STATION_STOP) {
            Station currentStation = this.getTrain().getHead().getTrainCarriageLocation().getSegmentLocation()
                    .getStation();

            // If the train is active, stop at this station
            // If the train is not active:
            //   - If the train is about to or has just turned around the end, keep going; the passengers have already
            //     been disembarked
            //   - Else:
            //     - If the train has not yet disembarked its passengers, stop at this station
            //     - Else, keep going, the passengers have already been disembarked
            if (this.isActive
                    || (!this.isActive
                    && this.previousPassedStation != currentStation
                    && !this.disembarkedWhenRemoved)) {
                // If the train is to be inactive, this will be the train's final stop before returning to the depot
                if (!this.isActive) {
                    this.disembarkedWhenRemoved = true;
                }

                // If the train is active, stop at this station if it is in its stops
                // If the train is inactive, always stop at this station
                if (this.stationStops.contains(currentStation) || !this.isActive) {
                    // Check if the train has already stopped for this station, in which case, it doesn't need to stop
                    // anymore
                    // However, when a train changes direction, its previous station will be the same as its next
                    // station, so take that into account
                    if (this.waitedAtEnd || this.previousStoppedStation != currentStation) {
                        // Stop the train
                        this.velocity = 0.0;

                        // If it hasn't been noted yet, the train has now passed the first station from the depot
                        if (this.previousPassedStation == null) {
                            // Tell the GUI to enable the add train button again
                            MainScreenController.ARM_ADD_TRAIN_BUTTON.release();
                        }

                        // The train has now passed this station
                        this.previousPassedStation = currentStation;

                        // This train has now also stopped in this station, so this station will now be considered a
                        // previous one
                        this.previousStoppedStation = currentStation;

                        // Since the train is in a station, we may now reset the waited at end variable
                        this.waitedAtEnd = false;

                        // Request a draw
                        GraphicsController.requestDraw(MainScreenController.getActiveSimulationContext().getCanvases(),
                                MainScreenController.getActiveSimulationContext().getTrainSystem(),
                                MainScreenController.getActiveSimulationContext().getScaleDownFactor(),
                                false);

                        // Signal to all trains waiting to process their movement that they may now proceed to do so
                        TrainMovement.MOVEMENT_LOCK.release();

                        return TrainAction.STATION_STOP;
                    }
                }
            } else {
                if (!this.isActive) {
                    this.disembarkedWhenRemoved = true;
                }
            }

            // If it hasn't been noted yet, the train has now passed the first station from the depot
            if (this.previousPassedStation == null) {
                // Tell the GUI to enable the add train button again
                MainScreenController.ARM_ADD_TRAIN_BUTTON.release();
            }

            // The train has now passed this station
            this.previousPassedStation = currentStation;
        } else if (actionTaken == TrainAction.SIGNAL_STOP) {
            // Stop the train
            this.velocity = 0.0;

            // If the action was to stop because of a train ahead, just draw this train's position and stop
            GraphicsController.requestDraw(MainScreenController.getActiveSimulationContext().getCanvases(),
                    MainScreenController.getActiveSimulationContext().getTrainSystem(),
                    MainScreenController.getActiveSimulationContext().getScaleDownFactor(), false);

            // Signal to all trains waiting to process their movement that they may now proceed to do so
            TrainMovement.MOVEMENT_LOCK.release();

            return TrainAction.SIGNAL_STOP;
        } else if (actionTaken == TrainAction.DEPOT_STOP) {
            // If this train is inactive, it is time for it to despawn
            if (!this.isActive) {
                // Signal to all trains waiting to process their movement that they may now proceed to do so
                TrainMovement.MOVEMENT_LOCK.release();

                // Deactivate this train thread
                return TrainAction.DEPOT_STOP;
            }
        }

        // If it reaches this point, move the train
        // Compute the location of each train carriage after moving forward by the specified velocity
        // Move each carriage one by one
        synchronized (this.train.getTrainCarriages()) {
            List<TrainCarriage> trainCarriages = this.train.getTrainCarriages();

            // Compute for the updated velocity
            this.updateVelocity();

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
                double clearedDistance = toMetersPerSecond(this.velocity);

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
                        // If the train has been deactivated, switch to the depot track when it's available
                        if (this.isActive) {
                            currentSegment = currentSegment.getTo().getOutSegments().get(0);
                        } else {
                            // TODO: Find something better to indicate the depot entry segment
                            if (currentSegment.getTo().getOutSegments().size() > 1) {
                                currentSegment = currentSegment.getTo().getOutSegments().get(1);
                            } else {
                                currentSegment = currentSegment.getTo().getOutSegments().get(0);
                            }
                        }
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
                MainScreenController.getActiveSimulationContext().getTrainSystem(),
                MainScreenController.getActiveSimulationContext().getScaleDownFactor(), false);

        // Signal to all trains waiting to process their movement that they may now proceed to do so
        TrainMovement.MOVEMENT_LOCK.release();

        return TrainAction.PROCEED;
    }

    // Convert a speed in km/h to m/s
    private double toMetersPerSecond(double velocity) {
        return velocity / 3600.0 * 1000.0;
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
    private TrainAction decideAction(final int lookaheadLimit) {
        // Get the front carriage of this train
        TrainCarriage frontCarriage = this.getTrain().getHead();

        // Take note of the next carriage
        TrainCarriage nextCarriage;

        // Get the current segment of this train
        Segment currentSegment = frontCarriage.getTrainCarriageLocation().getSegmentLocation();

        // Denotes the number of seconds the train looks ahead for stations, signals, or ends
        // This number is based on the acceleration increment
        // TODO: Review
//        final int secondsAhead = (int) (1.0 / accelerationIncrement);

        // Get the current train clearance
        double currentClearance = frontCarriage.getTrainCarriageLocation().getSegmentClearance();

        // Get the clearing distance (m/s) given the velocity
        double clearedDistance = toMetersPerSecond(this.maxVelocity);

        // Get the train clearance when the train will have moved one second later
        double nextClearance = currentClearance + clearedDistance;

//        // Get the train clearance when the train will have totally stopped starting from the current speed
//        double futureClearance = currentClearance + this.computeStoppingDistance();

        // Get the junction after this segment
        Junction nextJunction = currentSegment.getTo();

        // Check whether this train is currently in a depot
        // If it is, and the train has been deactivated, the train has to be despawned
        Depot currentDepot = currentSegment.getDepot();

        // Check whether this train is currently in a station
        // If it is, check whether this station is included in this train's stops
        // If it is, proceed until the train would have missed the station if it went any further
        Station currentStation = currentSegment.getStation();

        if (currentDepot != null) {
            return TrainAction.DEPOT_STOP;
        } else if (currentStation != null) {
            // If the would-be clearance would miss the station, it is time to stop
            if (nextClearance > currentSegment.getLength()) {
                return TrainAction.STATION_STOP;
//                } else if (futureClearance > currentSegment.getLength()) {
//                    // If the would-be clearance would miss the station a few seconds from now, it is time to slow down
//                    return TrainAction.SLOW_DOWN;
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
//            } else if (futureClearance > currentSegment.getLength()) {
//                // If the would-be clearance would miss the station a few seconds from now, it is time to slow down
//                return TrainAction.SLOW_DOWN;
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
            if (separation != TrainMovement.TRACK_ENDS && separation <= lookaheadLimit) {
                // Denotes the percentage of the lookahead distance within which the train has to slow down
                final double separationSlowDownPercentage = 0.75;

//                // If the separation is at least the specified percentage from the train in front, slow down
//                if (separation >= separationSlowDownPercentage * lookaheadLimit) {
//                    return TrainAction.SLOW_DOWN;
//                } else {
//                    // If it is dangerously close to the next train, stop immediately
//                    return TrainAction.HEADWAY_STOP;
//                }
                return TrainAction.HEADWAY_STOP;
            }

            // Finally, check whether it is safe to proceed (using signals)
            if (nextJunction.getSignal().availablePermits() == 0) {
                // If the signal at the next junction is a stop signal, proceed until the train would have missed the
                // signal
                if (nextClearance > currentSegment.getLength()) {
                    return TrainAction.SIGNAL_STOP;
                }/* else if (futureClearance > currentSegment.getLength()) {
                    // If the would-be clearance would miss the signal a few seconds from now, it is time to slow down
                    return TrainAction.SLOW_DOWN;
                }*/
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
    // If the track in front of the train is a dead end, return a special value
    private double computeDistance(TrainCarriage currentTrainHead, TrainCarriage nextTrainTail, int lookaheadLimit) {
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
                // TODO: Do not assume 0 index; maybe implement something in Junction to just select the main line route
                try {
                    // Look at the next segment, but only if there is
                    // If there isn't, no point in looking further
                    currentTrainSegment = currentTrainSegment.getTo().getOutSegments().get(0);
                } catch (IndexOutOfBoundsException ex) {
                    return TrainMovement.TRACK_ENDS;
                }

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

    // Computes the velocity of the train (km/h)
    public void updateVelocity() {
        this.velocity = this.maxVelocity;
    }

    // Represents the possible actions for the train
    public enum TrainAction {
        PROCEED, // Tell the train to move
        HEADWAY_STOP, // Tell the train to stop because of a train in front of it (for headway maintenance)
        END_STOP, // Tell the train to stop because it is at the end of the line
        STATION_STOP, // Tell the train to stop because the train is in a station,
        SIGNAL_STOP, // Tell the train to stop because a signal says so
        DEPOT_STOP, // Tell the train to stop at the depot - this also means that the train has been signaled to despawn
    }
}
