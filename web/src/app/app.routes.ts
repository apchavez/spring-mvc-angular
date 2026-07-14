import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'products', pathMatch: 'full' },
  {
    path: 'products',
    loadComponent: () =>
      import('./products/product-list/product-list.component').then(m => m.ProductListComponent)
  },
  {
    path: 'products/new',
    loadComponent: () =>
      import('./products/product-form/product-form.component').then(m => m.ProductFormComponent)
  },
  {
    path: 'products/:id/edit',
    loadComponent: () =>
      import('./products/product-form/product-form.component').then(m => m.ProductFormComponent)
  },
  { path: '**', redirectTo: 'products' }
];
