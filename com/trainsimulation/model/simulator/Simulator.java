package com.trainsimulation.model.simulator;

import com.crowdsimulation.model.core.agent.passenger.movement.RoutePlan;
import com.trainsimulation.controller.Main;
import com.trainsimulation.controller.graphics.GraphicsController;
import com.trainsimulation.controller.screen.MainScreenController;
import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
import com.trainsimulation.model.db.DatabaseInterface;
import com.trainsimulation.model.simulator.setup.EnvironmentSetup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

// The simulator has total control over the aspects of the train simulation
public class Simulator {
    // Denotes whether the simulation has started yet or not
    private final AtomicBoolean running = new AtomicBoolean(false);

    // The lock which manages each simulation tick
    public static final Object tickLock = new Object();

    // Denotes whether the simulation is done or not
    private final AtomicBoolean done = new AtomicBoolean(false);

    // Contains the database interfacing methods of the simulator
    private final DatabaseInterface databaseInterface;

    // Contains all the train systems in the simulation
    private final List<TrainSystem> trainSystems;

    // Stores the current time in the simulation
    private SimulationTime time;

    // Stores the time when the simulation will end
    private SimulationTime endTime;

    // Used to manage when the simulation is paused/played
    private final Semaphore playSemaphore;

    // Use the number of CPUs as the basis for the number of thread pools
    public static final int NUM_CPUS = Runtime.getRuntime().availableProcessors();

    public Simulator() throws Throwable {
        this.databaseInterface = new DatabaseInterface();
        this.trainSystems = new ArrayList<>();

        this.playSemaphore = new Semaphore(0);

        // Start the simulation thread, but in reality it would be activated much later
        this.start();
    }

    public AtomicBoolean getRunning() {
        return this.running;
    }

    public AtomicBoolean getDone() {
        return this.done;
    }

    public DatabaseInterface getDatabaseInterface() {
        return databaseInterface;
    }

    public SimulationTime getTime() {
        return time;
    }

    public void setTime(SimulationTime time) {
        this.time = time;
    }

    public List<TrainSystem> getTrainSystems() {
        return trainSystems;
    }

    public SimulationTime getEndTime() {
        return endTime;
    }

    public void setEndTime(SimulationTime endTime) {
        this.endTime = endTime;
    }

    public Semaphore getPlaySemaphore() {
        return playSemaphore;
    }

    // Set the simulation up with all its environments, agents, and attributes
    public void setup(SimulationTime startTime, SimulationTime endTime) {
        // Prepare the time at the start of the simulation
        this.time = startTime;
        this.endTime = endTime;

        // Prepare the train systems
        List<TrainSystem> trainSystems = EnvironmentSetup.setup(databaseInterface);

        // Then have the simulation take note of them
        // But not before clearing the current array of train systems, so the setup could be run indefinitely
        this.trainSystems.clear();
        this.trainSystems.addAll(trainSystems);
    }

