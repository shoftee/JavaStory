/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.login;

import javastory.config.ChannelInfo;


/**
 *
 * @author shoftee
 */
public class LoginChannelInfo extends ChannelInfo {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1791926284473788530L;
	private int load;
    
    public LoginChannelInfo(final ChannelInfo info, final int load) {
        super(info.getWorldId(), info.getId(), info.getName(), info.getHost(), info.getPort());
        this.load = load;
    }

    public int getLoad() {
        return this.load;
    }
    
    public void setLoad(final int load) {
        this.load = load;
    }
}
