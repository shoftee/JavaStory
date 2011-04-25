/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 * @author Tosho
 */
public abstract class GenericRemoteObject extends UnicastRemoteObject {
    protected GenericRemoteObject() throws RemoteException {
        super(0, Sockets.getClientFactory(), Sockets.getServerFactory());
    }
    
    public boolean ping() throws RemoteException {
        return true;
    }
}
