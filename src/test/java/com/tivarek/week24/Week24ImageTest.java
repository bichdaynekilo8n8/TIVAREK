package com.tivarek.week24;
import com.tivarek.week11.DeviceImageStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
class Week24ImageTest {
    @TempDir Path dir;
    @Test void newUploadsAreBoundedWithoutUpscalingOrLosingTransparency() throws Exception {
        var storage=new DeviceImageStorage(dir.toString());
        for(int width:new int[]{320,2400}) {
            var im=new BufferedImage(width,width/2,BufferedImage.TYPE_INT_ARGB);
            var out=new ByteArrayOutputStream();ImageIO.write(im,"png",out);
            var result=storage.prepare(new MockMultipartFile("file","photo.png","image/png",out.toByteArray()));
            assertEquals(Math.min(width,1600),result.image().getWidth());
            assertEquals(result.image().getWidth()/2,result.image().getHeight());
            assertTrue(result.image().getColorModel().hasAlpha());
            var key=storage.write(result);assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(storage.read(key))));
        }
    }
}
