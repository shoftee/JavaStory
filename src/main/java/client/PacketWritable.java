/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import org.javastory.io.PacketBuilder;

/**
 *
 * @author Tosho
 */
public interface PacketWritable {
    public void connectData(PacketBuilder builder);
}
