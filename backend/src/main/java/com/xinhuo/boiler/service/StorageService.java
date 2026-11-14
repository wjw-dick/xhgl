package com.xinhuo.boiler.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.region.Region;
import com.xinhuo.boiler.common.AuthContext;
import com.xinhuo.boiler.common.BizException;
import com.xinhuo.boiler.entity.FileRecord;
import com.xinhuo.boiler.entity.StorageConfig;
import com.xinhuo.boiler.mapper.FileRecordMapper;
import com.xinhuo.boiler.mapper.StorageConfigMapper;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StorageService {
    private static final Set<String> SECRET_KEYS = Set.of("secretKey", "accessKeySecret", "accessKeyId", "secretId", "sk", "ak");
    private final StorageConfigMapper storageConfigMapper;
    private final FileRecordMapper fileRecordMapper;
    private final CryptoService cryptoService;
    private final ObjectMapper objectMapper;

    @Value("${app.upload-dir:./uploads}")
    private String defaultUploadDir;

    public StorageConfig current(Long tenantId) {
        StorageConfig config = storageConfigMapper.selectOne(new LambdaQueryWrapper<StorageConfig>()
                .eq(StorageConfig::getTenantId, tenantId).last("limit 1"));
        if (config == null) {
            config = new StorageConfig();
            config.setTenantId(tenantId);
            config.setDriver("LOCAL");
            config.setActive(1);
            config.setStatus("NORMAL");
            config.setConfigJson(cryptoService.encrypt("{\"basePath\":\"./uploads\"}"));
            storageConfigMapper.insert(config);
        }
        return config;
    }

    public Map<String, Object> maskedConfig(Long tenantId) {
        StorageConfig config = current(tenantId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("driver", config.getDriver());
        result.put("active", config.getActive());
        result.put("status", config.getStatus());
        result.put("updatedAt", config.getUpdatedAt());
        result.put("config", mask(parse(config.getConfigJson())));
        return result;
    }

    public void test(String driver, Map<String, String> config) {
        try {
            driver(driver).test(config);
        } catch (Exception e) {
            throw new BizException("连接测试失败：" + e.getMessage());
        }
    }

    public void save(Long tenantId, String driver, Map<String, String> config) {
        test(driver, config);
        StorageConfig existing = current(tenantId);
        String encryptedOld = existing.getConfigJson();
        Map<String, String> merged = mergeSecrets(driver, config, parse(encryptedOld));
        try {
            existing.setDriver(driver.toUpperCase());
            existing.setConfigJson(cryptoService.encrypt(objectMapper.writeValueAsString(merged)));
            existing.setActive(1);
            existing.setStatus("NORMAL");
            storageConfigMapper.updateById(existing);
        } catch (Exception e) {
            throw new BizException("存储配置保存失败：" + e.getMessage());
        }
    }

    public FileRecord upload(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        var user = AuthContext.require();
        StorageConfig config = current(user.getTenantId());
        Map<String, String> cfg = parse(config.getConfigJson());
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = "";
        String lowerName = original.toLowerCase(Locale.ROOT);
        int dot = lowerName.lastIndexOf('.');
        if (dot >= 0 && lowerName.substring(dot).matches("\\.[a-z0-9]{1,10}")) {
            ext = lowerName.substring(dot);
        }
        String safeCategory = category == null ? "common" : category.replaceAll("[^A-Za-z0-9_-]", "");
        if (safeCategory.isBlank()) safeCategory = "common";
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = user.getTenantCode() + "/" + safeCategory + "/" + datePath + "/"
                + UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            byte[] bytes = file.getBytes();
            Driver driver = driver(config.getDriver());
            String url = driver.put(bytes, key, file.getContentType() == null ? "application/octet-stream" : file.getContentType(), cfg);
            FileRecord record = new FileRecord();
            record.setTenantId(user.getTenantId());
            record.setOriginalName(original);
            record.setStoredName(key);
            record.setUrl(url);
            record.setStoragePath(key);
            record.setSize(file.getSize());
            record.setContentType(file.getContentType());
            record.setCategory(safeCategory);
            record.setDriver(config.getDriver());
            fileRecordMapper.insert(record);
            return record;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("文件上传失败：" + e.getMessage());
        }
    }

    public FileRecord currentUpload(Long fileId) {
        FileRecord record = fileRecordMapper.selectById(fileId);
        if (record == null) throw new BizException(404, "文件不存在");
        return record;
    }

    public void deleteFile(Long fileId) {
        FileRecord record = fileRecordMapper.selectById(fileId);
        if (record == null) {
            throw new BizException(404, "文件不存在");
        }
        StorageConfig config = current(record.getTenantId());
        try {
            driver(config.getDriver()).delete(record.getStoragePath(), parse(config.getConfigJson()));
        } catch (Exception ignored) {
        }
        fileRecordMapper.deleteById(fileId);
    }

    public String signedUrl(FileRecord record) {
        return record.getUrl();
    }

    private Map<String, String> parse(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return new LinkedHashMap<>();
        try {
            String json = cryptoService.decrypt(encrypted);
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {
            });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private Map<String, Object> mask(Map<String, String> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        config.forEach((k, v) -> {
            if (SECRET_KEYS.contains(k) && v != null && !v.isBlank()) {
                result.put(k, "********");
            } else {
                result.put(k, v);
            }
        });
        return result;
    }

    private Map<String, String> mergeSecrets(String driver, Map<String, String> incoming, Map<String, String> old) {
        Map<String, String> merged = new LinkedHashMap<>(incoming);
        for (String key : SECRET_KEYS) {
            String value = merged.get(key);
            if ((value == null || value.isBlank() || value.contains("********")) && old.get(key) != null) {
                merged.put(key, old.get(key));
            }
        }
        return merged;
    }

    private Driver driver(String name) {
        return switch (name == null ? "LOCAL" : name.toUpperCase()) {
            case "S3" -> new S3Driver();
            case "OSS" -> new OssDriver();
            case "COS" -> new CosDriver();
            case "MINIO" -> new S3Driver();
            default -> new LocalDriver(defaultUploadDir);
        };
    }

    interface Driver {
        void test(Map<String, String> cfg) throws Exception;

        String put(byte[] bytes, String key, String contentType, Map<String, String> cfg) throws Exception;

        void delete(String key, Map<String, String> cfg) throws Exception;
    }

    static class LocalDriver implements Driver {
        private final String defaultBase;

        LocalDriver(String defaultBase) {
            this.defaultBase = defaultBase;
        }

        @Override
        public void test(Map<String, String> cfg) throws Exception {
            Path base = Paths.get(cfg.getOrDefault("basePath", defaultBase)).toAbsolutePath();
            Files.createDirectories(base);
        }

        @Override
        public String put(byte[] bytes, String key, String contentType, Map<String, String> cfg) throws Exception {
            Path base = Paths.get(cfg.getOrDefault("basePath", defaultBase)).toAbsolutePath();
            Path target = base.resolve(key);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            return "/uploads/" + key.replace('\\', '/');
        }

        @Override
        public void delete(String key, Map<String, String> cfg) throws Exception {
            Path target = Paths.get(cfg.getOrDefault("basePath", defaultBase)).toAbsolutePath().resolve(key);
            Files.deleteIfExists(target);
        }
    }

    static class S3Driver implements Driver {
        private MinioClient client(Map<String, String> cfg) {
            String endpoint = cfg.get("endpoint");
            if (endpoint == null || endpoint.isBlank()) throw new BizException("请填写 endpoint");
            return MinioClient.builder().endpoint(endpoint)
                    .credentials(cfg.get("accessKey"), cfg.get("secretKey")).build();
        }

        @Override
        public void test(Map<String, String> cfg) throws Exception {
            boolean exists = client(cfg).bucketExists(BucketExistsArgs.builder().bucket(bucket(cfg)).build());
            if (!exists) throw new BizException("Bucket 不存在或无权限访问");
        }

        @Override
        public String put(byte[] bytes, String key, String contentType, Map<String, String> cfg) throws Exception {
            client(cfg).putObject(PutObjectArgs.builder().bucket(bucket(cfg)).object(key)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(contentType).build());
            return publicUrl(cfg, key);
        }

        @Override
        public void delete(String key, Map<String, String> cfg) throws Exception {
            client(cfg).removeObject(RemoveObjectArgs.builder().bucket(bucket(cfg)).object(key).build());
        }

        private String bucket(Map<String, String> cfg) {
            String bucket = cfg.get("bucket");
            if (bucket == null || bucket.isBlank()) throw new BizException("请填写 bucket");
            return bucket;
        }

        private String publicUrl(Map<String, String> cfg, String key) {
            String base = cfg.get("publicBase");
            if (base != null && !base.isBlank()) return base.replaceAll("/$", "") + "/" + key;
            return cfg.getOrDefault("endpoint", "").replaceAll("/$", "") + "/" + bucket(cfg) + "/" + key;
        }
    }

    static class OssDriver implements Driver {
        private OSS client(Map<String, String> cfg) {
            String endpoint = cfg.get("endpoint");
            String ak = cfg.getOrDefault("accessKeyId", cfg.get("accessKey"));
            String sk = cfg.getOrDefault("accessKeySecret", cfg.get("secretKey"));
            if (endpoint == null || endpoint.isBlank() || ak == null || sk == null) {
                throw new BizException("OSS endpoint/AK/SK 不能为空");
            }
            return new OSSClientBuilder().build(endpoint, ak, sk);
        }

        @Override
        public void test(Map<String, String> cfg) {
            OSS oss = client(cfg);
            try {
                if (!oss.doesBucketExist(cfg.get("bucket"))) throw new BizException("OSS Bucket 不存在");
            } finally {
                oss.shutdown();
            }
        }

        @Override
        public String put(byte[] bytes, String key, String contentType, Map<String, String> cfg) {
            OSS oss = client(cfg);
            try {
                oss.putObject(cfg.get("bucket"), key, new ByteArrayInputStream(bytes));
            } finally {
                oss.shutdown();
            }
            return url(cfg, key);
        }

        @Override
        public void delete(String key, Map<String, String> cfg) {
            OSS oss = client(cfg);
            try {
                oss.deleteObject(cfg.get("bucket"), key);
            } finally {
                oss.shutdown();
            }
        }

        private String url(Map<String, String> cfg, String key) {
            String base = cfg.get("publicBase");
            if (base != null && !base.isBlank()) return base.replaceAll("/$", "") + "/" + key;
            String endpoint = cfg.get("endpoint").replace("https://", "").replace("http://", "").replaceAll("/$", "");
            return "https://" + cfg.get("bucket") + "." + endpoint + "/" + key;
        }
    }

    static class CosDriver implements Driver {
        private COSClient client(Map<String, String> cfg) {
            COSCredentials credentials = new BasicCOSCredentials(cfg.get("secretId"), cfg.get("secretKey"));
            String region = cfg.getOrDefault("region", "ap-guangzhou");
            return new COSClient(credentials, new ClientConfig(new Region(region)));
        }

        @Override
        public void test(Map<String, String> cfg) {
            COSClient cos = client(cfg);
            try {
                if (!cos.doesBucketExist(cfg.get("bucket"))) throw new BizException("COS Bucket 不存在");
            } finally {
                cos.shutdown();
            }
        }

        @Override
        public String put(byte[] bytes, String key, String contentType, Map<String, String> cfg) {
            COSClient cos = client(cfg);
            try {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(contentType);
                metadata.setContentLength(bytes.length);
                cos.putObject(cfg.get("bucket"), key, new ByteArrayInputStream(bytes), metadata);
            } finally {
                cos.shutdown();
            }
            return url(cfg, key);
        }

        @Override
        public void delete(String key, Map<String, String> cfg) {
            COSClient cos = client(cfg);
            try {
                cos.deleteObject(cfg.get("bucket"), key);
            } finally {
                cos.shutdown();
            }
        }

        private String url(Map<String, String> cfg, String key) {
            String base = cfg.get("publicBase");
            if (base != null && !base.isBlank()) return base.replaceAll("/$", "") + "/" + key;
            return "https://" + cfg.get("bucket") + ".cos." + cfg.getOrDefault("region", "ap-guangzhou")
                    + ".myqcloud.com/" + key;
        }
    }
}
