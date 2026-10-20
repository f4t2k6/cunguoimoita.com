package org.example.cunguoimoita.product.model;

import java.math.BigDecimal;

/** Trường nào null là không lọc theo trường đó. */
public class ProductFilter {

    public static final int PAGE_SIZE = 12;

    private String keyword;
    private String categoryId;      // lọc cả danh mục con của danh mục này
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private ProductCondition condition;
    private ListingType listingType;
    private Sort sort = Sort.NEWEST;
    private int page = 1;

    /** Cách sắp xếp kết quả. */
    public enum Sort { NEWEST, PRICE_ASC, PRICE_DESC }

    public int getOffset() {
        return (page - 1) * PAGE_SIZE;
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    public ProductCondition getCondition() { return condition; }
    public void setCondition(ProductCondition condition) { this.condition = condition; }
    public ListingType getListingType() { return listingType; }
    public void setListingType(ListingType listingType) { this.listingType = listingType; }
    public Sort getSort() { return sort; }
    public void setSort(Sort sort) { this.sort = sort == null ? Sort.NEWEST : sort; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(1, page); }
}
