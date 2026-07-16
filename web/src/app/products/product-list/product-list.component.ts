import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ProductService } from '../../core/services/product.service';
import { Product } from '../../core/models/product.model';

@Component({
  selector: 'app-product-list',
  imports: [
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss'
})
export class ProductListComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  products = signal<Product[]>([]);
  loading = signal(false);
  showInactive = signal(false);
  readonly displayedColumns = ['sku', 'name', 'category', 'price', 'stock', 'active', 'actions'];

  ngOnInit(): void {
    this.load();
  }

  toggleShowInactive(checked: boolean): void {
    this.showInactive.set(checked);
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    const source$ = this.showInactive() ? this.productService.getInactive() : this.productService.getActive();
    source$.subscribe({
      next: data => {
        this.products.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Error loading products', 'Close', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  edit(id: number): void {
    this.router.navigate(['/products', id, 'edit']);
  }

  delete(id: number): void {
    this.productService.delete(id).subscribe({
      next: () => {
        this.snackBar.open('Product deleted', 'Close', { duration: 2000 });
        this.load();
      },
      error: () => this.snackBar.open('Error deleting product', 'Close', { duration: 3000 })
    });
  }
}
