package xyz.msws.tracker.data.graph;

import xyz.msws.tracker.module.PlayerTrackerModule;

import java.io.File;

public abstract class Graph {

    protected PlayerTrackerModule tracker;

    public Graph(PlayerTrackerModule tracker) {
        this.tracker = tracker;
    }

    public abstract File generate();

}
