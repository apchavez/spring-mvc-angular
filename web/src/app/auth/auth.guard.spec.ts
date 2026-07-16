import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { vi } from 'vitest';

describe('authGuard', () => {
  const authServiceMock = {
    isAuthenticated: vi.fn()
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock }
      ]
    });
  });

  function runGuard() {
    return TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
  }

  it('should allow navigation when authenticated', () => {
    authServiceMock.isAuthenticated.mockReturnValue(true);

    expect(runGuard()).toBe(true);
  });

  it('should redirect to /login when not authenticated', () => {
    authServiceMock.isAuthenticated.mockReturnValue(false);
    const router = TestBed.inject(Router);
    const expectedTree = router.createUrlTree(['/login']);

    const result = runGuard();

    expect(result).toEqual(expectedTree);
  });
});
