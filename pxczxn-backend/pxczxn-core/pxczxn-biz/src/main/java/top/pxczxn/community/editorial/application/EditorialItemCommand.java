package top.pxczxn.community.editorial.application;
public record EditorialItemCommand(String targetType,Long targetId,Integer displayOrder) {}
