package com.tivarek.week11;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties={"spring.datasource.url=jdbc:h2:mem:tivarek_device_http;MODE=MSSQLServer;DB_CLOSE_DELAY=-1","tivarek.storage.directory=${java.io.tmpdir}/tivarek-week11-http-images"})
@ActiveProfiles("test")
class HttpDeviceWorkflowTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired DeviceImageStorage storage;
    static String token(String html) {
        var match=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);assertTrue(match.find());return match.group(1);
    }
    @Test void nativeHttpMultipartFormPersistsImagesAcrossLoginSessions() throws Exception {
        String code=("HTTP11-"+UUID.randomUUID().toString().substring(0,8)).toUpperCase(Locale.ROOT),username=code+"owner",password="Http-Week11-Password";
        jdbc.update("INSERT INTO dbo.Users(LoginName,PasswordHash,DisplayName) VALUES (?,?,?)",username,encoder.encode(password),"HTTP11 Test");
        long actor=jdbc.queryForObject("SELECT UserID FROM dbo.Users WHERE LoginName=?",Long.class,username);
        jdbc.update("INSERT INTO dbo.UserRoles(UserID,RoleID) SELECT ?,RoleID FROM dbo.Roles WHERE RoleCode='OWNER'",actor);
        jdbc.update("INSERT INTO dbo.Products(SKU,Name,CategoryID,UnitID,StockMode,ListPrice) VALUES (?,?,1,1,'SERIALIZED',0)",code,"HTTP Device Model");
        long product=jdbc.queryForObject("SELECT ProductID FROM dbo.Products WHERE SKU=?",Long.class,code);
        String base="http://127.0.0.1:"+port;
        var cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        try(var client=HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build()) {
            var page=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).GET().build(),HttpResponse.BodyHandlers.ofString());
            String form="username="+username+"&password="+password+"&_csrf="+URLEncoder.encode(token(page.body()),StandardCharsets.UTF_8);
            var login=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build(),HttpResponse.BodyHandlers.ofString());assertEquals(302,login.statusCode());
            var createPage=client.send(HttpRequest.newBuilder(URI.create(base+"/admin/devices/new")).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,createPage.statusCode());
            String createForm="assetTag="+code+"&productId="+product+"&description=Native+HTTP+device&conditionGrade=GOOD&version=&specifications%5BRAM_GB%5D=16&_csrf="+URLEncoder.encode(token(createPage.body()),StandardCharsets.UTF_8);
            var created=client.send(HttpRequest.newBuilder(URI.create(base+"/admin/devices")).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(createForm)).build(),HttpResponse.BodyHandlers.ofString());assertEquals(302,created.statusCode(),created.body());
            long id=jdbc.queryForObject("SELECT DeviceID FROM dbo.Devices WHERE AssetTag=?",Long.class,code);
            var detail=client.send(HttpRequest.newBuilder(URI.create(base+"/admin/devices/"+id)).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,detail.statusCode());
            String version=HexFormat.of().formatHex(jdbc.queryForObject("SELECT Version FROM dbo.Devices WHERE DeviceID=?",byte[].class,id));
            var image=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(16,12,BufferedImage.TYPE_INT_RGB),"png",image);
            String boundary="TivarekBoundary"+UUID.randomUUID();var body=new ByteArrayOutputStream();
            for(var field:Map.of("_csrf",token(detail.body()),"version",version).entrySet()) body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+field.getKey()+"\"\r\n\r\n"+field.getValue()+"\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"files\"; filename=\"device.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));body.write(image.toByteArray());body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
            var uploaded=client.send(HttpRequest.newBuilder(URI.create(base+"/admin/devices/"+id+"/images")).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());assertEquals(302,uploaded.statusCode(),uploaded.body());
            long imageId=jdbc.queryForObject("SELECT ImageID FROM dbo.DeviceImages WHERE DeviceID=?",Long.class,id);
            var served=client.send(HttpRequest.newBuilder(URI.create(base+"/api/admin/devices/"+id+"/images/"+imageId+"/content")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(200,served.statusCode());assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(served.body())));
            var fresh=client.send(HttpRequest.newBuilder(URI.create(base+"/admin/devices/"+id)).GET().build(),HttpResponse.BodyHandlers.ofString());
            var logout=client.send(HttpRequest.newBuilder(URI.create(base+"/logout")).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("_csrf="+URLEncoder.encode(token(fresh.body()),StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());assertEquals(302,logout.statusCode());
            var denied=client.send(HttpRequest.newBuilder(URI.create(base+"/api/admin/devices/"+id+"/images/"+imageId+"/content")).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(401,denied.statusCode());
            var next=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).GET().build(),HttpResponse.BodyHandlers.ofString());
            var again=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("username="+username+"&password="+password+"&_csrf="+URLEncoder.encode(token(next.body()),StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());assertEquals(302,again.statusCode());
            var persisted=client.send(HttpRequest.newBuilder(URI.create(base+"/admin/devices/"+id)).GET().build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,persisted.statusCode());assertTrue(persisted.body().contains(code));assertTrue(persisted.body().contains("Ảnh đại diện"));
        } finally {
            jdbc.queryForList("SELECT ImageURL FROM dbo.DeviceImages WHERE DeviceID IN (SELECT DeviceID FROM dbo.Devices WHERE AssetTag=?)",String.class,code).forEach(storage::delete);
            jdbc.update("DELETE FROM dbo.DeviceImages WHERE DeviceID IN (SELECT DeviceID FROM dbo.Devices WHERE AssetTag=?)",code);
            jdbc.update("DELETE FROM dbo.DeviceAttributeValues WHERE DeviceID IN (SELECT DeviceID FROM dbo.Devices WHERE AssetTag=?)",code);
            jdbc.update("DELETE FROM dbo.Devices WHERE AssetTag=?",code);jdbc.update("DELETE FROM dbo.Products WHERE ProductID=?",product);
            jdbc.update("DELETE FROM dbo.AuditLogs WHERE ActorID=?",actor);jdbc.update("DELETE FROM dbo.UserRoles WHERE UserID=?",actor);jdbc.update("DELETE FROM dbo.Users WHERE UserID=?",actor);
        }
    }
}
