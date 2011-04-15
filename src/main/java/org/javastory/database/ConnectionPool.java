/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.database;

import java.sql.SQLException;

/**
 *
 * @author Tosho
 */
interface ConnectionPool {
    public void release(PooledConnection connection) throws SQLException;
}
