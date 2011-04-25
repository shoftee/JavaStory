/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import handling.GamePacket;

/**
 *
 * @author Tosho
 */
public interface PlayerClient {
    public void write(GamePacket packet);
    public int getId();
    public void disconnect();
    public void disconnect(boolean immediately);
}
