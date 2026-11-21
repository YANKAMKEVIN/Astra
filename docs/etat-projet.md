# État du projet Astra

_Dernière mise à jour : 21 novembre 2026, branche `claude/suite` (à relire avant fusion)._

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

## Branche `claude/suite` (21 novembre 2026, à relire)

| Sujet | État |
|---|---|
| Gmail | Corps des mails multipart imbriqués (pièce jointe) lu en récursif : texte brut, sinon HTML converti. Pagination `nextPageToken` + bouton « Load 20 more » (100 messages max). 401/403 : « Reconnect your Gmail account » / « Gmail refused access », l'autorisation inutile est effacée. Testé sur réponses d'API simulées. |
| RAG | Routage des questions (`DocumentQuestionRouter`) : les questions ordinaires en français ne partent plus vers le résumé. Bibliothèque de plusieurs sources (PDF, e-mail, Gmail ; 10 max), index sauvegardé dans `filesDir/rag/index.json` et restauré au lancement (Android ; iOS en mémoire). Chat : la pièce jointe reste pour les questions suivantes jusqu'au retrait ou à une nouvelle conversation. |
| APK de debug | Un APK par ABI en debug : arm64-v8a 100 Mo, x86_64 111 Mo, armeabi-v7a 59 Mo (sans LiteRT-LM, qui n'a pas de bibliothèque 32 bits). Release inchangée. |
| GPU/NPU LiteRT-LM | Option « Hardware acceleration » dans Settings (CPU par défaut, GPU et NPU « TO TEST ») avec repli automatique sur CPU, à l'initialisation comme à la première génération. |
| Tests des adaptateurs Android | `AndroidModelAdaptersTest` : téléchargement sur serveur HTTP local, détection des modèles installés, chargeur du moteur, suppression. |

Tests JVM : 195 → 248 (`:shared:testAndroidHostTest`).

Valeurs par défaut choisies, à valider : Gmail par pages de 20, 100 messages au plus ; 10 sources au plus dans la bibliothèque RAG ; le contenu Gmail est sauvegardé dans l'index local (privé à l'app) et oublié à la déconnexion ; un 401/403 déconnecte le compte ; la pièce jointe du chat reste jusqu'au retrait ; avec plusieurs sources, une question de résumé passe par la recherche au lieu du résumé d'un seul document.

## Branche `claude/ios-keychain` (PR vers `main`, à relire)

| Sujet | État |
|---|---|
| Jetons Gmail iOS dans le Keychain | `IosKeychain` (iosMain, API Security : `SecItemAdd` / `SecItemUpdate` / `SecItemCopyMatching` / `SecItemDelete`, `kSecClassGenericPassword`, service `com.kevin.astra.gmail`, `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`) remplace `NSUserDefaults` dans `IosGmailAuth.kt`, via `GmailTokenStore` (commun). Migration au lancement (`GmailTokenMigration`) : les clés `astra_gmail_*` de `NSUserDefaults` sont copiées dans le Keychain, relues, puis effacées ; si l'écriture échoue elles restent et la migration est retentée au lancement suivant ; une connexion déjà présente dans le Keychain est gardée. `signOut()` supprime les entrées Keychain. Tests communs avec faux stockage (8) + `IosKeychainTest` sur la CI macOS : lecture/effacement des anciennes clés `NSUserDefaults` testés ; l'aller-retour Keychain et la migration complète sont sautés en CI, car iOS refuse un trousseau à l'exécutable de test lancé sans app (`errSecNotAvailable`, -25291), et tournent dès qu'un trousseau est disponible. Code compilé par la CI (simulateur et `iosArm64`). |

Tests JVM : 248 → 256. À vérifier sur iPhone ou simulateur dans l'app (Kevin) : un compte Gmail connecté avec l'ancienne version reste connecté après la mise à jour ; connexion, puis relance de l'app, puis déconnexion.

## Prochaines tâches possibles

1. **RAG iOS** : persister l'index (aujourd'hui `NoOpDocumentIndexFile`, comme l'historique des conversations).
2. **Tests des adaptateurs iOS** (moteurs, détection des modèles installés) : à écrire sur Mac.

## À vérifier sur appareil (Kevin)

- iOS : génération de texte sur un vrai iPhone (jamais testée).
- iPhone physique : téléchargement long écran verrouillé ; relance de l'app par iOS pendant un téléchargement.
- Android : le bouton « Allow » de la boîte d'explication (PR #27) ouvre la demande système puis lance le téléchargement.
- Section « Too large » avec un modèle réellement au-dessus de la RAM (téléphone de 6 Go ou moins).
- Nouveau (`claude/suite`) : GPU puis NPU sur S24 et S21+ (vitesse, et repli sur CPU affiché sous la réponse si ça échoue) ; installation de l'APK arm64-v8a ; index RAG retrouvé après un redémarrage de l'app ; mail Gmail avec pièce jointe ; « Load 20 more » ; message 401 après révocation de l'accès dans le compte Google.

## Appareils de test

- Galaxy S21+ (USB) et Galaxy S24 (adb sans fil, stockage presque plein).
- Simulateur iPhone 16 Pro.

## Commandes utiles

- Tests JVM : `./gradlew :shared:testAndroidHostTest`
- Tests iOS : `./gradlew :shared:iosSimulatorArm64Test`
- APK : `./gradlew :androidApp:assembleDebug` (un APK par ABI dans `androidApp/build/outputs/apk/debug/`)
