import { Component, ElementRef, ViewChild, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { PublicationService } from './service/publication';
import { Document } from './model/document';
import { AuthService } from './service/auth.service';
import { UserService, RegisterUserDto } from './service/user.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'frontendSisDis';
  activeTab: 'upload' | 'bibtex' = 'upload';
  uploadForm!: FormGroup;
  bibtexForm!: FormGroup;
  docs: Document[] = [];
  filteredDocs: Document[] = [];
  showSuccessPopup = false;

  // AUTH state
  isAuthenticated = false;
  userName = '';
  userInitial = '';
  authMode: 'login' | 'register' = 'login';
  showAuthModalFlag = false;
  authEmail = '';
  authPassword = '';
  authName = '';
  authError = '';

  // Password validation
  passwordRequirements = {
    minLength: false,
    hasUppercase: false,
    hasNumber: false,
    hasSpecialChar: false
  };
  showPasswordRequirements = false;

  // UPLOAD keyword state
  extractedKeywords: string[] = [];
  selectedKeywords: Set<string> = new Set();
  newKeyword: string = '';
  showKeywordPanel = false;
  isExtractingKeywords = false;
  keywordError = '';

  // BIBTEX keyword state
  extractedBibtexKeywords: string[] = [];
  selectedBibtexKeywords: Set<string> = new Set();
  newBibtexKeyword: string = '';
  showBibtexKeywordPanel = false;
  isExtractingBibtexKeywords = false;
  bibtexKeywordError = '';

  showUserDropdown = false;
  myDocs: Document[] = [];
  loadingMyDocs = false;

  // Popup states
  showDeleteSuccessPopup = false;
  showErrorPopup = false;
  showDeleteConfirmPopup = false;
  errorMessage = '';
  docToDelete: Document | null = null;

  useRake: boolean = false;         // default Comprehend per upload
  useRakeBibtex: boolean = false;

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @ViewChild('bibtexInput') bibtexInput!: ElementRef<HTMLInputElement>;
  @ViewChild('docFileInput') docFileInput!: ElementRef<HTMLInputElement>;

  constructor(
    private fb: FormBuilder,
    private publicationService: PublicationService, 
    private authService: AuthService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.uploadForm = this.fb.group({
      title: ['', Validators.required],
      authors: ['', Validators.required],
      year: [new Date().getFullYear(), Validators.required],
      paper: ['', Validators.required]
    });
    this.bibtexForm = this.fb.group({});
    this.getPublications();
  }

  setActiveTab(tab: 'upload' | 'bibtex') {
    this.activeTab = tab;
  }

  openAuthModal(mode: 'login' | 'register') {
    this.authMode = mode;
    this.showAuthModalFlag = true;
    this.authError = '';
    this.resetPasswordValidation();
  }

  closeAuthModal() {
    this.showAuthModalFlag = false;
    this.authEmail = '';
    this.authPassword = '';
    this.authName = '';
    this.authError = '';
    this.resetPasswordValidation();
  }

  toggleAuthMode() {
    this.authMode = this.authMode === 'login' ? 'register' : 'login';
    this.authError = '';
    this.resetPasswordValidation();
  }

  onPasswordChange() {
    if (this.authMode === 'register') {
      this.validatePassword(this.authPassword);
      this.showPasswordRequirements = this.authPassword.length > 0;
    }
  }

  validatePassword(password: string) {
    this.passwordRequirements = {
      minLength: password.length >= 8,
      hasUppercase: /[A-Z]/.test(password),
      hasNumber: /\d/.test(password),
      hasSpecialChar: /[_\-.:,;]/.test(password)
    };
  }

  resetPasswordValidation() {
    this.passwordRequirements = {
      minLength: false,
      hasUppercase: false,
      hasNumber: false,
      hasSpecialChar: false
    };
    this.showPasswordRequirements = false;
  }

  isPasswordValid(): boolean {
    return Object.values(this.passwordRequirements).every(req => req);
  }

  async handleAuth() {
  this.authError = '';

  // Controllo password se in modalità registrazione
  if (this.authMode === 'register' && !this.isPasswordValid()) {
    this.authError = 'La password non rispetta i requisiti di sicurezza';
    return;
  }

  try {
    if (this.authMode === 'login') {
      // Effettua login con await su observable convertito in Promise
      const res = await firstValueFrom(this.userService.login(this.authEmail, this.authPassword));
      
      localStorage.setItem('token', res.token);
      this.isAuthenticated = true;
      this.userName = this.authEmail;

      // Chiudi la modale solo dopo il successo
      this.closeAuthModal();

    } else {
      // Registrazione
      const user: RegisterUserDto = {
        username: this.authEmail,
        email: this.authEmail,
        fullName: this.authName,
        temporaryPassword: this.authPassword,
      };

      // Attendi registrazione
      await firstValueFrom(this.userService.register(user));

      // Effettua login automatico dopo registrazione
      const loginRes = await firstValueFrom(this.userService.login(this.authEmail, this.authPassword));
      
      localStorage.setItem('token', loginRes.token);
      this.isAuthenticated = true;
      this.userName = this.authName;

      this.closeAuthModal();
    }
  } catch (err: any) {
    console.error('Errore autenticazione:', err);

    if (err.status === 401 || err.status === 403) {
      this.authError = 'Email o password non corretti';
    } else if (err.status === 400 && err.error?.message?.includes('password')) {
      this.authError = 'La password non rispetta i requisiti di sicurezza richiesti';
    } else if (err.status === 400 && err.error?.message?.includes('esistente')) {
      this.authError = 'Un utente con questa email esiste già';
    } else if (err.error?.message) {
      this.authError = err.error.message;
    } else {
      this.authError = 'Email o password non corretti';
    }
  }
}
  // Modifica il metodo logout esistente per chiudere il dropdown
logout(): void {
  this.isAuthenticated = false;
  this.userName = '';
  this.userInitial = '';
  this.authEmail = '';
  this.showUserDropdown = false;
  this.myDocs = [];
  localStorage.removeItem('token');
}

  checkAuth() {
    this.isAuthenticated = false;
    this.userName = '';
    this.userInitial = '';
  }

  // Modifica il metodo getPublications esistente per ricaricare anche myDocs se necessario
  getPublications(): void {
    this.publicationService.getPublications().subscribe(
      (response) => {
        this.docs = response.map(doc => ({
          ...doc,
          keywords: doc.keywords || [],
          showAllKeywords: false
        }));
        this.filteredDocs = [...this.docs];
        
        // Se il dropdown è aperto, ricarica anche le pubblicazioni dell'utente
        if (this.showUserDropdown && this.isAuthenticated) {
          this.loadMyDocs();
        }
      },
      (error: HttpErrorResponse) => {
        alert(error.message);
      }
    );
  }

  onUpload(): void {
  const file = this.fileInput.nativeElement.files?.[0];
  if (!file) {
    alert('Devi selezionare un file');
    return;
  }
  const metadata = {
    title: this.uploadForm.value.title,
    authors: this.uploadForm.value.authors,
    year: this.uploadForm.value.year,
    paper: this.uploadForm.value.paper,
    keywords: Array.from(this.selectedKeywords),
    useRake: this.useRake  // <-- aggiunto
  };
  const formData = new FormData();
  formData.append('file', file);
  formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
  formData.append('email', this.userName);

  this.publicationService.uploadDocument(formData).subscribe(
    (newDoc) => {
      const docWithToggle: Document = {
        ...newDoc,
        keywords: newDoc.keywords || [],
        showAllKeywords: false
      };
      this.docs.push(docWithToggle);
      this.filteredDocs = [...this.docs];
      this.uploadForm.reset({ year: new Date().getFullYear() });
      this.fileInput.nativeElement.value = '';
      this.selectedKeywords.clear();
      this.extractedKeywords = [];
      this.showKeywordPanel = false;
      this.showSuccessPopup = true;
      setTimeout(() => (this.showSuccessPopup = false), 3000);
    },
    () => alert('Errore durante il caricamento')
  );
}

  // BibTeX - Estrazione e gestione keyword
  async onBibtexDocFileSelected(event: any) {
  const file = event.target.files[0];
  if (!file) return;

  this.isExtractingBibtexKeywords = true;
  this.bibtexKeywordError = '';
  this.extractedBibtexKeywords = [];
  this.selectedBibtexKeywords.clear();
  this.showBibtexKeywordPanel = false;

  try {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('useRake', String(this.useRakeBibtex));  // <-- flag tecnologia

    const keywords = await firstValueFrom(this.publicationService.extractKeywords(formData));
    this.extractedBibtexKeywords = keywords;
    keywords.forEach(k => this.selectedBibtexKeywords.add(k));
    this.showBibtexKeywordPanel = true;
  } catch (err) {
    this.bibtexKeywordError = "Errore durante l'estrazione delle keyword";
    this.extractedBibtexKeywords = [];
    this.selectedBibtexKeywords.clear();
    this.showBibtexKeywordPanel = false;
  } finally {
    this.isExtractingBibtexKeywords = false;
  }
}

  toggleBibtexKeyword(kw: string) {
    if (this.selectedBibtexKeywords.has(kw)) {
      this.selectedBibtexKeywords.delete(kw);
    } else {
      this.selectedBibtexKeywords.add(kw);
    }
  }
  addBibtexKeyword() {
    const kw = this.newBibtexKeyword.trim();
    if (!kw) return;
    if (!this.extractedBibtexKeywords.includes(kw)) {
      this.extractedBibtexKeywords.push(kw);
    }
    this.selectedBibtexKeywords.add(kw);
    this.newBibtexKeyword = '';
  }
  removeBibtexKeyword(kw: string) {
    this.selectedBibtexKeywords.delete(kw);
  }

  onImportBibtex(): void {
  const bibtexFile = this.bibtexInput.nativeElement.files?.[0];
  const docFile = this.docFileInput.nativeElement.files?.[0];

  if (!bibtexFile || !docFile) {
    alert('Devi selezionare sia il file .bib che il documento');
    return;
  }

  const metadata = {
    title: '',
    authors: '',
    year: new Date().getFullYear(),
    paper: '',
    keywords: Array.from(this.selectedBibtexKeywords),
    useRake: this.useRakeBibtex  // <-- aggiunto
  };

  const formData = new FormData();
  formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
  formData.append('file', docFile);
  formData.append('bibtex', bibtexFile);
  formData.append('email', this.userName);

  this.publicationService.uploadDocument(formData).subscribe(
    (newDoc) => {
      const docWithToggle: Document = {
        ...newDoc,
        keywords: newDoc.keywords || [],
        showAllKeywords: false
      };
      this.docs.push(docWithToggle);
      this.filteredDocs = [...this.docs];
      this.bibtexForm.reset();
      this.bibtexInput.nativeElement.value = '';
      this.docFileInput.nativeElement.value = '';
      this.selectedBibtexKeywords.clear();
      this.extractedBibtexKeywords = [];
      this.showBibtexKeywordPanel = false;
      this.showSuccessPopup = true;
      setTimeout(() => (this.showSuccessPopup = false), 3000);
    },
    () => alert("Errore durante l'import")
  );
}
  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value.toLowerCase();
    this.filteredDocs = this.docs.filter(doc =>
      doc.title.toLowerCase().includes(value) ||
      doc.authors.toLowerCase().includes(value) ||
      doc.keywords?.some(k => k.toLowerCase().includes(value))
    );
  }

  onDownload(doc: Document): void {
    this.publicationService.downloadPublication(doc.id).subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          const extension = this.getExtensionFromUrl(doc.fileUrl);
          const filename = `${doc.title}${extension}`;
          const url = window.URL.createObjectURL(res.body);
          const a = document.createElement('a');
          a.href = url;
          a.download = filename;
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
        } else {
          alert('Errore: corpo della risposta vuoto.');
        }
      },
      error: () => alert('Errore nel download')
    });
  }

  async onFileSelected(event: any) {
  const file = event.target.files[0];
  if (!file) return;

  this.isExtractingKeywords = true;
  this.keywordError = '';
  this.extractedKeywords = [];
  this.selectedKeywords.clear();
  this.showKeywordPanel = false;

  try {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('useRake', String(this.useRake));  // <-- flag tecnologia

    const keywords = await firstValueFrom(this.publicationService.extractKeywords(formData));
    this.extractedKeywords = keywords;
    keywords.forEach(k => this.selectedKeywords.add(k));
    this.showKeywordPanel = true;
  } catch (err) {
    this.keywordError = "Errore durante l'estrazione delle keyword";
    this.extractedKeywords = [];
    this.selectedKeywords.clear();
    this.showKeywordPanel = false;
  } finally {
    this.isExtractingKeywords = false;
  }
}


  toggleKeyword(kw: string) {
    if (this.selectedKeywords.has(kw)) {
      this.selectedKeywords.delete(kw);
    } else {
      this.selectedKeywords.add(kw);
    }
  }

  addKeyword() {
    const kw = this.newKeyword.trim();
    if (!kw) return;
    if (!this.extractedKeywords.includes(kw)) {
      this.extractedKeywords.push(kw);
    }
    this.selectedKeywords.add(kw);
    this.newKeyword = '';
  }

  removeKeyword(kw: string) {
    this.selectedKeywords.delete(kw);
  }

  private getExtensionFromUrl(url: string): string {
    if (!url) return '';
    const lastDot = url.lastIndexOf('.');
    if (lastDot === -1) return '';
    const ext = url.substring(lastDot);
    return ext.includes('/') ? '' : ext;
  }

  getVisibleKeywords(doc: Document): string[] {
    if (!doc.keywords) return [];
    return doc.showAllKeywords || doc.keywords.length <= 3 ? doc.keywords : doc.keywords.slice(0, 3);
  }
  showMoreKeywordsButton(doc: Document): boolean {
    return !!doc.keywords && doc.keywords.length > 3;
  }
  toggleKeywords(doc: any): void {
    doc.showAllKeywords = !doc.showAllKeywords;
  }

  deletePublication(id: number) {
    const token = this.authService.getToken();
    if (!token) {
      alert("Non autenticato!");
      return;
    }
    if (!confirm('Sicuro di voler eliminare la pubblicazione?')) return;
    this.publicationService.deletePublication(id).subscribe({
      next: () => {
        this.myDocs = this.myDocs.filter(d => d.id !== id);
        this.docs = this.docs.filter(d => d.id !== id);
        this.filteredDocs = this.filteredDocs.filter(d => d.id !== id);
        alert('Pubblicazione eliminata con successo!');
      },
      error: err => {
        alert('Errore durante la cancellazione: ' + (err.error?.message || ''));
      }
    });
  }

  popoverVisible = false;

  togglePopover() {
    this.popoverVisible = !this.popoverVisible;
    if (this.popoverVisible && this.isAuthenticated) {
      this.loadMyDocs();
    }
  }
