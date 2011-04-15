/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server;

import com.google.common.base.Preconditions;

/**
 *
 * @author Tosho
 */
public class EndpointInfo {

    private String host;
    private int port;

    public EndpointInfo(String host, int port) {
        Preconditions.checkNotNull(host);

        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }
}
