package com.tivarek.week13;

import com.tivarek.TivarekApplication;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=TivarekApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StorefrontIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired StorefrontService storefront;

    private String unique() {return "W13-"+UUID.randomUUID().toString().substring(0,8);}
    private long product(String stockMode) {
        String sku=unique();
        jdbc.update("INSERT INTO dbo.Products(SKU,Name,CategoryID,BrandID,UnitID,StockMode,ListPrice,IsPublished) VALUES (?,?,?,?,?,?,?,?)",
            sku,"Laptop hoặc phụ kiện thử nghiệm",1,1,1,stockMode,new BigDecimal("990000"),stockMode.equals("BULK"));
        return jdbc.queryForObject("SELECT ProductID FROM dbo.Products WHERE SKU=?",Long.class,sku);
    }
    private long document(String status) {
        String code=unique();
        jdbc.update("INSERT INTO dbo.InventoryDocuments(DocumentCode,DocumentType,Status) VALUES (?,'RECEIPT',?)",code,status);
        return jdbc.queryForObject("SELECT InventoryDocID FROM dbo.InventoryDocuments WHERE DocumentCode=?",Long.class,code);
    }
    private long device(long productId) {
        String asset=unique();
        jdbc.update("INSERT INTO dbo.Devices(AssetTag,ProductID,DeviceDescription,ConditionGrade,AskingPrice,IsPublished) VALUES (?,?,?,'GOOD',?,1)",
            asset,productId,"Thiết bị duy nhất tại cửa hàng",new BigDecimal("5500000"));
        return jdbc.queryForObject("SELECT DeviceID FROM dbo.Devices WHERE AssetTag=?",Long.class,asset);
    }

    @Test void bulkLedgerCountsOnlyPostedAndExcludesActiveReservations() {
        long id=product("BULK");
        long receipt=document("POSTED");
        jdbc.update("INSERT INTO dbo.BulkStockEntries(InventoryDocID,ProductID,LocationID,Delta,UnitCost) VALUES (?,?,1,10,10000)",receipt,id);
        long draft=document("DRAFT");
        jdbc.update("INSERT INTO dbo.BulkStockEntries(InventoryDocID,ProductID,LocationID,Delta,UnitCost) VALUES (?,?,1,-100,10000)",draft,id);
        long issue=document("POSTED");
        jdbc.update("INSERT INTO dbo.BulkStockEntries(InventoryDocID,ProductID,LocationID,Delta,UnitCost) VALUES (?,?,1,-3,10000)",issue,id);
        assertEquals(7,storefront.product(id).available(),"Nhập 10, xuất 3 thì còn 7; DRAFT không có tác dụng tồn");
        jdbc.update("INSERT INTO dbo.BulkSaleLines(SaleLineID,ProductID,Quantity) VALUES (?,?,2)",id,id);
        jdbc.update("INSERT INTO dbo.BulkReservations(SaleLineID,LocationID,Quantity,Status,ExpiresAt) VALUES (?,1,2,'ACTIVE',DATEADD('HOUR',1,CURRENT_TIMESTAMP))",id);
        assertEquals(5,storefront.product(id).available(),"Tồn khả dụng phải trừ hàng còn đang giữ");
        assertEquals(7,jdbc.queryForObject("SELECT SUM(Delta) FROM dbo.BulkStockEntries WHERE ProductID=? AND InventoryDocID<>?",Integer.class,id,draft));
    }

    @Test void serializedDeviceHasOneAvailabilityAndHidesCustomerEquipment() throws Exception {
        long template=product("SERIALIZED");
        long id=device(template);
        long receipt=document("POSTED");
        jdbc.update("INSERT INTO dbo.DeviceStockEntries(InventoryDocID,DeviceID,FromLocationID,ToLocationID,UnitCost) VALUES (?,?,NULL,1,4000000)",receipt,id);
        assertEquals(1,storefront.device(id).product().available());
        mvc.perform(get("/san-pham/thiet-bi/"+id)).andExpect(status().isOk());
        jdbc.update("INSERT INTO dbo.ActiveDeviceReservations(DeviceID,ReservationID) VALUES (?,?)",id,id);
        assertEquals(0,storefront.device(id).product().available());
        jdbc.update("DELETE FROM dbo.ActiveDeviceReservations WHERE DeviceID=?",id);
        long issue=document("POSTED");
        jdbc.update("INSERT INTO dbo.DeviceStockEntries(InventoryDocID,DeviceID,FromLocationID,ToLocationID,UnitCost) VALUES (?,?,1,NULL,4000000)",issue,id);
        assertEquals(0,storefront.device(id).product().available());
        jdbc.update("INSERT INTO dbo.Customers(CustomerCode,FullName,Phone) VALUES (?,?,?)",unique(),"Khách hàng riêng","0900000000");
        long customer=jdbc.queryForObject("SELECT MAX(CustomerID) FROM dbo.Customers",Long.class);
        jdbc.update("UPDATE dbo.Devices SET OwnerCustomerID=? WHERE DeviceID=?",customer,id);
        mvc.perform(get("/san-pham/thiet-bi/"+id)).andExpect(status().isNotFound());
        mvc.perform(get("/media/public/devices/"+id+"/images/1")).andExpect(status().isNotFound());
    }

    @Test void publicSearchIsSafeAndPrivateProductsRemainHidden() throws Exception {
        long unpublished=product("BULK");
        jdbc.update("UPDATE dbo.Products SET IsPublished=0 WHERE ProductID=?",unpublished);
        mvc.perform(get("/san-pham")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("TEST-MOUSE")));
        mvc.perform(get("/san-pham").param("q","%')). DROP TABLE dbo.Users;--"))
            .andExpect(status().isOk());
        mvc.perform(get("/san-pham").param("sort","not-a-column"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/san-pham").param("minPrice","999").param("maxPrice","1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/san-pham/phu-kien/"+unpublished)).andExpect(status().isNotFound());
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Users",Integer.class)>=0);
    }

    @Test void publicationRequiresPermissionAndCsrf() throws Exception {
        long id=product("BULK");
        mvc.perform(get("/admin/storefront")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/storefront/products/"+id+"/publication").param("published","false")
            .with(user("no-write").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PRODUCT_READ")))
            .with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/storefront/products/"+id+"/publication").param("published","false")
            .with(user("no-csrf").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PRODUCT_WRITE"))))
            .andExpect(status().isForbidden());
    }

    @Test void unsafeProductImagesAreNotRendered() {
        long id=product("BULK");
        jdbc.update("INSERT INTO dbo.ProductImages(ProductID,ImageURL,SortOrder) VALUES (?,?,0)",id,"javascript:alert(1)");
        assertNull(storefront.product(id).imageUrl());
    }
}
