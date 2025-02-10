package org.apache.ofbiz.product;

import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.*;
import org.apache.ofbiz.entity.condition.*;
import org.apache.ofbiz.service.*;

import java.math.BigDecimal;
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

        // Apply search filters based on user input
        if (UtilValidate.isNotEmpty(productId)) {
            conditions.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
        }

        if (UtilValidate.isNotEmpty(productName)) {
            conditions.add(EntityCondition.makeCondition("productName", EntityOperator.LIKE, "%" + productName.toLowerCase() + "%"));
        }

        if (minPrice != null || maxPrice != null) {
            if (minPrice != null && maxPrice != null) {
                conditions.add(EntityCondition.makeCondition("listPrice", EntityOperator.BETWEEN, Arrays.asList(minPrice, maxPrice)));
            } else if (minPrice != null) {
                conditions.add(EntityCondition.makeCondition("listPrice", EntityOperator.GREATER_THAN_EQUAL_TO, minPrice));
            } else {
                conditions.add(EntityCondition.makeCondition("listPrice", EntityOperator.LESS_THAN_EQUAL_TO, maxPrice));
            }
        }

        if (UtilValidate.isNotEmpty(productFeature)) {
            conditions.add(EntityCondition.makeCondition("productFeatureDescription", EntityOperator.LIKE, "%" + productFeature.toLowerCase() + "%"));
        }

        EntityCondition finalCondition = EntityCondition.makeCondition(conditions, EntityOperator.AND);
        List<Map<String, Object>> productList = new ArrayList<>();

        try {
            List<GenericValue> products = delegator.findList("FindProductView", finalCondition, null, null, null, false);

            for (GenericValue product : products) {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("productId", product.getString("productId"));
                productMap.put("productName", product.getString("productName"));
                productMap.put("listPrice", product.getBigDecimal("listPrice"));
                productMap.put("productFeatureDescription", product.getString("productFeatureDescription"));
                productList.add(productMap);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Error retrieving products: " + e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("products", productList);
        return result;
    }
}

