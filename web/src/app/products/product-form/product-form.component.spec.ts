import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { ProductFormComponent } from './product-form.component';
import { ProductService } from '../../core/services/product.service';
import { vi } from 'vitest';

describe('ProductFormComponent', () => {
  let fixture: ComponentFixture<ProductFormComponent>;
  let component: ProductFormComponent;

  const productServiceMock = {
    getById: vi.fn(),
    create: vi.fn().mockReturnValue(of({
      id: 1, sku: 'SKU-001', name: 'Wireless Mouse', description: 'desc', category: 'Electronics', price: 29.99, stock: 150, active: true
    })),
    update: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      imports: [ProductFormComponent],
      providers: [
        provideRouter([]),
        provideAnimations(),
        { provide: ProductService, useValue: productServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should be in create mode when no id param', () => {
    expect(component.isEdit).toBe(false);
    expect(component.productId()).toBeNull();
  });

  it('should have invalid form when empty', () => {
    component.form.reset();
    expect(component.form.invalid).toBe(true);
  });

  it('should have valid form with correct values', () => {
    component.form.patchValue({
      sku: 'SKU-001', name: 'Wireless Mouse', description: 'desc', category: 'Electronics', price: 29.99, stock: 150, active: true
    });
    expect(component.form.valid).toBe(true);
  });
});
