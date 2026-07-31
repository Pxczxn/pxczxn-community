package top.pxczxn.platform.oss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinioFileStorageTest {

    @Test
    void exposesApplicationProxyUrlInsteadOfBucketUrl() {
        MinioFileStorage storage = new MinioFileStorage();

        assertEquals("/api/files/community/42/avatar.png", storage.getUrl("community/42/avatar.png"));
        assertEquals("/api/files/community/42/avatar.png", storage.getUrl("/community/42/avatar.png"));
    }
}
