package com.tivarek.week11;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.tivarek.week10.CatalogProblem;
@Component
public class DeviceImageStorage {
    public static final long MAX_BYTES=5L*1024*1024;
    private final Path root;
    public DeviceImageStorage(@Value("${tivarek.storage.directory:./var/device-images}") String directory) { root=Path.of(directory).toAbsolutePath().normalize(); }
    public record Prepared(BufferedImage image,String format) {}
    public Prepared prepare(MultipartFile file) {
        if(file==null||file.isEmpty()||file.getSize()>MAX_BYTES) throw CatalogProblem.invalid("Mỗi ảnh phải có dữ liệu và không vượt quá 5 MB.");
        try(var input=file.getInputStream();var stream=ImageIO.createImageInputStream(input)) {
            if(stream==null) throw CatalogProblem.invalid("Không đọc được ảnh.");
            var readers=ImageIO.getImageReaders(stream);
            if(!readers.hasNext()) throw CatalogProblem.invalid("Chỉ chấp nhận nội dung ảnh JPEG hoặc PNG hợp lệ.");
            var reader=readers.next();
            try {
                reader.setInput(stream,true,true);String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!Set.of("jpeg","jpg","png").contains(format)) throw CatalogProblem.invalid("Chỉ nhận JPEG và PNG.");
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1||height<1||width>8000||height>8000||(long)width*height>16000000) throw CatalogProblem.invalid("Ảnh tối đa 16 triệu điểm ảnh và mỗi chiều không quá 8.000 px.");
                BufferedImage source=reader.read(0);if(source==null) throw CatalogProblem.invalid("Ảnh không hợp lệ.");
                boolean png=format.equals("png");
                // Re-encode chỉ dữ liệu pixel, không lưu tên gốc hoặc metadata đi kèm.
                double ratio=Math.min(1.0,1600.0/Math.max(width,height));
                int outputWidth=Math.max(1,(int)Math.round(width*ratio)),outputHeight=Math.max(1,(int)Math.round(height*ratio));
                BufferedImage clean=new BufferedImage(outputWidth,outputHeight,png?BufferedImage.TYPE_INT_ARGB:BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics=clean.createGraphics();try {
                    graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.drawImage(source,0,0,outputWidth,outputHeight,null);
                } finally { graphics.dispose(); }
                return new Prepared(clean,png?"png":"jpg");
            } finally { reader.dispose(); }
        } catch(IOException e) { throw CatalogProblem.invalid("Tệp ảnh hỏng hoặc không đọc được."); }
    }
    public String write(Prepared image) {
        String key=UUID.randomUUID()+"."+image.format();Path path=resolve(key);
        try {
            Files.createDirectories(root);
            try(var out=Files.newOutputStream(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {
                if(!ImageIO.write(image.image(),image.format(),out)) throw new IOException("Image encoder unavailable");
            }
            return key;
        } catch(IOException e) {
            delete(key);throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Không lưu được ảnh. Kiểm tra thư mục ảnh và quyền ghi.");
        }
    }
    public byte[] read(String key) {
        Path path=resolve(key);
        try { if(Files.isSymbolicLink(path)||!Files.isRegularFile(path)) throw CatalogProblem.missing();return Files.readAllBytes(path); }
        catch(IOException e) { throw CatalogProblem.missing(); }
    }
    public void delete(String key) {
        try { Files.deleteIfExists(resolve(key)); }
        catch(IOException|CatalogProblem e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Không dọn được tệp ảnh {}. Cần kiểm tra thư mục ảnh.",key); }
    }
    private Path resolve(String key) {
        if(key==null||!key.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)")) throw CatalogProblem.missing();
        return root.resolve(key).normalize();
    }
}
