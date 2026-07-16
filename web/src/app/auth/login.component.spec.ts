import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from './auth.service';
import { vi } from 'vitest';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let router: Router;

  const authServiceMock = {
    login: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        provideAnimations(),
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not submit when the form is invalid', () => {
    component.submit();

    expect(authServiceMock.login).not.toHaveBeenCalled();
  });

  it('should navigate to /products on successful login', () => {
    authServiceMock.login.mockReturnValue(of({ token: 't', tokenType: 'Bearer', expiresIn: 3600, username: 'admin', roles: ['ADMIN', 'USER'] }));
    component.form.setValue({ username: 'admin', password: 'admin123' });

    component.submit();

    expect(authServiceMock.login).toHaveBeenCalledWith('admin', 'admin123');
    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('should show an error message and stop submitting on failed login', () => {
    authServiceMock.login.mockReturnValue(throwError(() => new Error('401')));
    component.form.setValue({ username: 'admin', password: 'wrong' });

    component.submit();

    expect(component.errorMessage()).toBe('Usuario o contraseña incorrectos');
    expect(component.submitting()).toBe(false);
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
