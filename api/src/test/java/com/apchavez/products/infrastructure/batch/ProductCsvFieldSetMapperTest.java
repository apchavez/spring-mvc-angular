package com.apchavez.products.infrastructure.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.file.transform.DefaultFieldSet;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCsvFieldSetMapperTest {

    private static final String[] NAMES = {"sku", "name", "description", "category", "price", "stock", "active"};

    private final ProductCsvFieldSetMapper mapper = new ProductCsvFieldSetMapper();

    @Test
    void mapFieldSet_shouldTrimAllColumns() throws Exception {
        FieldSet fieldSet = new DefaultFieldSet(
                new String[]{" SKU-100 ", " Mouse ", " desc ", " Electronics ", " 29.99 ", " 150 ", " true "}, NAMES);

        ProductCsvRow row = mapper.mapFieldSet(fieldSet);

        assertThat(row.sku()).isEqualTo("SKU-100");
        assertThat(row.name()).isEqualTo("Mouse");
        assertThat(row.description()).isEqualTo("desc");
        assertThat(row.category()).isEqualTo("Electronics");
        assertThat(row.price()).isEqualTo("29.99");
        assertThat(row.stock()).isEqualTo("150");
        assertThat(row.active()).isEqualTo("true");
    }

    @Test
    void mapFieldSet_shouldPassThroughEmptyColumns() throws Exception {
        FieldSet fieldSet = new DefaultFieldSet(
                new String[]{"", "NoSku", "desc", "Electronics", "10.0", "5", "true"}, NAMES);

        ProductCsvRow row = mapper.mapFieldSet(fieldSet);

        assertThat(row.sku()).isEmpty();
        assertThat(row.name()).isEqualTo("NoSku");
    }
}
