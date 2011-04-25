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
package handling.world;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedHashMap;
import java.util.Map;

import client.Mount;
import client.GameCharacter;
import client.QuestStatus;
import client.ISkill;
import client.SkillEntry;
import client.BuddyListEntry;
import client.CharacterNameAndId;
import com.google.common.collect.Maps;
import server.quest.Quest;

public class CharacterTransfer implements Externalizable {

    public int characterid, accountid, fame, str, dex, int_, luk, maxhp, maxmp, hp, mp, exp, hpApUsed, mpApUsed,
	    remainingAp, meso, skinColor, job, hair, face, mapId,
	    initialSpawnPoint, worldId, rank, rankMove, jobRank, jobRankMove, guildId,
	    buddyListCapacity, partyId, messengerId, messengerPosition, monsterBookCover, dojo, ACash, vpoints, MaplePoints,
	    mount_level, mount_itemid, mount_Fatigue, mount_exp, reborns, subcategory;
    public byte channel, dojoRecord, gender, gmLevel, guildRank, unionRank;
	public boolean ondmg, callgm;
    public long lastFameTime, TranferTime;
    public String name, accountname, blessOfFairy;
    public short level;
    public Object monsterbook, inventorys, skillmacro, keyLayout, savedlocation, storage, rocks, wishlist, questInfo, remainingSp;
    public final Map<CharacterNameAndId, Boolean> buddies = Maps.newLinkedHashMap();
    public final Map<Integer, Object> quest = Maps.newLinkedHashMap(); // Questid instead of MapleQuest, as it's huge. Cant be transporting MapleQuest.java
    public final Map<Integer, Object> skills = Maps.newLinkedHashMap(); // Skillid instead of Skill.java, as it's huge. Cant be transporting Skill.java and MapleStatEffect.java
  

    public CharacterTransfer() {
    }

