package javastory.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import javastory.server.EndpointInfo;

/**
 * 
 * @author shoftee
 */
public class ChannelInfo extends EndpointInfo {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8022870720043372903L;

	private final int worldId;
	private final int id;
	private final String name;

	public ChannelInfo(int worldId, int id, String name, String host, int port) {
		super(host, port);
		checkArgument(worldId >= 0, "'worldId' must be non-negative");
		checkArgument(id >= 0, "'id' must be non-negative.");
		checkNotNull(name);

		this.worldId = worldId;
		this.id = id;
		this.name = name;
	}

	public final int getId() {
		return id;
	}

	public final String getName() {
		return name;
	}

	public final int getWorldId() {
		return worldId;
	}
}
