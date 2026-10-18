package io.github.saboooor.shelfview;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class PreviewItemsTest {
    private ItemStack item(String identity) {
        ItemStack stack = mock(ItemStack.class, identity);
        when(stack.isSimilar(any())).thenAnswer(call ->
                mockingDetails(call.getArgument(0)).getMockCreationSettings().getMockName().toString().equals(identity));
        when(stack.clone()).thenAnswer(call -> mock(ItemStack.class, identity + " preview"));
        return stack;
    }

    @Test
    void skipsEmptySlotsAndDuplicateStacksWithoutChangingSource() {
        ItemStack diamond = item("diamond");
        ItemStack duplicate = item("diamond");
        ItemStack gold = item("gold");
        ItemStack air = item("air");
        when(air.isEmpty()).thenReturn(true);

        List<ItemStack> preview = PreviewItems.select(new ItemStack[]{null, air, diamond, duplicate, gold}, 0);

        assertEquals(2, preview.size());
        verify(diamond).clone();
        verify(gold).clone();
        verify(duplicate, never()).clone();
        verify(diamond, never()).setAmount(anyInt());
        preview.forEach(stack -> verify(stack).setAmount(1));
    }

    @Test
    void twoShelvesShowSixDistinctItemsAcrossDuplicateFilledInventory() {
        ItemStack a = item("a"), b = item("b"), c = item("c");
        ItemStack d = item("d"), e = item("e"), f = item("f"), g = item("g");
        ItemStack[] contents = {a, item("a"), b, c, item("b"), d, e, f, g};

        assertEquals(3, PreviewItems.select(contents, 0).size());
        verify(a).clone();
        verify(b).clone();
        verify(c).clone();
        verify(d, never()).clone();
        assertEquals(3, PreviewItems.select(contents, 3).size());
        verify(d).clone();
        verify(e).clone();
        verify(f).clone();
        verify(g, never()).clone();
    }

    @Test
    void keepsDifferentMetadataAndLeavesUnusedSecondShelfSlotsEmpty() {
        ItemStack[] contents = {item("plain sword"), item("named sword"), item("enchanted sword"), item("apple")};
        assertEquals(3, PreviewItems.select(contents, 0).size());
        assertEquals(1, PreviewItems.select(contents, 3).size());
        assertTrue(PreviewItems.select(new ItemStack[]{contents[0]}, 3).isEmpty());
        assertTrue(PreviewItems.select(new ItemStack[0], 0).isEmpty());
    }
}
