# The Book of Normies

Interface NeoForge (style Famille Addams) listant **toutes les commandes du serveur**, triées par mod.

| Version | Loader | JAR |
|---------|--------|-----|
| **1.21.1** | NeoForge | racine → `gradlew.bat build` |
| **1.20.1** | Forge | `forge-1.20.1/` (livre — port GUI à venir) |

## Important (1.21.1)

Le vrai menu GUI nécessite le mod **sur le client et le serveur** (NeoForge seulement, aucune autre dépendance).  
Sur ATM : ajoute le JAR au pack client + serveur.

## Usage

```
/bookn
```

- Colonne gauche : mods (minecraft → priorités config → alpha)
- Colonne droite : commandes du mod sélectionné
- Params : bouton cycle `[option ⟳]` ou champ libre `[param]`
- Confirmation avant exécution si paramètres

## Config

`config/book_of_normies.json` (serveur) :

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
gradlew.bat build
```

JAR : `build/libs/book_of_normies-neoforge-1.21.1-1.0.2.jar`
