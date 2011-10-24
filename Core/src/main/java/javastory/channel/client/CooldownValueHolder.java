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
package javastory.channel.client;

import java.util.concurrent.ScheduledFuture;

public class CooldownValueHolder {

	public int skillId;
	public long startTime;
	public long length;
	public ScheduledFuture<?> timer;

	public CooldownValueHolder(int skillId, long startTime, long length, ScheduledFuture<?> timer) {
		super();
		this.skillId = skillId;
		this.startTime = startTime;
		this.length = length;
		this.timer = timer;
	}
}
