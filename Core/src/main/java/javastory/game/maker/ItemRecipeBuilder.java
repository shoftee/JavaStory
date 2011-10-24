package javastory.game.maker;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * 
 * @author shoftee
 */
public class ItemRecipeBuilder {
	private List<ItemRecipeEntry> entries;

	public ItemRecipeBuilder() {
		this.entries = Lists.newLinkedList();
	}

	public void addEntry(int itemId, int quantity) {
		this.entries.add(new ItemRecipeEntry(itemId, quantity));
	}

	public ItemRecipe build() {
		return new ConcreteItemRecipe(entries);
	}

	private static class ConcreteItemRecipe implements ItemRecipe {

		public List<ItemRecipeEntry> entries;

		public ConcreteItemRecipe(List<ItemRecipeEntry> entries) {
			this.entries = Lists.newArrayList(entries);
		}

		@Override
		public Iterator<ItemRecipeEntry> iterator() {
			return this.entries.listIterator();
		}

	}
}
