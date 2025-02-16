<form method="post" action="<@ofbizUrl>updateProductEvent</@ofbizUrl>">
    <#if parameters.successMessage??>
        <div style="color: green; font-weight: bold; margin-bottom: 10px;">
            ${parameters.successMessage}
        </div>
    </#if>
    <#if parameters._ERROR_MESSAGE_??>
        <div style="color: red; font-weight: bold; margin-bottom: 10px;">
            ${parameters._ERROR_MESSAGE_}
        </div>
    </#if>
    <label for="productId">Product ID:</label>
    <input type="text" id="productId" name="productId" required/><br/><br/>

    <label for="newPrice">New Price:</label>
    <input type="number" id="newPrice" name="newPrice" step="0.01"/><br/><br/>

    <label for="productFeatureDescription">Product Feature Description:</label>
    <input type="text" id="productFeatureDescription" name="productFeatureDescription"/><br/><br/>

    <input type="submit" value="Update Product"/>
</form>

