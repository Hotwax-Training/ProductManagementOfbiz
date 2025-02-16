<form method="post" action="<@ofbizUrl>createProductEvent</@ofbizUrl>">
<#if parameters.successMessage??>
    <div style="color: green;">${parameters.successMessage}</div>
</#if>
 <#if parameters._ERROR_MESSAGE_??>
    <div style="color: red;">${parameters._ERROR_MESSAGE_}</div>
 </#if>
    <label>Product Name: <input type="text" name="productName" required/></label><br/><br/>
    <label>Category ID: <input type="text" name="categoryId" required/></label><br/><br/>
    <label>Price: <input type="number" step="0.01" name="price" required/></label><br/><br/>
    <label>isVirtual: <input type="text" name="isVirtual" required/></label><br/><br/>
    <input type="submit" value="Create Product"/>
</form>

