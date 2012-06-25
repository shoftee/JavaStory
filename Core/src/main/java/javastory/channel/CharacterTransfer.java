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
package javastory.channel;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.EnumMap;
import java.util.Map;

import javastory.channel.client.BuddyListEntry;
import javastory.channel.client.ISkill;
import javastory.channel.client.KeyLayout;
import javastory.channel.client.MemberRank;
import javastory.channel.client.MonsterBook;
import javastory.channel.client.Mount;
import javastory.channel.client.MultiInventory;
import javastory.channel.client.SkillEntry;
import javastory.channel.maps.SavedLocationType;
import javastory.channel.server.Storage;
import javastory.client.SimpleCharacterInfo;
import javastory.game.Gender;
import javastory.game.quest.QuestStatus;

import com.google.common.collect.Maps;

public class CharacterTransfer implements Externalizable {

	public final long Timestamp;
	
	public int CharacterId, AccountId, Fame, STR, DEX, INT, LUK, MaxHP, MaxMP, HP, MP, Exp, hpApUsed, mpApUsed, RemainingAP, Meso, SkinColorId, JobId, HairId,
		FaceId, MapId, InitialSpawnPoint, WorldId, GuildId, BuddyListCapacity, PartyId, MessengerId, MessengerPosition, MonsterBookCover, Dojo, RebornCount,
		Subcategory;
	public int ACash, vpoints, MaplePoints;
	public int MountLevel, MountItemId, MountFatigue, MountExp;
	public byte ChannelId, DojoRecord, GmLevel;
	public Gender Gender;
	public MemberRank GuildRank;
	public boolean ondmg, callgm;
	public long LastFameTime, TranferTime;
	public String CharacterName, AccountName, BlessOfFairy;
	public int Level;
	public MonsterBook MonsterBook;
	public MultiInventory Inventories;
	public SkillMacroSet SkillMacros;
	public KeyLayout KeyLayout;
	public EnumMap<SavedLocationType, Integer> SavedLocations;

	public Storage Storage;
	public int[] TeleportRocks;
	public int[] Wishlist;
	public int[] RemainingSP;
	public Map<Integer, String> QuestInfoEntries;
	public final Map<SimpleCharacterInfo, Boolean> BuddyListEntries = Maps.newLinkedHashMap();
	public final Map<Integer, QuestStatus> Quests = Maps.newLinkedHashMap();
	public final Map<Integer, SkillEntry> Skills = Maps.newLinkedHashMap();

	public CharacterTransfer() {
		this.Timestamp = System.currentTimeMillis();
	}

	public CharacterTransfer(final ChannelCharacter chr) {
		this.Timestamp = System.currentTimeMillis();
		
		this.CharacterId = chr.getId();
		this.AccountId = chr.getAccountId();
		this.AccountName = chr.getClient().getAccountName();
		this.ChannelId = (byte) chr.getClient().getChannelId();
		this.ACash = chr.getCSPoints(1);
		this.vpoints = chr.getVPoints();
		this.MaplePoints = chr.getCSPoints(2);
		this.CharacterName = chr.getName();
		this.Fame = chr.getFame();
		this.Gender = chr.getGender();
		this.Level = chr.getLevel();
		this.STR = chr.getStats().getStr();
		this.DEX = chr.getStats().getDex();
		this.INT = chr.getStats().getInt();
		this.LUK = chr.getStats().getLuk();
		this.HP = chr.getStats().getHp();
		this.MP = chr.getStats().getMp();
		this.MaxHP = chr.getStats().getMaxHp();
		this.MaxMP = chr.getStats().getMaxMp();
		this.Exp = chr.getExp();
		this.hpApUsed = chr.getHpApUsed();
		this.mpApUsed = chr.getMpApUsed();
		this.RemainingAP = chr.getRemainingAp();
		this.RemainingSP = chr.getRemainingSps();
		this.Meso = chr.getMeso();
		this.SkinColorId = chr.getSkinColorId();
		this.JobId = chr.getJobId();
		this.HairId = chr.getHairId();
		this.FaceId = chr.getFaceId();
		this.MapId = chr.getMapId();
		this.InitialSpawnPoint = chr.getInitialSpawnPoint();
		this.WorldId = chr.getWorldId();
		this.GuildId = chr.getGuildId();
		this.GuildRank = chr.getGuildRank();
		this.GmLevel = (byte) chr.getGmLevel();
		this.Subcategory = chr.getSubcategory();
		this.ondmg = chr.isOnDMG();
		this.callgm = chr.isCallGM();

		for (final BuddyListEntry qs : chr.getBuddyList().getBuddies()) {
			this.BuddyListEntries.put(new SimpleCharacterInfo(qs.getCharacterId(), qs.getName(), qs.getLevel(), qs.getJob()), qs.isVisible());
		}
		this.BuddyListCapacity = chr.getBuddyCapacity();

		final PartyMember member = chr.getPartyMembership();
		this.PartyId = member != null ? member.getPartyId() : -1;

		final Messenger messenger = chr.getMessenger();
		if (messenger != null) {
			this.MessengerId = messenger.getId();
			this.MessengerPosition = chr.getMessengerPosition();
		} else {
			this.MessengerId = 0;
			this.MessengerPosition = 4;
		}

		this.MonsterBookCover = chr.getMonsterBookCover();
		this.MonsterBook = chr.getMonsterBook();
		this.Dojo = chr.getDojo();
		this.DojoRecord = (byte) chr.getDojoRecord();
		this.RebornCount = chr.getReborns();
		this.QuestInfoEntries = chr.getQuestInfoMap();

		for (final Map.Entry<Integer, QuestStatus> qs : chr.getQuestStatusMap().entrySet()) {
			this.Quests.put(qs.getKey(), qs.getValue());
		}

		this.Inventories = chr.getInventories();

		for (final Map.Entry<ISkill, SkillEntry> qs : chr.getSkills().entrySet()) {
			this.Skills.put(qs.getKey().getId(), qs.getValue());
		}

		this.BlessOfFairy = chr.getBlessOfFairyOrigin();
		this.SkillMacros = chr.getSkillMacros();
		this.KeyLayout = chr.getKeyLayout();
		this.SavedLocations = chr.getSavedLocations();
		this.LastFameTime = chr.getLastFameTime();
		this.Storage = chr.getStorage();
		this.TeleportRocks = chr.getRocks();
		this.Wishlist = chr.getWishlist();

		final Mount mount = chr.getMount();
		this.MountItemId = mount.getItemId();
		this.MountFatigue = mount.getFatigue();
		this.MountLevel = mount.getLevel();
		this.MountExp = mount.getExp();
	}

