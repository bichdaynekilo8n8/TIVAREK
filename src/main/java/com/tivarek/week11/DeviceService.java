package com.tivarek.week11;
import java.math.*;
import java.util.*;
import com.tivarek.week8.AuditService;
import com.tivarek.week10.CatalogProblem;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import static com.tivarek.week11.DeviceViews.*;
@Service
public class DeviceService {
    private final DeviceRepository repo;private final AuditService audit;
    public DeviceService(DeviceRepository repo,AuditService audit) { this.repo=repo;this.audit=audit; }
    public static final Map<String,String> GRADES=Collections.unmodifiableMap(new LinkedHashMap<>(Map.of(
        "NEW","Mới","LIKE_NEW","Như mới","GOOD","Tốt","FAIR","Đã qua sử dụng nhiều","FAULTY","Có lỗi")));
    @PreAuthorize("hasAuthority('DEVICE_READ')") @Transactional(readOnly=true)
    public Page search(String q,Long productId,Long categoryId,Long brandId,String grade,BigDecimal min,BigDecimal max,int page) {
        if(q.length()>100||page<0||page>100000) throw CatalogProblem.invalid("Từ khóa tối đa 100 ký tự; trang không hợp lệ.");
        if(grade!=null&&!grade.isBlank()&&!GRADES.containsKey(grade)) throw CatalogProblem.invalid("Tình trạng không hợp lệ.");
        if((min!=null&&min.signum()<0)||(max!=null&&max.signum()<0)||(min!=null&&max!=null&&min.compareTo(max)>0)) throw CatalogProblem.invalid("Khoảng giá không hợp lệ.");
        var rows=repo.search(q.strip(),productId,categoryId,brandId,grade,min,max,page);
        return new Page(rows.stream().limit(25).toList(),page,rows.size()>25);
    }
    @PreAuthorize("hasAuthority('DEVICE_READ')") @Transactional(readOnly=true)
    public Detail get(long id) { return detail(id); }
    @PreAuthorize("hasAuthority('DEVICE_READ')") @Transactional(readOnly=true)
    public List<ProductChoice> choices(Long includeId) { return repo.choices(includeId); }
    @PreAuthorize("hasAuthority('DEVICE_READ')") @Transactional(readOnly=true)
    public List<Definition> definitions() { return repo.definitions(); }
    @PreAuthorize("hasAuthority('DEVICE_WRITE')") @Transactional
    public Detail create(DeviceInput d) {
        condition(d);repo.requireProduct(d.productId());var values=validateSpecifications(d.specifications());
        long id=repo.insert(d);repo.specifications(id,values);
        audit.writeCurrent("DEVICE_CREATE","DEVICE",Long.toString(id),"Thêm máy "+d.assetTag()+"; giá dự kiến: "+d.askingPrice());return detail(id);
    }
    @PreAuthorize("hasAuthority('DEVICE_WRITE')") @Transactional
    public Detail update(long id,DeviceInput d) {
        // ProductID cố định sau khi tạo; không đổi mẫu khi máy có lịch sử kho/bán/sửa.
        condition(d);repo.lock(id);var old=repo.get(id);
        if(old.productId()!=0 && old.productId()!=d.productId()) throw CatalogProblem.invalid("Mẫu sản phẩm được cố định sau khi tạo. Không chuyển máy sang mẫu khác.");
        if(old.productId()==0) repo.requireProduct(d.productId());
        var values=validateSpecifications(d.specifications());
        if(repo.update(id,d,version(d.version()))!=1) throw stale();
        repo.specifications(id,values);
        audit.writeCurrent("DEVICE_UPDATE","DEVICE",Long.toString(id),"Sửa "+old.assetTag()+" → "+d.assetTag()+"; tình trạng: "+old.conditionGrade()+" → "+d.conditionGrade());
        if(!Objects.equals(old.askingPrice(),d.askingPrice())&&(old.askingPrice()==null||d.askingPrice()==null||old.askingPrice().compareTo(d.askingPrice())!=0))
            audit.writeCurrent("DEVICE_PRICE_UPDATE","DEVICE",Long.toString(id),"Giá "+old.askingPrice()+" → "+d.askingPrice());
        return detail(id);
    }
    @PreAuthorize("hasAuthority('DEVICE_CHECK_WRITE')") @Transactional
    public Detail addCheck(long id,CheckInput c) {
        repo.lock(id);var old=repo.get(id);
        if(old.conditionGrade().equals("FAULTY")&&c.result().equals("PASS")) throw CatalogProblem.invalid("Máy đang đánh dấu có lỗi. Người quản lý cần cập nhật tình trạng trước khi ghi đạt kiểm tra.");
        if(repo.touch(id,version(c.version()))!=1) throw stale();
        repo.check(id,SecurityContextHolder.getContext().getAuthentication().getName(),c);
        audit.writeCurrent("DEVICE_CHECK_CREATE","DEVICE",Long.toString(id),"Ghi kiểm tra: "+c.result());return detail(id);
    }
    @PreAuthorize("hasAuthority('DEVICE_READ')") @Transactional(readOnly=true)
    public List<Option> categories() { return repo.categories(); }
    @PreAuthorize("hasAuthority('DEVICE_READ')") @Transactional(readOnly=true)
    public List<Option> brands() { return repo.brands(); }
    private Detail detail(long id) { var d=repo.get(id);return new Detail(d,repo.specifications(id),repo.images(id),repo.checks(id),repo.duplicateSerial(id,d.manufacturerSerial())); }
    private static void condition(DeviceInput input) {
        if("FAULTY".equals(input.conditionGrade())&&(input.faultDescription()==null||input.faultDescription().isBlank())) throw CatalogProblem.invalid("Máy có lỗi cần ghi rõ lỗi và điểm cần lưu ý.");
    }
    private Map<Long,String> validateSpecifications(Map<String,String> input) {
        var definitions=new HashMap<String,Definition>();repo.definitions().forEach(d->definitions.put(d.code(),d));
        Map<Long,String> result=new LinkedHashMap<>();
        for(var entry:input.entrySet()) {
            if(entry.getKey()==null||!definitions.containsKey(entry.getKey())) throw CatalogProblem.invalid("Thông số không tồn tại: "+entry.getKey());
            String value=entry.getValue()==null?"":entry.getValue().strip();if(value.isEmpty()) continue;
            var d=definitions.get(entry.getKey());
            if(value.length()>250) throw CatalogProblem.invalid(d.name()+": tối đa 250 ký tự.");
            try {
                switch(d.valueType()) {
                    case "INTEGER" -> { if(!value.matches("[0-9]{1,18}")) throw new IllegalArgumentException();value=new BigInteger(value).toString(); }
                    case "DECIMAL" -> { if(!value.matches("[0-9]{1,12}(\\.[0-9]{1,4})?")) throw new IllegalArgumentException();value=new BigDecimal(value).stripTrailingZeros().toPlainString(); }
                    case "BOOLEAN" -> { if(!Set.of("true","false").contains(value.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException();value=value.toLowerCase(Locale.ROOT); }
                    case "TEXT" -> { }
                    default -> throw new IllegalArgumentException();
                }
            } catch(IllegalArgumentException e) { throw CatalogProblem.invalid(d.name()+": giá trị không đúng kiểu "+d.valueType()+" (số phải không âm)."); }
            result.put(d.attributeId(),value);
        }
        return result;
    }
    static byte[] version(String value) {
        if(value==null||!value.matches("[0-9a-fA-F]{16}")) throw CatalogProblem.invalid("Phiên bản thiết bị không hợp lệ. Tải lại trang.");
        return HexFormat.of().parseHex(value);
    }
    static CatalogProblem stale() { return CatalogProblem.conflict("Thiết bị đã được người khác thay đổi. Tải lại dữ liệu mới nhất trước khi lưu."); }
}
