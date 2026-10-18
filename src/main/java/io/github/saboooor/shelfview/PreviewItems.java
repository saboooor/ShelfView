package io.github.saboooor.shelfview;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;

final class PreviewItems {
    private PreviewItems() {}

    static List<ItemStack> select(ItemStack[] contents, int offset) {
        List<ItemStack> unique = new ArrayList<>();
        for (ItemStack stack : contents) {
            if (stack == null || stack.isEmpty()) continue;
            // Ignore stack counts, but preserve differences such as names and enchantments.
            if (unique.stream().anyMatch(item -> item.isSimilar(stack))) continue;
            unique.add(stack);
            if (unique.size() == offset + 3) break;
        }
        List<ItemStack> result = new ArrayList<>();
        for (int index = offset; index < unique.size(); index++) {
            ItemStack display = unique.get(index).clone();
            display.setAmount(1);
            result.add(display);
        }
        return result;
    }
}
