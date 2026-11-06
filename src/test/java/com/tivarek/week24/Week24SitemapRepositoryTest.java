package com.tivarek.week24;
import com.tivarek.TivarekApplication;
import com.tivarek.week13.StorefrontRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes=TivarekApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Week24SitemapRepositoryTest {
    @Autowired JdbcTemplate db;
    @Autowired StorefrontRepository repository;
    @Test void sitemapFollowsPublicationRulesAndNeverIncludesCustomerDevices() {
        String sku="S24-"+UUID.randomUUID().toString().substring(0,10);
        db.update("INSERT INTO dbo.Products(SKU,Name,ListPrice,CategoryID,BrandID,UnitID,StockMode,IsPublished) VALUES (?,N'Test SEO',10,1,1,1,'BULK',1)",sku);
        long id=db.queryForObject("SELECT ProductID FROM dbo.Products WHERE SKU=?",Long.class,sku);
        String path="/san-pham/phu-kien/"+id;
        assertTrue(repository.sitemapPaths().contains(path));
        db.update("UPDATE dbo.Products SET IsPublished=0 WHERE ProductID=?",id);
        assertFalse(repository.sitemapPaths().contains(path));
        db.update("UPDATE dbo.Products SET StockMode='SERIALIZED',IsPublished=1 WHERE ProductID=?",id);
        String asset="D24-"+UUID.randomUUID().toString().substring(0,10);
        db.update("INSERT INTO dbo.Devices(AssetTag,ProductID,DeviceDescription,ConditionGrade,AskingPrice,IsPublished) VALUES (?,?,N'Test SEO device','GOOD',20,1)",asset,id);
        long deviceId=db.queryForObject("SELECT DeviceID FROM dbo.Devices WHERE AssetTag=?",Long.class,asset);
        String devicePath="/san-pham/thiet-bi/"+deviceId;
        assertTrue(repository.sitemapPaths().contains(devicePath));
        db.update("INSERT INTO dbo.Customers(CustomerCode,FullName,Phone) VALUES (?,N\'Test SEO owner\',\'0912345678\')",sku);
        Long customer=db.queryForObject("SELECT CustomerID FROM dbo.Customers WHERE CustomerCode=?",Long.class,sku);
        db.update("UPDATE dbo.Devices SET OwnerCustomerID=? WHERE DeviceID=?",customer,deviceId);
        assertFalse(repository.sitemapPaths().contains(devicePath));
    }
}
