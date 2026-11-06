package com.tivarek.week13;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/media/public/devices")
public class PublicDeviceMediaController {
    private final StorefrontService storefront;
    public PublicDeviceMediaController(StorefrontService storefront) {this.storefront=storefront;}

    @GetMapping("/{deviceId}/images/{imageId}")
    public ResponseEntity<byte[]> image(@PathVariable long deviceId,@PathVariable long imageId) {
        var content=storefront.publicDeviceImage(deviceId,imageId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(content.contentType()))
            .cacheControl(CacheControl.noCache()).header("X-Content-Type-Options","nosniff")
            .body(content.bytes());
    }
}
