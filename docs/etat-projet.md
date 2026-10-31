# État du projet Astra

_Dernière mise à jour : 31 octobre 2026, `main` au merge de la PR #28._

## Ce qui est livré (mergé dans `main`)

| PR | Sujet |
|---|---|
| #23 | Écran Modèles unique : sections par taille de RAM, section « trop lourds », recherche ; RAM et stockage réels sur Android et iOS |
| #24 | RAG : prompt système envoyé au modèle, ajustement au vrai contexte, découpage des mots compatible français ; Docs suit le modèle actif |
| #25 | Android : modèles `.litertlm` exécutés par `litertlm-android` 0.18.0 (MediaPipe gardé pour `.task`) ; réponses sur document directes |
| #26 | Android : téléchargement en service de premier plan + reprise (HTTP `Range`) ; tailles du catalogue corrigées |
| #27 | Permission de notification demandée au premier téléchargement, plus au lancement |
| #28 | iOS : téléchargement par session réseau d'arrière-plan (`NSURLSession` background) |

## Prouvé sur appareil réel

- **Galaxy S24** : réponse RAG générée sur le téléphone par Qwen3 0.6B à partir d'un PDF français (« 12 bars », 3,2 s).
- **Galaxy S24** : téléchargement de 2 Go terminé avec l'app en arrière-plan et le téléphone forcé en Doze profond.
- **Simulateur iPhone 16 Pro** : téléchargement de 2 Go terminé avec l'app en arrière-plan.

## Non vérifié

- Android : le bouton « Allow » de la boîte d'explication (PR #27) ouvre la demande système puis lance le téléchargement.
- iPhone physique : téléchargement long écran verrouillé ; relance de l'app par iOS pendant un téléchargement.
- iOS : génération de texte sur un vrai iPhone (jamais testée).
- Section « Too large » avec un modèle réellement au-dessus de la RAM (il faut un téléphone de 6 Go ou moins).

## Prochaines tâches possibles

1. **Gmail** : corps perdu pour les mails multipart imbriqués (pièce jointe) ; pas de pagination (20 messages) ; erreurs 401/403 affichées comme « aucun message » ; jetons iOS en clair dans NSUserDefaults.
2. **RAG** : questions générales en français court-circuitées vers le résumé ; index non sauvegardé ; un seul document à la fois ; pièce jointe du chat retirée après chaque réponse.
3. **APK de debug** de 239 Mo : découper par architecture (ABI splits).
4. **Accélération GPU/NPU** pour LiteRT-LM (CPU uniquement aujourd'hui).
5. **Tests des adaptateurs natifs** (moteurs d'inférence, détection des modèles installés, côté iOS). Le modèle de test HTTP local de `ResumableDownloaderTest` est réutilisable.

## Appareils de test

- Galaxy S21+ (USB) et Galaxy S24 (adb sans fil, stockage presque plein).
- Simulateur iPhone 16 Pro.

## Commandes utiles

- Tests JVM : `./gradlew :shared:testAndroidHostTest`
- Tests iOS : `./gradlew :shared:iosSimulatorArm64Test`
- APK : `./gradlew :androidApp:assembleDebug`
