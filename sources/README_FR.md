# Sworld Waystones — Minecraft 26.3 / NeoForge 26.3.0.102

Waystones utilisant le modèle Tripo fourni par Jordan, converti en OBJ : 9 823 triangles, texture couleur 1 024 × 1 024. Le rendu utilise le chargeur OBJ de NeoForge, sans bibliothèque de rendu supplémentaire. Les cartes de normal et de métal du GLB ne sont pas utilisées par le rendu standard de Minecraft.

## Mise à jour depuis la version 1.0.0

Fermer Minecraft et arrêter le serveur. Retirer `sworldwaystones-1.0.0.jar` du dossier `mods`, puis mettre uniquement `sworldwaystones-1.1.0.jar` à sa place sur le client et le serveur. Les pierres, leurs noms et les découvertes existantes sont conservés : le format de sauvegarde et les identifiants des blocs ne changent pas.

## Installation

Installer Java 25 et NeoForge 26.3.0.102. Placer `sworldwaystones-1.1.0.jar` dans `mods` sur le client et le serveur. En solo, uniquement dans le dossier `mods` du profil.

## Utilisation

- Trouver « Waystone arcanique » dans l'onglet créatif des blocs fonctionnels ou utiliser `/give @s sworldwaystones:waystone`.
- Recette : un éclat d'améthyste en haut au centre ; obsidienne, perle de l'Ender, obsidienne au milieu ; trois obsidiennes en bas.
- Prévoir deux blocs libres en hauteur. À la pose, écrire un nom dans l'écran arcanique et cliquer sur « Graver le nom » (ou Entrée). Fermer sans valider conserve « Nouvelle Waystone ».
- Faire un clic droit sur une pierre pour la découvrir et ouvrir le menu. Un objet tenu en main peut avoir sa propre action : utiliser la main vide si nécessaire.
- Le menu arcanique liste uniquement les autres pierres découvertes par CE joueur, par pages de cinq. Sélectionner une ligne puis cliquer sur « Se téléporter ». La sélection cyan affiche les coordonnées et la dimension.
- Saisir un nom dans la recherche puis appuyer sur Entrée ou cliquer sur « OK ». La recherche couvre toutes les destinations découvertes, y compris les autres pages. Les flèches et la molette sur la liste changent de page.
- Le bouton « R » actualise les destinations ; « X » ou Échap ferme le menu. Aucun inventaire n'est affiché dans cette interface.
- Le propriétaire peut cliquer sur « Renommer » ; le même thème arcanique est utilisé pour la saisie. Le nouveau nom est commun à tous les visiteurs.
- Les téléportations sont gratuites et possibles entre dimensions existantes. Il faut un emplacement praticable à proximité de la destination. Descendre d'une monture avant utilisation.
- Détruire puis reposer une pierre crée une nouvelle identité : chaque joueur devra la découvrir de nouveau.

## Sauvegarde

Les noms, propriétaires et découvertes (UUID des joueurs) sont stockés dans `data/sworldwaystones.json` à la racine du monde. Chaque modification est enregistrée avec remplacement atomique lorsque le système de fichiers le permet. Sauvegarder le monde complet avant d'installer une nouvelle version. Garder le même mode d'authentification du serveur pour conserver les UUID.

## Compiler sous Windows

Installer un JDK 25, extraire le projet, puis lancer `CONSTRUIRE.bat`. Le premier lancement télécharge Gradle et les dépendances. Le JAR final est dans `build/libs`.

Sous Linux/macOS : `./gradlew build`.

## Vérifications en jeu

1. Avec le joueur A, poser deux pierres nommées Maison et Mine ; vérifier l'aller-retour.
2. Avec B, cliquer sur Maison seulement : Mine doit être absente. Visiter Mine et cliquer dessus : Maison devient une destination.
3. Renommer Mine avec A : B doit voir le nouveau nom après actualisation. Le bouton de renommage doit être désactivé pour B.
4. Redémarrer complètement le serveur : noms et découvertes doivent rester présents.
5. Détruire Mine : la destination est retirée à l'actualisation si sa zone est chargée, ou à sa vérification lors d'une téléportation. Reposer au même endroit : B doit redécouvrir la nouvelle pierre.
6. Bloquer les emplacements autour d'une destination : la téléportation doit être refusée.
7. Tester une recherche avec des résultats sur plusieurs pages, une recherche vide et aucun résultat. Vérifier le clavier dans les champs et le défilement à la molette.
8. Vérifier une téléportation entre dimensions et une destination dans un chunk déchargé.
9. Essayer Maj-clic, touches numériques, glisser et déposer, double-clic et mode créatif dans les menus : aucune icône ne doit pouvoir être récupérée.
10. Vérifier la destruction de chacune des moitiés de la pierre et l'absence de duplication d'objet.

## État de validation

Le projet comprend cinq tests de persistance et d'isolation des découvertes, quatre tests de recherche, pagination, droits et transmission des données du menu, et un test de chargement des enregistrements, de la recette et de la table de butin dans un serveur de test NeoForge. La géométrie OBJ, ses indices, les fichiers JSON et la présence des ressources dans le JAR sont vérifiés séparément.

Le rendu graphique, les clics avec un vrai client, la téléportation dans un monde et les interactions à deux joueurs restent à valider en jeu. La compilation et ces tests ne remplacent pas cette validation graphique et multijoueur.
