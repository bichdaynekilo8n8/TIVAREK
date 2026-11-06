package com.tivarek.week10;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
abstract class ProductManagementContract {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    String prefix;
    long category,brand,unit;
    static final String PASSWORD="Week10-Integration-Password";
    static final String HASH=new BCryptPasswordEncoder(4).encode(PASSWORD);
    @BeforeEach void fixture() {
        prefix="W10-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();
        for(String role:new String[]{"OWNER","SALES","INVENTORY","TECHNICIAN","CUSTOMER"}) {
            String login=prefix+role;
            jdbc.update("INSERT INTO dbo.Users(LoginName,PasswordHash,DisplayName) VALUES (?,?,?)",login,HASH,"Tuần 10 Test");
            long id=jdbc.queryForObject("SELECT UserID FROM dbo.Users WHERE LoginName=?",Long.class,login);
            jdbc.update("INSERT INTO dbo.UserRoles(UserID,RoleID) SELECT ?,RoleID FROM dbo.Roles WHERE RoleCode=?",id,role);
        }
        for(CatalogKind k:CatalogKind.values()) jdbc.update("INSERT INTO dbo."+k.table+"("+k.code+",Name) VALUES (?,?)",prefix,k.label);
        category=lookupId(CatalogKind.categories);brand=lookupId(CatalogKind.brands);unit=lookupId(CatalogKind.units);
    }
    long lookupId(CatalogKind k) { return jdbc.queryForObject("SELECT "+k.id+" FROM dbo."+k.table+" WHERE "+k.code+"=?",Long.class,prefix); }
    MockHttpSession login(String role) throws Exception {
        return (MockHttpSession)mvc.perform(formLogin().user(prefix+role).password(PASSWORD))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(role.equals("CUSTOMER")?"/account":"/admin"))
            .andReturn().getRequest().getSession(false);
    }
    String payload(String sku,String name,int revision) {
        return "{\"sku\":\""+sku+"\",\"name\":\""+name+"\",\"categoryId\":"+category+",\"brandId\":"+brand+",\"unitId\":"+unit+",\"stockMode\":\"SERIALIZED\",\"listPrice\":5200000,\"description\":\"Mẫu laptop\",\"revision\":"+revision+"}";
    }
    long create(MockHttpSession s,String suffix) throws Exception {
        String sku=prefix+suffix;
        mvc.perform(post("/api/admin/products").session(s).with(csrf()).contentType("application/json").content(payload(sku,"Dell Latitude 5420",0)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.sku").value(sku)).andExpect(jsonPath("$.stockMode").value("SERIALIZED"));
        return jdbc.queryForObject("SELECT ProductID FROM dbo.Products WHERE SKU=?",Long.class,sku);
    }
    @Test void oneLoginEntryAndAuthenticatedLoginRedirect() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(not(containsString(">Quản trị</a>"))));
        mvc.perform(get("/login").session(login("OWNER"))).andExpect(redirectedUrl("/admin"));
        mvc.perform(get("/login").session(login("CUSTOMER"))).andExpect(redirectedUrl("/account"));
    }
    @Test void createEditDiscontinueReopenAndReloginPersist() throws Exception {
        var s=login("OWNER");long id=create(s,"-LAP");
        mvc.perform(put("/api/admin/products/"+id).session(s).with(csrf()).contentType("application/json").content(payload(prefix+"-LAP","Mẫu đã sửa",0)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));
        mvc.perform(post("/api/admin/products/"+id+"/status").session(s).with(csrf()).contentType("application/json").content("{\"active\":false,\"revision\":1}"))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/public/products")).andExpect(content().string(not(containsString(prefix+"-LAP"))));
        mvc.perform(post("/logout").session(s).with(csrf())).andExpect(status().is3xxRedirection());
        var relog=login("OWNER");
        mvc.perform(get("/api/admin/products/"+id).session(relog)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Mẫu đã sửa")).andExpect(jsonPath("$.active").value(false));
        mvc.perform(post("/api/admin/products/"+id+"/status").session(relog).with(csrf()).contentType("application/json").content("{\"active\":true,\"revision\":2}"))
            .andExpect(status().isNoContent());
        assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.AuditLogs WHERE EntityType='PRODUCT' AND EntityKey=?",Integer.class,Long.toString(id)));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.AuditLogs a JOIN dbo.Users u ON u.UserID=a.ActorID WHERE a.EntityKey=? AND a.Action='PRODUCT_CREATE' AND u.LoginName=?",Integer.class,Long.toString(id),prefix+"OWNER"));
    }
    @Test void duplicateCaseAndWhitespaceSkuRejectedByDatabaseAndApi() throws Exception {
        var s=login("OWNER");create(s,"-DUP");
        mvc.perform(post("/api/admin/products").session(s).with(csrf()).contentType("application/json").content(payload("  "+(prefix+"-DUP").toLowerCase()+"  ","Trùng",0)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(containsString("đã tồn tại")));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO dbo.Products(SKU,Name,CategoryID,UnitID,StockMode,ListPrice) VALUES (?,?,?,?,?,?)","  "+(prefix+"-DUP").toLowerCase()+"  ","Direct duplicate",category,unit,"BULK",0));
    }
    @Test void invalidPriceModeNamesAndReferencesCannotPersist() throws Exception {
        var s=login("OWNER");String json=payload(prefix+"-BAD","Valid",0);
        for(String bad:new String[]{json.replace("5200000","-1"),json.replace("SERIALIZED","OTHER"),json.replace("Valid","   "),json.replace("\"categoryId\":"+category,"\"categoryId\":99999999"),json.replace("5200000","1.234")})
            mvc.perform(post("/api/admin/products").session(s).with(csrf()).contentType("application/json").content(bad)).andExpect(status().isBadRequest());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Products WHERE SKU=?",Integer.class,prefix+"-BAD"));
    }
    @Test void optimisticVersionPreventsOverwriteAndOldStatusSubmission() throws Exception {
        var s=login("OWNER");long id=create(s,"-VERSION");String body=payload(prefix+"-VERSION","New name",0);
        mvc.perform(put("/api/admin/products/"+id).session(s).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(put("/api/admin/products/"+id).session(s).with(csrf()).contentType("application/json").content(body.replace("New name","Stale name"))).andExpect(status().isConflict());
        mvc.perform(post("/api/admin/products/"+id+"/status").session(s).with(csrf()).contentType("application/json").content("{\"active\":false,\"revision\":0}")).andExpect(status().isConflict());
        assertEquals("New name",jdbc.queryForObject("SELECT Name FROM dbo.Products WHERE ProductID=?",String.class,id));
    }
    @ParameterizedTest @CsvSource({"OWNER,200,201,200","INVENTORY,200,201,200","SALES,200,403,403","TECHNICIAN,403,403,403","CUSTOMER,403,403,403"})
    void permissionsCoverApiAndForms(String role,int read,int write,int catalog) throws Exception {
        var s=login(role);
        mvc.perform(get("/api/admin/products").session(s)).andExpect(status().is(read));
        mvc.perform(get("/admin/products").session(s)).andExpect(status().is(read));
        mvc.perform(get("/admin/products/new").session(s)).andExpect(status().is(write==201?200:403));
        mvc.perform(get("/admin/catalog/categories").session(s)).andExpect(status().is(catalog));
        mvc.perform(post("/api/admin/products").session(s).with(csrf()).contentType("application/json").content(payload(prefix+"-"+role,"Role test",0))).andExpect(status().is(write));
        if(write==403) mvc.perform(post("/admin/products").session(s).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void anonymousAndCsrfAreEnforced() throws Exception {
        mvc.perform(get("/api/admin/products")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/products")).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/api/admin/products").session(login("OWNER")).contentType("application/json").content(payload(prefix+"-CSRF","No csrf",0))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/products/1").session(login("OWNER")).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void dictionariesManageCodesStatusAndLinkedProducts() throws Exception {
        var s=login("OWNER");long product=create(s,"-REF");
        for(CatalogKind k:CatalogKind.values()) {
            long id=lookupId(k);String path="/api/admin/catalog/"+k.name();
            mvc.perform(post(path).session(s).with(csrf()).contentType("application/json").content("{\"code\":\" "+prefix.toLowerCase()+" \",\"name\":\"Duplicate\",\"revision\":0}")).andExpect(status().isConflict());
            mvc.perform(put(path+"/"+id).session(s).with(csrf()).contentType("application/json").content("{\"code\":\""+prefix+"\",\"name\":\"Tên mới\",\"revision\":0}")).andExpect(status().isOk());
            mvc.perform(post(path+"/"+id+"/status").session(s).with(csrf()).contentType("application/json").content("{\"active\":false,\"revision\":1}")).andExpect(status().isConflict());
        }
        mvc.perform(post("/api/admin/products/"+product+"/status").session(s).with(csrf()).contentType("application/json").content("{\"active\":false,\"revision\":0}")).andExpect(status().isNoContent());
        mvc.perform(post("/api/admin/catalog/categories/"+category+"/status").session(s).with(csrf()).contentType("application/json").content("{\"active\":false,\"revision\":1}")).andExpect(status().isNoContent());
        mvc.perform(post("/api/admin/products").session(s).with(csrf()).contentType("application/json").content(payload(prefix+"-INACTIVE","Inactive reference",0))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/products/"+product+"/status").session(s).with(csrf()).contentType("application/json").content("{\"active\":true,\"revision\":1}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/catalog/categories/"+category+"/status").session(s).with(csrf()).contentType("application/json").content("{\"active\":true,\"revision\":2}")).andExpect(status().isNoContent());
    }
    @Test void productAndDictionaryFormsRenderAndKeepInvalidInput() throws Exception {
        var s=login("OWNER");long id=create(s,"-FORM");
        mvc.perform(get("/admin/products/new").session(s)).andExpect(status().isOk()).andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/admin/products/"+id+"/edit").session(s)).andExpect(status().isOk()).andExpect(content().string(containsString(prefix+"-FORM")));
        mvc.perform(post("/admin/products").session(s).with(csrf()).param("sku",prefix+"-KEEP").param("name","").param("categoryId",Long.toString(category)).param("unitId",Long.toString(unit)).param("stockMode","BULK").param("listPrice","0").param("revision","0"))
            .andExpect(status().isOk()).andExpect(content().string(containsString(prefix+"-KEEP"))).andExpect(content().string(containsString("Kiểm tra lại")));
        mvc.perform(post("/admin/products").session(s).with(csrf()).param("sku",prefix+"-VALIDFORM").param("name","Chuột USB").param("categoryId",Long.toString(category)).param("unitId",Long.toString(unit)).param("stockMode","BULK").param("listPrice","0").param("revision","0"))
            .andExpect(redirectedUrl("/admin/products"));
        for(CatalogKind k:CatalogKind.values()) {
            mvc.perform(get("/admin/catalog/"+k.name()).session(s).param("editId",Long.toString(lookupId(k)))).andExpect(status().isOk());
            mvc.perform(post("/admin/catalog/"+k.name()).session(s).with(csrf()).param("code",prefix+"-NEW").param("name","Mục mới").param("revision","0")).andExpect(status().is3xxRedirection());
        }
    }
    @Test void searchFiltersPaginationAndLiteralWildcards() throws Exception {
        var s=login("OWNER");
        for(int i=0;i<27;i++) jdbc.update("INSERT INTO dbo.Products(SKU,Name,CategoryID,BrandID,UnitID,StockMode,ListPrice) VALUES (?,?,?,?,?,?,?)",prefix+"-PAGE-"+i,"Mẫu 100%_"+i,category,brand,unit,"BULK",0);
        mvc.perform(get("/api/admin/products").session(s).param("q",prefix+"-PAGE").param("categoryId",Long.toString(category)).param("stockMode","BULK"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items",hasSize(25))).andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/api/admin/products").session(s).param("q",prefix+"-PAGE").param("page","1")).andExpect(jsonPath("$.items",hasSize(2))).andExpect(jsonPath("$.hasNext").value(false));
        mvc.perform(get("/api/admin/products").session(s).param("q","%_").param("categoryId",Long.toString(category))).andExpect(jsonPath("$.items",hasSize(25)));
        mvc.perform(get("/api/admin/products").session(s).param("q","' OR 1=1 --")).andExpect(jsonPath("$.items",hasSize(0)));
        mvc.perform(get("/api/admin/products").session(s).param("page","-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/products").session(s).param("page","1").param("q",prefix)).andExpect(status().isOk());
    }
    @Test void auditFailureRollsBackProductWrite() throws Exception {
        var s=login("OWNER");
        jdbc.update("UPDATE dbo.Users SET LoginName=? WHERE LoginName=?",prefix+"RENAMED",prefix+"OWNER");
        assertThrows(Exception.class,()->mvc.perform(post("/api/admin/products").session(s).with(csrf()).contentType("application/json").content(payload(prefix+"-ROLLBACK","Must rollback",0))));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Products WHERE SKU=?",Integer.class,prefix+"-ROLLBACK"));
    }
}
