/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.rmi;

import java.rmi.RemoteException;

/**
 *
 * @author Tosho
 */
public abstract class RemoteClient<TClient extends GenericRemoteObject> {

    protected TClient reference;
    
    protected RemoteClient(TClient reference) {
        this.reference = reference;
    }
    
    /**
     * Checks if the remote instance of this object is active.
     * @return 
     *      true if the connection is active; 
     *      false if there was a RemoteException thrown.
     */
    public boolean ping() {
        try {
            return reference.ping();
        } catch (RemoteException ex) {
            return false;
        }
    }  
}
