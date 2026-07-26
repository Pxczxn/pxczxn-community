package top.pxczxn.community.file.application;

public interface CommunityObjectStorage {

    String provider();

    void upload(byte[] content, String objectKey);

    byte[] read(String objectKey);

    void delete(String objectKey);
}
