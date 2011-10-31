/*
 * This file is part of the OdinMS Maple Story Server Copyright (C) 2008 ~ 2010
 * Patrick Huy <patrick.huy@frz.cc> Matthias Butz <matze@odinms.de> Jan
 * Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation. You may not use, modify or distribute this
 * program under any other version of the GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.tools;

import java.io.Serializable;

/**
 * Represents a pair of values.
 * 
 * @author Frz
 * @since Revision 333
 * @version 1.0
 * 
 * @param <E>
 *            The type of the left value.
 * @param <F>
 *            The type of the right value.
 */
public class Pair<E, F> implements Serializable {

	private static final long serialVersionUID = 9179541993413738569L;
	public E left;
	public F right;

	/**
	 * Class constructor - pairs two objects together.
	 * 
	 * @param left
	 *            The left object.
	 * @param right
	 *            The right object.
	 */
	public Pair(final E left, final F right) {
		this.left = left;
		this.right = right;
	}

	/**
	 * Gets the left value.
	 * 
	 * @return The left value.
	 */
	public E getLeft() {
		return this.left;
	}

	/**
	 * Gets the right value.
	 * 
	 * @return The right value.
	 */
	public F getRight() {
		return this.right;
	}

	/**
	 * Turns the pair into a string.
	 * 
	 * @return Each value of the pair as a string joined by a colon.
	 */
	@Override
	public String toString() {
		return this.left.toString() + ":" + this.right.toString();
	}

	/**
	 * Gets the hash code of this pair.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.left == null ? 0 : this.left.hashCode());
		result = prime * result + (this.right == null ? 0 : this.right.hashCode());
		return result;
	}

	/**
	 * Checks to see if two pairs are equal.
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final Pair<?, ?> other = (Pair<?, ?>) obj;
		if (this.left == null) {
			if (other.left != null) {
				return false;
			}
		} else if (!this.left.equals(other.left)) {
			return false;
		}
		if (this.right == null) {
			if (other.right != null) {
				return false;
			}
		} else if (!this.right.equals(other.right)) {
			return false;
		}
		return true;
	}
}