    // Start the simulation and keep it running until the given ending time
    public void start() {
        new Thread(() -> {
            // Initialize a thread pool to run stations in parallel
            final ExecutorService stationExecutorService = Executors.newFixedThreadPool(Simulator.NUM_CPUS);

            // TODO: Implement simultaneous train systems

            // TODO: List all floors to update in parallel

            while (true) {
                try {
                    // Wait until the play button has been pressed
                    playSemaphore.acquire();

                    // Update the pertinent variables when ticking
                    boolean isTimeBeforeOrDuring = this.time.isTimeBeforeOrDuring(this.endTime);

                    // Keep looping until paused
                    while (this.running.get() && isTimeBeforeOrDuring) {
                        // Redraw the updated time
                        Main.mainScreenController.requestUpdateSimulationTime(this.time);

                        // Manage all passenger-related updates
                        updateTrainSystems(stationExecutorService);

                        // Update the view of the current station only
                        GraphicsController.requestDrawStationView(
                                MainScreenController.getActiveSimulationContext().getStationViewCanvases(),
                                MainScreenController.getActiveSimulationContext().getCurrentStation(),
                                MainScreenController.getActiveSimulationContext().getStationScaleDownFactor(),
                                false
                        );

                        // Increment (tick) the clock
                        this.time.tick();

                        // Pause this simulation thread for a brief amount of time so it could be followed at a pace
                        // conducive to visualization
                        isTimeBeforeOrDuring = this.time.isTimeBeforeOrDuring(this.endTime);
                        Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS.get());

                        synchronized (Simulator.tickLock) {
                            Simulator.tickLock.notifyAll();
                        }
                    }

                    if (!isTimeBeforeOrDuring) {
                        // Once the simulation time stops, stop all the threads
                        Main.simulator.getDone().set(true);

                        // Then tell the UI thread to disable all buttons
                        Main.mainScreenController.requestDisableButtons();

                        break;
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }).start();

        //////////////

//        // Signal to everyone that the simulator has been started
//        this.started.set(true);
//
//        // Run this on a thread so it won't choke the JavaFX UI thread
//        new Thread(() -> {
//            // From the starting time until the ending time, increment the time of the simulation
//            while (this.time.isTimeBeforeOrDuring(this.endTime)) {
//                // Redraw the updated time
//                Main.mainScreenController.requestUpdateSimulationTime(this.time);
//
//                // Pause this simulation thread for a brief amount of time so it could be followed at a pace conducive
//                // to visualization
//                try {
//                    Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS.get());
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                // Increment (tick) the clock
//                this.time.tick();
//            }
//
//            // Once the simulation time stops, stop all the threads
//            Main.simulator.getDone().set(true);
//
//            // Then tell the UI thread to disable all buttons
//            Main.mainScreenController.requestDisableButtons();
//        }).start();
    }

    // Update all train systems
    private void updateTrainSystems(ExecutorService stationExecutorService) throws InterruptedException {
        List<StationUpdateTask> stationUpdateTasks = new ArrayList<>();

        // Update each train system
        for (TrainSystem trainSystem : this.trainSystems) {
            // TODO: Consider all train systems
            if (trainSystem.getTrainSystemInformation().getName().equals("LRT-2")) {
                // Remove trips that happen before the simulation start time
                trainSystem.removeTripsBeforeStartTime(this.time);

                // Collect all stations in the train system
                List<Station> stationsInTrainSystem = trainSystem.getStations();

                // Collect all passengers to be spawned in this tick
                HashMap<Station, List<RoutePlan.PassengerTripInformation>> passengersToSpawn
                        = trainSystem.getPassengersToSpawn(this.time);

                // Update each station in parallel
                for (Station station : stationsInTrainSystem) {
                    // Collect all passengers to be spawned in this station
                    List<RoutePlan.PassengerTripInformation> passengersToSpawnInStation
                            = passengersToSpawn.get(station);

                    // If, in this station, there are passengers to be spawned, spawn them
                    stationUpdateTasks.add(new StationUpdateTask(station, passengersToSpawnInStation));
                }

                // Update each station
                stationExecutorService.invokeAll(stationUpdateTasks);

                // Clear the list of stations to update
                stationUpdateTasks.clear();
            }
        }
    }

    // Contains the necessary operations to update stations in parallel
    public static class StationUpdateTask implements Callable<Void> {
        private final Station station;
        private final List<RoutePlan.PassengerTripInformation> passengersToSpawn;

        public StationUpdateTask(Station station, List<RoutePlan.PassengerTripInformation> passengersToSpawn) {
            this.station = station;
            this.passengersToSpawn = passengersToSpawn;
        }

        @Override
        public Void call() throws Exception {
            com.crowdsimulation.model.simulator.Simulator.updatePassengersInStation(
                    this.station.getFloorExecutorService(),
                    station.getStationLayout(),
                    this.passengersToSpawn
            );

            return null;
        }
    }
}
