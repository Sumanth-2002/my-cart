<?xml version="1.0" encoding="UTF-8"?>
<Reviews xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="schema.xsd">
<#list items as item>
  <item id="${item.id}">
  <#list item.review as review> <!-- NOT 'reviews', it's 'review' from Mongo -->
    <Review>
      <rating>${review.rating}</rating>
      <comment>${review.comment}</comment>
    </Review>
  </#list>
  </item>
</#list>
</Reviews>
