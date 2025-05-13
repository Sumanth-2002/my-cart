<?xml version="1.0" encoding="utf-8"?>
<Reviews xsi:noNamespaceSchemaLocation="schema.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <#if items?? && items?size gt 0>
        <#list items as item>
            <#if item?? && item.itemId??>
                <item id="${item.itemId}">
                    <#if item.review?? && item.review?size gt 0>
                        <#list item.review as review>
                            <Review>
                                <reviewrating>${review.rating}</reviewrating>
                                <reviewcomment>${review.comment!}</reviewcomment>
                            </Review>
                        </#list>
                    </#if>
                </item>
            </#if>
        </#list>
    <#else>
        <!-- Debug: No items found -->
        <comment>No items found in the model</comment>
    </#if>
</Reviews>