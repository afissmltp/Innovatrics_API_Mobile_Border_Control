## 📄 Mobile Border Control – Innovatrics

Cette section décrit le processus complet de **lecture**, **analyse** et **vérification** d’un document avec l’application, en utilisant la solution **Innovatrics**.

---

### 🔧 Backend & API utilisée
Le traitement biométrique et documentaire est effectué via le **backend Innovatrics** :  
**`dot-digital-identity-service-1.53.0-amd64`**

- Cette API est responsable de :  
  - L’analyse et l’extraction des données (OCR).  
  - La vérification de l’authenticité du document.  
  - Le matching biométrique (comparaison des visages entre document, RFID et selfie).  
- L’application communique avec cette API en REST pour envoyer les images et recevoir les résultats.

---

### 🔄 Processus de Scan de Document

1. **Démarrage du processus**  
   - L’utilisateur clique sur **Lecture Document** dans l’écran d’accueil.  
   - L’application initialise la communication avec le backend.

2. **Capture du document**  
   - La caméra s’ouvre et l’utilisateur positionne correctement le document pour une capture optimale (bonne luminosité, pas de reflets).  
   - Après la prise de vue, l’image est envoyée à l’API Innovatrics pour extraction.
     
3. **Barre de menu supérieure**  
   Lors de la capture, un menu avec **4 boutons** est affiché en haut :  
   - 🏠 **Home** : Retourne à la page d’accueil.  
   - 📡 **Lecture NFC** : Lance la lecture de la puce RFID si le document en possède une.  
   - 📷 **Capture** : Permet de relancer la capture si nécessaire.  
   - 📤 **Partager** : Permet de partager le document ou les résultats.

4. **Analyse et extraction**  
   - L’API retourne :  
     - Les champs **texte** (nom, prénom, date de naissance, date d’expiration, etc.).  
     - Le **portrait extrait** du document.  
   - L’application affiche ces informations à l’utilisateur.

5. **Navigation par onglets**  
   En bas de l’écran, un système d’onglets permet de consulter toutes les informations :  
   - 🏷️ **INFO** : Données personnelles extraites.  
   - ✅ **Authenticité** : Résultats de la vérification du document (MRZ, hologrammes, sécurité).  
   - 🖼️ **Images** : Portrait extrait, photo du document et image RFID (si disponible).  
   - 🔎 **Check** : Comparaison biométrique :  
     - Portrait extrait 🆚 Selfie.  
     - Photo RFID 🆚 Selfie.  
   - 🔄 **Matching Données** : Vérifie la cohérence entre les données RFID et celles extraites du document.

6. **Partage des résultats**  
   L’utilisateur peut exporter :  
   - Un **rapport PDF** contenant les images, les données et les résultats de vérification.  
   - Partager ce rapport via **WhatsApp**, email ou toute autre application installée.

