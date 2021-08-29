package com.trainsimulation.model.simulator;

import com.crowdsimulation.controller.controls.feature.main.MainScreenController;
import com.trainsimulation.controller.Main;
import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
import com.trainsimulation.model.db.DatabaseInterface;
import com.trainsimulation.model.simulator.setup.EnvironmentSetup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
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

                        // Pause this simulation thread for a brief amount of time so it could be followed at a pace
                        // conducive to visualization

                        // Increment (tick) the clock
                        this.time.tick();

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
}
