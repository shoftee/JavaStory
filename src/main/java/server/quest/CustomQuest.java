/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package server.quest;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import database.DatabaseConnection;

public class CustomQuest extends Quest implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;

    public CustomQuest(int id) {
	try {
	    this.id = id;
	    startActs = new LinkedList<QuestAction>();
	    completeActs = new LinkedList<QuestAction>();
	    startReqs = new LinkedList<QuestRequirement>();
	    completeReqs = new LinkedList<QuestRequirement>();

	    PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM questrequirements WHERE questid = ?");
	    ps.setInt(1, id);
	    ResultSet rs = ps.executeQuery();
	    QuestRequirement req;
	    CustomQuestData data;
	    while (rs.next()) {
		Blob blob = rs.getBlob("data");
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(blob.getBytes(1, (int) blob.length())));
		data = (CustomQuestData) ois.readObject();
		req = new QuestRequirement(this, QuestRequirementType.getByWZName(data.getName()), data);
		final byte status = rs.getByte("status");
		if (status == 0) {
		    startReqs.add(req);
		} else if (status == 1) {
		    completeReqs.add(req);
		}
	    }
	    rs.close();
	    ps.close();

	    ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM questactions WHERE questid = ?");
	    ps.setInt(1, id);
	    rs = ps.executeQuery();
	    QuestAction act;
	    while (rs.next()) {
		Blob blob = rs.getBlob("data");
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(blob.getBytes(1, (int) blob.length())));
		data = (CustomQuestData) ois.readObject();
		act = new QuestAction(QuestActionType.getByWZName(data.getName()), data, this);
		final byte status = rs.getByte("status");
		if (status == 0) {
		    startActs.add(act);
		} else if (status == 1) {
		    completeActs.add(act);
		}
	    }
	    rs.close();
	    ps.close();
	} catch (Exception ex) {
	    ex.printStackTrace();
	    System.err.println("Error loading custom quest from SQL." + ex);
	}
    }
}
