Les ressources sous android sont organsisées dans des répertoires, avec des attributs, permettant de choisir la bonne version suivant la langue, la position du téléphone (horizontal, vertical), etc.

Vous trouverez dans le répertoire res, les répertoires pour la langue par défaut et pour le français.
Pour réaliser une traduction, il faut copier un répertoire vers un autre, avec le suffixe du pays.

Par exemple:
>cp values values-de

Puis editer les fichiers présent dans le répertoire -de. Des commentaires doivent aider.
S'il y a d'autres répertoires comme "values-lang" au autre (lang pour "paysage"), il faut faire de même.

* Les textes des menus et des boutons doivent être "court" ;
* Les textes des erreurs n'ont pas de limite ;



