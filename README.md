# The Book of Normies

Mod **NeoForge 1.21.1** server-side only. Ouvre un grimoire style Famille Addams listant automatiquement toutes les commandes du serveur (vanilla + mods), utilisable avec des clients vanilla ou un modpack type ATM.

## Installation

1. Serveur NeoForge **1.21.1**
2. Placer `book_of_normies-1.0.0.jar` dans `mods/`
3. Les clients n’ont **pas** besoin du mod
4. Au premier démarrage : `config/book_of_normies.json`

## Usage

```
/bookn
```

Réservé aux OP (niveau configurable) ou aux groupes LuckPerms listés dans `allowed_roles`.

## Config

```json
{
  "permission_level": 2,
  "allowed_roles": ["admin", "moderator"],
  "priority_mods": ["minecraft", "worldedit", "luckperms"],
  "commands_per_page": 7
}
```

- Première section : commandes Minecraft, titre **The Book of Normies**
- Ensuite : `priority_mods` dans l’ordre exact
- Puis les autres mods ayant des commandes, ordre alphabétique

## Build

```bat
gradlew.bat build
```

JAR : `build/libs/book_of_normies-1.0.0.jar`

## Note versions

- **v1 :** NeoForge 1.21.1 (ATM10, etc.)
- **Phase 2 :** Forge 1.20.1
