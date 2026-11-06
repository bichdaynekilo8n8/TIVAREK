package com.tivarek.week11;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import static com.tivarek.week11.DeviceViews.*;
@RestController
@RequestMapping("/api/admin/devices")
public class DeviceApiController {
    private final DeviceService devices;private final DeviceImageService images;
    public DeviceApiController(DeviceService devices,DeviceImageService images) { this.devices=devices;this.images=images; }
    @GetMapping public Page list(@RequestParam(defaultValue="") String q,@RequestParam(required=false) Long productId,
        @RequestParam(required=false) Long categoryId,@RequestParam(required=false) Long brandId,@RequestParam(required=false) String grade,
        @RequestParam(required=false) BigDecimal minPrice,@RequestParam(required=false) BigDecimal maxPrice,@RequestParam(defaultValue="0") int page) {
        return devices.search(q,productId,categoryId,brandId,grade,minPrice,maxPrice,page);
    }
    @GetMapping("/metadata") public Map<String,Object> metadata() { return Map.of("products",devices.choices(null),"definitions",devices.definitions(),"grades",DeviceService.GRADES,"categories",devices.categories(),"brands",devices.brands()); }
    @GetMapping("/{id}") public Detail detail(@PathVariable long id) { return devices.get(id); }
    @PostMapping public ResponseEntity<Detail> create(@Valid @RequestBody DeviceInput input) {
        var saved=devices.create(input);return ResponseEntity.created(URI.create("/api/admin/devices/"+saved.device().deviceId())).body(saved);
    }
    @PutMapping("/{id}") public Detail update(@PathVariable long id,@Valid @RequestBody DeviceInput input) { return devices.update(id,input); }
    @PostMapping("/{id}/checks") public ResponseEntity<Detail> check(@PathVariable long id,@Valid @RequestBody CheckInput input) { return ResponseEntity.status(201).body(devices.addCheck(id,input)); }
    @PostMapping(value="/{id}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Detail> upload(@PathVariable long id,@RequestParam List<MultipartFile> files,@RequestParam String version) { return ResponseEntity.status(201).body(images.upload(id,files,version)); }
    @GetMapping("/{id}/images/{imageId}/content") public ResponseEntity<byte[]> content(@PathVariable long id,@PathVariable long imageId) {
        var content=images.content(id,imageId);return ResponseEntity.ok().contentType(MediaType.parseMediaType(content.contentType())).cacheControl(CacheControl.noStore())
            .header("Content-Disposition","inline").body(content.bytes());
    }
    @DeleteMapping("/{id}/images/{imageId}") public Detail remove(@PathVariable long id,@PathVariable long imageId,@RequestParam String version) { return images.remove(id,imageId,version); }
    @PutMapping("/{id}/images/order") public Detail reorder(@PathVariable long id,@Valid @RequestBody ImageOrderInput input) { return images.reorder(id,input.imageIds(),input.version()); }
}
