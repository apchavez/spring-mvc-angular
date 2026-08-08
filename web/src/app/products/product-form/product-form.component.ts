import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { ProductService } from '../../core/services/product.service';
import { ProductRequest } from '../../core/models/product.model';

@Component({
  selector: 'app-product-form',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatIconModule
  ],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.scss'
})
export class ProductFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly productService = inject(ProductService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);

  productId = signal<number | null>(null);
  loading = signal(false);
  submitting = signal(false);

  form = this.fb.group({
    sku:         ['', [Validators.required, Validators.maxLength(64)]],
    name:        ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(1000)]],
    category:    ['', [Validators.maxLength(100)]],
    price:       [null as number | null, [Validators.required, Validators.min(0)]],
    stock:       [null as number | null, [Validators.required, Validators.min(0)]],
    active:      [true, Validators.required]
  });

  get isEdit(): boolean { return this.productId() !== null; }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.productId.set(+id);
      this.loading.set(true);
      this.productService.getById(+id).subscribe({
        next: p => { this.form.patchValue(p); this.loading.set(false); },
        error: () => {
          this.snackBar.open('Product not found', 'Close', { duration: 3000 });
          this.router.navigate(['/products']);
        }
      });
    }
  }

  submit(): void {
    if (this.form.invalid) return;
    this.submitting.set(true);
    const value = this.form.value as ProductRequest;
    const op = this.isEdit
      ? this.productService.update(this.productId()!, value)
      : this.productService.create(value);
    op.subscribe({
      next: () => {
        this.snackBar.open(this.isEdit ? 'Product updated' : 'Product created', 'Close', { duration: 2000 });
        this.router.navigate(['/products']);
      },
      error: () => {
        this.snackBar.open('Error saving product', 'Close', { duration: 3000 });
        this.submitting.set(false);
      }
    });
  }
}
