import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  roles: string[];
}

const TOKEN_KEY = 'auth_token';
const USERNAME_KEY = 'auth_username';
const ROLES_KEY = 'auth_roles';

function readStoredRoles(): string[] | null {
  const raw = localStorage.getItem(ROLES_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as string[];
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/v1/auth`;

  readonly token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  readonly username = signal<string | null>(localStorage.getItem(USERNAME_KEY));
  readonly roles = signal<string[] | null>(readStoredRoles());

  isAuthenticated(): boolean {
    return this.token() !== null;
  }

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, { username, password })
      .pipe(tap(response => {
        localStorage.setItem(TOKEN_KEY, response.token);
        localStorage.setItem(USERNAME_KEY, response.username);
        localStorage.setItem(ROLES_KEY, JSON.stringify(response.roles));
        this.token.set(response.token);
        this.username.set(response.username);
        this.roles.set(response.roles);
      }));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    localStorage.removeItem(ROLES_KEY);
    this.token.set(null);
    this.username.set(null);
    this.roles.set(null);
  }
}
