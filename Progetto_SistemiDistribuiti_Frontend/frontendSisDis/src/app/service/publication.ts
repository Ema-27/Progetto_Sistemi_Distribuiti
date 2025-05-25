// publication.service.ts
import { HttpClient, HttpResponse } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { Document } from "../model/document";

@Injectable({
  providedIn: 'root'
})
export class PublicationService {
  private apiUrl = 'http://localhost:8080/api/publications';

  constructor(private http: HttpClient) {}

  public getPublications(): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.apiUrl}/`);
  }

  public getPublicationsByTitle(title: string): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.apiUrl}/search?title=${title}`);
  }

  public uploadDocument(data: FormData): Observable<Document> {
    return this.http.post<Document>('http://localhost:8080/api/publications/upload', data);
  }

  public importBibtex(data: FormData): Observable<Document> {
    return this.http.post<Document>('http://localhost:8080/api/publications/importBibtex', data);
  }

  public downloadPublication(id: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`http://localhost:8080/api/publications/download/${id}`, {
      observe: 'response',
      responseType: 'blob'
    });
  }

}