/*
  loadMyDocs() {
    const myEmail = this.authEmail || this.getEmailFromToken();
    this.myDocs = this.docs.filter(d => d.owner?.email === myEmail);
  }

  deleteMyDoc(id: number) {
    const token = this.authService.getToken();
    if (!token) {
      alert("Non autenticato!");
      return;
    }
    if (!confirm('Sicuro di voler eliminare questa pubblicazione?')) return;
    this.publicationService.deletePublication(id).subscribe({
      next: () => {
        this.myDocs = this.myDocs.filter(d => d.id !== id);
        this.docs = this.docs.filter(d => d.id !== id);
        this.filteredDocs = this.filteredDocs.filter(d => d.id !== id);
        alert('Pubblicazione eliminata con successo!');
      },
      error: err => {
        alert('Errore durante la cancellazione: ' + (err.error?.message || ''));
      }
    });
  }
    */

  getEmailFromToken(): string {
    const token = this.authService.getToken();
    if (!token) return '';
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.email || '';
    } catch {
      return '';
    }
  }
  // Gestione dropdown utente
toggleUserDropdown(): void {
  this.showUserDropdown = !this.showUserDropdown;
  if (this.showUserDropdown && this.isAuthenticated) {
    this.loadMyDocs();
  }
  
  // Chiudi il dropdown quando si clicca fuori
  if (this.showUserDropdown) {
    setTimeout(() => {
      document.addEventListener('click', this.closeDropdownOnOutsideClick.bind(this));
    }, 0);
  }
}