	@Override
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.CharacterId = in.readInt();
		this.AccountId = in.readInt();
		this.AccountName = (String) in.readObject();
		this.ChannelId = in.readByte();
		this.ACash = in.readInt();
		this.vpoints = in.readInt();
		this.MaplePoints = in.readInt();
		this.CharacterName = (String) in.readObject();
		this.Fame = in.readInt();
		this.Gender = javastory.game.Gender.fromNumber(in.readByte());
		this.Level = in.readShort();
		this.STR = in.readInt();
		this.DEX = in.readInt();
		this.INT = in.readInt();
		this.LUK = in.readInt();
		this.HP = in.readInt();
		this.MP = in.readInt();
		this.MaxHP = in.readInt();
		this.MaxMP = in.readInt();
		this.Exp = in.readInt();
		this.hpApUsed = in.readInt();
		this.mpApUsed = in.readInt();
		this.RemainingAP = in.readInt();
		this.RemainingSP = (int[]) in.readObject();
		this.Meso = in.readInt();
		this.SkinColorId = in.readInt();
		this.JobId = in.readInt();
		this.HairId = in.readInt();
		this.FaceId = in.readInt();
		this.MapId = in.readInt();
		this.InitialSpawnPoint = in.readByte();
		this.WorldId = in.readByte();
		this.GuildId = in.readInt();
		this.GuildRank = MemberRank.fromNumber(in.readByte());
		this.GmLevel = in.readByte();

		this.BlessOfFairy = (String) in.readObject();

		this.SkillMacros = (SkillMacroSet) in.readObject();
		this.KeyLayout = (KeyLayout) in.readObject();
		this.SavedLocations = (EnumMap<SavedLocationType, Integer>) in.readObject();
		this.LastFameTime = in.readLong();
		this.Storage = (Storage) in.readObject();
		this.TeleportRocks = (int[]) in.readObject();
		this.Wishlist = (int[]) in.readObject();
		this.MountItemId = in.readInt();
		this.MountFatigue = in.readInt();
		this.MountLevel = in.readInt();
		this.MountExp = in.readInt();
		this.PartyId = in.readInt();
		this.MessengerId = in.readInt();
		this.MessengerPosition = in.readInt();
		this.MonsterBookCover = in.readInt();
		this.Dojo = in.readInt();
		this.DojoRecord = in.readByte();
		this.RebornCount = in.readInt();
		this.MonsterBook = (MonsterBook) in.readObject();
		this.Inventories = (MultiInventory) in.readObject();
		this.QuestInfoEntries = (Map<Integer, String>) in.readObject();

