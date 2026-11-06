package com.tivarek.week11;
import java.util.*;
import com.tivarek.week8.AuditService;
import com.tivarek.week10.CatalogProblem;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import static com.tivarek.week11.DeviceViews.*;
@Service
public class DeviceImageService {
    private final DeviceRepository repo;private final DeviceImageStorage storage;private final AuditService audit;private final DeviceService devices;
    public DeviceImageService(DeviceRepository repo,DeviceImageStorage storage,AuditService audit,DeviceService devices) { this.repo=repo;this.storage=storage;this.audit=audit;this.devices=devices; }
    @PreAuthorize("hasAuthority('DEVICE_IMAGE_WRITE')") @Transactional
    public Detail upload(long id,List<MultipartFile> files,String version) {
        repo.lock(id);var existing=repo.images(id);
        if(files==null||files.isEmpty()||files.size()+existing.size()>10) throw CatalogProblem.invalid("Mỗi máy tối đa 10 ảnh. Chọn ít nhất một ảnh.");
        // Mỗi lần chỉ giải mã một ảnh; rollback dọn toàn bộ lô khi bất kỳ tệp nào lỗi.
        if(repo.touch(id,DeviceService.version(version))!=1) throw DeviceService.stale();
        List<String> written=new ArrayList<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) written.forEach(storage::delete); }
        });
        for(var file:files) { var picture=storage.prepare(file);String key=storage.write(picture);written.add(key);repo.addImage(id,key); }
        audit.writeCurrent("DEVICE_IMAGE_ADD","DEVICE",Long.toString(id),"Thêm "+written.size()+" ảnh thật");return devices.get(id);
    }
    @PreAuthorize("hasAuthority('DEVICE_READ')") @Transactional(readOnly=true)
    public ImageContent content(long id,long imageId) { repo.get(id);String key=repo.imageKey(id,imageId);return new ImageContent(storage.read(key),key.endsWith(".png")?"image/png":"image/jpeg"); }
    @PreAuthorize("hasAuthority('DEVICE_IMAGE_WRITE')") @Transactional
    public Detail remove(long id,long imageId,String version) {
        repo.lock(id);String key=repo.imageKey(id,imageId);
        if(repo.touch(id,DeviceService.version(version))!=1) throw DeviceService.stale();
        repo.removeImage(id,imageId);
        audit.writeCurrent("DEVICE_IMAGE_REMOVE","DEVICE",Long.toString(id),"Gỡ ảnh #"+imageId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { storage.delete(key); }
        });return devices.get(id);
    }
    @PreAuthorize("hasAuthority('DEVICE_IMAGE_WRITE')") @Transactional
    public Detail reorder(long id,List<Long> order,String version) {
        repo.lock(id);var current=repo.images(id).stream().map(Photo::imageId).toList();
        if(order==null||order.size()!=current.size()||!new HashSet<>(order).equals(new HashSet<>(current))) throw CatalogProblem.invalid("Thứ tự ảnh phải chứa đúng toàn bộ ảnh của máy, không trùng và không chứa ảnh của máy khác.");
        if(repo.touch(id,DeviceService.version(version))!=1) throw DeviceService.stale();
        repo.reorder(id,order);audit.writeCurrent("DEVICE_IMAGE_REORDER","DEVICE",Long.toString(id),"Đổi thứ tự ảnh; ảnh đầu làm đại diện");return devices.get(id);
    }
    @PreAuthorize("hasAuthority('DEVICE_IMAGE_WRITE')") @Transactional
    public Detail move(long id,long imageId,String action,String version) {
        repo.lock(id);var ids=new ArrayList<>(repo.images(id).stream().map(Photo::imageId).toList());int index=ids.indexOf(imageId);
        if(index<0) throw CatalogProblem.missing();
        switch(action) {
            case "cover" -> { ids.remove(index);ids.addFirst(imageId); }
            case "up" -> { if(index>0) Collections.swap(ids,index,index-1); }
            case "down" -> { if(index+1<ids.size()) Collections.swap(ids,index,index+1); }
            default -> throw CatalogProblem.invalid("Thao tác ảnh không hợp lệ.");
        }
        return reorder(id,ids,version);
    }
    public record ImageContent(byte[] bytes,String contentType) {}
}
