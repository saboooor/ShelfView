# ShelfView

Paper **26.3**, Java **25**. Uses vanilla shelf blocks; no resource pack or client mod.

## Install

Build with `./gradlew build` (Windows: `gradlew.bat build`), copy `build/libs/shelfview-1.0.0.jar` into your server's `plugins/` folder, and restart the server.

## Use

1. Place a chest and put items inside.
2. Place an **empty shelf directly against the front of the chest**, at the same height, facing the same direction as the chest. Sneak while placing it to avoid opening the chest.
3. The shelf shows the first three unique items in the chest, refreshing every half second. Repeated stacks appear only once, with one item per display slot. Items with different names, enchantments, or other metadata remain distinct.
4. Place two adjacent, matching-facing empty shelves across the front of a double chest to display up to six unique items: three on each shelf, without repeats between them. With only one eligible shelf, it shows the first three items from the combined chest inventory.
5. Right-click either shelf to open the linked chest, including while sneaking.

All wood variants work. Shelves without a matching chest, or shelves already holding real items, keep their normal behavior. Remove any stored shelf items before using a shelf as a preview. Breaking either block removes the preview; chest items remain in the chest. No commands or registration are needed, and existing shelves are discovered when their chunks load.

## Behavior and limits

- Preview stacks are sent only to clients. They are never inserted into the shelf inventory, so breaking a shelf, hoppers, and redstone cannot extract preview items.
- A shelf has three display slots; a pair in front of a double chest shares six. Extra unique items beyond that capacity are not shown.
- Locked chests and chests with pending loot tables do not show previews or open through shelves. Open a loot chest normally first.
- Blocked chests cannot be opened through shelves. A chest interaction event is dispatched for protection plugins, and opening also fires the standard inventory event. Check compatibility with your specific protection plugin before using this on a protected server.
- Preview visibility is shared with anyone who can see the shelf, like a public storage label. Region permissions are checked when opening, not when viewing the preview.
- Designed for ordinary Paper servers, not Folia. Shelves placed by external world editors require a chunk reload or plugin restart to be discovered.

## Verification

Build: `./gradlew build` (requires Java 25; the wrapper downloads Gradle automatically).

In-game checks (require a Paper 26.3 server): place shelves in all four directions; test single/double/trapped chests; update contents via players and hoppers; right-click with both hands and while sneaking; break either block; reload chunks and restart; check locked/blocked chests and region protection. Also power the shelf and confirm that no preview stacks enter player inventories.

API references: [Paper project setup](https://docs.papermc.io/paper/dev/project-setup/), [Shelf inventory](https://jd.papermc.io/paper/26.3/org/bukkit/inventory/ShelfInventory.html).
