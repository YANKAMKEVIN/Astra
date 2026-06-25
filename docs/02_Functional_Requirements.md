# 02_Functional_Requirements.md

# ASTRA — Functional Requirements

## 1. Scope

ASTRA v1 est une application KMP Android/iOS destinée à démontrer une plateforme mobile d’Edge AI.

Elle doit permettre à un utilisateur, appelé **Engineer**, de :

* consulter les capacités de son appareil ;
* sélectionner un modèle IA ;
* sélectionner un backend d’inférence ;
* interroger un assistant documentaire local ;
* mesurer les performances d’inférence ;
* comparer plusieurs modèles sur un même prompt ;
* configurer les paramètres d’exécution.

## 2. Écrans principaux

ASTRA v1 contient 6 écrans principaux :

1. Splash Screen
2. Dashboard
3. Assistant
4. Documents
5. Benchmark
6. Settings

## 3. Splash Screen

### Objectif

Présenter l’identité du produit et simuler l’initialisation de l’environnement Edge AI.

### Contenu

* Logo ASTRA
* Nom : **ASTRA**
* Slogan : **Secure Local AI for Critical Operations**
* Animation d’initialisation
* Étapes affichées :

  * Checking device capabilities
  * Detecting CPU
  * Detecting GPU
  * Detecting NPU
  * Loading inference backends
  * Loading available models
  * ASTRA Ready

### Critères d’acceptation

* Le splash screen s’affiche au lancement.
* Le slogan est visible.
* Les étapes d’initialisation s’affichent progressivement.
* L’utilisateur est redirigé vers le Dashboard.

## 4. Dashboard

### Objectif

Fournir une vue cockpit de l’état de l’appareil et de la configuration IA courante.

### Contenu

* Message : **Welcome Engineer**
* Statut offline
* Device card :

  * platform
  * OS version
  * device model
  * memory
  * storage
* Capabilities card :

  * CPU
  * GPU
  * NPU
  * Local AI
  * Document QA
  * Benchmark
* Current AI configuration :

  * selected model
  * selected backend
  * quantization
  * context window
* Quick actions :

  * Open Assistant
  * Open Documents
  * Run Benchmark
  * Open Settings

### Critères d’acceptation

* Le Dashboard affiche les informations principales.
* Les quick actions naviguent vers les bons écrans.
* Les données peuvent être mockées si non disponibles sur la plateforme.
* Le design doit ressembler à un cockpit professionnel.

## 5. Assistant

### Objectif

Permettre à l’utilisateur de poser une question à ASTRA et d’obtenir une réponse générée localement ou simulée selon le backend disponible.

### Contenu

* Sélecteur de secteur :

  * Industrial Maintenance
  * Aerospace
  * Defense
  * Energy
  * Healthcare
* Champ de question
* Bouton **Ask ASTRA**
* Zone de réponse
* Indicateur :

  * Local inference
  * Offline mode
  * Selected model
  * Selected backend
* Metrics panel :

  * latency
  * time to first token
  * tokens generated
  * tokens per second
  * memory usage
  * backend
  * model

### Comportement

* L’utilisateur saisit une question.
* L’application construit un prompt métier selon le secteur choisi.
* Le use case appelle le moteur IA.
* Une réponse est affichée.
* Les métriques sont affichées après génération.
* En cas d’indisponibilité du moteur réel, un moteur mock doit retourner une réponse crédible et des métriques simulées.

### Critères d’acceptation

* L’utilisateur peut poser une question.
* Une réponse est affichée.
* Les métriques sont visibles.
* La UI ne dépend pas directement d’un backend IA concret.
* La réponse indique clairement si elle provient d’un moteur réel ou simulé.

## 6. Documents

### Objectif

Démontrer le concept d’assistant documentaire local.

### Contenu

* Liste de documents embarqués ou importés
* Document par défaut :

  * Industrial Pump Maintenance Guide
* État du document :

  * indexed
  * not indexed
* Bouton :

  * Import document
  * Index document
  * Ask about document
* Résumé du document
* Nombre de chunks
* Taille estimée

### Comportement V1

Pour rester réaliste dans le délai, la V1 peut utiliser un document embarqué en texte statique.

Le RAG peut être simulé avec :

* découpage du texte en sections ;
* recherche simple par mots-clés ;
* sélection des passages pertinents ;
* injection du contexte dans le prompt ;
* génération ou simulation de réponse.

### Critères d’acceptation

* L’utilisateur voit au moins un document disponible.
* Il peut poser une question liée au document.
* ASTRA affiche une réponse basée sur un contexte documentaire.
* L’application indique que le traitement est local.

