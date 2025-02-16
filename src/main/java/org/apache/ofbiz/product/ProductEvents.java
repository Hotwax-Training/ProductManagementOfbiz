package org.apache.ofbiz.product;


import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import java.util.Map;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.service.ServiceUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class ProductEvents {

    public static String createProductEvent(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map<String, Object> context = UtilHttp.getParameterMap(request);

        try {
            Map<String, Object> result = dispatcher.runSync("createProduct", context);
            if (ServiceUtil.isError(result)) {
                request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(result));
                return "error";
            }
            request.setAttribute("successMessage", "Product created successfully!");
            return "success";
        } catch (Exception e) {
            request.setAttribute("_ERROR_MESSAGE_", "Error creating product: " + e.getMessage());
            return "error";
        }
    }

    public static String updateProductEvent(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map<String, Object> context = UtilHttp.getParameterMap(request);


        try {
            Map<String, Object> result = dispatcher.runSync("updateProduct", context);

            if (ServiceUtil.isError(result)) {
                request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(result));
                return "error";
            }

            request.setAttribute("successMessage", "Product updated successfully!");
            return "success";
        } catch (Exception e) {
            request.setAttribute("_ERROR_MESSAGE_", "Error updating product: " + e.getMessage());
            return "error";
        }
    }

    public static String findProductEvent(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map<String, Object> context = UtilHttp.getParameterMap(request);

        try {
            Map<String, Object> result = dispatcher.runSync("findProduct", context);
            if (ServiceUtil.isError(result)) {
                request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(result));
                return "error";
            }

            request.setAttribute("products", result.get("products"));
            return "success";
        } catch (Exception e) {
            request.setAttribute("_ERROR_MESSAGE_", "Error finding products: " + e.getMessage());
            return "error";
        }
    }
}