closeUserDropdown(): void {
  this.showUserDropdown = false;
  document.removeEventListener('click', this.closeDropdownOnOutsideClick.bind(this));
}

private closeDropdownOnOutsideClick(event: Event): void {
  const target = event.target as HTMLElement;
  const dropdown = target.closest('.user-dropdown-container');
  if (!dropdown) {
    this.closeUserDropdown();
  }
}

// Carica le pubblicazioni dell'utente loggato
loadMyDocs(): void {
  if (!this.isAuthenticated) return;
  
  this.loadingMyDocs = true;
  const userEmail = this.getUserEmail();
  
  // Filtra i documenti dell'utente corrente
  this.myDocs = this.docs.filter(doc => {
    return doc.owner?.email === userEmail;
  });
  
  this.loadingMyDocs = false;
}

// Ottieni l'email dell'utente corrente
private getUserEmail(): string {
  // Se disponibile nell'authEmail
  if (this.authEmail) {
    return this.authEmail;
  }
  
  // Altrimenti cerca di estrarlo dal token
  const token = localStorage.getItem('token');
  if (!token) return '';
  
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.email || '';
  } catch {
    return '';
  }
}

// Ottieni l'iniziale dell'utente per l'avatar
getUserInitial(): string {
  if (this.userName) {
    return this.userName.charAt(0).toUpperCase();
  }
  if (this.authEmail) {
    return this.authEmail.charAt(0).toUpperCase();
  }
  return 'U';
}
/*
// Elimina una pubblicazione dell'utente
deleteMyDoc(id: number): void {
  if (!confirm('Sei sicuro di voler eliminare questa pubblicazione?')) {
    return;
  }

  const token = localStorage.getItem('token');
  if (!token) {
    alert('Non autenticato!');
    return;
  }

  this.publicationService.deletePublication(id).subscribe({
    next: (response) => {
      // Rimuovi il documento da tutte le liste
      this.myDocs = this.myDocs.filter(d => d.id !== id);
      this.docs = this.docs.filter(d => d.id !== id);
      this.filteredDocs = this.filteredDocs.filter(d => d.id !== id);
      
      alert('Pubblicazione eliminata con successo!');
    },
    error: (err) => {
      console.error('Errore durante la cancellazione:', err);
      const errorMessage = err.error?.message || err.message || 'Errore durante la cancellazione';
      alert('Errore: ' + errorMessage);
    }
  });
}

*/

