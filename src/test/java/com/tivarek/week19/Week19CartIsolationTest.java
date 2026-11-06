package com.tivarek.week19;

import com.tivarek.week15.SaleCart;
import com.tivarek.week15.SaleProblem;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.*;

/** The cart is per-session and never implicitly reserves real inventory. */
class Week19CartIsolationTest {
    @Test void cartChangesInOneSessionNeverLeakToAnother() {
        var customerA = new MockHttpSession();
        var customerB = new MockHttpSession();

        SaleCart.put(customerA, "BULK", 100, 2);
        assertEquals(2, SaleCart.get(customerA).get("BULK:100"));
        assertTrue(SaleCart.get(customerB).isEmpty());

        SaleCart.put(customerB, "DEVICE", 21, 1);
        SaleCart.put(customerA, "BULK", 100, 0);
        assertTrue(SaleCart.get(customerA).isEmpty());
        assertEquals(1, SaleCart.get(customerB).get("DEVICE:21"));
    }

    @Test void oneOffDeviceCannotBeAddedAsBulkQuantity() {
        var customer = new MockHttpSession();
        SaleCart.put(customer, "DEVICE", 90, 1);
        assertThrows(SaleProblem.class, () -> SaleCart.put(customer, "DEVICE", 90, 2));
        assertEquals(1, SaleCart.get(customer).get("DEVICE:90"));
    }
}