## 7. Benchmark

### Objectif

Comparer plusieurs modèles IA sur un même prompt.

### Contenu

* Prompt benchmark
* Sélection multiple de modèles :

  * Gemma
  * Phi
  * Llama
  * Qwen
* Sélection du backend
* Bouton **Run Benchmark**
* Tableau de résultats :

  * model
  * backend
  * latency
  * tokens/s
  * memory
  * quality score
  * status
* Recommandation :

  * recommended model
  * reason

### Comportement

* L’utilisateur saisit ou sélectionne un prompt.
* Il choisit plusieurs modèles.
* ASTRA exécute ou simule le test.
* Les résultats s’affichent sous forme de tableau.
* Un gagnant est proposé selon une règle simple :

  * meilleure combinaison latence + tokens/s + mémoire + qualité.

### Critères d’acceptation

* Au moins 3 modèles sont comparables.
* Le benchmark produit des métriques.
* Le modèle recommandé est affiché.
* La logique de scoring est séparée dans le domaine.

## 8. Settings

### Objectif

Permettre de configurer l’environnement IA.

### Contenu

* Model selection
* Backend selection
* Industry persona
* Temperature
* Max tokens
* Context window
* Quantization
* Theme
* Experimental features toggle

### Modèles proposés

* Gemma
* Phi
* Llama
* Qwen
* Mock Model

### Backends proposés

* LiteRT
* ONNX Runtime
* llama.cpp
* Core ML
* Mock Engine

### Critères d’acceptation

* L’utilisateur peut changer le modèle courant.
* L’utilisateur peut changer le backend courant.
* Les paramètres sont persistés localement.
* Les écrans Assistant et Benchmark utilisent la configuration courante.

## 9. Navigation

La navigation principale doit permettre d’accéder rapidement à :

* Dashboard
* Assistant
* Documents
* Benchmark
* Settings

Sur mobile, privilégier une bottom navigation ou une navigation rail selon la taille d’écran.

## 10. États MVI

Chaque écran doit être piloté par :

* State
* Intent
* Effect

Exemple pour Assistant :

### State

* selectedIndustry
* question
* answer
* isGenerating
* metrics
* error
* selectedModel
* selectedBackend

### Intent

* UpdateQuestion
* SelectIndustry
* AskQuestion
* Retry
* ClearAnswer

### Effect

* ShowError
* NavigateToSettings

## 11. Offline behavior

ASTRA doit être utilisable sans connexion réseau.

La V1 ne doit pas nécessiter :

* backend distant ;
* authentification ;
* API externe ;
* service cloud.

Si un modèle réel doit être téléchargé, cela doit être documenté mais non obligatoire pour la démo.

## 12. Error handling

ASTRA doit gérer :

* modèle non disponible ;
* backend non disponible ;
* mémoire insuffisante ;
* document non indexé ;
* question vide ;
* erreur d’inférence ;
* timeout benchmark.

Chaque erreur doit avoir :

* un message clair ;
* une action possible ;
* un fallback vers Mock Engine si pertinent.

## 13. Persistence

ASTRA doit persister localement :

* modèle sélectionné ;
* backend sélectionné ;
* secteur sélectionné ;
* paramètres d’inférence ;
* dernier benchmark si simple à faire.

La persistance peut être faite avec Settings/DataStore côté Android et équivalent multiplateforme si disponible.

## 14. MVP Priority

Priorité haute :

* Splash
* Dashboard
* Assistant
* Settings
* Benchmark mocké
* Architecture IA abstraite

Priorité moyenne :

* Documents avec document embarqué
* Scoring benchmark
* Device capabilities détaillées

Priorité basse :

* Import PDF réel
* Export PDF
* Streaming token réel
* intégration complète iOS avec moteur IA réel

## 15. Démo attendue

La démo doit permettre de montrer :

1. Lancement premium d’ASTRA
2. Dashboard cockpit
3. Sélection d’un modèle
4. Question à l’assistant
5. Réponse offline
6. Affichage des métriques
7. Benchmark entre plusieurs modèles
8. Recommandation finale
9. Explication de l’architecture KMP Android/iOS

## 16. Definition of Done

ASTRA v1 est considérée prête si :

* l’app compile sur Android ;
* l’app compile sur iOS si l’environnement le permet ;
* les 6 écrans principaux existent ;
* l’architecture MVI + Clean est respectée ;
* Koin est utilisé ;
* au moins un moteur Mock fonctionne ;
* l’abstraction LocalLlmEngine est en place ;
* les données affichées sont cohérentes ;
* le design est premium ;
* le README explique comment lancer la démo.
