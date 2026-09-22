# The Book of Normies

Grimoire **server-side only** style Famille Addams : liste automatiquement toutes les commandes du serveur dans un livre écrit vanilla.

| Version | Loader | JAR |
|---------|--------|-----|
| **1.21.1** | NeoForge | racine → `gradlew.bat build` |
| **1.20.1** | Forge | `forge-1.20.1/` → `gradlew.bat build` |

Clients vanilla ou pack (ATM, etc.) : **pas besoin** d’installer ce mod.

## Installation

1. Prendre le JAR adapté à ton loader / version MC
2. Le placer dans `mods/` du **serveur**
3. Au premier démarrage : `config/book_of_normies.json`
4. En jeu : `/bookn` (OP ou rôles LuckPerms)

## Config

```json
{
  "permission_level": 2,
  "allowed_roles": ["admin", "moderator"],
  "priority_mods": ["minecraft", "worldedit", "luckperms"],
  "commands_per_page": 7
}
```

## Build

```bat
rem NeoForge 1.21.1
gradlew.bat build

rem Forge 1.20.1
cd forge-1.20.1
gradlew.bat build
```
