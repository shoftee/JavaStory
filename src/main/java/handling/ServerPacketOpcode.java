package handling;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public enum ServerPacketOpcode implements WritableIntValueHolder {

    PING,
    LOGIN_STATUS,
    PIN_OPERATION,
    SECONDPW_ERROR,
    SERVERLIST,
    SERVERSTATUS,
    SERVER_IP,
    CHARLIST,
    CHAR_NAME_RESPONSE,
    RELOG_RESPONSE,
    ADD_NEW_CHAR_ENTRY,
    DELETE_CHAR_RESPONSE,
    CHANNEL_SELECTED,
    ALL_CHARLIST,
    CHANGE_CHANNEL,
    UPDATE_STATS,
    FAME_RESPONSE,
    UPDATE_SKILLS,
    WARP_TO_MAP,
    SERVERMESSAGE,
    AVATAR_MEGA,
    SPAWN_NPC,
    REMOVE_NPC,
    SPAWN_NPC_REQUEST_CONTROLLER,
    SPAWN_MONSTER,
    SPAWN_MONSTER_CONTROL,
    MOVE_MONSTER_RESPONSE,
    CHATTEXT,
    SHOW_STATUS_INFO,
    SHOW_MESO_GAIN,
    SHOW_QUEST_COMPLETION,
    WHISPER,
    SPAWN_PLAYER,
    ANNOUNCE_PLAYER_SHOP,
    SHOW_SCROLL_EFFECT,
    SHOW_ITEM_GAIN_INCHAT,
    CURRENT_MAP_WARP,
    KILL_MONSTER,
    DROP_ITEM_FROM_MAPOBJECT,
    FACIAL_EXPRESSION,
    MOVE_PLAYER,
    MOVE_MONSTER,
    CLOSE_RANGE_ATTACK,
    RANGED_ATTACK,
    MAGIC_ATTACK,
    OPEN_NPC_SHOP,
    CONFIRM_SHOP_TRANSACTION,
    OPEN_STORAGE,
    MODIFY_INVENTORY_ITEM,
    REMOVE_PLAYER_FROM_MAP,
    REMOVE_ITEM_FROM_MAP,
    UPDATE_CHAR_LOOK,
    SHOW_FOREIGN_EFFECT,
    GIVE_FOREIGN_BUFF,
    CANCEL_FOREIGN_BUFF,
    DAMAGE_PLAYER,
    CHAR_INFO,
    UPDATE_QUEST_INFO,
    GIVE_BUFF,
    CANCEL_BUFF,
    PLAYER_INTERACTION,
    UPDATE_CHAR_BOX,
    NPC_TALK,
    KEYMAP,
    SHOW_MONSTER_HP,
    PARTY_OPERATION,
    UPDATE_PARTYMEMBER_HP,
    MULTICHAT,
    APPLY_MONSTER_STATUS,
    CANCEL_MONSTER_STATUS,
    CLOCK,
    SPAWN_PORTAL,
    SPAWN_DOOR,
    REMOVE_DOOR,
    SPAWN_SUMMON,
    REMOVE_SUMMON,
    SUMMON_ATTACK,
    MOVE_SUMMON,
    SPAWN_MIST,
    REMOVE_MIST,
    DAMAGE_SUMMON,
    DAMAGE_MONSTER,
    BUDDYLIST,
    SHOW_ITEM_EFFECT,
    SHOW_CHAIR,
    CANCEL_CHAIR,
    SKILL_EFFECT,
    CANCEL_SKILL_EFFECT,
    BOSS_ENV,
    REACTOR_SPAWN,
    REACTOR_HIT,
    REACTOR_DESTROY,
    MAP_EFFECT,
    GUILD_OPERATION,
    ALLIANCE_OPERATION,
    BBS_OPERATION,
    FAMILY,
    EARN_TITLE_MSG,
    SHOW_MAGNET,
    MERCH_ITEM_MSG,
    MERCH_ITEM_STORE,
    MESSENGER,
    NPC_ACTION,
    SPAWN_PET,
    MOVE_PET,
    PET_CHAT,
    PET_COMMAND,
    PET_NAMECHANGE,
    COOLDOWN,
    PLAYER_HINT,
    SUMMON_HINT,
    SUMMON_HINT_MSG,
    CYGNUS_INTRO_DISABLE_UI,
    CYGNUS_INTRO_LOCK,
    USE_SKILL_BOOK,
    FINISH_SORT,
    FINISH_GATHER,
    SHOW_EQUIP_EFFECT,
    SKILL_MACRO,
    CS_OPEN,
    CS_UPDATE,
    CS_OPERATION,
    MTS_OPEN,
    PLAYER_NPC,
    SHOW_NOTES,
    SUMMON_SKILL,
    ARIANT_PQ_START,
    CATCH_MONSTER,
    ARIANT_SCOREBOARD,
    ZAKUM_SHRINE,
    BOAT_EFFECT,
    CHALKBOARD,
    DUEY,
    ENABLE_TV,
    REMOVE_TV,
    SEND_TV,
    TROCK_LOCATIONS,
    MONSTER_CARNIVAL_START,
    MONSTER_CARNIVAL_OBTAINED_CP,
    MONSTER_CARNIVAL_PARTY_CP,
    MONSTER_CARNIVAL_SUMMON,
    MONSTER_CARNIVAL_DIED,
    SPAWN_HIRED_MERCHANT,
    UPDATE_HIRED_MERCHANT,
    SEND_TITLE_BOX,
    DESTROY_HIRED_MERCHANT,
    UPDATE_MOUNT,
    MONSTERBOOK_ADD,
    MONSTERBOOK_CHANGE_COVER,
    FAIRY_PEND_MSG,
    VICIOUS_HAMMER,
    FISHING_BOARD_UPDATE,
    FISHING_CAUGHT,
    ENERGY,
    DRAGON_MOVE,
    DRAGON_REMOVE,
    DRAGON_SPAWN,
    ARAN_COMBO;
    private int code = -2;

    @Override
    public void setValue(int code) {
        this.code = code;
    }

    @Override
    public int getValue() {
        return code;
    }

    @Override
    public boolean isFirst() {
        return true;
    }

    public static Properties getDefaultProperties()
            throws FileNotFoundException, IOException {
        Properties props = new Properties();
        FileInputStream fileInputStream =
                new FileInputStream("serveropcodes.properties");
        props.load(fileInputStream);
        fileInputStream.close();
        return props;
    }

    static {
        reloadValues();
    }

    public static final void reloadValues() {
        try {
            ExternalCodeTableGetter.populateValues(
                    getDefaultProperties(), values());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load server op codes", e);
        }
    }
}