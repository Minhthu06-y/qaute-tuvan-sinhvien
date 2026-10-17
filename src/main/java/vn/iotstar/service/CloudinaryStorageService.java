package vn.iotstar.service;

import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryStorageService {
    @Value("${cloudinary.url:}") private String cloudinaryUrl;
    public boolean isConfigured() { return cloudinaryUrl != null && !cloudinaryUrl.isBlank(); }
    public String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        if (!isConfigured()) throw new IllegalStateException("Chưa cấu hình CLOUDINARY_URL. Câu hỏi vẫn được lưu, tệp đính kèm chưa được tải lên.");
        Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto", "folder", "qaute/questions"));
        return (String) result.get("secure_url");
    }
}
