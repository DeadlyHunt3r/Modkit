# Modkit

**Create your own Minecraft mods in-game — no code required.**

*An in-game modding engine for Minecraft 1.21.1 (NeoForge).*

### What is Modkit?

Modkit is a Minecraft mod that lets you build *other* mods from inside the game,
through a graphical interface — without writing a single line of code.

You open a menu, fill in some fields, click a few buttons, and Modkit produces a
real, standalone `.jar` mod that you can drop into any other instance, share with
friends, or ship in a modpack. The items, blocks, recipes, armor, tools and tweaks
you design behave exactly like content from a hand-written mod, because that is what
they become.

Modkit currently supports creating:

- **Items** (with custom textures, stack size, rarity, tooltips, fuel value)
- **Food** (nutrition, saturation, status effects on eating)
- **Blocks** (hardness, tool requirements, light, per-side textures, drops, XP)
- **Ores** (with world generation)
- **Weapons & Tools** (swords, pickaxes, axes, shovels, hoes — vanilla or custom tiers)
- **Armor sets** (up to four pieces, custom defense, worn textures)
- **Recipes** (all seven vanilla types: shaped, shapeless, smelting, blasting,
  smoking, stonecutting, smithing)
- **Recipe Overrides** (disable or replace any recipe from vanilla *or* other mods)
- **Tags** (put your content into existing tags, use tags as recipe ingredients)

### How it works

Modkit is built around one central idea: **separate the engine from the content.**

There are always two parts in play:

1. **The Modkit base mod** — the engine. It contains all the GUI, the logic that
   reads content definitions, and the runtime classes that turn those definitions
   into real registered items, blocks, and so on.

2. **The mods you create** — pure *content*. When you design something in the GUI,
   Modkit writes a small JSON file describing it (e.g. "an item called `ruby` with
   this texture and a stack size of 16"). On export, those JSON files plus your
   textures are bundled into a `.jar`.

The exported `.jar` does **not** contain compiled Java code. It contains JSON
definitions and assets, and it declares a dependency on the Modkit base mod. When
Minecraft loads, the base mod scans for these definition files — both inside loaded
`.jar`s and in your local workspace folder — and registers everything at runtime.

This is why an exported mod is tiny, builds instantly (there is no compilation step),
and is safe: it cannot contain arbitrary executable code, only data the engine knows
how to read.

#### The workflow in practice

```
You design in GUI
      │
      ▼
JSON written to  .minecraft/modkit/workspaces/<modname>/
      │
      ▼
Export  ──►  .jar in  .minecraft/modkit/exports/
      │
      ▼
Drop the .jar (plus Modkit) into any 1.21.1 NeoForge instance
```

### Why I made the decisions I did

The interesting part of any project is not *what* it does but *why* it is built the
way it is. These are the deliberate choices behind Modkit.

#### Why a separate base mod instead of one self-contained `.jar`?

I could have generated a fully standalone mod that needs nothing else. I chose not
to. Splitting "engine" from "content" means:

- The content `.jar`s are tiny and contain no code — they are just data.
- I can fix bugs and add features in the base mod, and **every** mod made with
  Modkit benefits immediately, without re-exporting.
- It is fundamentally safer to share. A Modkit content mod cannot do anything the
  engine does not explicitly allow, because it has no code of its own.

The cost is that users need the Modkit base mod installed alongside the content
mods. I think that trade-off is clearly worth it.

#### Why JSON + the `lowcodefml` loader, instead of generating and compiling Java?

The "obvious" way to make a mod generator is to spit out Java source and compile it.
I deliberately avoided that:

- Compiling Java in-game would be slow, fragile, and would require a full toolchain.
- Generated code is hard to validate and a natural place for things to go wrong.
- NeoForge already ships `lowcodefml`, a mod loader designed for exactly this — mods
  that are *data only*, with no `@Mod` class and no bytecode.

By describing content as JSON and letting the base mod interpret it at runtime, we
get instant "builds," strong validation (every definition is checked before it is
accepted), and a format that is easy to read, diff, and debug.

#### Why an in-game GUI instead of an external editor?

The whole point is to remove friction for people who do not code. An external tool
would mean leaving the game, learning another program, and dealing with file paths.
Inside the game, you can design something, test it immediately, and iterate — all in
one place. Textures are picked with a native file dialog; everything else is buttons
and fields.

#### Why Recipe Overrides work by "disable + create new" for other mods

Minecraft loads recipes as resources, and the rule is simple: **whoever loads last
wins.** Vanilla always loads first, so overriding a vanilla recipe is reliable. But
against *another mod*, load order is decided roughly alphabetically by mod id — which
is not something you can count on.

So for foreign mods I recommend a robust pattern: **disable** the original recipe
and **create your own new one** with a unique id. Your new recipe has a path no other
mod competes for, so it always loads. To make the *disable* reliable too, Modkit does
something automatic on export:

> When your overrides touch recipes from other mods, Modkit detects which mods those
> are and writes `ordering = "AFTER"` dependencies into the exported mod's
> `mods.toml`. This forces your mod to load after them, so your changes win.

This is the same fundamental limitation every recipe-tweaking tool has (KubeJS,
CraftTweaker, …). Modkit just handles the load-order part for you.

#### Why Tags are *merged*, not managed as standalone files

A tag (e.g. `c:gems`) is a shared, additive list. If you put your `ruby` into
`c:gems`, it should appear *alongside* every other mod's gems — not replace them.
Modkit writes tag files with `"replace": false`, and on export it **merges** all tag
contributions (your manual item/block tags *and* the automatic tool-mining tags)
into one file per tag id, deduplicating entries. You never accidentally wipe another
mod's tag.

I also decided **not** to add a standalone "tag manager." Tags are only useful when
something *uses* them, so instead of a separate screen you simply add tags directly
on the item or block, and use tags as ingredients directly in the recipe editor.

#### What I deliberately leave out

Scope discipline is a feature. Modkit intentionally does **not** try to do everything:

- **No multiblock / large-scale tech machinery** — competing with mods like
  Mekanism is explicitly not the goal.
- **No arbitrary scripting** — Modkit is about safe, structured content, not a
  general programming environment.

Saying no to these keeps Modkit focused, safe, and approachable for its actual
audience: people who want to make real content without becoming programmers.

### Project status

Modkit is in active development. Releases are published on CurseForge. The source
in this repository tracks ongoing work and may be ahead of the latest public build.

Recipe Overrides cooming in the next days

### License

Modkit is licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0)**.
See [LICENSE](LICENSE) for the full text.
