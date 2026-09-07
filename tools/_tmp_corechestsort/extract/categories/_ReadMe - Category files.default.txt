# NOTE: This build uses a classic ChestSort-like 900-series ordering.
# Categories shipped:
# 900-weapons, 905-common-tools (includes buckets), 907-other-tools, 909-food, 910-valuables,
# 920-armor-and-arrows, 930-brewing, 950-redstone, 960-wood, 970-stone, 980-plants, 981-corals,
# plus extended categories:
# 990-building, 995-decorations, 997-functional (includes transport), 999-mobs.

#################
# CoreChestSort #
#################

CoreChestsort uses simple .txt files to define categories.

Category names are determined by the file names.
Files must start with a numeric prefix (000–999) and end with .txt.

Lower numbers are sorted earlier.
Example:
900-weapons.default.txt
910-valuables.default.txt

The numeric prefix defines the category priority.

Default categories in this build use the 900–999 range
to replicate classic ChestSort ordering.

If you want to customize a category:
- Copy or rename the .default.txt file
- Remove ".default" from the filename
- Edit the new file

Files ending with ".default.txt" may be overwritten during updates.
Custom files without ".default" will never be touched.

If you use {category} in your sorting-method,
it will be replaced with the category name.

If you use {keepCategoryOrder} after {category},
items will be ordered exactly as listed in the category file.

Otherwise:
Items are grouped by category,
then sorted using the remaining sorting-method variables.

Wildcards:
You can use * at the beginning and/or end of a line.
Example:
*_log
*_bucket
*_sword

Comments:
Lines starting with # are ignored.

# Extra (CoreChestsort features):

priority=<number>  -> override numeric prefix priority
id=<name>          -> optional internal category id
tag:<bukkitTag>    -> match Bukkit/Paper material tags (future-proof)
                      Example: tag:logs, tag:planks
