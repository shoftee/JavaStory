/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.server;

import org.javastory.server.ChannelInfo;

/**
 *
 * @author Tosho
 */
public class LoginChannelInfo extends ChannelInfo {

    private int load;
    
    public LoginChannelInfo(ChannelInfo info, int load) {
        super(info.getId(), info.getName(), info.getHost(), info.getPort());
        this.load = load;
    }

    public int getLoad() {
        return load;
    }
    
    public void setLoad(int load) {
        this.load = load;
    }
}
