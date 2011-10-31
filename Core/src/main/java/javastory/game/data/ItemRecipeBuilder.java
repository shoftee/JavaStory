package javastory.game.data;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * 
 * @author shoftee
 */
public class ItemRecipeBuilder {
	private final List<ItemRecipeEntry> entries;

	public ItemRecipeBuilder() {
		this.entries = Lists.newLinkedList();
	}

	public void addEntry(final int itemId, final int quantity) {
		this.entries.add(new ItemRecipeEntry(itemId, quantity));
	}

	public ItemRecipe build() {
		return new ConcreteItemRecipe(this.entries);
	}

	private static class ConcreteItemRecipe implements ItemRecipe {

		public List<ItemRecipeEntry> entries;

		public ConcreteItemRecipe(final List<ItemRecipeEntry> entries) {
			this.entries = Lists.newArrayList(entries);
		}

		@Override
		public Iterator<ItemRecipeEntry> iterator() {
			return this.entries.listIterator();
		}

	}
}
