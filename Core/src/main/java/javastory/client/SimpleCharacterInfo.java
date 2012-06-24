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

package javastory.client;

import java.io.Serializable;

public class SimpleCharacterInfo implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6588000376596664820L;
	
	public final int Id, Level, Job;
	public final String Name;

	public SimpleCharacterInfo(final int id, final String name, final int level, final int job) {
		super();
		this.Id = id;
		this.Name = name;
		this.Level = level;
		this.Job = job;
	}
}
