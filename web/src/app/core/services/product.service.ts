import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { PageResponse, Product, ProductRequest } from '../models/product.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/v1/products`;

  // The API returns a paginated envelope ({content, page, size, ...}), not a bare array —
  // unwrap it here so callers keep working with plain Product[].
  getActive(): Observable<Product[]> {
    return this.http.get<PageResponse<Product>>(`${this.apiUrl}/active`)
      .pipe(map(page => page.content));
  }

  getInactive(): Observable<Product[]> {
    return this.http.get<PageResponse<Product>>(`${this.apiUrl}/inactive`)
      .pipe(map(page => page.content));
  }

  getById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  create(product: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, product);
  }

  update(id: number, product: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${id}`, product);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
