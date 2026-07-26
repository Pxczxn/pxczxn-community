package top.pxczxn.community.web.file;

import top.pxczxn.platform.oss.FileStorage;
import top.pxczxn.platform.system.storage.FileStorageFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.pxczxn.community.file.application.CommunityObjectStorage;

import java.io.ByteArrayInputStream;

@Component
@RequiredArgsConstructor
public class MarsCommunityObjectStorage implements CommunityObjectStorage {

    private final FileStorageFactory storageFactory;

    @Override
    public String provider() {
        return storage().getStorageType();
    }

    @Override
    public void upload(byte[] content, String objectKey) {
        int separator = objectKey.lastIndexOf('/');
        String path = separator < 0 ? "community" : objectKey.substring(0, separator);
        String fileName = separator < 0 ? objectKey : objectKey.substring(separator + 1);
        storage().upload(new ByteArrayInputStream(content), path, fileName);
    }

    @Override
    public byte[] read(String objectKey) {
        return storage().getFile(objectKey);
    }

    @Override
    public void delete(String objectKey) {
        storage().delete(objectKey);
    }

    private FileStorage storage() {
        return storageFactory.getStorage();
    }
}
