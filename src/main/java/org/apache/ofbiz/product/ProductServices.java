package org.apache.ofbiz.product;

import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.*;
import org.apache.ofbiz.entity.condition.*;
import org.apache.ofbiz.service.*;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

public class ProductServices {

    public static Map<String, Object> findProduct(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        List<EntityCondition> conditions = new ArrayList<>();

        String productId = (String) context.get("productId");
        String productName = (String) context.get("productName");
        BigDecimal minPrice = (BigDecimal) context.get("minPrice");
        BigDecimal maxPrice = (BigDecimal) context.get("maxPrice");
        String productFeature = (String) context.get("productFeature");

        if (UtilValidate.isNotEmpty(productId)) {
            conditions.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
        }

        if (UtilValidate.isNotEmpty(productName)) {
            conditions.add(EntityCondition.makeCondition("productName", EntityOperator.LIKE, "%" + productName + "%"));
        }

        if (minPrice != null && maxPrice != null) {
            conditions.add(EntityCondition.makeCondition(
                    Arrays.asList(
                            EntityCondition.makeCondition("listPrice", EntityOperator.GREATER_THAN_EQUAL_TO, minPrice),
                            EntityCondition.makeCondition("listPrice", EntityOperator.LESS_THAN_EQUAL_TO, maxPrice)
                    ),
                    EntityOperator.AND
            ));
        } else if (minPrice != null) {
            conditions.add(EntityCondition.makeCondition("listPrice", EntityOperator.GREATER_THAN_EQUAL_TO, minPrice));
        } else if (maxPrice != null) {
            conditions.add(EntityCondition.makeCondition("listPrice", EntityOperator.LESS_THAN_EQUAL_TO, maxPrice));
        }

        if (UtilValidate.isNotEmpty(productFeature)) {
            conditions.add(EntityCondition.makeCondition("productFeatureDescription", EntityOperator.LIKE, "%" + productFeature + "%"));
        }

        EntityCondition finalCondition = conditions.isEmpty() ? null : EntityCondition.makeCondition(conditions, EntityOperator.AND);

        List<GenericValue> products;
        try {
            products = delegator.findList("FindProductView", finalCondition, null, null, null, false);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Error retrieving products: " + e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("products", products);
        return result;
    }

    public static Map<String, Object> createProduct(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = new HashMap<>();

        String productName = (String) context.get("productName");
        String categoryId = (String) context.get("categoryId");
        String isVirtual = (String) context.get("isVirtual");
        Object priceObj = context.get("price");
        String currencyUomId = "USD"; // Default currency

        BigDecimal price = null;
        if (priceObj instanceof BigDecimal) {
            price = (BigDecimal) priceObj;
        } else if (priceObj instanceof String) {
            try {
                price = new BigDecimal((String) priceObj);
            } catch (NumberFormatException e) {
                return ServiceUtil.returnError("Invalid price value: " + priceObj);
            }
        }

        if (UtilValidate.isEmpty(productName) || UtilValidate.isEmpty(categoryId) || price == null) {
            return ServiceUtil.returnError("Missing required fields: productName, categoryId, or price.");
        }

        try {
            GenericValue category = delegator.findOne("ProductCategory", UtilMisc.toMap("productCategoryId", categoryId), false);
            if (category == null) {
                return ServiceUtil.returnError("Category ID " + categoryId + " does not exist.");
            }

            EntityCondition productCondition = EntityCondition.makeCondition("productName", EntityOperator.EQUALS, productName);
            List<GenericValue> existingProducts = delegator.findList("Product", productCondition, null, null, null, false);
            if (!existingProducts.isEmpty()) {
                return ServiceUtil.returnError("Product with name '" + productName + "' already exists.");
            }

            String productId = delegator.getNextSeqId("Product");

            GenericValue newProduct = delegator.makeValue("Product");
            newProduct.set("productId", productId);
            newProduct.set("productName", productName);
            newProduct.set("productTypeId", "FINISHED_GOOD"); // Default Type
            newProduct.set("isVirtual", isVirtual);
            newProduct.set("isVariant", "N");
            newProduct.set("createdStamp", UtilDateTime.nowTimestamp());
            newProduct.set("lastUpdatedStamp", UtilDateTime.nowTimestamp());
            delegator.create(newProduct);

            GenericValue categoryMember = delegator.makeValue("ProductCategoryMember");
            categoryMember.set("productCategoryId", categoryId);
            categoryMember.set("productId", productId);
            categoryMember.set("fromDate", UtilDateTime.nowTimestamp());
            delegator.create(categoryMember);

            GenericValue productPrice = delegator.makeValue("ProductPrice");
            productPrice.set("productId", productId);
            productPrice.set("productPriceTypeId", "DEFAULT_PRICE");
            productPrice.set("productPricePurposeId", "PURCHASE");
            productPrice.set("currencyUomId", currencyUomId);
            productPrice.set("productStoreGroupId", "_NA_");
            productPrice.set("fromDate", UtilDateTime.nowTimestamp());
            productPrice.set("price", price);
            delegator.create(productPrice);

            System.out.println("99999999999999999999999999999999" + productId);
            result.put("productId", productId);

            return result;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Error creating product: " + e.getMessage());
        }
    }

    public static Map<String, Object> updateProduct(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String productId = (String) context.get("productId");
        Object newPriceObj = context.get("newPrice");
        String productFeatureDescription = (String) context.get("productFeatureDescription");
        Timestamp now = UtilDateTime.nowTimestamp();

        if (UtilValidate.isEmpty(productId)) {
            return ServiceUtil.returnError("Missing required field: productId.");
        }

        try {
            GenericValue product = EntityQuery.use(delegator)
                    .from("Product")
                    .where("productId", productId)
                    .queryOne();

            if (product == null) {
                return ServiceUtil.returnError("Product ID " + productId + " does not exist.");
            }

            if (newPriceObj != null) {
                BigDecimal newPrice = null;
                if (newPriceObj instanceof BigDecimal) {
                    newPrice = (BigDecimal) newPriceObj;
                } else if (newPriceObj instanceof String) {
                    try {
                        newPrice = new BigDecimal((String) newPriceObj);
                    } catch (NumberFormatException e) {
                        return ServiceUtil.returnError("Invalid price value: " + newPriceObj);
                    }
                }

                if (newPrice != null) {
                    List<GenericValue> existingPrices = EntityQuery.use(delegator)
                            .from("ProductPrice")
                            .where("productId", productId,
                                    "productPriceTypeId", "DEFAULT_PRICE",
                                    "productPricePurposeId", "PURCHASE",
                                    "currencyUomId", "USD",
                                    "productStoreGroupId", "_NA_")
                            .orderBy("-fromDate")
                            .queryList();

                    if (!existingPrices.isEmpty()) {
                        GenericValue latestPrice = existingPrices.get(0);
                        latestPrice.set("thruDate", now);
                        delegator.store(latestPrice);
                    }

                    GenericValue newProductPrice = delegator.makeValue("ProductPrice");
                    newProductPrice.set("productId", productId);
                    newProductPrice.set("productPriceTypeId", "DEFAULT_PRICE");
                    newProductPrice.set("productPricePurposeId", "PURCHASE");
                    newProductPrice.set("currencyUomId", "USD");
                    newProductPrice.set("productStoreGroupId", "_NA_");
                    newProductPrice.set("fromDate", now);
                    newProductPrice.set("price", newPrice);
                    newProductPrice.set("createdStamp", now);
                    newProductPrice.set("lastUpdatedStamp", now);
                    delegator.create(newProductPrice);
                }
            }
            if (UtilValidate.isNotEmpty(productFeatureDescription)) {
                List<GenericValue> productFeatures = EntityQuery.use(delegator)
                        .from("ProductFeatureAppl")
                        .where("productId", productId)
                        .queryList();

                if (!productFeatures.isEmpty()) {
                    for (GenericValue productFeatureAppl : productFeatures) {
                        String productFeatureId = productFeatureAppl.getString("productFeatureId");

                        GenericValue feature = EntityQuery.use(delegator)
                                .from("ProductFeature")
                                .where("productFeatureId", productFeatureId)
                                .queryOne();

                        if (feature != null) {
                            feature.set("description", productFeatureDescription);
                            feature.set("lastUpdatedStamp", now);
                            delegator.store(feature);
                        }
                    }
                } else {
                    String newFeatureId = delegator.getNextSeqId("ProductFeature");

                    GenericValue newFeature = delegator.makeValue("ProductFeature");
                    newFeature.set("productFeatureId", newFeatureId);
                    newFeature.set("productFeatureTypeId", "GENERAL_FEATURE");
                    newFeature.set("description", productFeatureDescription);
                    newFeature.set("createdStamp", now);
                    newFeature.set("lastUpdatedStamp", now);
                    delegator.create(newFeature);

                    GenericValue newFeatureAppl = delegator.makeValue("ProductFeatureAppl");
                    newFeatureAppl.set("productId", productId);
                    newFeatureAppl.set("productFeatureId", newFeatureId);
                    newFeatureAppl.set("fromDate", now);
                    newFeatureAppl.set("createdStamp", now);
                    delegator.create(newFeatureAppl);
                }
            }
            Map<String, Object> updatedProductMap = new HashMap<>();
            updatedProductMap.put("productId", productId);
            updatedProductMap.put("newPrice", newPriceObj);
            updatedProductMap.put("productFeatureDescription", productFeatureDescription);

            result.put("updatedProduct", updatedProductMap);
            result.put("message", "Product updated successfully.");

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Error updating product: " + e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> assocProductToVirtual(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String productId = (String) context.get("productId");
        String virtualProductId = (String) context.get("virtualProductId");


        if (UtilValidate.isEmpty(productId) || UtilValidate.isEmpty(virtualProductId)) {
            return ServiceUtil.returnError("Missing required fields: productId or virtualProductId.");
        }

        try {
            GenericValue product = EntityQuery.use(delegator)
                    .from("Product")
                    .where("productId", productId)
                    .queryOne();

            GenericValue virtualProduct = EntityQuery.use(delegator)
                    .from("Product")
                    .where("productId", virtualProductId)
                    .queryOne();

            if (product == null) {
                return ServiceUtil.returnError("Product ID " + productId + " does not exist.");
            }
            if (virtualProduct == null) {
                return ServiceUtil.returnError("Virtual Product ID " + virtualProductId + " does not exist.");
            }
            if (!"Y".equals(virtualProduct.getString("isVirtual"))) {
                return ServiceUtil.returnError("Product ID " + virtualProductId + " is not marked as a virtual product.");
            }

            GenericValue existingAssoc = EntityQuery.use(delegator)
                    .from("ProductAssoc")
                    .where("productId", productId, "productIdTo", virtualProductId, "productAssocTypeId", "PRODUCT_VARIANT")
                    .queryOne();

            if (existingAssoc != null) {
                return ServiceUtil.returnError("This product is already associated with the virtual product.");
            }

            GenericValue newAssoc = delegator.makeValue("ProductAssoc");
            newAssoc.set("productId", productId);
            newAssoc.set("productIdTo", virtualProductId);
            newAssoc.set("productAssocTypeId", "PRODUCT_VARIANT");
            newAssoc.set("fromDate", UtilDateTime.nowTimestamp());
            delegator.create(newAssoc);

            result.put("message", "Product successfully associated with virtual product.");

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Error associating product: " + e.getMessage());
        }

        return result;
    }

    public static Map<String, Object> updateProductVariant(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String productId = (String) context.get("productId");
        String virtualProductId = (String) context.get("virtualProductId");
        String newVirtualProductId = (String) context.get("newVirtualProductId");
        Timestamp newFromDate = (Timestamp) context.get("fromDate");
        Timestamp now = UtilDateTime.nowTimestamp();

        if (UtilValidate.isEmpty(productId) || UtilValidate.isEmpty(virtualProductId) || UtilValidate.isEmpty(newVirtualProductId)) {
            return ServiceUtil.returnError("Missing required fields: productId, virtualProductId, or newVirtualProductId.");
        }

        try {
            GenericValue existingVirtualProduct = EntityQuery.use(delegator)
                    .from("Product")
                    .where("productId", virtualProductId, "isVirtual", "Y")
                    .queryOne();

            if (existingVirtualProduct == null) {
                return ServiceUtil.returnError("Virtual Product ID " + virtualProductId + " does not exist or is not a virtual product.");
            }
            GenericValue newVirtualProduct = EntityQuery.use(delegator)
                    .from("Product")
                    .where("productId", newVirtualProductId, "isVirtual", "Y")
                    .queryOne();

            if (newVirtualProduct == null) {
                return ServiceUtil.returnError("New Virtual Product ID " + newVirtualProductId + " does not exist or is not a virtual product.");
            }
            GenericValue existingAssoc = EntityQuery.use(delegator)
                    .from("ProductAssoc")
                    .where("productId", productId, "productIdTo", virtualProductId, "productAssocTypeId", "PRODUCT_VARIANT")
                    .queryOne();

            if (existingAssoc == null) {
                return ServiceUtil.returnError("No variant association exists between Product ID " + productId + " and Virtual Product ID " + virtualProductId + ".");
            }
            existingAssoc.set("thruDate", now);
            delegator.store(existingAssoc);
            GenericValue newAssoc = delegator.makeValue("ProductAssoc");
            newAssoc.set("productId", productId);
            newAssoc.set("productIdTo", newVirtualProductId);
            newAssoc.set("productAssocTypeId", "PRODUCT_VARIANT");
            newAssoc.set("fromDate", (newFromDate != null) ? newFromDate : now);
            newAssoc.set("createdStamp", now);
            delegator.create(newAssoc);

            result.put("message", "Product variant relationship updated successfully from Virtual Product ID " + virtualProductId + " to " + newVirtualProductId);

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Error updating product variant relationship: " + e.getMessage());
        }

        return result;
    }
}

