export interface Document {
  id: number;
  title: string;
  year: number;
  authors: string;
  paper: string;
  fileUrl: string;
  keywords: Array<string>;
  showAllKeywords?: boolean; // Proprietà aggiunta per il toggle
}