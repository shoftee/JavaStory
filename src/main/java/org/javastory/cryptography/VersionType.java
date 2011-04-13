/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.javastory.cryptography;

/**
 * Version representation.
 * 
 * @author shoftee
 */
public enum VersionType {
    /**
     * Regular version representation.
     * The version is used as-is.
     */
    REGULAR, 
    /**
     * One's complement version representation.
     * The bit-wise NOT of the version is used.
     */
    COMPLEMENT;
}
