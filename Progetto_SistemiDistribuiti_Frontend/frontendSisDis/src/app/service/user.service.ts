import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

export interface RegisterUserDto {
  username: string;
  email: string;
  fullName: string;
  temporaryPassword: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private baseUrl = 'http://'+environment.host+':8080/api/users'; 

  constructor(private http: HttpClient) {}

  register(user: RegisterUserDto): Observable<any> {
    return this.http.post(`${this.baseUrl}/register`, user);
  }

  login(username: string, password: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>(
      `${this.baseUrl}/login`,
      { username, temporaryPassword: password }
    );
  }
}
