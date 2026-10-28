package vn.iotstar.service;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryStorageService {
    private static final Logger log = LoggerFactory.getLogger(CloudinaryStorageService.class);

    @Value("${cloudinary.url:}")
    private String cloudinaryUrl;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    private volatile Cloudinary client;

    public boolean isConfigured() {
        return hasText(cloudinaryUrl) || (hasText(cloudName) && hasText(apiKey) && hasText(apiSecret));
    }

    public String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        Cloudinary cloudinary = getClient();
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("resource_type", "auto", "folder", "qaute/questions"));
            Object secureUrl = result.get("secure_url");
            if (secureUrl instanceof String url && hasText(url)) return url;
            throw new IllegalStateException("Cloudinary không trả về đường dẫn tệp đã lưu.");
        } catch (RuntimeException ex) {
            log.error("Cloudinary upload failed for question attachment", ex);
            throw new IllegalStateException("Không thể lưu tệp lên Cloudinary. Kiểm tra cấu hình và thử lại.", ex);
        }
    }

    private Cloudinary getClient() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Chưa cấu hình Cloudinary. Đặt CLOUDINARY_URL hoặc CLOUDINARY_CLOUD_NAME, "
                            + "CLOUDINARY_API_KEY và CLOUDINARY_API_SECRET rồi khởi động lại ứng dụng.");
        }
        Cloudinary current = client;
        if (current != null) return current;
        synchronized (this) {
            if (client == null) {
                try {
                    client = hasText(cloudinaryUrl)
                            ? new Cloudinary(cloudinaryUrl)
                            : new Cloudinary(ObjectUtils.asMap(
                                    "cloud_name", cloudName.trim(),
                                    "api_key", apiKey.trim(),
                                    "api_secret", apiSecret.trim(),
                                    "secure", true));
                } catch (RuntimeException ex) {
                    throw new IllegalStateException("Cấu hình Cloudinary không hợp lệ.", ex);
                }
            }
            return client;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
