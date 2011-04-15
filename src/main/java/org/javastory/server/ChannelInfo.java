/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server;

import com.google.common.base.Preconditions;
import org.javastory.server.EndpointInfo;

/**
 *
 * @author Tosho
 */
public class ChannelInfo extends EndpointInfo {

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
