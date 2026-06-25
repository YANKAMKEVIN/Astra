# 01_Product_Vision.md

# ASTRA

**Secure Local AI for Critical Operations**

## 1. Vision

ASTRA est une plateforme multiplateforme d’expérimentation et d’évaluation de l’Edge AI.

Son objectif est de permettre à des ingénieurs, architectes et consultants innovation de tester, comparer et benchmarker des Small Language Models exécutés localement sur des appareils Android et iOS, sans dépendance au cloud.

ASTRA n’est pas un simple chatbot. C’est un laboratoire mobile d’IA embarquée conçu pour démontrer la faisabilité technique, les limites et les opportunités de l’inférence locale dans des environnements critiques.

## 2. Problème adressé

Les entreprises industrielles, notamment dans l’aéronautique, la défense, l’énergie, la santé et l’industrie, souhaitent exploiter l’IA générative tout en respectant des contraintes fortes :

* confidentialité des données ;
* fonctionnement hors ligne ;
* faible latence ;
* maîtrise des coûts cloud ;
* souveraineté technologique ;
* contraintes réseau sur le terrain ;
* sécurité des environnements critiques.

Les solutions cloud sont puissantes, mais elles ne sont pas toujours adaptées aux contextes sensibles ou isolés.

ASTRA répond à cette problématique en démontrant comment un assistant IA peut fonctionner directement sur l’appareil.

## 3. Proposition de valeur

ASTRA permet de :

* exécuter un modèle IA localement ;
* sélectionner différents modèles IA ;
* comparer plusieurs backends d’inférence ;
* mesurer les performances réelles sur device ;
* poser des questions à un assistant documentaire ;
* analyser les compromis entre cloud et edge ;
* produire des résultats exploitables pour une décision technique ou business.

## 4. Positionnement produit

ASTRA se positionne comme une plateforme de démonstration Edge AI pour les équipes innovation, avant-vente, architecture et mobile.

Elle permet de répondre à une question clé :

> Quel modèle, quel backend et quelle architecture Edge AI sont les plus adaptés à un cas d’usage industriel donné ?

## 5. Utilisateurs cibles

Les utilisateurs principaux sont :

* ingénieurs mobiles ;
* architectes logiciels ;
* consultants innovation ;
* équipes avant-vente ;
* responsables techniques ;
* experts IA embarquée ;
* décideurs techniques dans des secteurs critiques.

Dans l’application, l’utilisateur est appelé **Engineer** afin de renforcer l’identité professionnelle du produit.

## 6. Cas d’usage principal

Le cas d’usage principal de la V1 est :

**Assistant documentaire local pour opérations critiques.**

Un ingénieur ou technicien peut interroger une documentation embarquée directement depuis son téléphone ou sa tablette, sans connexion Internet.

Exemple :

> “How do I restart Pump A after an emergency shutdown?”

ASTRA répond localement, affiche les métriques d’inférence et indique le modèle utilisé.

## 7. Cas d’usage secondaires

ASTRA pourra aussi servir à :

* comparer Gemma, Phi, Llama ou Qwen sur un même prompt ;
* mesurer la latence et les tokens/seconde ;
* vérifier la consommation mémoire ;
* évaluer l’intérêt du cloud vs edge ;
* préparer des recommandations de POC pour des clients industriels ;
* démontrer l’intérêt d’un assistant IA offline en environnement sensible.

## 8. Secteurs visés

ASTRA cible prioritairement les secteurs suivants :

### Industrie

Maintenance terrain, diagnostic de panne, consultation de procédures techniques.

### Énergie

Intervention sur site isolé, centrales, plateformes, zones sans réseau fiable.

### Défense

Analyse documentaire locale, environnement sécurisé, interdiction d’envoyer des données vers le cloud.

### Aéronautique

Assistance cockpit, documentation embarquée, procédures critiques.

### Santé

Support local sur dispositifs médicaux, confidentialité des données patients.

## 9. Principes produit

ASTRA doit respecter les principes suivants :

* offline-first ;
* privacy-by-design ;
* local inference first ;
* architecture extensible ;
* benchmark transparent ;
* expérience utilisateur premium ;
* design professionnel ;
* absence de dépendance obligatoire au cloud ;
* séparation stricte entre UI, logique métier et moteur IA.

## 10. Objectifs de la V1

La V1 doit permettre de démontrer :

* une application KMP Android/iOS ;
* une UI premium en Compose Multiplatform ;
* une architecture MVI + Clean Architecture ;
* une injection de dépendances avec Koin ;
* une abstraction de moteur IA local ;
* un assistant documentaire simple ;
* un écran benchmark ;
* un écran dashboard device/capabilities ;
* un écran settings permettant de choisir modèle, backend et paramètres ;
* une simulation ou implémentation réelle d’inférence locale selon la plateforme.

## 11. Non-objectifs de la V1

La V1 ne doit pas chercher à couvrir :

* authentification utilisateur ;
* backend cloud ;
* synchronisation multi-device ;
* stockage distant ;
* historique conversationnel avancé ;
* fine-tuning de modèles ;
* entraînement de modèles ;
* multi-utilisateurs ;
* analytics cloud ;
* notifications push.

Ces éléments pourront être envisagés en V2 ou V3 uniquement s’ils servent la vision Edge AI.

## 12. Différenciation

La majorité des démonstrations IA embarquées se limitent à un chatbot local.

ASTRA se différencie par :

* son approche plateforme ;
* son orientation benchmark ;
* sa compatibilité Android/iOS via KMP ;
* sa capacité à comparer modèles et backends ;
* son positionnement industriel ;
* son design premium ;
* sa lecture métier orientée secteurs critiques.

## 13. Message de démonstration

Lors d’une présentation, ASTRA doit permettre de dire :

> ASTRA démontre qu’il est possible d’exécuter un assistant IA directement sur un appareil mobile, sans envoyer de données vers le cloud, tout en mesurant précisément les performances du modèle utilisé. Cette approche est particulièrement pertinente pour les secteurs industriels, défense, énergie et aéronautique, où confidentialité, latence et fonctionnement offline sont critiques.

## 14. Vision long terme

À terme, ASTRA pourrait devenir une plateforme complète d’évaluation Edge AI intégrant :

* SLM locaux ;
* RAG local multi-documents ;
* OCR embarqué ;
* speech-to-text local ;
* text-to-speech local ;
* vision models ;
* comparaison cloud vs edge ;
* export PDF de benchmarks ;
* scénarios par secteur client ;
* recommandations automatiques de POC.

## 15. Phrase de synthèse

ASTRA est une plateforme Edge AI multiplateforme permettant d’évaluer concrètement l’exécution locale de Small Language Models dans des environnements critiques, afin d’aider les entreprises à choisir entre cloud, edge ou architecture hybride.
