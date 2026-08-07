import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ProductService } from './product.service';
import { Product, ProductRequest } from '../models/product.model';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;

  const mockProduct: Product = {
    id: 1, sku: 'SKU-001', name: 'Wireless Mouse', description: 'desc', category: 'Electronics', price: 29.99, stock: 150, active: true
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should get active products', () => {
    service.getActive().subscribe(products => {
      expect(products).toEqual([mockProduct]);
    });
    const req = httpMock.expectOne(r => r.url.endsWith('/active'));
    expect(req.request.method).toBe('GET');
    req.flush({ content: [mockProduct], page: 0, size: 20, totalElements: 1, totalPages: 1, last: true });
  });

  it('should get inactive products', () => {
    const inactiveProduct: Product = { ...mockProduct, id: 2, active: false };
    service.getInactive().subscribe(products => {
      expect(products).toEqual([inactiveProduct]);
    });
    const req = httpMock.expectOne(r => r.url.endsWith('/inactive'));
    expect(req.request.method).toBe('GET');
    req.flush({ content: [inactiveProduct], page: 0, size: 20, totalElements: 1, totalPages: 1, last: true });
  });

  it('should get product by id', () => {
    service.getById(1).subscribe(p => expect(p).toEqual(mockProduct));
    const req = httpMock.expectOne(r => r.url.endsWith('/1'));
    expect(req.request.method).toBe('GET');
    req.flush(mockProduct);
  });

  it('should create a product', () => {
    const request: ProductRequest = { sku: 'SKU-001', name: 'Wireless Mouse', description: 'desc', category: 'Electronics', price: 29.99, stock: 150, active: true };
    service.create(request).subscribe(p => expect(p).toEqual(mockProduct));
    const req = httpMock.expectOne(r => r.method === 'POST');
    req.flush(mockProduct);
  });

  it('should update a product', () => {
    const request: ProductRequest = { sku: 'SKU-001', name: 'Wireless Mouse Pro', description: 'desc2', category: 'Electronics', price: 34.99, stock: 120, active: false };
    const updated: Product = { id: 1, ...request };
    service.update(1, request).subscribe(p => expect(p).toEqual(updated));
    const req = httpMock.expectOne(r => r.method === 'PUT' && r.url.endsWith('/1'));
    req.flush(updated);
  });

  it('should delete a product', () => {
    service.delete(1).subscribe();
    const req = httpMock.expectOne(r => r.method === 'DELETE' && r.url.endsWith('/1'));
    req.flush(null);
  });
});
