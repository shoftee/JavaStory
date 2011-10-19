/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.server;

import com.google.common.base.Preconditions;

/**
 *
 * @author shoftee
 */
public class ChannelInfo extends EndpointInfo {

    /**
	 * 
	 */
	private static final long serialVersionUID = -8022870720043372903L;
	
	private int id;
    private String name;

    public ChannelInfo(int id, String name, String host, int port) {
        super(host, port);
        Preconditions.checkArgument(id >= 0, "'id' must be non-negative.");
        Preconditions.checkNotNull(name);

        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
