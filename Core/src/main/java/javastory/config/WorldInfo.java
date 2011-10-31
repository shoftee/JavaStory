package javastory.config;

import javastory.server.EndpointInfo;

public class WorldInfo extends EndpointInfo {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6847919196506532889L;

	private final int worldId;
	private final String name;
	private final int expRate;
	private final int mesoRate;
	private final int itemRate;

	public WorldInfo(final int id, final String name, final int expRate, final int mesoRate, final int itemRate, final String host, final int port) {
		super(host, port);

		this.worldId = id;
		this.name = name;

		this.expRate = expRate;
		this.mesoRate = mesoRate;
		this.itemRate = itemRate;
	}

	public int getWorldId() {
		return this.worldId;
	}

	public String getName() {
		return this.name;
	}

	public int getExpRate() {
		return this.expRate;
	}

	public int getMesoRate() {
		return this.mesoRate;
	}

	public int getItemRate() {
		return this.itemRate;
	}
}
