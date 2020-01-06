package com.ass;

import com.dibs.service.v1.data.anchor.Anchor;
import com.dibs.service.v1.data.anchor.AnchorSourceType;
import com.dibs.service.v1.identity.data.seller.SellerKey;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Assert extends BaseAssert
{
    public static void isFullySet(SellerKey sellerKey)
    {
        Assert.isNotNull(sellerKey, "seller key is null");
        Assert.isNotNull(sellerKey.getId(), "seller id is null");
        Assert.isNotNull(sellerKey.getVertical(), "seller vertical is null");
    }

    public static void isFullySet(Anchor anchor, AnchorSourceType... validSources)
    {

        Set<AnchorSourceType> validSourceSet = new HashSet<AnchorSourceType>(validSources == null || validSources.length == 0
                ? Arrays.asList(AnchorSourceType.values()) :
                Arrays.asList(validSources));

        Assert.isNotNull(anchor, "anchor is null");
        Assert.isNotNull(anchor.getSourceId(), "anchor is null");
        Assert.isTrue(validSourceSet.contains(anchor.getSourceType()), " unsupported anchor source - [" + anchor.getSourceType() + "]");
    }
}
