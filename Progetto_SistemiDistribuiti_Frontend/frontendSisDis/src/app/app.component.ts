import { Component, ElementRef, ViewChild, OnInit } from '@angular/core'; // Aggiunto OnInit
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { PublicationService } from './service/publication'; // Assicurati che il percorso sia corretto
import { Document } from './model/document'; // Assicurati che il percorso sia corretto

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit { // Implementato OnInit
  title = 'frontendSisDis';
  activeTab: 'upload' | 'bibtex' = 'upload';
  uploadForm!: FormGroup;
  bibtexForm!: FormGroup;
  docs: Document[] = [];
  filteredDocs: Document[] = [];
  showSuccessPopup = false;

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @ViewChild('bibtexInput') bibtexInput!: ElementRef<HTMLInputElement>;
  @ViewChild('docFileInput') docFileInput!: ElementRef<HTMLInputElement>;

  constructor(private fb: FormBuilder, private publicationService: PublicationService) {}

  ngOnInit(): void {
    this.uploadForm = this.fb.group({
      title: ['', Validators.required],
      authors: ['', Validators.required],
      year: [new Date().getFullYear(), Validators.required],
      paper: ['', Validators.required]
    });

    this.bibtexForm = this.fb.group({}); // Lasciato vuoto come nell'originale
    this.getPublications();
  }

  getPublications(): void {
    this.publicationService.getPublications().subscribe(
      (response) => {
        // Aggiungi showAllKeywords a ogni documento e assicurati che keywords sia un array
        this.docs = response.map(doc => ({
          ...doc,
          keywords: doc.keywords || [], // Assicura che keywords sia sempre un array
          showAllKeywords: false
        }));
        this.filteredDocs = [...this.docs];
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
      paper: this.uploadForm.value.paper
    };

    const formData = new FormData();
    formData.append('file', file);
    formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));

    this.publicationService.uploadDocument(formData).subscribe(
      (newDoc) => {
        // Aggiungi showAllKeywords al nuovo documento
        const docWithToggle: Document = {
          ...newDoc,
          keywords: newDoc.keywords || [],
          showAllKeywords: false
        };
        this.docs.push(docWithToggle);
        this.filteredDocs = [...this.docs]; // Ricrea l'array per triggerare il change detection se necessario
        this.uploadForm.reset({ year: new Date().getFullYear() });
        this.fileInput.nativeElement.value = '';
        this.showSuccessPopup = true;
        setTimeout(() => this.showSuccessPopup = false, 3000);
      },
      () => alert('Errore durante il caricamento')
    );
  }

  onImportBibtex(): void {
    const bibtexFile = this.bibtexInput.nativeElement.files?.[0];
    const docFile = this.docFileInput.nativeElement.files?.[0];

    if (!bibtexFile || !docFile) {
      alert('Devi selezionare sia il file .bib che il documento');
      return;
    }

    const formData = new FormData();
    const metadata = { // Placeholder metadata come nell'originale
      title: '',
      authors: '',
      year: new Date().getFullYear(),
      paper: ''
    };

    formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
    formData.append('file', docFile);
    formData.append('bibtex', bibtexFile);

    // L'originale chiamava uploadDocument. Se hai un endpoint specifico per importBibtex che restituisce Document,
    // potresti voler usare this.publicationService.importBibtex(formData).subscribe(...)
    this.publicationService.uploadDocument(formData).subscribe(
      (newDoc) => {
        // Aggiungi showAllKeywords al nuovo documento
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
        this.showSuccessPopup = true;
        setTimeout(() => this.showSuccessPopup = false, 3000);
      },
      () => alert("Errore durante l'import")
    );
  }

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value.toLowerCase();
    this.filteredDocs = this.docs.filter(doc =>
      doc.title.toLowerCase().includes(value) ||
      doc.authors.toLowerCase().includes(value) ||
      doc.keywords?.some(k => k.toLowerCase().includes(value)) // keywords è ora garantito come array o undefined
    );
  }

  onDownload(doc: Document): void {
    this.publicationService.downloadPublication(doc.id).subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) { // Controllo aggiunto per sicurezza
          const extension = this.getExtensionFromUrl(doc.fileUrl);
          const filename = `${doc.title}${extension}`;
          const url = window.URL.createObjectURL(res.body);
          const a = document.createElement('a');
          a.href = url;
          a.download = filename;
          document.body.appendChild(a); // Necessario per Firefox
          a.click();
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a); // Pulisci
        } else {
          alert('Errore: corpo della risposta vuoto.');
        }
      },
      error: () => alert('Errore nel download')
    });
  }

  private getExtensionFromUrl(url: string): string {
    if (!url) return ''; // Controllo aggiunto
    const lastDot = url.lastIndexOf('.');
    if (lastDot === -1) return '';
    const ext = url.substring(lastDot);
    return ext.includes('/') ? '' : ext; // Semplice controllo per evitare path come estensioni
  }
}