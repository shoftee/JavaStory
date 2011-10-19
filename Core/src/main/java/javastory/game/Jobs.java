/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javastory.game;

/**
 *
 * @author Tosho
 */
public final class Jobs {

    private Jobs() {
    }

    public static boolean isBeginner(int jobId) {
        return jobId == 0 || jobId == 1000 || jobId == 2000 || jobId == 2001;
    }

    public static boolean isJobFamily(final int baseJob, final int currentJob) {
        return currentJob >= baseJob && currentJob / 100 == baseJob / 100;
    }

    public static boolean isAdventurer(final int job) {
        return job >= 0 && job < 1000;
    }

    public static boolean isCygnus(final int job) {
        return job >= 1000 && job < 2000;
    }

    public static boolean isAran(final int job) {
        return job >= 2000 && job <= 2112 && job != 2001;
    }

    public static boolean isEvan(final int job) {
        return job == 2001 || (job >= 2200 && job <= 2218);
    }

    public static String getJobNameById(int job) {
        switch (job) {
            case 100:
                return "Warrior";
            case 110:
                return "Fighter";
            case 111:
                return "Crusader";
            case 112:
                return "Hero";
            case 120:
                return "Page";
            case 121:
                return "White Knight";
            case 122:
                return "Paladin";
            case 130:
                return "Spearman";
            case 131:
                return "Dragon Knight";
            case 132:
                return "Dark Knight";

            case 200:
                return "Magician";
            case 210:
                return "Wizard(Fire,Poison)";
            case 211:
                return "Mage(Fire,Poison)";
            case 212:
                return "Arch Mage(Fire,Poison)";
            case 220:
                return "Wizard(Ice,Lightning)";
            case 221:
                return "Mage(Ice,Lightning)";
            case 222:
                return "Arch Mage(Ice,Lightning)";
            case 230:
                return "Cleric";
            case 231:
                return "Priest";
            case 232:
                return "Bishop";

            case 300:
                return "Archer";
            case 310:
                return "Hunter";
            case 311:
                return "Ranger";
            case 312:
                return "Bowmaster";
            case 320:
                return "Crossbow man";
            case 321:
                return "Sniper";
            case 322:
                return "Crossbow Master";

            case 400:
                return "Rogue";
            case 410:
                return "Assassin";
            case 411:
                return "Hermit";
            case 412:
                return "Night Lord";
            case 420:
                return "Bandit";
            case 421:
                return "Chief Bandit";
            case 422:
                return "Shadower";

            case 500:
                return "Pirate";
            case 510:
                return "Infighter";
            case 511:
                return "Buccaneer";
            case 512:
                return "Viper";
            case 520:
                return "Gunslinger";
            case 521:
                return "Valkyrie";
            case 522:
                return "Captain";

            case 1100:
                return "Soul Master";
            case 1110:
                return "Soul Master";
            case 1111:
                return "Soul Master";
            case 1112:
                return "Soul Master";

            case 1200:
                return "Flame Wizard";
            case 1210:
                return "Flame Wizard";
            case 1211:
                return "Flame Wizard";
            case 1212:
                return "Flame Wizard";

            case 1300:
                return "Wind Breaker";
            case 1310:
                return "Wind Breaker";
            case 1311:
                return "Wind Breaker";
            case 1312:
                return "Wind Breaker";

            case 1400:
                return "Night Walker";
            case 1410:
                return "Night Walker";
            case 1411:
                return "Night Walker";
            case 1412:
                return "Night Walker";

            case 1500:
                return "Striker";
            case 1510:
                return "Striker";
            case 1511:
                return "Striker";
            case 1512:
                return "Striker";

            case 2100:
                return "Aran";
            case 2110:
                return "Aran";
            case 2111:
                return "Aran";
            case 2112:
                return "Aran";

            default:
                return "Unknown Job";
        }
    }
}
