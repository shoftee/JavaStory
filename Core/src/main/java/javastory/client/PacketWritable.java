/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.client;

import javastory.io.PacketBuilder;

/**
 *
 * @author Tosho
 */
public interface PacketWritable {
    public void connectData(PacketBuilder builder);
}
