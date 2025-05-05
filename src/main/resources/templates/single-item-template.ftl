## Debug: Inspect the model
<p>Debug: itemId exists: $!itemId</p>
<p>Debug: reviews exists: $!reviews</p>
<p>Debug: reviews size: #if($reviews)$reviews.size()#else0#end</p>

<p>Item ID: $!itemId</p>

#if($reviews && $reviews.size() > 0)
    <ul>
        #foreach($rev in $reviews)
            <li>Rating: $rev.rating, Comment: $!rev.comment</li>
        #end
    </ul>
#else
    <p>No reviews available for this item.</p>
#end