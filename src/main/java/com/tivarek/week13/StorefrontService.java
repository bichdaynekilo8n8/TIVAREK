package com.tivarek.week13;

import com.tivarek.week10.CatalogProblem;
import com.tivarek.week11.DeviceImageStorage;
import com.tivarek.week8.AuditService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorefrontService {
    private final StorefrontRepository repository;
    private final DeviceImageStorage imageStorage;
    private final AuditService audit;

    public StorefrontService(StorefrontRepository repository,DeviceImageStorage imageStorage,AuditService audit) {
        this.repository=repository;this.imageStorage=imageStorage;this.audit=audit;
    }

    public record Page(List<StorefrontProductView> items,long total,int currentPage,int totalPages) {
        public boolean hasPrevious() {return currentPage>0;}
        public boolean hasNext() {return currentPage+1<totalPages;}
    }

    @Transactional(readOnly=true)
    public Page search(StorefrontSearch filter) {
        var data=repository.search(filter);
        int pages=(int)((data.total()+StorefrontSearch.PAGE_SIZE-1)/StorefrontSearch.PAGE_SIZE);
        return new Page(data.items(),data.total(),filter.page(),pages);
    }
    public record BulkDetail(StorefrontProductView item,List<StorefrontDeviceView.Spec> specs,
                             List<StorefrontDeviceView.Photo> photos) { }
    @Transactional(readOnly=true)
    public BulkDetail bulkDetail(long id) {
        // One publication check; both following reads are inside this read-only service call.
        var item=repository.detail("BULK",id);
        return new BulkDetail(item,repository.productSpecifications(id),repository.productImages(id));
    }
    @Transactional(readOnly=true)
    public StorefrontProductView product(long id) { return repository.detail("BULK",id); }
    @Transactional(readOnly=true)
    public StorefrontDeviceView device(long id) {
        var item=repository.detail("DEVICE",id);
        return new StorefrontDeviceView(item,repository.description(id),repository.inspection(id),
            repository.deviceSpecifications(id),repository.deviceImages(id));
    }
    @Transactional(readOnly=true)
    public List<StorefrontDeviceView.Spec> productSpecifications(long id) {
        repository.detail("BULK",id);
        return repository.productSpecifications(id);
    }
    @Transactional(readOnly=true)
    public List<StorefrontDeviceView.Photo> productImages(long id) {
        repository.detail("BULK",id);
        return repository.productImages(id);
    }
    @Transactional(readOnly=true)
    public List<StorefrontRepository.Option> categories() {return repository.categories();}
    @Transactional(readOnly=true)
    public List<StorefrontRepository.Option> brands() {return repository.brands();}

    @Transactional(readOnly=true)
    public DeviceImageContent publicDeviceImage(long deviceId,long imageId) {
        if(deviceId<=0||imageId<=0) throw CatalogProblem.missing();
        String key=repository.publicImageKey(deviceId,imageId);
        return new DeviceImageContent(imageStorage.read(key),key.endsWith(".png")?"image/png":"image/jpeg");
    }
    public record DeviceImageContent(byte[] bytes,String contentType) { }

    @PreAuthorize("hasAnyAuthority('PRODUCT_WRITE','DEVICE_WRITE')")
    @Transactional(readOnly=true)
    public Management management() {return new Management(repository.manageProducts(),repository.manageDevices());}
    public record Management(List<StorefrontRepository.Publication> products,List<StorefrontRepository.Publication> devices) { }

    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    @Transactional(readOnly=true)
    public List<StorefrontRepository.ManagedImage> manageProductImages(long id) {
        if(!repository.bulkProductExists(id)) throw CatalogProblem.missing();
        return repository.managedImages(id);
    }
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    @Transactional
    public void addProductImage(long id,String url) {
        if(id<=0||!repository.bulkProductExists(id)) throw CatalogProblem.missing();
        String cleaned=url==null?"":url.strip();
        var preview=new StorefrontProductView("BULK",id,"","","","",null,
            java.math.BigDecimal.ZERO,cleaned,null,0,0,null);
        if(preview.imageUrl()==null || !preview.imageUrl().equals(cleaned))
            throw CatalogProblem.invalid("Địa chỉ ảnh phải là HTTPS hợp lệ hoặc bắt đầu bằng /images/.");
        if(repository.managedImages(id).size()>=10)
            throw CatalogProblem.invalid("Mỗi sản phẩm hỗ trợ tối đa 10 ảnh.");
        repository.addProductImage(id,cleaned);
        audit.writeCurrent("PRODUCT_IMAGE_ADD","PRODUCT",Long.toString(id),"Thêm ảnh hiển thị sản phẩm");
    }
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    @Transactional
    public void removeProductImage(long id,long imageId) {
        if(id<=0||imageId<=0||!repository.bulkProductExists(id)||repository.removeProductImage(id,imageId)!=1)
            throw CatalogProblem.missing();
        audit.writeCurrent("PRODUCT_IMAGE_REMOVE","PRODUCT",Long.toString(id),"Gỡ ảnh sản phẩm #"+imageId);
    }

    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    @Transactional
    public void publishProduct(long id,boolean published) {
        if(id<=0||published&&!repository.productReady(id))
            throw CatalogProblem.invalid("Chỉ có thể đăng sản phẩm BULK đang kinh doanh.");
        if(repository.publish("BULK",id,published)!=1) throw CatalogProblem.missing();
        audit.writeCurrent(published?"PRODUCT_PUBLISH":"PRODUCT_UNPUBLISH","PRODUCT",Long.toString(id),
            published?"Đăng sản phẩm lên website":"Ẩn sản phẩm khỏi website");
    }
    @PreAuthorize("hasAuthority('DEVICE_WRITE')")
    @Transactional
    public void publishDevice(long id,boolean published) {
        if(id<=0||published&&!repository.deviceReady(id))
            throw CatalogProblem.invalid("Thiết bị phải thuộc cửa hàng, có mẫu SERIALIZED đang bán và có giá riêng.");
        if(repository.publish("DEVICE",id,published)!=1) throw CatalogProblem.missing();
        audit.writeCurrent(published?"DEVICE_PUBLISH":"DEVICE_UNPUBLISH","DEVICE",Long.toString(id),
            published?"Đăng thiết bị lên website":"Ẩn thiết bị khỏi website");
    }
}
