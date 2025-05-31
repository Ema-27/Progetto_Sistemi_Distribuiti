import { HttpClient, HttpResponse } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { Document } from "../model/document";
import { UserRegistration } from "../model/user-registration";

@Injectable({
  providedIn: 'root'
})
export class PublicationService {
  private apiUrl = 'http://localhost:8080/api/publications';
  private userApiUrl = 'http://localhost:8080/api/users'; // ✅ nuova base URL per utenti

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): any {
    const token = localStorage.getItem('token');
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  public getPublications(): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.apiUrl}/`);
  }

  public getPublicationsByTitle(title: string): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.apiUrl}/search?title=${title}`);
  }

  public uploadDocument(data: FormData): Observable<Document> {
    return this.http.post<Document>(`${this.apiUrl}/upload`, data, { headers: this.getAuthHeaders() });
  }

  public importBibtex(data: FormData): Observable<Document> {
    return this.http.post<Document>(`${this.apiUrl}/importBibtex`, data, { headers: this.getAuthHeaders() });
  }

  public downloadPublication(id: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.apiUrl}/download/${id}`, {
      observe: 'response',
      responseType: 'blob'
    });
  }

  public registerUser(userData: UserRegistration): Observable<any> {
    return this.http.post(`${this.userApiUrl}/register`, userData);
  }

  public deletePublication(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  

  extractKeywords(formData: FormData): Observable<string[]> {
    // Non serve auth qui, ma puoi aggiungerla se necessario
    return this.http.post<string[]>(`${this.apiUrl}/extract-keywords`, formData);
  }
}
