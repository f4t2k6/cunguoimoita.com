package org.example.cunguoimoita.admin.model;

/** Class Category trong class diagram (danh mục cha – con). */
public class Category {

    private String id;
    private String name;
    private String description;
    private String parentCategoryId;
    private String parentName;      // chỉ để hiển thị
    private int productCount;       // chỉ để hiển thị

    public boolean isTopLevel() { return parentCategoryId == null; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getParentCategoryId() { return parentCategoryId; }
    public void setParentCategoryId(String parentCategoryId) { this.parentCategoryId = parentCategoryId; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public int getProductCount() { return productCount; }
    public void setProductCount(int productCount) { this.productCount = productCount; }
}
