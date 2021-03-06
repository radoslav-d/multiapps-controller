package org.cloudfoundry.multiapps.controller.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CachedObjectTest {

    @SuppressWarnings("unchecked")
    @Test
    void testGet() {
        LongSupplier currentTimeSupplier = Mockito.mock(LongSupplier.class);
        Mockito.when(currentTimeSupplier.getAsLong())
               .thenReturn(0L, toMillis(5), toMillis(15), toMillis(25), toMillis(30));

        Supplier<String> refreshFunction = Mockito.mock(Supplier.class);
        Mockito.when(refreshFunction.get())
               .thenReturn("a", "b");

        CachedObject<String> cachedName = new CachedObject<>(10, currentTimeSupplier);

        assertEquals("a", cachedName.get(refreshFunction));
        assertEquals("a", cachedName.get(refreshFunction));
        assertEquals("b", cachedName.get(refreshFunction));
        assertEquals("b", cachedName.get(refreshFunction));
        assertEquals("b", cachedName.get(refreshFunction));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testForceRefresh() {
        LongSupplier currentTimeSupplier = Mockito.mock(LongSupplier.class);
        Mockito.when(currentTimeSupplier.getAsLong())
               .thenReturn(0L, toMillis(5), toMillis(10), toMillis(15), toMillis(25));

        Supplier<String> refreshFunction = Mockito.mock(Supplier.class);
        Mockito.when(refreshFunction.get())
               .thenReturn("a", "b", "c");

        CachedObject<String> cachedName = new CachedObject<>(20, currentTimeSupplier);

        assertEquals("a", cachedName.get(refreshFunction));
        assertEquals("a", cachedName.get(refreshFunction));
        assertEquals("b", cachedName.forceRefresh(refreshFunction));
        assertEquals("b", cachedName.get(refreshFunction));
    }

    private Long toMillis(int seconds) {
        return TimeUnit.SECONDS.toMillis(seconds);
    }

}
