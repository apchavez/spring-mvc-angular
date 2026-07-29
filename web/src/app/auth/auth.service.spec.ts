import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService, LoginResponse } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const mockResponse: LoginResponse = {
    token: 'fake-jwt', tokenType: 'Bearer', expiresIn: 3600, username: 'admin', roles: ['ADMIN', 'USER']
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should start unauthenticated when localStorage is empty', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
  });

  it('should store the token/username/roles and become authenticated on successful login', () => {
    service.login('admin', 'admin123').subscribe(response => {
      expect(response).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(r => r.url.endsWith('/api/v1/auth/login'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'admin', password: 'admin123' });
    req.flush(mockResponse);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.token()).toBe('fake-jwt');
    expect(service.username()).toBe('admin');
    expect(service.roles()).toEqual(['ADMIN', 'USER']);
    expect(localStorage.getItem('auth_token')).toBe('fake-jwt');
  });

  it('should propagate the error and stay unauthenticated on failed login', () => {
    let errored = false;
    service.login('admin', 'wrong').subscribe({
      error: () => { errored = true; }
    });

    const req = httpMock.expectOne(r => r.url.endsWith('/api/v1/auth/login'));
    req.flush({ status: 401 }, { status: 401, statusText: 'Unauthorized' });

    expect(errored).toBe(true);
    expect(service.isAuthenticated()).toBe(false);
  });

  it('should clear token/username/roles and localStorage on logout', () => {
    service.login('admin', 'admin123').subscribe();
    httpMock.expectOne(r => r.url.endsWith('/api/v1/auth/login')).flush(mockResponse);
    expect(service.isAuthenticated()).toBe(true);

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
    expect(service.username()).toBeNull();
    expect(service.roles()).toBeNull();
    expect(localStorage.getItem('auth_token')).toBeNull();
  });
});