deleteMyDoc(id: number): void {
  // Trova il documento da eliminare
  this.docToDelete = this.myDocs.find(doc => doc.id === id) || null;
  
  if (!this.docToDelete) {
    this.showError('Documento non trovato');
    return;
  }

  // Mostra il popup di conferma
  this.showDeleteConfirmPopup = true;
}
// Gestione popup di conferma
confirmDelete(): void {
  if (!this.docToDelete) return;

  const token = localStorage.getItem('token');
  if (!token) {
    this.showError('Non autenticato!');
    this.showDeleteConfirmPopup = false;
    return;
  }

  this.publicationService.deletePublication(this.docToDelete.id).subscribe({
    next: (response) => {
      // Rimuovi il documento da tutte le liste
      const docId = this.docToDelete!.id;
      this.myDocs = this.myDocs.filter(d => d.id !== docId);
      this.docs = this.docs.filter(d => d.id !== docId);
      this.filteredDocs = this.filteredDocs.filter(d => d.id !== docId);
      
      // Mostra popup di successo
      this.showDeleteSuccessPopup = true;
      setTimeout(() => this.showDeleteSuccessPopup = false, 3000);
      
      // Chiudi popup di conferma
      this.showDeleteConfirmPopup = false;
      this.docToDelete = null;
    },
    error: (err) => {
      console.error('Errore durante la cancellazione:', err);
      const errorMessage = err.error?.message || err.message || 'Errore durante la cancellazione';
      this.showError(errorMessage);
      this.showDeleteConfirmPopup = false;
    }
  });
}

cancelDelete(): void {
  this.showDeleteConfirmPopup = false;
  this.docToDelete = null;
}

// Metodo per mostrare errori
private showError(message: string): void {
  this.errorMessage = message;
  this.showErrorPopup = true;
  setTimeout(() => this.showErrorPopup = false, 3000);
}

}


