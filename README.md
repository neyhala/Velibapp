# VelibApp

Application Android développée en kotlin permettant de consulter les stations Vélib de la métropole parisienne sur une carte interactive.

## Fonctionnalités de base

### Affichage des stations Vélib
- Affichage de l'ensemble des stations Vélib sur une carte OpenStreetMap.
- Positionnement des stations à l'aide de marqueurs verts.


### Consultation du détail d'une station en cliquant sur un marqueur
- Nom de la station.
- Nombre de vélos disponibles.
- Nombre de places disponibles.
- Capacité totale de la station.
- Coordonnées GPS.

### Gestion des favoris
- Ajout et suppression de stations favorites en cliquant deux fois sur un marqueur.
- Marqueur vert (station classique), Marqueur rouge (station favori)
- Liste des favoris accessible rapidement dans une liste déroulante intéractive.
- Redirection et centrage automatique de la carte sur la station lors de la sélection dans la liste déroulante
- Sauvegarde locale des favoris avec Room Database.
- Consultation des favoris même hors connexion.


### Recherche des stations proches
- Possibilité de placer un marqueur bleu personnalisé sur la carte.
- Recherche des stations dans un rayon défini autour de ce marqueur.
- Pour annuler, appuyer sur le bouton "tout afficher".
- L'utilisateur peut sélectionner un point sur la carte puis choisir un rayon de recherche parmi : 500 m, 1 km, 2.5 km et 5 km
(Seules les stations situées dans le périmètre sélectionné sont affichées)

## Fonctionnalités supplémentaires

### Filtrage des stations
Possibilité de filtrer l'affichage des stations :

- Toutes les stations
- Stations possédant au moins un vélo disponible
- Stations possédant au moins une place disponible

### Recherche intelligente de stations
Barre de recherche permettant :

- La recherche d'une station par son nom.
- L'affichage de suggestions en temps réel.
- Redirection et centrage automatique de la carte sur la station sélectionnée.
- Le zoom automatique sur la station recherchée.

### Contrôle du zoom
Ajout de boutons permettant de :
- Zoomer sur la carte.
- Dézoomer sur la carte.

Projet réalisé dans le cadre du module Matériels mobiles : 
HUYNH Alexandre et JOVANOVIC Marko
