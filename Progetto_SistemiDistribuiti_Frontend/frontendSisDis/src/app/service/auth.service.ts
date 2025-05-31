import { Injectable } from '@angular/core';
import {
  AuthenticationDetails,
  CognitoUser,
  CognitoUserPool,
  CognitoUserSession, 
  CognitoUserAttribute
} from 'amazon-cognito-identity-js';

import { environment } from 'src/environments/environment';
import { UserService, RegisterUserDto } from './user.service';

const poolData = {
  UserPoolId: environment.cognito.userPoolId, 
  ClientId: environment.cognito.clientId  
};

console.log('DEBUG - Cognito poolData:', poolData);

const userPool = new CognitoUserPool(poolData);

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  
  constructor() {}

  login(email: string, password: string): Promise<CognitoUserSession> {
    const userData = { Username: email, Pool: userPool };
    const authData = { Username: email, Password: password };
    const cognitoUser = new CognitoUser(userData);
    const authDetails = new AuthenticationDetails(authData);

    return new Promise((resolve, reject) => {
      cognitoUser.authenticateUser(authDetails, {
        onSuccess: session => {
          localStorage.setItem('token', session.getIdToken().getJwtToken());
          resolve(session);
        },
        onFailure: err => reject(err)
      });
    });
  }

  register(email: string, password: string, name: string): Promise<any> {
    return new Promise((resolve, reject) => {
      const attributeList = [
        new CognitoUserAttribute({ Name: 'name', Value: name }),
        new CognitoUserAttribute({ Name: 'email', Value: email })
      ];

      userPool.signUp(email, password, attributeList, [], (err, result) => {
        if (err) {
          reject(err);
        } else {
          resolve(result);
        }
      });
    });
  }

  logout(): void {
    const user = userPool.getCurrentUser();
    if (user) {
      user.signOut();
    }
    localStorage.removeItem('token');
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }


}