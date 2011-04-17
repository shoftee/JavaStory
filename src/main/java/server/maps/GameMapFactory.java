package server.maps;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.File;

import client.OdinSEA;
import provider.WzData;
import provider.WzDataProvider;
import provider.WzDataProviderFactory;
import provider.WzDataTool;
import server.PortalFactory;
import server.life.AbstractLoadedGameLife;
import server.life.LifeFactory;
import server.life.Monster;
import tools.StringUtil;

public class GameMapFactory {

    private static final WzDataProvider source = WzDataProviderFactory.getDataProvider(new File(System.getProperty("org.javastory.wzpath") + "/Map.wz"));
    private static final WzData nameData = WzDataProviderFactory.getDataProvider(new File(System.getProperty("org.javastory.wzpath") + "/String.wz")).getData("Map.img");
    private final Map<Integer, GameMap> maps = new HashMap<Integer, GameMap>();
    private final WeakHashMap<Integer, GameMap> instanceMap = new WeakHashMap<Integer, GameMap>();
    private int channel;
    private int world;

    public int getWorld() {
        return world;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public final GameMap getMap(final int mapid) {
        return getMap(mapid, true, true, true);
    }

    //backwards-compatible
    public final GameMap getMap(final int mapid, final boolean respawns, final boolean npcs) {
        return getMap(mapid, respawns, npcs, true);
    }

    public final GameMap getMap(final int mapid, final boolean respawns, final boolean npcs, final boolean reactors) {
        Integer omapid = Integer.valueOf(mapid);
        GameMap map = maps.get(omapid);
        if (map == null) {
            synchronized (this) {
                // check if someone else who was also synchronized has loaded the map already
                map = maps.get(omapid);
                if (map != null) {
                    return map;
                }
                WzData mapData = source.getData(getMapName(mapid));
                WzData link = mapData.getChildByPath("info/link");
                if (link != null) {
                    mapData = source.getData(getMapName(WzDataTool.getIntConvert("info/link", mapData)));
                }
                float monsterRate = 0;
                if (respawns) {
                    WzData mobRate = mapData.getChildByPath("info/mobRate");
                    if (mobRate != null) {
                        monsterRate = ((Float) mobRate.getData()).floatValue();
                    }
                }
                map = new GameMap(mapid, channel, WzDataTool.getInt("info/returnMap", mapData), monsterRate, world);
                PortalFactory portalFactory = new PortalFactory();
                for (WzData portal : mapData.getChildByPath("portal")) {
                    map.addPortal(portalFactory.makePortal(WzDataTool.getInt(portal.getChildByPath("pt")), portal));
                }
                List<Foothold> allFootholds = new LinkedList<Foothold>();
                Point lBound = new Point();
                Point uBound = new Point();
                Foothold fh;
                for (WzData footRoot : mapData.getChildByPath("foothold")) {
                    for (WzData footCat : footRoot) {
                        for (WzData footHold : footCat) {
                            fh = new Foothold(new Point(
                                    WzDataTool.getInt(footHold.getChildByPath("x1")), WzDataTool.getInt(footHold.getChildByPath("y1"))), new Point(
                                    WzDataTool.getInt(footHold.getChildByPath("x2")), WzDataTool.getInt(footHold.getChildByPath("y2"))), Integer.parseInt(footHold.getName()));
                            fh.setPrev((short) WzDataTool.getInt(footHold.getChildByPath("prev")));
                            fh.setNext((short) WzDataTool.getInt(footHold.getChildByPath("next")));

                            if (fh.getX1() < lBound.x) {
                                lBound.x = fh.getX1();
                            }
                            if (fh.getX2() > uBound.x) {
                                uBound.x = fh.getX2();
                            }
                            if (fh.getY1() < lBound.y) {
                                lBound.y = fh.getY1();
                            }
                            if (fh.getY2() > uBound.y) {
                                uBound.y = fh.getY2();
                            }
                            allFootholds.add(fh);
                        }
                    }
                }
                FootholdTree fTree = new FootholdTree(lBound, uBound);
                for (Foothold foothold : allFootholds) {
                    fTree.insert(foothold);
                }
                map.setFootholds(fTree);
                // load areas (EG PQ platforms)
                if (mapData.getChildByPath("area") != null) {
                    int x1, y1, x2, y2;
                    Rectangle mapArea;
                    for (WzData area : mapData.getChildByPath("area")) {
                        x1 = WzDataTool.getInt(area.getChildByPath("x1"));
                        y1 = WzDataTool.getInt(area.getChildByPath("y1"));
                        x2 = WzDataTool.getInt(area.getChildByPath("x2"));
                        y2 = WzDataTool.getInt(area.getChildByPath("y2"));
                        mapArea = new Rectangle(x1, y1, (x2 - x1), (y2 - y1));
                        map.addMapleArea(mapArea);
                    }
                }
                int bossid = -1;
                String msg = null;
                if (mapData.getChildByPath("info/timeMob") != null) {
                    bossid = WzDataTool.getInt(mapData.getChildByPath("info/timeMob/id"), 0);
                    msg = WzDataTool.getString(mapData.getChildByPath("info/timeMob/message"), null);
                }
                // load life data (npc, monsters)
                String type;
                AbstractLoadedGameLife myLife;
                for (WzData life : mapData.getChildByPath("life")) {
                    type = WzDataTool.getString(life.getChildByPath("type"));
                    if (npcs || !type.equals("n")) {
                        myLife = loadLife(life, WzDataTool.getString(life.getChildByPath("id")), type);
                        if (myLife instanceof Monster) {
                            final Monster mob = (Monster) myLife;
                            map.addMonsterSpawn(mob,
                                                WzDataTool.getInt("mobTime", life, 0),
                                                (byte) WzDataTool.getInt("team", life, -1),
                                                mob.getId() == bossid ? msg : null);
                        } else {
                            boolean bAdd = true;
                            final int idd = myLife.getId();
                            for (byte t = 0; t < OdinSEA.BlockedNPC.length; t++) {
                                if (idd == OdinSEA.BlockedNPC[t]) {
                                    bAdd = false;
                                    break;
                                }
                            }
                            if (bAdd) {
                                map.addMapObject(myLife);
                            }
                        }
                    }
                }
                addAreaBossSpawn(map);
                map.loadMonsterRate(true);
                //load reactor data
                String id;
                if (reactors && mapData.getChildByPath("reactor") != null) {
                    for (WzData reactor : mapData.getChildByPath("reactor")) {
                        id = WzDataTool.getString(reactor.getChildByPath("id"));
                        if (id != null) {
                            map.spawnReactor(loadReactor(reactor, id, (byte) WzDataTool.getInt(reactor.getChildByPath("f"), 0)));
                        }
                    }
                }
                try {
                    map.setMapName(WzDataTool.getString("mapName", nameData.getChildByPath(getMapStringName(omapid)), ""));
                    map.setStreetName(WzDataTool.getString("streetName", nameData.getChildByPath(getMapStringName(omapid)), ""));
                } catch (Exception e) {
                    map.setMapName("");
                    map.setStreetName("");
                }
                map.setClock(mapData.getChildByPath("clock") != null);
                map.setEverlast(mapData.getChildByPath("info/everlast") != null);
                map.setTown(mapData.getChildByPath("info/town") != null);
                map.setPersonalShop(mapData.getChildByPath("info/personalShop") != null);
                map.setHPDec(WzDataTool.getInt(mapData.getChildByPath("info/decHP"), 0));
                map.setHPDecProtect(WzDataTool.getInt(mapData.getChildByPath("info/protectItem"), 0));
                map.setForcedReturnMap(WzDataTool.getInt(mapData.getChildByPath("info/forcedReturn"), 999999999));
                map.setTimeLimit(WzDataTool.getInt(mapData.getChildByPath("info/timeLimit"), -1));
                map.setFieldLimit(WzDataTool.getInt(mapData.getChildByPath("info/fieldLimit"), 0));
                map.setFirstUserEnter(WzDataTool.getString(mapData.getChildByPath("info/onFirstUserEnter"), ""));
                map.setUserEnter(WzDataTool.getString(mapData.getChildByPath("info/onUserEnter"), ""));
                map.setRecoveryRate(WzDataTool.getFloat(mapData.getChildByPath("info/recovery"), 1));
                map.setCreateMobInterval((short) WzDataTool.getInt(mapData.getChildByPath("info/createMobInterval"), 9000));
                maps.put(omapid, map);
            }
        }
        return map;
    }

    public GameMap getInstanceMap(final int instanceid) {
        return instanceMap.get(instanceid);
    }

    public void removeInstanceMap(final int instanceid) {
        instanceMap.remove(instanceid);
    }

    public GameMap CreateInstanceMap(int mapid, boolean respawns, boolean npcs, boolean reactors, int instanceid) {
        WzData mapData = source.getData(getMapName(mapid));
        WzData link = mapData.getChildByPath("info/link");
        if (link != null) {
            mapData = source.getData(getMapName(WzDataTool.getIntConvert("info/link", mapData)));
        }

        float monsterRate = 0;
        if (respawns) {
            WzData mobRate = mapData.getChildByPath("info/mobRate");
            if (mobRate != null) {
                monsterRate = ((Float) mobRate.getData()).floatValue();
            }
        }
        GameMap map = new GameMap(mapid, channel, WzDataTool.getInt("info/returnMap", mapData), monsterRate, world);


        PortalFactory portalFactory = new PortalFactory();
        for (WzData portal : mapData.getChildByPath("portal")) {
            map.addPortal(portalFactory.makePortal(WzDataTool.getInt(portal.getChildByPath("pt")), portal));
        }
        List<Foothold> allFootholds = new LinkedList<Foothold>();
        Point lBound = new Point();
        Point uBound = new Point();
        for (WzData footRoot : mapData.getChildByPath("foothold")) {
            for (WzData footCat : footRoot) {
                for (WzData footHold : footCat) {
                    Foothold fh = new Foothold(new Point(
                            WzDataTool.getInt(footHold.getChildByPath("x1")), WzDataTool.getInt(footHold.getChildByPath("y1"))), new Point(
                            WzDataTool.getInt(footHold.getChildByPath("x2")), WzDataTool.getInt(footHold.getChildByPath("y2"))), Integer.parseInt(footHold.getName()));
                    fh.setPrev((short) WzDataTool.getInt(footHold.getChildByPath("prev")));
                    fh.setNext((short) WzDataTool.getInt(footHold.getChildByPath("next")));

                    if (fh.getX1() < lBound.x) {
                        lBound.x = fh.getX1();
                    }
                    if (fh.getX2() > uBound.x) {
                        uBound.x = fh.getX2();
                    }
                    if (fh.getY1() < lBound.y) {
                        lBound.y = fh.getY1();
                    }
                    if (fh.getY2() > uBound.y) {
                        uBound.y = fh.getY2();
                    }
                    allFootholds.add(fh);
                }
            }
        }
        FootholdTree fTree = new FootholdTree(lBound, uBound);
        for (Foothold fh : allFootholds) {
            fTree.insert(fh);
        }
        map.setFootholds(fTree);
        // load areas (EG PQ platforms)
        if (mapData.getChildByPath("area") != null) {
            int x1, y1, x2, y2;
            Rectangle mapArea;
            for (WzData area : mapData.getChildByPath("area")) {
                x1 = WzDataTool.getInt(area.getChildByPath("x1"));
                y1 = WzDataTool.getInt(area.getChildByPath("y1"));
                x2 = WzDataTool.getInt(area.getChildByPath("x2"));
                y2 = WzDataTool.getInt(area.getChildByPath("y2"));
                mapArea = new Rectangle(x1, y1, (x2 - x1), (y2 - y1));
                map.addMapleArea(mapArea);
            }
        }
        int bossid = -1;
        String msg = null;
        if (mapData.getChildByPath("info/timeMob") != null) {
            bossid = WzDataTool.getInt(mapData.getChildByPath("info/timeMob/id"), 0);
            msg = WzDataTool.getString(mapData.getChildByPath("info/timeMob/message"), null);
        }
        // load life data (npc, monsters)
        String type;
        AbstractLoadedGameLife myLife;

        for (WzData life : mapData.getChildByPath("life")) {
            type = WzDataTool.getString(life.getChildByPath("type"));
            if (npcs || !type.equals("n")) {
                myLife = loadLife(life, WzDataTool.getString(life.getChildByPath("id")), type);

                if (myLife instanceof Monster) {
                    final Monster mob = (Monster) myLife;

                    map.addMonsterSpawn(mob,
                                        WzDataTool.getInt("mobTime", life, 0),
                                        (byte) WzDataTool.getInt("team", life, -1),
                                        mob.getId() == bossid ? msg : null);

                } else {
                    map.addMapObject(myLife);
                }
            }
        }
        addAreaBossSpawn(map);
        map.loadMonsterRate(true);
        //load reactor data
        String id;
        if (reactors && mapData.getChildByPath("reactor") != null) {
            for (WzData reactor : mapData.getChildByPath("reactor")) {
                id = WzDataTool.getString(reactor.getChildByPath("id"));
                if (id != null) {
                    map.spawnReactor(loadReactor(reactor, id, (byte) WzDataTool.getInt(reactor.getChildByPath("f"), 0)));
                }
            }
        }
        try {
            map.setMapName(WzDataTool.getString("mapName", nameData.getChildByPath(getMapStringName(mapid)), ""));
            map.setStreetName(WzDataTool.getString("streetName", nameData.getChildByPath(getMapStringName(mapid)), ""));
        } catch (Exception e) {
            map.setMapName("");
            map.setStreetName("");
        }
        map.setClock(mapData.getChildByPath("clock") != null);
        map.setEverlast(mapData.getChildByPath("info/everlast") != null);
        map.setTown(mapData.getChildByPath("info/town") != null);
        map.setHPDec(WzDataTool.getInt(mapData.getChildByPath("info/decHP"), 0));
        map.setHPDecProtect(WzDataTool.getInt(mapData.getChildByPath("info/protectItem"), 0));
        map.setForcedReturnMap(WzDataTool.getInt(mapData.getChildByPath("info/forcedReturn"), 999999999));
        map.setTimeLimit(WzDataTool.getInt(mapData.getChildByPath("info/timeLimit"), -1));
        map.setFieldLimit(WzDataTool.getInt(mapData.getChildByPath("info/fieldLimit"), 0));
        map.setFirstUserEnter(WzDataTool.getString(mapData.getChildByPath("info/onFirstUserEnter"), ""));
        map.setUserEnter(WzDataTool.getString(mapData.getChildByPath("info/onUserEnter"), ""));
        map.setRecoveryRate(WzDataTool.getFloat(mapData.getChildByPath("info/recovery"), 1));
        map.setCreateMobInterval((short) WzDataTool.getInt(mapData.getChildByPath("info/createMobInterval"), 9000));

        instanceMap.put(instanceid, map);
        return map;
    }

    public int getLoadedMaps() {
        return maps.size();
    }

    public boolean isMapLoaded(int mapId) {
        return maps.containsKey(mapId);
    }

    public boolean isInstanceMapLoaded(int instanceid) {
        return instanceMap.containsKey(instanceid);
    }

    public void clearLoadedMap() {
        maps.clear();
    }

    public Map<Integer, GameMap> getMaps() {
        return maps;
    }

    private AbstractLoadedGameLife loadLife(WzData life, String id, String type) {
        AbstractLoadedGameLife myLife = LifeFactory.getLife(Integer.parseInt(id), type);
        myLife.setCy(WzDataTool.getInt(life.getChildByPath("cy")));
        WzData dF = life.getChildByPath("f");
        if (dF != null) {
            myLife.setF(WzDataTool.getInt(dF));
        }
        myLife.setFh(WzDataTool.getInt(life.getChildByPath("fh")));
        myLife.setRx0(WzDataTool.getInt(life.getChildByPath("rx0")));
        myLife.setRx1(WzDataTool.getInt(life.getChildByPath("rx1")));
        myLife.setPosition(new Point(WzDataTool.getInt(life.getChildByPath("x")), WzDataTool.getInt(life.getChildByPath("y"))));

        if (WzDataTool.getInt("hide", life, 0) == 1) {
            myLife.setHide(true);
            //		} else if (hide > 1) {
            //			System.err.println("Hide > 1 ("+ hide +")");
        }
        return myLife;
    }

    private Reactor loadReactor(final WzData reactor, final String id, final byte FacingDirection) {
        final ReactorStats stats = ReactorFactory.getReactor(Integer.parseInt(id));
        final Reactor myReactor = new Reactor(stats, Integer.parseInt(id));

        stats.setFacingDirection(FacingDirection);
        myReactor.setPosition(new Point(WzDataTool.getInt(reactor.getChildByPath("x")), WzDataTool.getInt(reactor.getChildByPath("y"))));
        myReactor.setDelay(WzDataTool.getInt(reactor.getChildByPath("reactorTime")) * 1000);
        myReactor.setState((byte) 0);
        myReactor.setName(WzDataTool.getString(reactor.getChildByPath("name"), ""));

        return myReactor;
    }

    private String getMapName(int mapid) {
        String mapName = StringUtil.getLeftPaddedStr(Integer.toString(mapid), '0', 9);
        StringBuilder builder = new StringBuilder("Map/Map");
        builder.append(mapid / 100000000);
        builder.append("/");
        builder.append(mapName);
        builder.append(".img");

        mapName = builder.toString();
        return mapName;
    }

    private String getMapStringName(int mapid) {
        StringBuilder builder = new StringBuilder();
        if (mapid < 100000000) {
            builder.append("maple");
        } else if (mapid >= 100000000 && mapid < 200000000) {
            builder.append("victoria");
        } else if (mapid >= 200000000 && mapid < 300000000) {
            builder.append("ossyria");
        } else if (mapid >= 540000000 && mapid < 541010110) {
            builder.append("singapore");
        } else if (mapid >= 600000000 && mapid < 620000000) {
            builder.append("MasteriaGL");
        } else if (mapid >= 670000000 && mapid < 682000000) {
            builder.append("weddingGL");
        } else if (mapid >= 682000000 && mapid < 683000000) {
            builder.append("HalloweenGL");
        } else if (mapid >= 800000000 && mapid < 900000000) {
            builder.append("jp");
        } else {
            builder.append("etc");
        }
        builder.append("/");
        builder.append(mapid);

        return builder.toString();
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    private void addAreaBossSpawn(final GameMap map) {
        int monsterid = -1;
        int mobtime = -1;
        String msg = null;
        Point pos1 = null, pos2 = null, pos3 = null;

        switch (map.getId()) {
            case 104000400: // Mano
                mobtime = 2700;
                monsterid = 2220000;
                msg = "A cool breeze was felt when Mano appeared.";
                pos1 = new Point(439, 185);
                pos2 = new Point(301, -85);
                pos3 = new Point(107, -355);
                break;
            case 101030404: // Stumpy
                mobtime = 2700;
                monsterid = 3220000;
                msg = "Stumpy has appeared with a stumping sound that rings the Stone Mountain.";
                pos1 = new Point(867, 1282);
                pos2 = new Point(810, 1570);
                pos3 = new Point(838, 2197);
                break;
            case 110040000: // King Clang
                mobtime = 1200;
                monsterid = 5220001;
                msg = "A strange turban shell has appeared on the beach.";
                pos1 = new Point(-355, 179);
                pos2 = new Point(-1283, -113);
                pos3 = new Point(-571, -593);
                break;
            case 250010304: // Tae Roon
                mobtime = 2100;
                monsterid = 7220000;
                msg = "Tae Roon appeared with a loud grow.";
                pos1 = new Point(-210, 33);
                pos2 = new Point(-234, 393);
                pos3 = new Point(-654, 33);
                break;
            case 200010300: // Eliza
                mobtime = 1200;
                monsterid = 8220000;
                msg = "Eliza has appeared with a black whirlwind.";
                pos1 = new Point(665, 83);
                pos2 = new Point(672, -217);
                pos3 = new Point(-123, -217);
                break;
            case 250010503: // Ghost Priest
                mobtime = 1800;
                monsterid = 7220002;
                msg = "The area fills with an unpleasant force of evil.. even the occasional ones of the cats sound disturbing";
                pos1 = new Point(-303, 543);
                pos2 = new Point(227, 543);
                pos3 = new Point(719, 543);
                break;
            case 222010310: // Old Fox
                mobtime = 2700;
                monsterid = 7220001;
                msg = "As the moon light dims,a long fox cry can be heard and the presence of the old fox can be felt.";
                pos1 = new Point(-169, -147);
                pos2 = new Point(-517, 93);
                pos3 = new Point(247, 93);
                break;
            case 107000300: // Dale
                mobtime = 1800;
                monsterid = 6220000;
                msg = "The huge crocodile Dale has come out from the swamp.";
                pos1 = new Point(710, 118);
                pos2 = new Point(95, 119);
                pos3 = new Point(-535, 120);
                break;
            case 100040105: // Faust
                mobtime = 1800;
                monsterid = 5220002;
                msg = "The blue fog became darker when Faust appeared.";
                pos1 = new Point(1000, 278);
                pos2 = new Point(557, 278);
                pos3 = new Point(95, 278);
                break;
            case 100040106: // Faust
                mobtime = 1800;
                monsterid = 5220002;
                msg = "The blue fog became darker when Faust appeared.";
                pos1 = new Point(1000, 278);
                pos2 = new Point(557, 278);
                pos3 = new Point(95, 278);
                break;
            case 220050100: // Timer
                mobtime = 1500;
                monsterid = 5220003;
                msg = "Click clock! Timer has appeared with an irregular clock sound.";
                pos1 = new Point(-467, 1032);
                pos2 = new Point(532, 1032);
                pos3 = new Point(-47, 1032);
                break;
            case 221040301: // Jeno
                mobtime = 2400;
                monsterid = 6220001;
                msg = "Jeno has appeared with a heavy sound of machinery.";
                pos1 = new Point(-4134, 416);
                pos2 = new Point(-4283, 776);
                pos3 = new Point(-3292, 776);
                break;
            case 240040401: // Lev
                mobtime = 7200;
                monsterid = 8220003;
                msg = "Leviathan has appeared with a cold wind from over the gorge.";
                pos1 = new Point(-15, 2481);
                pos2 = new Point(127, 1634);
                pos3 = new Point(159, 1142);
                break;
            case 260010201: // Dewu
                mobtime = 3600;
                monsterid = 3220001;
                msg = "Dewu slowly appeared out of the sand dust.";
                pos1 = new Point(-215, 275);
                pos2 = new Point(298, 275);
                pos3 = new Point(592, 275);
                break;
            case 261030000: // Chimera
                mobtime = 2700;
                monsterid = 8220002;
                msg = "Chimera has appeared out of the darkness of the underground with a glitter in her eyes.";
                pos1 = new Point(-1094, -405);
                pos2 = new Point(-772, -116);
                pos3 = new Point(-108, 181);
                break;
            case 230020100: // Sherp
                mobtime = 2700;
                monsterid = 4220000;
                msg = "A strange shell has appeared from a grove of seaweed.";
                pos1 = new Point(-291, -20);
                pos2 = new Point(-272, -500);
                pos3 = new Point(-462, 640);
                break;
            /*case 910000000: // FM
            mobtime = 300;
            monsterid = 9420015;
            msg = "NooNoo has appeared out of anger, it seems that NooNoo is stuffed with Christmas gifts!";
            pos1 = new Point(498, 4);
            pos2 = new Point(498, 4);
            pos3 = new Point(498, 4);
            break;*/
            case 209000000: // Happyvile
                mobtime = 300;
                monsterid = 9500317;
                msg = "Little Snowman has appeared!";
                pos1 = new Point(-115, 154);
                pos2 = new Point(-115, 154);
                pos3 = new Point(-115, 154);
                break;
            default:
                return;
        }
        map.addAreaMonsterSpawn(
                LifeFactory.getMonster(monsterid),
                pos1, pos2, pos3,
                mobtime,
                msg);
    }
}