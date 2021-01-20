package xyz.msws.tracker.module;

import xyz.msws.tracker.Client;

public abstract class Module {
	protected Client client;

	public Module(Client client) {
		this.client = client;
	}

	public abstract void load();

	public abstract void unload();

}
