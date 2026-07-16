package com.apchavez.products.infrastructure.batch;

import org.springframework.batch.infrastructure.item.file.mapping.FieldSetMapper;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

/**
 * Strict CSV row -&gt; record mapping. Exceptions thrown here are wrapped by
 * {@code FlatFileItemReader} into {@code FlatFileParseException} (which carries the real
 * 1-based line number), so this class deliberately does no type coercion or business
 * validation itself — that happens downstream in {@link ProductImportItemProcessor}, where a
 * failure is a process-skip instead of a read-skip.
 */
public class ProductCsvFieldSetMapper implements FieldSetMapper<ProductCsvRow> {

    @Override
    public ProductCsvRow mapFieldSet(FieldSet fieldSet) throws BindException {
        return new ProductCsvRow(
                trim(fieldSet.readString("sku")),
                trim(fieldSet.readString("name")),
                trim(fieldSet.readString("description")),
                trim(fieldSet.readString("category")),
                trim(fieldSet.readString("price")),
                trim(fieldSet.readString("stock")),
                trim(fieldSet.readString("active")));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
