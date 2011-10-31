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
package javastory.login;

import java.rmi.RemoteException;

import javastory.config.ChannelInfo;
import javastory.rmi.GenericRemoteObject;
import javastory.rmi.LoginWorldInterface;

public class LoginWorldInterfaceImpl extends GenericRemoteObject implements LoginWorldInterface {

	private static final long serialVersionUID = -3405666366539470037L;

	public LoginWorldInterfaceImpl() throws RemoteException {
		super();
	}

	@Override
	public void channelOnline(final ChannelInfo info) throws RemoteException {
		LoginServer.getInstance().addChannel(info);
	}

	@Override
	public void channelOffline(final int channel) throws RemoteException {
		LoginServer.getInstance().removeChannel(channel);
	}

	@Override
	public void shutdown() throws RemoteException {
		LoginServer.getInstance().shutdown();
	}
}
