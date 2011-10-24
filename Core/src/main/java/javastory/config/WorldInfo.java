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

	public WorldInfo(int id, String name, int expRate, int mesoRate, int itemRate, String host, int port) {
		super(host, port);

		this.worldId = id;
		this.name = name;

		this.expRate = expRate;
		this.mesoRate = mesoRate;
		this.itemRate = itemRate;
	}

	public int getWorldId() {
		return worldId;
	}

	public String getName() {
		return name;
	}

	public int getExpRate() {
		return expRate;
	}

	public int getMesoRate() {
		return mesoRate;
	}

	public int getItemRate() {
		return itemRate;
	}
}