		final int skillEntryCount = in.readShort();
		int skillId;
		SkillEntry entry; // SkillEntry
		for (int i = 0; i < skillEntryCount; i++) {
			skillId = in.readInt();
			entry = (SkillEntry) in.readObject();
			this.Skills.put(skillId, entry);
		}

		this.BuddyListCapacity = in.readShort();
		final short buddyEntrySize = in.readShort();
		for (int i = 0; i < buddyEntrySize; i++) {
			this.BuddyListEntries.put(new SimpleCharacterInfo(in.readInt(), in.readUTF(), in.readInt(), in.readInt()), in.readBoolean());
		}

		final int questStatusEntryCount = in.readShort();
		int questId;
		QuestStatus questStatus;
		for (int i = 0; i < questStatusEntryCount; i++) {
			questId = in.readInt();
			questStatus = (QuestStatus) in.readObject();
			this.Quests.put(questId, questStatus);
		}
		this.ondmg = in.readByte() == 1;
		this.callgm = in.readByte() == 1;
		this.TranferTime = System.currentTimeMillis();
	}

	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.CharacterId);
		out.writeInt(this.AccountId);
		out.writeObject(this.AccountName);
		out.write(this.ChannelId);
		out.writeInt(this.ACash);
		out.writeInt(this.vpoints);
		out.writeInt(this.MaplePoints);
		out.writeObject(this.CharacterName);
		out.writeInt(this.Fame);
		out.write(this.Gender.asNumber());
		out.writeShort(this.Level);
		out.writeInt(this.STR);
		out.writeInt(this.DEX);
		out.writeInt(this.INT);
		out.writeInt(this.LUK);
		out.writeInt(this.HP);
		out.writeInt(this.MP);
		out.writeInt(this.MaxHP);
		out.writeInt(this.MaxMP);
		out.writeInt(this.Exp);
		out.writeInt(this.hpApUsed);
		out.writeInt(this.mpApUsed);
		out.writeInt(this.RemainingAP);
		out.writeObject(this.RemainingSP);
		out.writeInt(this.Meso);
		out.writeInt(this.SkinColorId);
		out.writeInt(this.JobId);
		out.writeInt(this.HairId);
		out.writeInt(this.FaceId);
		out.writeInt(this.MapId);
		out.write(this.InitialSpawnPoint);
		out.write(this.WorldId);
		out.writeInt(this.GuildId);
		out.write(this.GuildRank.asNumber());
		out.write(this.GmLevel);

		out.writeObject(this.BlessOfFairy);

		out.writeObject(this.SkillMacros);
		out.writeObject(this.KeyLayout);
		out.writeObject(this.SavedLocations);
		out.writeLong(this.LastFameTime);
		out.writeObject(this.Storage);
		out.writeObject(this.TeleportRocks);
		out.writeObject(this.Wishlist);
		out.writeInt(this.MountItemId);
		out.writeInt(this.MountFatigue);
		out.writeInt(this.MountLevel);
		out.writeInt(this.MountExp);
		out.writeInt(this.PartyId);
		out.writeInt(this.MessengerId);
		out.writeInt(this.MessengerPosition);
		out.writeInt(this.MonsterBookCover);
		out.writeInt(this.Dojo);
		out.write(this.DojoRecord);
		out.writeInt(this.RebornCount);
		out.writeObject(this.MonsterBook);
		out.writeObject(this.Inventories);
		out.writeObject(this.QuestInfoEntries);

		out.writeShort(this.Skills.size());
		for (final Map.Entry<Integer, SkillEntry> qs : this.Skills.entrySet()) {
			// Bless of fairy is transported here too.
			out.writeInt(qs.getKey());
			out.writeObject(qs.getValue());
		}

		out.writeShort(this.BuddyListCapacity);
		out.writeShort(this.BuddyListEntries.size());
		for (final Map.Entry<SimpleCharacterInfo, Boolean> qs : this.BuddyListEntries.entrySet()) {
			final SimpleCharacterInfo characterInfo = qs.getKey();
			out.writeInt(characterInfo.Id);
			out.writeUTF(characterInfo.Name);
			out.writeInt(characterInfo.Level);
			out.writeInt(characterInfo.Job);
			out.writeBoolean(qs.getValue());
		}

		out.writeShort(this.Quests.size());
		for (final Map.Entry<Integer, QuestStatus> qs : this.Quests.entrySet()) {
			out.writeInt(qs.getKey());
			out.writeObject(qs.getValue());
		}

		out.writeByte(this.ondmg ? 1 : 0);
		out.writeByte(this.callgm ? 1 : 0);
	}
}
