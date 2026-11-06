package com.tivarek.week11;
import java.util.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
abstract class DeviceManagementContract {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired DeviceService devices;
    @Autowired DeviceImageStorage storage;
    static final Path IMAGE_DIRECTORY=Path.of(System.getProperty("java.io.tmpdir"),"tivarek-week11-tests-"+UUID.randomUUID());
    @DynamicPropertySource static void storageProperties(DynamicPropertyRegistry r) { r.add("tivarek.storage.directory",()->IMAGE_DIRECTORY.toString()); }
    static final String PASSWORD="Week11-Integration-Password";
    static final String HASH=new BCryptPasswordEncoder(4).encode(PASSWORD);
    String prefix;long category,brand,unit,product,bulk;
    abstract void verifyDatabase();
    @BeforeEach void fixture() {
        verifyDatabase();prefix="W11-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
        for(String role:List.of("OWNER","INVENTORY","SALES","TECHNICIAN","CUSTOMER")) {
            jdbc.update("INSERT INTO dbo.Users(LoginName,PasswordHash,DisplayName) VALUES (?,?,?)",prefix+role,HASH,"Week 11 Test");
            long actor=jdbc.queryForObject("SELECT UserID FROM dbo.Users WHERE LoginName=?",Long.class,prefix+role);
            jdbc.update("INSERT INTO dbo.UserRoles(UserID,RoleID) SELECT ?,RoleID FROM dbo.Roles WHERE RoleCode=?",actor,role);
        }
        jdbc.update("INSERT INTO dbo.Categories(CategoryCode,Name) VALUES (?,?)",prefix,"Laptop Test");
        jdbc.update("INSERT INTO dbo.Brands(BrandCode,Name) VALUES (?,?)",prefix,"Dell Test");
        jdbc.update("INSERT INTO dbo.Units(UnitCode,Name) VALUES (?,?)",prefix,"Chiếc Test");
        category=jdbc.queryForObject("SELECT CategoryID FROM dbo.Categories WHERE CategoryCode=?",Long.class,prefix);
        brand=jdbc.queryForObject("SELECT BrandID FROM dbo.Brands WHERE BrandCode=?",Long.class,prefix);
        unit=jdbc.queryForObject("SELECT UnitID FROM dbo.Units WHERE UnitCode=?",Long.class,prefix);
        for(String mode:List.of("SERIALIZED","BULK")) jdbc.update("INSERT INTO dbo.Products(SKU,Name,CategoryID,BrandID,UnitID,StockMode,ListPrice) VALUES (?,?,?,?,?,?,?)",prefix+mode,"Dell Latitude 5420",category,brand,unit,mode,6000000);
        product=jdbc.queryForObject("SELECT ProductID FROM dbo.Products WHERE SKU=?",Long.class,prefix+"SERIALIZED");
        bulk=jdbc.queryForObject("SELECT ProductID FROM dbo.Products WHERE SKU=?",Long.class,prefix+"BULK");
    }
    @AfterEach void cleanup() {
        if(prefix==null) return;
        String like=prefix+"%";
        jdbc.queryForList("SELECT ImageURL FROM dbo.DeviceImages WHERE DeviceID IN (SELECT DeviceID FROM dbo.Devices WHERE AssetTag LIKE ?)",String.class,like).forEach(storage::delete);
        jdbc.update("DELETE FROM dbo.DeviceImages WHERE DeviceID IN (SELECT DeviceID FROM dbo.Devices WHERE AssetTag LIKE ?)",like);
        jdbc.update("DELETE FROM dbo.DeviceChecks WHERE DeviceID IN (SELECT DeviceID FROM dbo.Devices WHERE AssetTag LIKE ?)",like);
        jdbc.update("DELETE FROM dbo.DeviceAttributeValues WHERE DeviceID IN (SELECT DeviceID FROM dbo.Devices WHERE AssetTag LIKE ?)",like);
        jdbc.update("DELETE FROM dbo.Devices WHERE AssetTag LIKE ?",like);
        jdbc.update("DELETE FROM dbo.Products WHERE SKU LIKE ?",like);
        jdbc.update("DELETE FROM dbo.Customers WHERE CustomerCode LIKE ?",like);
        jdbc.update("DELETE FROM dbo.Categories WHERE CategoryCode=?",prefix);
        jdbc.update("DELETE FROM dbo.Brands WHERE BrandCode=?",prefix);
        jdbc.update("DELETE FROM dbo.Units WHERE UnitCode=?",prefix);
        jdbc.update("DELETE FROM dbo.AuditLogs WHERE ActorID IN (SELECT UserID FROM dbo.Users WHERE LoginName LIKE ?)",like);
        jdbc.update("DELETE FROM dbo.UserRoles WHERE UserID IN (SELECT UserID FROM dbo.Users WHERE LoginName LIKE ?)",like);
        jdbc.update("DELETE FROM dbo.Users WHERE LoginName LIKE ?",like);
    }
    MockHttpSession login(String role) throws Exception {
        return (MockHttpSession)mvc.perform(formLogin().user(prefix+role).password(PASSWORD)).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(role.equals("CUSTOMER")?"/account":"/admin")).andReturn().getRequest().getSession(false);
    }
    String version(long id) { return HexFormat.of().formatHex(jdbc.queryForObject("SELECT Version FROM dbo.Devices WHERE DeviceID=?",byte[].class,id)); }
    String payload(String suffix,String version) {
        return "{\"assetTag\":\""+prefix+suffix+"\",\"productId\":"+product+",\"manufacturerSerial\":\"SN-"+prefix+suffix+"\",\"description\":\"Máy thử nghiệm\",\"conditionGrade\":\"GOOD\",\"askingPrice\":5200000,\"faultDescription\":\"Trầy nhẹ\",\"version\":\""+version+"\",\"specifications\":{\"RAM_GB\":\"16\",\"SSD_GB\":\"256\",\"CPU\":\"Intel Core i5\"}}";
    }
    long create(MockHttpSession s,String suffix) throws Exception {
        mvc.perform(post("/api/admin/devices").session(s).with(csrf()).contentType("application/json").content(payload(suffix,"")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.device.assetTag").value(prefix+suffix)).andExpect(jsonPath("$.specifications",hasSize(3)));
        return jdbc.queryForObject("SELECT DeviceID FROM dbo.Devices WHERE AssetTag=?",Long.class,prefix+suffix);
    }
    MockMultipartFile png(String name) throws Exception {
        var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(12,8,BufferedImage.TYPE_INT_RGB),"png",output);
        return new MockMultipartFile("files",name,"image/png",output.toByteArray());
    }
    long addImage(MockHttpSession s,long id,String name) throws Exception {
        mvc.perform(multipart("/api/admin/devices/"+id+"/images").file(png(name)).session(s).with(csrf()).param("version",version(id))).andExpect(status().isCreated());
        return jdbc.queryForObject("SELECT MAX(ImageID) FROM dbo.DeviceImages WHERE DeviceID=?",Long.class,id);
    }
    @Test void twoMachinesKeepIndependentConfigurationPriceAndProduct() throws Exception {
        var s=login("OWNER");long first=create(s,"-01"),second=create(s,"-02");String old=version(first);
        mvc.perform(put("/api/admin/devices/"+first).session(s).with(csrf()).contentType("application/json").content(payload("-01",old).replace("5200000","4700000").replace("\"16\"","\"8\"")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.device.askingPrice").value(4700000));
        assertNotEquals(old,version(first));
        assertEquals(0,new BigDecimal("5200000").compareTo(jdbc.queryForObject("SELECT AskingPrice FROM dbo.Devices WHERE DeviceID=?",BigDecimal.class,second)));
        assertEquals("16",jdbc.queryForObject("SELECT v.ValueText FROM dbo.DeviceAttributeValues v JOIN dbo.AttributeDefinitions a ON a.AttributeID=v.AttributeID WHERE v.DeviceID=? AND a.AttributeCode='RAM_GB'",String.class,second));
        assertEquals(0,new BigDecimal("6000000").compareTo(jdbc.queryForObject("SELECT ListPrice FROM dbo.Products WHERE ProductID=?",BigDecimal.class,product)));
        mvc.perform(post("/logout").session(s).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/admin/devices/"+first).session(login("OWNER"))).andExpect(status().isOk()).andExpect(jsonPath("$.device.askingPrice").value(4700000));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.AuditLogs WHERE EntityKey=? AND Action='DEVICE_PRICE_UPDATE'",Integer.class,Long.toString(first)));
    }
    @Test void duplicateAssetTagIsBlockedByApiAndNormalizedDatabaseKey() throws Exception {
        var s=login("OWNER");create(s,"-DUP");
        mvc.perform(post("/api/admin/devices").session(s).with(csrf()).contentType("application/json").content(payload("-DUP","").replace(prefix+"-DUP","  "+(prefix+"-DUP").toLowerCase(Locale.ROOT)+"  "))).andExpect(status().isConflict());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO dbo.Devices(AssetTag,ProductID,DeviceDescription,ConditionGrade) VALUES (?,?,?,?)"," "+(prefix+"-DUP").toLowerCase()+" ",product,"Duplicate","GOOD"));
    }
    @Test void validatesPriceGradeConfigurationAndProductMode() throws Exception {
        var s=login("OWNER");String body=payload("-BAD","");
        for(String invalid:List.of(body.replace("5200000","-1"),body.replace("GOOD","WRONG"),body.replace("\"16\"","\"abc\""),body.replace("RAM_GB","UNKNOWN"),body.replace("Máy thử nghiệm"," "),body.replace("\"productId\":"+product,"\"productId\":"+bulk),body.replace("\"productId\":"+product,"\"productId\":99999999")))
            mvc.perform(post("/api/admin/devices").session(s).with(csrf()).contentType("application/json").content(invalid)).andExpect(status().isBadRequest());
        jdbc.update("UPDATE dbo.Products SET IsActive=0 WHERE ProductID=?",product);
        mvc.perform(post("/api/admin/devices").session(s).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Devices WHERE AssetTag=?",Integer.class,prefix+"-BAD"));
    }
    @Test void nullPriceBlankSerialAndDuplicateSerialWarning() throws Exception {
        var s=login("OWNER");String body=payload("-NULL","").replace("5200000","null").replace("SN-"+prefix+"-NULL","   ");
        mvc.perform(post("/api/admin/devices").session(s).with(csrf()).contentType("application/json").content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.device.askingPrice").isEmpty()).andExpect(jsonPath("$.device.manufacturerSerial").isEmpty());
        long first=create(s,"-SN1"),second=create(s,"-SN2");
        jdbc.update("UPDATE dbo.Devices SET ManufacturerSerial=? WHERE DeviceID=?","SN-"+prefix+"-SN1",second);
        mvc.perform(get("/api/admin/devices/"+first).session(s)).andExpect(jsonPath("$.duplicateSerial").value(true));
    }
    @Test void rowVersionPreventsOverwritingAnotherUsersChange() throws Exception {
        var s=login("OWNER");long id=create(s,"-VERSION");String body=payload("-VERSION",version(id));
        mvc.perform(put("/api/admin/devices/"+id).session(s).with(csrf()).contentType("application/json").content(body.replace("5200000","4900000"))).andExpect(status().isOk());
        mvc.perform(put("/api/admin/devices/"+id).session(s).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
        mvc.perform(put("/api/admin/devices/"+id).session(s).with(csrf()).contentType("application/json").content(body.replace("\"version\":\""+body.split("\"version\":\"")[1].split("\"")[0],"\"version\":\"invalid"))).andExpect(status().isBadRequest());
    }
    @ParameterizedTest @CsvSource({"OWNER,200,201,201","INVENTORY,200,201,201","SALES,200,403,403","TECHNICIAN,200,403,201","CUSTOMER,403,403,403"})
    void permissionsCoverReadingCreatingEditingImagesAndChecks(String role,int read,int write,int technical) throws Exception {
        var owner=login("OWNER");long id=create(owner,"-ROLE");var s=login(role);
        mvc.perform(get("/api/admin/devices").session(s)).andExpect(status().is(read));
        mvc.perform(get("/admin/devices").session(s)).andExpect(status().is(read));
        mvc.perform(get("/admin/devices/"+id).session(s)).andExpect(status().is(read));
        mvc.perform(get("/admin/devices/new").session(s)).andExpect(status().is(write==201?200:403));
        mvc.perform(post("/api/admin/devices").session(s).with(csrf()).contentType("application/json").content(payload("-"+role,""))).andExpect(status().is(write));
        mvc.perform(put("/api/admin/devices/"+id).session(s).with(csrf()).contentType("application/json").content(payload("-ROLE",version(id)))).andExpect(status().is(write==201?200:403));
        mvc.perform(post("/api/admin/devices/"+id+"/checks").session(s).with(csrf()).contentType("application/json").content("{\"findings\":\"Test\",\"result\":\"PASS\",\"version\":\""+version(id)+"\"}" )).andExpect(status().is(technical));
        mvc.perform(multipart("/api/admin/devices/"+id+"/images").file(png("test.png")).session(s).with(csrf()).param("version",version(id))).andExpect(status().is(technical));
    }
    @Test void customerOwnedDevicesAreHiddenOnEveryRoute() throws Exception {
        var s=login("OWNER");
        jdbc.update("INSERT INTO dbo.Customers(CustomerCode,FullName,Phone) VALUES (?,?,?)",prefix,"Khách Test","0000000000");
        long customer=jdbc.queryForObject("SELECT CustomerID FROM dbo.Customers WHERE CustomerCode=?",Long.class,prefix);
        jdbc.update("INSERT INTO dbo.Devices(AssetTag,ProductID,OwnerCustomerID,DeviceDescription,ConditionGrade) VALUES (?,?,?,?,?)",prefix+"-PRIVATE",product,customer,"Máy khách","FAULTY");
        long id=jdbc.queryForObject("SELECT DeviceID FROM dbo.Devices WHERE AssetTag=?",Long.class,prefix+"-PRIVATE");
        mvc.perform(get("/api/admin/devices").session(s).param("q",prefix+"-PRIVATE")).andExpect(jsonPath("$.items",hasSize(0)));
        for(String path:List.of("/api/admin/devices/"+id,"/admin/devices/"+id,"/admin/devices/"+id+"/edit","/api/admin/devices/"+id+"/images/1/content")) mvc.perform(get(path).session(s)).andExpect(status().isNotFound());
        mvc.perform(put("/api/admin/devices/"+id).session(s).with(csrf()).contentType("application/json").content(payload("-PRIVATE",version(id)))).andExpect(status().isNotFound());
    }
    @Test void imagesAreRealPrivateFilesAndOrderCanChangeWithoutUniqueConflict() throws Exception {
        var s=login("OWNER");long id=create(s,"-IMAGES"),first=addImage(s,id,"../../outside.png"),second=addImage(s,id,"another.png");
        String key=jdbc.queryForObject("SELECT ImageURL FROM dbo.DeviceImages WHERE ImageID=?",String.class,first);assertTrue(key.matches("[0-9a-f-]+\\.png"));assertTrue(Files.isRegularFile(IMAGE_DIRECTORY.resolve(key)));
        mvc.perform(get("/api/admin/devices/"+id+"/images/"+first+"/content")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/devices/"+id+"/images/"+first+"/content").session(s)).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(header().string("Cache-Control",containsString("no-store")));
        mvc.perform(put("/api/admin/devices/"+id+"/images/order").session(s).with(csrf()).contentType("application/json").content("{\"imageIds\":["+second+","+first+"],\"version\":\""+version(id)+"\"}" )).andExpect(status().isOk()).andExpect(jsonPath("$.images[0].imageId").value(second));
        mvc.perform(get("/admin/devices/"+id).session(s)).andExpect(status().isOk()).andExpect(content().string(containsString("Ảnh đại diện")));
        mvc.perform(post("/admin/devices/"+id+"/images/"+first+"/move").session(s).with(csrf()).param("action","cover").param("version",version(id))).andExpect(status().is3xxRedirection());
        mvc.perform(delete("/api/admin/devices/"+id+"/images/"+first).session(s).with(csrf()).param("version",version(id))).andExpect(status().isOk());
        assertFalse(Files.exists(IMAGE_DIRECTORY.resolve(key)));
        mvc.perform(get("/api/admin/devices/"+id+"/images/"+first+"/content").session(s)).andExpect(status().isNotFound());
    }
    @Test void rejectsInvalidOversizedTooManyAndForeignImages() throws Exception {
        var s=login("OWNER");long id=create(s,"-BADIMG"),other=create(s,"-OTHERIMG");long image=addImage(s,other,"other.png");
        for(var file:List.of(new MockMultipartFile("files","script.png","image/png","<script>bad</script>".getBytes()),new MockMultipartFile("files","large.png","image/png",new byte[(int)DeviceImageStorage.MAX_BYTES+1])))
            mvc.perform(multipart("/api/admin/devices/"+id+"/images").file(file).session(s).with(csrf()).param("version",version(id))).andExpect(status().isBadRequest());
        var tooMany=multipart("/api/admin/devices/"+id+"/images");for(int i=0;i<11;i++) tooMany.file(png("a.png"));
        mvc.perform(tooMany.session(s).with(csrf()).param("version",version(id))).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/admin/devices/"+id+"/images/"+image).session(s).with(csrf()).param("version",version(id))).andExpect(status().isNotFound());
        mvc.perform(put("/api/admin/devices/"+id+"/images/order").session(s).with(csrf()).contentType("application/json").content("{\"imageIds\":["+image+"],\"version\":\""+version(id)+"\"}" )).andExpect(status().isBadRequest());
    }
    @Test void checkIsAppendOnlyAndActorComesFromSession() throws Exception {
        var owner=login("OWNER");long id=create(owner,"-CHECK");var tech=login("TECHNICIAN");
        for(String result:List.of("NEEDS_REPAIR","PASS")) mvc.perform(post("/api/admin/devices/"+id+"/checks").session(tech).with(csrf()).contentType("application/json").content("{\"findings\":\"Kiểm tra máy\",\"result\":\""+result+"\",\"version\":\""+version(id)+"\",\"checkedBy\":1,\"askingPrice\":0}" )).andExpect(status().isCreated());
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.DeviceChecks WHERE DeviceID=?",Integer.class,id));
        assertEquals(prefix+"TECHNICIAN",jdbc.queryForObject("SELECT TOP (1) u.LoginName FROM dbo.DeviceChecks c JOIN dbo.Users u ON u.UserID=c.CheckedBy WHERE c.DeviceID=? ORDER BY c.CheckID DESC",String.class,id));
        mvc.perform(get("/api/admin/devices/"+id).session(owner)).andExpect(jsonPath("$.device.inspectionResult").value("PASS")).andExpect(jsonPath("$.device.askingPrice").value(5200000));
    }
    @Test void formBindingsTemplatesAndValidationPreserveInput() throws Exception {
        var s=login("OWNER");mvc.perform(get("/admin/devices/new").session(s)).andExpect(status().isOk()).andExpect(content().string(containsString("specifications[RAM_GB]")));
        mvc.perform(post("/admin/devices").session(s).with(csrf()).param("assetTag",prefix+"-FORM").param("productId",Long.toString(product)).param("description","Máy từ biểu mẫu").param("conditionGrade","GOOD").param("askingPrice","4500000").param("version","").param("specifications[RAM_GB]","8").param("specifications[CPU]","Intel Core i7"))
            .andExpect(status().is3xxRedirection());
        long id=jdbc.queryForObject("SELECT DeviceID FROM dbo.Devices WHERE AssetTag=?",Long.class,prefix+"-FORM");
        mvc.perform(get("/admin/devices/"+id+"/edit").session(s)).andExpect(status().isOk()).andExpect(content().string(containsString("Intel Core i7")));
        mvc.perform(post("/admin/devices/"+id).session(s).with(csrf()).param("assetTag",prefix+"-FORM").param("productId",Long.toString(product)).param("description","Máy đã sửa form").param("conditionGrade","GOOD").param("version",version(id)).param("specifications[RAM_GB]","16"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/devices").session(s).with(csrf()).param("assetTag",prefix+"-KEEP").param("productId",Long.toString(product)).param("description","").param("conditionGrade","GOOD").param("version","").param("specifications[RAM_GB]","8"))
            .andExpect(status().isOk()).andExpect(content().string(containsString(prefix+"-KEEP")));
        mvc.perform(multipart("/admin/devices/"+id+"/images").file(png("form.png")).session(s).with(csrf()).param("version",version(id))).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/devices/"+id).session(s)).andExpect(status().isOk()).andExpect(content().string(containsString("Máy đã sửa form")));
    }
    @Test void searchFiltersLiteralCharactersAndPagination() throws Exception {
        var s=login("OWNER");for(int i=0;i<27;i++) jdbc.update("INSERT INTO dbo.Devices(AssetTag,ProductID,ManufacturerSerial,DeviceDescription,ConditionGrade,AskingPrice) VALUES (?,?,?,?,?,?)",prefix+"-PAGE-"+i,product,"100%_"+i,"Paging","GOOD",100);
        mvc.perform(get("/api/admin/devices").session(s).param("q",prefix).param("categoryId",Long.toString(category)).param("brandId",Long.toString(brand)).param("minPrice","50").param("maxPrice","150")).andExpect(status().isOk()).andExpect(jsonPath("$.items",hasSize(25))).andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/api/admin/devices").session(s).param("q",prefix).param("page","1")).andExpect(jsonPath("$.items",hasSize(2)));
        mvc.perform(get("/api/admin/devices").session(s).param("q","%_").param("productId",Long.toString(product))).andExpect(jsonPath("$.items",hasSize(25)));
        mvc.perform(get("/api/admin/devices").session(s).param("q","' OR 1=1 --")).andExpect(jsonPath("$.items",hasSize(0)));
        mvc.perform(get("/api/admin/devices").session(s).param("minPrice","200").param("maxPrice","100")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/devices").session(s).param("q",prefix).param("page","1")).andExpect(status().isOk());
    }
    @Test void mixedBatchFailureRollsBackFilesAndStaleImageWritesAreRejected() throws Exception {
        var s=login("OWNER");long id=create(s,"-MIXED");String initial=version(id);
        long before;try(var paths=Files.exists(IMAGE_DIRECTORY)?Files.list(IMAGE_DIRECTORY):java.util.stream.Stream.<Path>empty()) { before=paths.count(); }
        mvc.perform(multipart("/api/admin/devices/"+id+"/images").file(png("valid.png")).file(new MockMultipartFile("files","broken.png","image/png","bad".getBytes())).session(s).with(csrf()).param("version",initial)).andExpect(status().isBadRequest());
        assertEquals(initial,version(id));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.DeviceImages WHERE DeviceID=?",Integer.class,id));
        try(var paths=Files.list(IMAGE_DIRECTORY)) { assertEquals(before,paths.count()); }
        long image=addImage(s,id,"good.png");
        mvc.perform(delete("/api/admin/devices/"+id+"/images/"+image).session(s).with(csrf()).param("version",initial)).andExpect(status().isConflict());
        mvc.perform(put("/api/admin/devices/"+id+"/images/order").session(s).with(csrf()).contentType("application/json").content("{\"imageIds\":["+image+","+image+"],\"version\":\""+version(id)+"\"}" )).andExpect(status().isBadRequest());
    }
    @Test void decimalSpecificationsAndInactiveProductEditsWork() throws Exception {
        var s=login("OWNER");String body=payload("-DECIMAL","").replace("\"CPU\":\"Intel Core i5\"","\"CPU\":\"Intel Core i5\",\"SCREEN_INCH\":\"14.0\"");
        mvc.perform(post("/api/admin/devices").session(s).with(csrf()).contentType("application/json").content(body)).andExpect(status().isCreated());
        long id=jdbc.queryForObject("SELECT DeviceID FROM dbo.Devices WHERE AssetTag=?",Long.class,prefix+"-DECIMAL");
        jdbc.update("UPDATE dbo.Products SET IsActive=0 WHERE ProductID=?",product);
        mvc.perform(put("/api/admin/devices/"+id).session(s).with(csrf()).contentType("application/json").content(payload("-DECIMAL",version(id)))).andExpect(status().isOk());
        mvc.perform(get("/admin/devices/"+id+"/edit").session(s)).andExpect(status().isOk());
    }
    @Test void legacyUnlinkedDeviceCanBeAssignedOnceAndFaultsMustBeDescribed() throws Exception {
        var s=login("OWNER");
        jdbc.update("INSERT INTO dbo.Devices(AssetTag,DeviceDescription,ConditionGrade) VALUES (?,?,?)",prefix+"-LEGACY","Hồ sơ cũ chưa có mẫu","GOOD");
        long id=jdbc.queryForObject("SELECT DeviceID FROM dbo.Devices WHERE AssetTag=?",Long.class,prefix+"-LEGACY");
        mvc.perform(get("/admin/devices/"+id+"/edit").session(s)).andExpect(status().isOk());
        mvc.perform(put("/api/admin/devices/"+id).session(s).with(csrf()).contentType("application/json").content(payload("-LEGACY",version(id)))).andExpect(status().isOk()).andExpect(jsonPath("$.device.productId").value(product));
        mvc.perform(put("/api/admin/devices/"+id).session(s).with(csrf()).contentType("application/json").content(payload("-LEGACY",version(id)).replace("\"productId\":"+product,"\"productId\":"+bulk))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/devices").session(s).with(csrf()).contentType("application/json").content(payload("-FAULT","").replace("GOOD","FAULTY").replace("Trầy nhẹ",""))).andExpect(status().isBadRequest());
    }
    @Test void noCsrfOrAnonymousCannotWrite() throws Exception {
        mvc.perform(get("/api/admin/devices")).andExpect(status().isUnauthorized());mvc.perform(get("/admin/devices")).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/api/admin/devices").session(login("OWNER")).contentType("application/json").content(payload("-CSRF",""))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/devices/1").session(login("OWNER")).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void failedAuditRollsBackDeviceAndWrittenImage() throws Exception {
        var s=login("OWNER");long id=create(s,"-ROLLBACK");String oldVersion=version(id);
        long filesBefore;try(var paths=Files.exists(IMAGE_DIRECTORY)?Files.list(IMAGE_DIRECTORY):java.util.stream.Stream.<Path>empty()) { filesBefore=paths.count(); }
        jdbc.update("UPDATE dbo.Users SET LoginName=? WHERE LoginName=?",prefix+"RENAMED",prefix+"OWNER");
        assertThrows(Exception.class,()->mvc.perform(multipart("/api/admin/devices/"+id+"/images").file(png("rollback.png")).session(s).with(csrf()).param("version",oldVersion)));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.DeviceImages WHERE DeviceID=?",Integer.class,id));assertEquals(oldVersion,version(id));
        try(var paths=Files.list(IMAGE_DIRECTORY)) { assertEquals(filesBefore,paths.count()); }
        assertThrows(Exception.class,()->mvc.perform(post("/api/admin/devices").session(s).with(csrf()).contentType("application/json").content(payload("-MUSTROLLBACK",""))));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Devices WHERE AssetTag=?",Integer.class,prefix+"-MUSTROLLBACK"));
    }
}
