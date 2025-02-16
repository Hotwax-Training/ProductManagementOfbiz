<form method="post" action="<@ofbizUrl>findProductEvent</@ofbizUrl>">
    <#if parameters.successMessage??>
        <div style="color: green;">${parameters.successMessage}</div>
    </#if>
    <#if parameters._ERROR_MESSAGE_??>
        <div style="color: red;">${parameters._ERROR_MESSAGE_}</div>
    </#if>
    <label>Product ID: <input type="text" name="productId"/></label><br/><br/>
    <label>Product Name: <input type="text" name="productName"/></label><br/><br/>
    <label>Category: <input type="text" name="productCategoryId"/></label><br/><br/>
    <label>Min Price: <input type="number" step="0.01" name="minPrice"/></label><br/><br/>
    <label>Max Price: <input type="number" step="0.01" name="maxPrice"/></label><br/><br/>
    <label>Feature: <input type="text" name="productFeature"/></label><br/><br/>
    <input type="submit" value="Search"/><br/><br/>
</form>

<#if parameters.products?? && parameters.products?size gt 0>
    <h3>Search Results:</h3><br/>
    <table border="1">
        <thead>
            <tr>
                <th>Product ID</th>
                <th>Product Name</th>
                <th>Category</th>
                <th>Price (USD)</th>
                <th>Features</th>
            </tr>
        </thead>
        <tbody>
            <#list parameters.products as product>
                <tr>
                    <td>${product.productId}</td>
                    <td>${product.productName}</td>
                    <td>${product.productCategoryId!''}</td>
                    <td>${product.listPrice?string.currency}</td>
                    <td>${product.description!''}</td>
                </tr>
            </#list>
        </tbody>
    </table>
<#else>
    <p>No products found.</p>
</#if>
