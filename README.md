## Glimpsy Void Survival
Glimpsy is a plugin adding the basic void survival features to papermc. It was built for 1.21.10 but will probably support a larger variaty of versions and/or platforms.

I started this java project not very long ago, it's a pretty strong codebase and I've recently stopped it's development because I got bored. To not see my efforts go to waste I've decided to publish the code publicly so other developers with similar ambitions can continue it's development or base their projects of off this one.
## Contents
There are 2 main packages within the Glimpsy plugin, the features and util (api), this is a list of all features:
### Collections
Collections is a system that allows players to collect a preset of items, after collecting items you get collection points which unlock other parts of the server (like right clicking a crafting table in your inventory opens the menu directly)
### Competitions
Comptetitions are in game events that occur every 40-60 minutes, they occur to engage the playerbase and reward players afterwards (**Rewards aren't fully implemented**)
### Explosive Enchantment
The explosive enchantment was mainly addded because I was fittling with the new papermc datapack api, it's a fun little enchantment and does similar things to trail TNT.
### Help
The help modules are fairly powerful, I provided a system that allows the parsing of articles. An article is a part of content written for the glimpsy in game wiki accessable through `/help`, these are loaded on server startup async. Other help features include a list of commands, custom enchantments and custom recipes.
### Map
The map reset modules are very powerful, the process power needed for a map reset can be fairly large and thus I've landed on an async multi-threaded design. `MapSetter#reset(int)` is the main method, where `int` is the requested threads (this wil cap at all available threads). The plugin devides the map into regions based on the amount of threads provided and then processes each thread separatly, after all threads are done it finishes and does the rest (teleporting players, effects, chat, etc)
### Nametags
Nametags are above player holograms used to display information like: prefix, name, suffix, ping, etc. It uses a util class `Hologram` which provides a bunch of methods for holograms. Nametags also support chat above head similar to roblox (with a faiding effect)
### Nonchest
The nonchest is a publicly accessable chest, `/nonchest`, the contents is saved to the database uppon server shutdown, then loaded after startup.
### Randomitems
Random items are given every 5 seconds. The database stores all disabled items (togglable through `/toggleitems`)
### Recipes
Glimpsy provides a few custom recipes, these are api based, it is very easy to add more.
### Simple
This package contains all simple stuff: blood particles, chat formatting, too expensive anvil removal, interacting with a crafting table if enough collection points, luck bottle creation, tab formatting, tnt trails.
### Voting
This package is very incomplete and contains listening to votes, that's it though, this was the last thing I was working on before I stopped development.
### Wind
Wind within Glimpsy is a cool feature, wind changes every 15m. Fishing in the wind gives your fishing rod 1 more lure level (decrements if you stop fishing in that direction), to avoid dupes this is stored in the PDC of the fishing rod ItemStack.
### API
The api is pretty easy to use and sometimes documented. 

If anyone wishes to contribute or maybe even open source this project, you can decide to make a PR and it might be merged! I will still actively monitor this project (even if I myself don't contribute much)