    public CharacterTransfer(final GameCharacter chr) {
	this.characterid = chr.getId();
	this.accountid = chr.getAccountId();
	this.accountname = chr.getClient().getAccountName();
	this.channel = (byte) chr.getClient().getChannelId();
	this.ACash = chr.getCSPoints(1);
        this.vpoints = chr.getVPoints();
        this.vpoints = chr.getVPoints();
	this.MaplePoints = chr.getCSPoints(2);
	this.name = chr.getName();
	this.fame = chr.getFame();
	this.gender = (byte) chr.getGender();
	this.level = chr.getLevel();
	this.str = chr.getStat().getStr();
	this.dex = chr.getStat().getDex();
	this.int_ = chr.getStat().getInt();
	this.luk = chr.getStat().getLuk();
	this.hp = chr.getStat().getHp();
	this.mp = chr.getStat().getMp();
	this.maxhp = chr.getStat().getMaxHp();
	this.maxmp = chr.getStat().getMaxMp();
	this.exp = chr.getExp();
	this.hpApUsed = chr.getHpApUsed();
	this.mpApUsed = chr.getMpApUsed();
	this.remainingAp = chr.getRemainingAp();
	this.remainingSp = chr.getRemainingSps();
	this.meso = chr.getMeso();
	this.skinColor = chr.getSkinColor();
	this.job = chr.getJob();
	this.hair = chr.getHair();
	this.face = chr.getFace();
	this.mapId = chr.getMapId();
	this.initialSpawnPoint = chr.getInitialSpawnpoint();
	this.worldId = chr.getWorld();
	this.rank = chr.getRank();
	this.rankMove = chr.getRankMove();
	this.jobRank = (byte) chr.getJobRank();
	this.jobRankMove = chr.getJobRankMove();
	this.guildId = chr.getGuildId();
	this.guildRank = (byte) chr.getGuildRank();
	this.unionRank = (byte) chr.getGuildUnionRank();
	this.gmLevel = (byte) chr.getGMLevel();
	this.subcategory = chr.getSubcategory();
	this.ondmg = chr.isOnDMG();
	this.callgm = chr.isCallGM();
        
	for (final BuddyListEntry qs : chr.getBuddylist().getBuddies()) {
	    this.buddies.put(new CharacterNameAndId(qs.getCharacterId(), qs.getName(), qs.getLevel(), qs.getJob()), qs.isVisible());
	}
	this.buddyListCapacity = chr.getBuddyCapacity();

	this.partyId = chr.getPartyId();

	if (chr.getMessenger() != null) {
	    this.messengerId = chr.getMessenger().getId();
	    this.messengerPosition = chr.getMessengerPosition();
	} else {
	    this.messengerId = 0;
	    this.messengerPosition = 4;
	}

	this.monsterBookCover = chr.getMonsterBookCover();
	this.dojo = chr.getDojo();
	this.dojoRecord = (byte) chr.getDojoRecord();
        this.reborns = chr.getReborns();
	this.questInfo = chr.getInfoQuest_Map();

	for (final Map.Entry<Quest, QuestStatus> qs : chr.getQuest_Map().entrySet()) {
	    this.quest.put(qs.getKey().getId(), qs.getValue());
	}

	this.monsterbook = chr.getMonsterBook();
	this.inventorys = chr.getInventories();

	for (final Map.Entry<ISkill, SkillEntry> qs : chr.getSkills().entrySet()) {
	    this.skills.put(qs.getKey().getId(), qs.getValue());
	}

	this.blessOfFairy = chr.getBlessOfFairyOrigin();
	this.skillmacro = chr.getMacros();
	this.keyLayout = chr.getKeyLayout();
	this.savedlocation = chr.getSavedLocations();
	this.lastFameTime = chr.getLastFameTime();
	this.storage = chr.getStorage();
	this.rocks = chr.getRocks();
	this.wishlist = chr.getWishlist();

	final Mount mount = chr.getMount();
	this.mount_itemid = mount.getItemId();
	this.mount_Fatigue = mount.getFatigue();
	this.mount_level = mount.getLevel();
	this.mount_exp = mount.getExp();
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
	this.characterid = in.readInt();
	this.accountid = in.readInt();
	this.accountname = (String) in.readObject();
	this.channel = in.readByte();
	this.ACash = in.readInt();
        this.vpoints = in.readInt();
	this.MaplePoints = in.readInt();
	this.name = (String) in.readObject();
	this.fame = in.readInt();
	this.gender = in.readByte();
	this.level = in.readShort();
	this.str = in.readInt();
	this.dex = in.readInt();
	this.int_ = in.readInt();
	this.luk = in.readInt();
	this.hp = in.readInt();
	this.mp = in.readInt();
	this.maxhp = in.readInt();
	this.maxmp = in.readInt();
	this.exp = in.readInt();
	this.hpApUsed = in.readInt();
	this.mpApUsed = in.readInt();
	this.remainingAp = in.readInt();
	this.remainingSp = in.readObject();
	this.meso = in.readInt();
	this.skinColor = in.readInt();
	this.job = in.readInt();
	this.hair = in.readInt();
	this.face = in.readInt();
	this.mapId = in.readInt();
	this.initialSpawnPoint = in.readByte();
	this.worldId = in.readByte();
	this.rank = in.readInt();
	this.rankMove = in.readInt();
	this.jobRank = in.readInt();
	this.jobRankMove = in.readInt();
	this.guildId = in.readInt();
	this.guildRank = in.readByte();
	this.unionRank = in.readByte();
	this.gmLevel = in.readByte();
        

	this.blessOfFairy = (String) in.readObject();

	this.skillmacro = in.readObject();
	this.keyLayout = in.readObject();
	this.savedlocation = in.readObject();
	this.lastFameTime = in.readLong();
	this.storage = in.readObject();
	this.rocks = in.readObject();
	this.wishlist = in.readObject();
	this.mount_itemid = in.readInt();
	this.mount_Fatigue = in.readInt();
	this.mount_level = in.readInt();
	this.mount_exp = in.readInt();
	this.partyId = in.readInt();
	this.messengerId = in.readInt();
	this.messengerPosition = in.readInt();
	this.monsterBookCover = in.readInt();
	this.dojo = in.readInt();
	this.dojoRecord = in.readByte();
        this.reborns = in.readInt();
	this.monsterbook = in.readObject();
	this.inventorys = in.readObject();
	this.questInfo = in.readObject();

	final int skillsize = in.readShort();
	int skillid;
	Object skill; // SkillEntry
	for (int i = 0; i < skillsize; i++) {
	    skillid = in.readInt();
	    skill = in.readObject();
	    this.skills.put(skillid, skill);
	}

	this.buddyListCapacity = in.readShort();
	final short addedbuddysize = in.readShort();
	for (int i = 0; i < addedbuddysize; i++) {
	    buddies.put(new CharacterNameAndId(in.readInt(), in.readUTF(), in.readInt(), in.readInt()), in.readBoolean());
	}

	final int questsize = in.readShort();
	int quest;
	Object queststatus;
	for (int i = 0; i < questsize; i++) {
	    quest = in.readInt();
	    queststatus = in.readObject();
	    this.quest.put(quest, queststatus);
	}
	this.ondmg = in.readByte() == 1;
	this.callgm = in.readByte() == 1;
	TranferTime = System.currentTimeMillis();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
	out.writeInt(this.characterid);
	out.writeInt(this.accountid);
	out.writeObject(this.accountname);
	out.write(this.channel);
	out.writeInt(this.ACash);
        out.writeInt(this.vpoints);
	out.writeInt(this.MaplePoints);
	out.writeObject(this.name);
	out.writeInt(this.fame);
	out.write(this.gender);
	out.writeShort(this.level);
	out.writeInt(this.str);
	out.writeInt(this.dex);
	out.writeInt(this.int_);
	out.writeInt(this.luk);
	out.writeInt(this.hp);
	out.writeInt(this.mp);
	out.writeInt(this.maxhp);
	out.writeInt(this.maxmp);
	out.writeInt(this.exp);
	out.writeInt(this.hpApUsed);
	out.writeInt(this.mpApUsed);
	out.writeInt(this.remainingAp);
	out.writeObject(this.remainingSp);
	out.writeInt(this.meso);
	out.writeInt(this.skinColor);
	out.writeInt(this.job);
	out.writeInt(this.hair);
	out.writeInt(this.face);
	out.writeInt(this.mapId);
	out.write(this.initialSpawnPoint);
	out.write(this.worldId);
	out.writeInt(this.rank);
	out.writeInt(this.rankMove);
	out.writeInt(this.jobRank);
	out.writeInt(this.jobRankMove);
	out.writeInt(this.guildId);
	out.write(this.guildRank);
	out.write(this.unionRank);
	out.write(this.gmLevel);
        
	out.writeObject(this.blessOfFairy);

	out.writeObject(this.skillmacro);
	out.writeObject(this.keyLayout);
	out.writeObject(this.savedlocation);
	out.writeLong(this.lastFameTime);
	out.writeObject(this.storage);
	out.writeObject(this.rocks);
	out.writeObject(this.wishlist);
	out.writeInt(this.mount_itemid);
	out.writeInt(this.mount_Fatigue);
	out.writeInt(this.mount_level);
	out.writeInt(this.mount_exp);
	out.writeInt(this.partyId);
	out.writeInt(this.messengerId);
	out.writeInt(this.messengerPosition);
	out.writeInt(this.monsterBookCover);
	out.writeInt(this.dojo);
	out.write(this.dojoRecord);
        out.writeInt(this.reborns);
	out.writeObject(this.monsterbook);
	out.writeObject(this.inventorys);
	out.writeObject(this.questInfo);

	out.writeShort(this.skills.size());
	for (final Map.Entry<Integer, Object> qs : this.skills.entrySet()) {
	    out.writeInt(qs.getKey()); // Questid instead of Skill, as it's huge :(
	    out.writeObject(qs.getValue());
	    // Bless of fairy is transported here too.
	}

	out.writeShort(this.buddyListCapacity);
	out.writeShort(this.buddies.size());
	for (final Map.Entry<CharacterNameAndId, Boolean> qs : this.buddies.entrySet()) {

	    out.writeInt(qs.getKey().getId());

	    out.writeUTF(qs.getKey().getName());

            out.writeInt(qs.getKey().getLevel());

            out.writeInt(qs.getKey().getJob());

	    out.writeBoolean(qs.getValue());
	}
	
	out.writeShort(this.quest.size());
	for (final Map.Entry<Integer, Object> qs : this.quest.entrySet()) {
	    out.writeInt(qs.getKey()); // Questid instead of MapleQuest, as it's huge :(
	    out.writeObject(qs.getValue());
	}

	out.writeByte(this.ondmg ? 1 : 0);
	out.writeByte(this.callgm ? 1 : 0);
    }
}
