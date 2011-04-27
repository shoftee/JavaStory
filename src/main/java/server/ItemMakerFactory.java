package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.javastory.server.maker.ItemRecipe;
import org.javastory.server.maker.ItemRecipeBuilder;
import org.javastory.server.maker.RandomRewardFactory;
import org.javastory.server.maker.RandomRewardList;

import provider.WzData;
import provider.WzDataProviderFactory;
import provider.WzDataTool;

public final class ItemMakerFactory {

    private final static ItemMakerFactory instance = new ItemMakerFactory();
    protected Map<Integer, MakerItemInfo> itemCache = new HashMap<>();
    protected Map<Integer, GemInfo> gemCache = new HashMap<>();

    public static ItemMakerFactory getInstance() {
        // DO ItemMakerFactory.getInstance() on ChannelServer startup.
        return instance;
    }

    private ItemMakerFactory() {
        System.out.println(":: Loading ItemMakerFactory ::");
        // 0 = Item upgrade crystals
        // 1 / 2/ 4/ 8 = Item creation

        final WzData info = WzDataProviderFactory.getDataProvider("Etc.wz").getData("ItemMake.img");

        for (WzData directory : info.getChildren()) {
            int type = Integer.parseInt(directory.getName());
            switch (type) {
                case 0: { // Caching of gem

                    break;
                }
                case 1: // Warrior
                case 2: // Magician
                case 4: // Bowman
                case 8: // Thief
                case 16: { // Pirate
                    cacheItems(directory);
                    break;
                }
            }
        }
    }

    private void cacheGems(WzData directory) {
        byte reqMakerLevel;
        int reqLevel, cost, quantity;
        for (WzData gemDirectory : directory.getChildren()) {
            reqLevel = WzDataTool.getInt("reqLevel", gemDirectory, 0);
            reqMakerLevel = (byte) WzDataTool.getInt("reqSkillLevel", gemDirectory, 0);
            cost = WzDataTool.getInt("meso", gemDirectory, 0);
            quantity = WzDataTool.getInt("itemNum", gemDirectory, 0);

            RandomRewardList rewards = new RandomRewardList();
            ItemRecipeBuilder recipe = new ItemRecipeBuilder();
            for (WzData gemInfo : gemDirectory.getChildren()) {
                for (WzData ind : gemInfo.getChildren()) {
                    switch (gemInfo.getName()) {
                        case "randomReward":
                            rewards.addEntry(WzDataTool.getInt("prob", ind, 0), WzDataTool.getInt("item", ind, 0));
                            break;
                        case "recipe":
                            recipe.addEntry(WzDataTool.getInt("item", ind, 0), WzDataTool.getInt("count", ind, 0));
                            break;
                    }
                }
            }
            gemCache.put(Integer.parseInt(gemDirectory.getName()),
                         new GemInfo(rewards.build(), recipe.build(), cost, reqLevel, reqMakerLevel, quantity));
        }
    }

    private void cacheItems(WzData directory) {
        byte totalupgrades, reqMakerLevel;
        int reqLevel, cost, quantity, stimulator;
        for (WzData itemFolder : directory.getChildren()) {
            reqLevel = WzDataTool.getInt("reqLevel", itemFolder, 0);
            reqMakerLevel = (byte) WzDataTool.getInt("reqSkillLevel", itemFolder, 0);
            cost = WzDataTool.getInt("meso", itemFolder, 0);
            quantity = WzDataTool.getInt("itemNum", itemFolder, 0);
            totalupgrades = (byte) WzDataTool.getInt("tuc", itemFolder, 0);
            stimulator = WzDataTool.getInt("catalyst", itemFolder, 0);

            ItemRecipeBuilder recipe = new ItemRecipeBuilder();

            for (WzData recipeFolder : itemFolder.getChildren()) {
                for (WzData ind : recipeFolder.getChildren()) {
                    if (recipeFolder.getName().equals("recipe")) {
                        recipe.addEntry(WzDataTool.getInt("item", ind, 0), WzDataTool.getInt("count", ind, 0));
                    }
                }
            }
            itemCache.put(Integer.parseInt(itemFolder.getName()), 
                            new MakerItemInfo(recipe.build(), cost, reqLevel, reqMakerLevel, quantity, totalupgrades, stimulator));
        }
    }

    public GemInfo getGemInfo(int itemid) {
        return gemCache.get(itemid);
    }

    public MakerItemInfo getItemInfo(int itemid) {
        return itemCache.get(itemid);
    }

    public static class GemInfo {

        private int reqLevel, reqMakerLevel;
        private int cost, quantity;
        private RandomRewardFactory rewards;
        private ItemRecipe recipe;

        public GemInfo(RandomRewardFactory rewards, ItemRecipe recipe,
                int cost, int reqLevel, int reqMakerLevel, int quantity) {
            this.rewards = rewards;
            this.recipe = recipe;

            this.cost = cost;
            this.reqLevel = reqLevel;
            this.reqMakerLevel = reqMakerLevel;
            this.quantity = quantity;
        }

        public int getManufacturedQuantity() {
            return quantity;
        }

        public ItemRecipe getRecipe() {
            return recipe;
        }

        public int getRequiredLevel() {
            return reqLevel;
        }

        public int getRequiredSkillLevel() {
            return reqMakerLevel;
        }

        public int getCost() {
            return cost;
        }

        public int chooseRandomReward() {
            return rewards.getRandomItem();
        }
    }

    public static class MakerItemInfo {

        private int reqLevel;
        private int cost, quantity, stimulator;
        private byte tuc, reqMakerLevel;
        private ItemRecipe recipe;
        private List<Integer> reqEquips = new ArrayList<Integer>();

        public MakerItemInfo(ItemRecipe recipe, int cost, int reqLevel, byte reqMakerLevel, int quantity, byte tuc, int stimulator) {
            this.recipe = recipe;

            this.cost = cost;
            this.tuc = tuc;
            this.reqLevel = reqLevel;
            this.reqMakerLevel = reqMakerLevel;
            this.quantity = quantity;
            this.stimulator = stimulator;
        }

        public byte getTUC() {
            return tuc;
        }

        public int getManufacturedQuantity() {
            return quantity;
        }

        public ItemRecipe getRecipe() {
            return this.recipe;
        }

        public List<Integer> getReqEquips() {
            return reqEquips;
        }

        public int getRequiredLevel() {
            return reqLevel;
        }

        public byte getRequiredSkillLevel() {
            return reqMakerLevel;
        }

        public int getCost() {
            return cost;
        }

        public int getStimulator() {
            return stimulator;
        }
    }
}
