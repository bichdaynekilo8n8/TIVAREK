package com.tivarek.week11;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
public final class DeviceViews {
    private DeviceViews() {}
    public record Device(long deviceId,String assetTag,long productId,String productName,String sku,
        String categoryName,String brandName,String manufacturerSerial,String description,String conditionGrade,
        BigDecimal askingPrice,String faultDescription,String version,LocalDateTime createdAt,LocalDateTime updatedAt,
        Long coverImageId,String inspectionResult) {}
    public record Definition(long attributeId,String code,String name,String unitLabel,String valueType) {}
    public record Specification(String code,String name,String unitLabel,String valueType,String value) {}
    public record Photo(long imageId,int sortOrder,String contentUrl) {}
    public record Inspection(long checkId,String checkedBy,LocalDateTime checkedAt,String findings,String result) {}
    public record Option(long id,String name) {}
    public record ProductChoice(long productId,String sku,String name,boolean active) {}
    public record Detail(Device device,List<Specification> specifications,List<Photo> images,
                         List<Inspection> checks,boolean duplicateSerial) {
        public DeviceInput input() {
            Map<String,String> values=new LinkedHashMap<>();specifications.forEach(s->values.put(s.code(),s.value()));
            return new DeviceInput(device.assetTag(),device.productId(),device.manufacturerSerial(),device.description(),
                device.conditionGrade(),device.askingPrice(),device.faultDescription(),device.version(),values);
        }
    }
    public record Page(List<Device> items,int page,boolean hasNext) {}
}
