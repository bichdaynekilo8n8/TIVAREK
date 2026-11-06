package com.tivarek.week8;

import com.tivarek.TivarekApplication;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=TivarekApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties="spring.datasource.url=jdbc:h2:mem:tivarek_http;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class HttpLoginTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Test void realHttpLoginRotatesSessionAndLogoutRevokesAccess() throws Exception {
        String username="http."+java.util.UUID.randomUUID();
        String password="Http-Integration-Password-9";
        String sku="HTTP-"+java.util.UUID.randomUUID().toString().substring(0,8).toUpperCase();
        jdbc.update("INSERT INTO dbo.Users(LoginName,PasswordHash,DisplayName) VALUES (?,?,N'HTTP Test')",username,encoder.encode(password));
        long id=jdbc.queryForObject("SELECT UserID FROM dbo.Users WHERE LoginName=?",Long.class,username);
        jdbc.update("INSERT INTO dbo.UserRoles(UserID,RoleID) SELECT ?,RoleID FROM dbo.Roles WHERE RoleCode='OWNER'",id);
        var cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        try(var client=HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build()) {
            String base="http://127.0.0.1:"+port;
            var login=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,login.statusCode());
            var matcher=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(login.body());
            assertTrue(matcher.find());
            String token=matcher.group(1);
            String oldSession=cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
            String form="username="+URLEncoder.encode(username,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(password,StandardCharsets.UTF_8)+"&_csrf="+URLEncoder.encode(token,StandardCharsets.UTF_8);
            var signed=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(302,signed.statusCode());
            String newSession=cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
            assertNotEquals(oldSession,newSession);
            var admin=client.send(HttpRequest.newBuilder(URI.create(base+"/admin")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,admin.statusCode());
            assertTrue(admin.body().contains("Giá nhập nội bộ"));
            var csrf=client.send(HttpRequest.newBuilder(URI.create(base+"/api/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString());
            var tokenJson=Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(csrf.body());
            assertTrue(tokenJson.find());
            assertEquals("/admin",URI.create(signed.headers().firstValue("Location").orElseThrow()).getPath());
            String json="{\"sku\":\""+sku+"\",\"name\":\"HTTP persisted product\",\"categoryId\":1,\"unitId\":1,\"stockMode\":\"SERIALIZED\",\"listPrice\":100,\"revision\":0}";
            var created=client.send(HttpRequest.newBuilder(URI.create(base+"/api/admin/products")).header("X-CSRF-TOKEN",tokenJson.group(1)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(201,created.statusCode(),created.body());
            long productId=jdbc.queryForObject("SELECT ProductID FROM dbo.Products WHERE SKU=?",Long.class,sku);
            for(String path:new String[]{"/admin/products","/admin/products/new","/admin/products/"+productId+"/edit","/admin/catalog/brands"}) {
                var page=client.send(HttpRequest.newBuilder(URI.create(base+path)).GET().build(),HttpResponse.BodyHandlers.ofString());
                assertEquals(200,page.statusCode(),path);
            }
            var logout=client.send(HttpRequest.newBuilder(URI.create(base+"/logout")).header("X-CSRF-TOKEN",tokenJson.group(1)).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(302,logout.statusCode());
            var denied=client.send(HttpRequest.newBuilder(URI.create(base+"/api/me")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(401,denied.statusCode());
            var nextLogin=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).GET().build(),HttpResponse.BodyHandlers.ofString());
            var nextToken=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(nextLogin.body());
            assertTrue(nextToken.find());
            String again="username="+URLEncoder.encode(username,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(password,StandardCharsets.UTF_8)+"&_csrf="+URLEncoder.encode(nextToken.group(1),StandardCharsets.UTF_8);
            var loggedAgain=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(again)).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(302,loggedAgain.statusCode());
            var persisted=client.send(HttpRequest.newBuilder(URI.create(base+"/api/admin/products/"+productId)).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,persisted.statusCode());
            assertTrue(persisted.body().contains(sku));
        } finally {
            jdbc.update("DELETE FROM dbo.AuditLogs WHERE ActorID=?",id);
            jdbc.update("DELETE FROM dbo.Products WHERE SKU=?",sku);
            jdbc.update("DELETE FROM dbo.UserRoles WHERE UserID=?",id);
            jdbc.update("DELETE FROM dbo.Users WHERE UserID=?",id);
        }
    }
}
