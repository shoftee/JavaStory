/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.server;

import javastory.server.ChannelInfo;

/**
 *
 * @author Tosho
 */
public class LoginChannelInfo extends ChannelInfo {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1791926284473788530L;
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
