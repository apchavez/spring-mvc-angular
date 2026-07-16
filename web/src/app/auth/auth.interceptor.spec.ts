import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { vi } from 'vitest';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let router: Router;

  const authServiceMock = {
    token: vi.fn().mockReturnValue(null),
    logout: vi.fn()
  };

  beforeEach(() => {
    vi.clearAllMocks();
    authServiceMock.token.mockReturnValue(null);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => httpMock.verify());

  it('should not add an Authorization header when there is no token', () => {
    http.get('/api/v1/products/active').subscribe();

    const req = httpMock.expectOne('/api/v1/products/active');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should add a Bearer Authorization header when a token exists', () => {
    authServiceMock.token.mockReturnValue('fake-jwt');

    http.get('/api/v1/products/active').subscribe();

    const req = httpMock.expectOne('/api/v1/products/active');
    expect(req.request.headers.get('Authorization')).toBe('Bearer fake-jwt');
    req.flush({});
  });

  it('should log out and redirect to /login on a 401 from a protected endpoint', () => {
    authServiceMock.token.mockReturnValue('expired-jwt');

    http.get('/api/v1/products/active').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/v1/products/active');
    req.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceMock.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should NOT redirect on a 401 from the login endpoint itself', () => {
    http.post('/api/v1/auth/login', {}).subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/v1/auth/login');
    req.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceMock.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
