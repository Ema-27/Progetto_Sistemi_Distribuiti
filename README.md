# PubliSisDis 📚

**La piattaforma per gestire le tue pubblicazioni** 

PubliSisDIS è un’applicazione web moderna che ti permette di organizzare, catalogare e ricercare facilmente le tue pubblicazioni. Grazie all’integrazione con servizi cloud AWS e a potenti algoritmi di Natural Language Processing, caricare, analizzare e gestire documenti in PDF, DOCX o LaTeX non è mai stato così semplice!
---

## 🚀 Caratteristiche Principali

### 📤 **Upload Intelligente**
- **Caricamento multi-formato**: PDF, DOCX, LaTeX
- **Parsing automatico BibTeX**: estrazione metadati da file .bib
- **Validazione e conversione**: controllo del contenuto e conversione text extraction

  ### 🤖 **Estrazione Avanzata di Keyword**
- **Amazon Comprehend**: rilevamento automatico della lingua e estrazione di key-phrases multi-lingua
- **RAKE Algorithm**: alternativa open-source per estrazione da testi in inglese
- **Filtro stop-words**: pulizia del “rumore” dai risultati

  ### 📋 **Anteprima e Modifica Keywords**
- **Report interattivo**: visualizza le keyword estratte prima della pubblicazione
- **Editing manualei**: aggiungi o rimuovi keyword secondo le tue preferenze

### 🔍 **Ricerca Avanzata**
- **Ricerca per titolo, autore o keyword**: trova rapidamente le pubblicazioni che ti servono

### 👥 **Gestione Utenti e Sicurezza**
- **Autenticazione AWS Cognito**: registrazione e login
- **Permessi granulari**: accesso controllato a upload, estrazione e download
- **Libreria personale**: ogni utente ha accesso alle proprie pubblicazioni che possono essere eliminate

### ☁️ **Infrastruttura Cloud Scalabile**
- **Storage su S3**: archiviazione sicura dei documenti
- **Database relazionale RDS (MySQL)**: gestione dati performante
- **Hosting su EC2**: backend e frontend live 24/7

---

## 🛠️ Stack Tecnologico

### Frontend 🎨
- **Angular 15  & TypeScript**
- **Modular Architecture**
- **Responsive Design**

### Backend ⚡
- **Spring Boot & Java 17**
- **Layered Architecture**
- **AWS SDK Integration**
  

### Cloud & DevOps ☁️
- **Amazon Web Services**
  - Cognito (autenticazione)
  - S3 (object storage)
  - IAM (gestione credenziali e permessi)
  - RDS MySQL (database)
  - Comprehend (NLP)
  - EC2 (hosting)
  
---

## 🎯 Come Funziona

1. **📝 Registrati** con email e password (AWS Cognito)
2. **🔐 Effettua il login** e accedi all’interfaccia personalizzata
3. **📤 Carica** il tuo documento (PDF, DOCX o LaTeX)
4. **📋 Fornisci metadati** manualmente o importa un file BibTeX
5. **🤖 Scegli il metodo di estrazione** (Comprehend o RAKE)
6. **🔍 Visualizza e modifica** le keyword estratte
7. **✅ Pubblica** il documento e gestiscilo nella tua libreria
8. **🔎 Cerca** tra le tue pubblicazioni per autore, titolo o keyword

---

